# Spot matching-engine

### add content section here & binance app

### Test coverage
Test coverage is the most important thing in any app, so here we covered our core part a lot with both unit testing & integration testing.
There are 2 types of tests in this project:
* unit tests - where we test only source code. All code except `MatchingEngine` covered by unit tests.
* integration test - where we actually run the app, send incoming messages & listen for outgoing. Good candidate here is `MatchingEngine` class. Cause to test it you have to run it, provide 2 queues and send messages into `inbound` queue and verify that you receive messages in the `outbound` queue.



### 2 types of orderbook
* MapOrderBook - orderbook based on `java.utils.TreeMap` where all book manipulation (adding/removing order to orderbook) is done inside TreeMap and hidden from us. It's easier, but less efficent.
* ArrayOrderBook - advanced version of order book, compared to `MapOrderBook` order book, here we can achieve faster performance by directly manipulating the underlying array. In `TreeMap` there is also underlying array and each time we insert/remove, it's sorted internally/implicitly. But here we do adding/removing from orderbook explicitly

### matching engine architecture
we have 2 implementation of order book:
* naive `MapOrderBook` - here we are using default JDK implementations like `TreeMap/HashMap` to store internal state of orderbook like bids/asks remaining orders by orderId. Since these JDK implementations are not designed for low latency, they won't perform well under high load due to: gc problems or primitive unboxing/auto-boxing. So this orderbook implementation is good for educational purpose, but completely useless for real-world apps.
* advanced `ArrayOrderBook` - this is close to real-world orderbook implementation, without any of JDK implementations:
    * use pure array to store bids/asks
    * use low-latency Map to store orders by ID
    * use object pooling for order object
* real-world app - build on top of advanced orderbook replicated system:
    * use aeron cluster for multi-node zero-downtime 
    * use aeron archive for persistence
    * https://github.com/real-logic/aeron/wiki/Cluster-Tutorial
    * with client as simple http order api, which receive messages from clients, and then act as client to cluster, send order to ME (cluster) and receive messages from it
    * checkout message lost in case of failed leader (https://github.com/real-logic/aeron/wiki/Cluster-Tutorial#55-failed-nodes-and-leader-election). This answer suggest that we should have app logic to check ack from cluster and in case of leader change, and message lost, client won't receive ack for these few messages, and have to re-send messages to cluster https://stackoverflow.com/questions/73370605/what-are-the-aeron-cluster-delivery-guarantees

### matching engine architecture
exchange components (based of https://www.youtube.com/watch?v=b1e4t2k2KJY):
* primary me with orderbook per instrument (add/cancel/replace) - cancel - 40%
* message bus (in-memory but can be used aeron) - use multicast so once event happen (like exectuion) all component can receive it at the same time and return to clients
    * drop copy (of some specific clients)
    * MD (market data)
    * trade reporter (cause MD just show active orderbook, once order executed it just disappers from MD, so we need trade reporter)
* cancel manager (you can submit cancelRequest to cancel order at some point in future)
* auction manager (aggregate all prices & find the price that maximize the profit)
* secondary me (also called passive me) that listen output from primary me (this is important, it shouldn't listen incoming messages from message bus, only result of already processed messages by first me) and builds exactly the same state as first ME (so in case first me fail, we can start second me from exactly same place where first fail) => you can use paxos to decide who is primary me and when you should switch to secondary me. But no exchange use it, most exchanges has simple failover logic, if main me fail, promote passive me to primary.
* multi-threading - new thread per orderbook - good, but centralization has benefits
* state-machine recovery is a replay
* speed, latency, throughput, determenism - basic principle of exchange

### aeron - internal communication bus
* aeron + busy loop is faster then unix select (https://en.wikipedia.org/wiki/Select_(Unix))
* if message size greater than MTU (1500byte) we need FragmentAssembler
* aeron use `/dev/shm` => any file created there treated as simple file yet stored in-memory
* since udp doens't gurantee order & delivery we aeron use position to ensure order & delivery
* conductor on receiver side will check all position and if missing will send back NAK (negative acknolegement) and ask sender to re-send again
* tcp philosophy - better late then never, low-latency philosophy - better never then late (so it's better not to send order at all, rather then send order with outdated price)
* publisher write into log.buffer

### derivative exchange
This exchange will include following items:
* dated futures
* [perpetual futures aka perpetual swap](https://github.com/dgaydukov/exchange-core/blob/master/src/main/java/com/exchange/core/futures/RiskEngine.java) - only part that implemented as part of this project
* options
* auctions
* swaps (interest rate swap, like swap between fixed & floating income for defi or on perpetual 8 hour payment)
* move options
* spreads

### exchange components
exchange should include 3 components:
* matching engine (2 instances should be running, see [architecture](#matching-engine-architecture))
* communication channel or bus - [aeron](#aeron---internal-communication-bus) should be used
* external api - how clients can communicate with the system. Most top exchanges uses 3 level of communication
    * http-api
    * ws-api
    * fix-api

### fix-api
Logon details
There are several ways to logon with fix:
* [deribit](https://docs.deribit.com/#logon-a) - here we just take hash256 from secret key
* [coinbase](https://docs.cloud.coinbase.com/exchange/docs/messages#logon-a) - here we take hmac of several fields like SendingTime/MsgType/MsgSeqNum/SenderCompID/TargetCompID/Password
here we have interesting concept of api key = key+secret+passphrase. And third param provided by end-user and stored hashed in server. So to access api/fix all 3 need to be used.
This ensure if credentials got leaked from exchange, attacker still not be able to login/sendRequests cause he doesn't know passphrase, which only user knows.
* [etorox](https://etorox.com/etorox-fix-api/#FIX-Session-Level-Messages) - here we are using hmac to sign randomly generated payload
* [cme](https://www.cmegroup.com/confluence/display/EPICSANDBOX/Session+Layer+-+Logon#SessionLayerLogon-Step2-CreateSignatureusingSecretKeyandCanonicalFIXMessage) - here we use hmac to sign whole body

As you see there is no standard across the ecosystem, different exchanges uses different approach. The most consistent in my opinion is cme case, where you have body and sign it.
Also best practice is to have key rotation logic, but it should be handled by different component, where basically you urge your clients to rotate keys every year, and if clients is not dot doing this, you disable such keys after expiration date.