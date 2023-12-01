# Spot matching-engine

### Content
* [Description](#description)
* [Matching engine architecture](#matching-engine-architecture)
* [Test coverage](#test-coverage)
* [Exchange architecture](#exchange-architecture)
  * [Aeron - internal communication bus](#aeron---internal-communication-bus)
  * [Derivative exchange](#derivative-exchange)
  * [FIX Gateway](#fix-gateway)

### Description
This is the public part of my `derivative matching-engine` project, and can serve 2 purposes:
* educational - you can see what is matching engine and how it's supposed to work
* backbone to your own matching-engine - you can take this project and work on top of it to create fully-operational exchange system
Derivative matching engine consist of several parts:
* spot matching engine - this is the part that is shared here in public github repository
* derivative matching engine
* REST API for client communication who prefer REST
* WS read/write - for clients who prefer websocket connection
* FIX - for institutional clients who prefer FIX protocol communication with exchange
* aeron - message bus that connects matching-engine with 3 services REST/WS/FIX

### Matching engine architecture
Matching engine consist of following parts:
* inbound queue - message queue by which clients can send messages to matching engine
* outbound queue - message queue by which clients can receive responses from matching engine
* matching thread - the heart of matching engine, a constantly-running thread in `while(true)` loop that read & process messages from `inbound queue` and result of processing is written into `outbound queue`
* order book - core part of matching process, the actual place where order matching between BUY & SELL happens
* additional components - different components that participate in matching on pre- & post- matching phases like:
  * instrument management - before any matching can happen we should know what coins & pairs are available for us
  * user & balance management - again before any matching we should verify that users who submit orders do exist in our system and have enough corresponding balance to perform order execution
  * post-matching settlement - after we match the orders, we should settle user balances to ensure integrity of the system
Since order book is the core part - it's implementation is very important, cause if matching is slow, the whole system would be slow. We have 2 implementation of order book:
* [MapOrderBook](https://github.com/dgaydukov/exchange-core/blob/master/src/main/java/com/exchange/core/matching/orderbook/OrderBook.java) - naive implementation, here we are using default JDK implementations `java.utils.TreeMap` to store internal state of order book like bids/asks. Since these JDK implementations are not designed for low latency, they won't perform well under high load due to: gc problems or primitive unboxing/auto-boxing. Such implementation is good for educational purpose, but completely useless for real-world apps.
* [ArrayOrderBook](https://github.com/dgaydukov/exchange-core/blob/master/src/main/java/com/exchange/core/matching/orderbook/array/ArrayOrderBook.java) - advanced version of order book, compared to `MapOrderBook` order book, here we can achieve faster performance by directly manipulating the underlying array. In `TreeMap` there is also underlying array and each time we insert/remove, it's sorted internally/implicitly. But here we are adding/removing from order book explicitly

### Test coverage
Test coverage is the most important thing in any app, so here we covered our core part a lot with both unit testing & integration testing.
There are 2 types of tests in this project:
* unit tests - where we test only source code. All code except `MatchingEngine` covered by unit tests.
* integration test - where we actually run the app, send incoming messages & listen for outgoing. Good candidate here is `MatchingEngine` class. Cause to test it you have to run it, provide 2 queues and send messages into `inbound` queue and verify that you receive messages in the `outbound` queue.
* Since we have 2 order books - and both implement same interface and are expected to behave the same way, I've created [parametrized test](https://github.com/dgaydukov/exchange-core/blob/master/src/test/java/com/exchange/core/matching/orderbook/OrderBookTest.java#L26) where I run all the tests at the same time for 2 orderbooks, using Junit `@ParameterizedTest` annotation
Performance testing - this is integration test that measures system performance overall. It should be integration, cause you need to actually run your system end-to-end, and then put extreme load into it and measure performance. I've created [MatchingEnginePerformanceTest](https://github.com/dgaydukov/exchange-core/blob/master/src/test/java/performance/MatchingEnginePerformanceTest.java) that measures 2 things:
* TPS - how many messages/orders system can handle per second
* latency - what is average latency per single request. Here we measure end-to-end latency from the moment user send his order to our system and to the point when he received message back (in case of order - message would be execution report)

### Exchange architecture
Example of exchange components (based on https://www.youtube.com/watch?v=b1e4t2k2KJY):
* primary me with order book per instrument (add/cancel/replace) -  on average `cancel` requests take up to 40%
* message bus (in-memory but can be used aeron) - use multicast so once event happen (like execution) all component can receive it at the same time and return to clients
  * drop copy (of some specific clients)
  * MD (market data)
  * trade reporter (cause MD just show active order book, once order executed it just disappears from MD, so we need trade reporter)
* cancel manager (you can submit cancelRequest to cancel order at some point in future)
* auction manager (aggregate all prices & find the price that maximize the profit)
* secondary me (also called passive me) that listen output from primary me (this is important, it shouldn't listen incoming messages from message bus, only result of already processed messages by first me) and builds exactly the same state as first ME (so in case first me fail, we can start second me from exactly same place where first fail) => you can use paxos to decide who is primary me and when you should switch to secondary me. But no exchange use it, most exchanges has simple fail over logic, if main me fail, promote passive me to primary.
* multi-threading - new thread per order book - good, but centralization has benefits
* state-machine recovery is a replay
* speed, latency, throughput, determinism - basic principle of exchange
Exchange should include 3 components:
  * matching engine (2 instances should be running, see [architecture](#matching-engine-architecture))
  * communication channel or bus - [aeron](#aeron---internal-communication-bus) should be used
  * external api - how clients can communicate with the system. Most top exchanges uses 3 level of communication
    * http-api
    * ws-api
    * fix-api
Real-world app - build on top of advanced order book replicated system:
  * use aeron cluster for multi-node zero-downtime
  * use aeron archive for persistence
  * https://github.com/real-logic/aeron/wiki/Cluster-Tutorial
  * with client as simple http order api, which receive messages from clients, and then act as client to cluster, send order to ME (cluster) and receive messages from it
  * checkout message lost in case of failed leader (https://github.com/real-logic/aeron/wiki/Cluster-Tutorial#55-failed-nodes-and-leader-election). This answer suggest that we should have app logic to check ack from cluster and in case of leader change, and message lost, client won't receive ack for these few messages, and have to re-send messages to cluster https://stackoverflow.com/questions/73370605/what-are-the-aeron-cluster-delivery-guarantees

#### Aeron - internal communication bus
* aeron + busy loop is faster than unix select (https://en.wikipedia.org/wiki/Select_(Unix))
* if message size greater than MTU (1500byte) we need FragmentAssembler
* aeron use `/dev/shm` => any file created there treated as simple file yet stored in-memory
* since udp doesn't guarantee order & delivery we aeron use position to ensure order & delivery
* conductor on receiver side will check all position and if missing will send back NAK (negative acknolegement) and ask sender to re-send again
* tcp philosophy - better late than never, low-latency philosophy - better never than late (so it's better not to send order at all, rather than send order with outdated price)
* publisher write into `log.buffer`

#### Derivative exchange
This exchange will include following items:
* dated futures
* perpetual futures aka perpetual swap
* options
* auctions

#### FIX Gateway
There are several ways to logon with fix:
* [deribit](https://docs.deribit.com/#logon-a) - here we just take hash256 from secret key
* [coinbase](https://docs.cloud.coinbase.com/exchange/docs/messages#logon-a) - here we take `HMAC` of several fields like SendingTime/MsgType/MsgSeqNum/SenderCompID/TargetCompID/Password. We have interesting concept of `apiKey = key+secret+passphrase`. And third param provided by end-user and stored hashed in server. So to access api/fix all 3 need to be used. This ensures if credentials got leaked from exchange, attacker still not be able to login/sendRequests because he doesn't know passphrase, which only user knows.
* [etorox](https://etorox.com/etorox-fix-api/#FIX-Session-Level-Messages) - here we are using `HMAC` to sign randomly generated payload
* [cme](https://www.cmegroup.com/confluence/display/EPICSANDBOX/Session+Layer+-+Logon#SessionLayerLogon-Step2-CreateSignatureusingSecretKeyandCanonicalFIXMessage) - here we use `HMAC` to sign whole body
As you see there is no standard across the ecosystem, different exchanges uses different approach. The most consistent in my opinion is cme case, where you have body and sign it.
Also best practice is to have key rotation logic, but it should be handled by different component, where basically you urge your clients to rotate keys every year, and if clients is not dot doing this, you disable such keys after expiration date.