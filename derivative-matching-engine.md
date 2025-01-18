# Spot matching-engine

### Content
* [Description](#description)
* [Exchange architecture](#exchange-architecture)
* [Aeron - internal communication bus](#aeron---internal-communication-bus)
* [Derivative exchange](#derivative-exchange)
* [FIX Gateway](#fix-gateway)

### Description
Below is proposed architecture design for real-world matching-engine solution with end-to-end integration using `aeron` as a communication bus between different component and API & WS for clients and FIX gateway for institutional clients.
Derivative matching engine consist of several parts:
* [spot matching engine](/README.md) - this is the part that is shared here in public Github repository
* derivative matching engine - second ME running separately and communicating with spot through Aeron
* REST API for client communication who prefer REST
* WS read/write - for clients who prefer websocket connection
* FIX - for institutional clients who prefer FIX protocol communication with exchange
* Aeron - message bus that connects matching-engine with 3 services REST/WS/FIX

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
* basic principle of exchange - speed, latency, throughput, determinism
Exchange should include 3 components:
  * matching engine - 2 instances should be running, see [architecture](#matching-engine-architecture)
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

### Aeron - internal communication bus
* aeron + busy loop is faster than unix select (https://en.wikipedia.org/wiki/Select_(Unix))
* if message size greater than MTU (1500byte) we need FragmentAssembler
* aeron use `/dev/shm` => any file created there treated as simple file yet stored in-memory
* since udp doesn't guarantee order & delivery we aeron use position to ensure order & delivery
* conductor on receiver side will check all position and if missing will send back NAK (negative acknolegement) and ask sender to re-send again
* tcp philosophy - better late than never, low-latency philosophy - better never than late (so it's better not to send order at all, rather than send order with outdated price)
* publisher write into `log.buffer`

### Derivative exchange
This exchange will include following items:
* dated futures
* perpetual futures aka perpetual swap
* options
* auctions

### FIX Gateway
There are several ways to logon with fix:
* [deribit](https://docs.deribit.com/#logon-a) - here we just take hash256 from secret key
* [coinbase](https://docs.cloud.coinbase.com/exchange/docs/messages#logon-a) - here we take `HMAC` of several fields like SendingTime/MsgType/MsgSeqNum/SenderCompID/TargetCompID/Password. We have interesting concept of `apiKey = key+secret+passphrase`. And third param provided by end-user and stored hashed in server. So to access api/fix all 3 need to be used. This ensures if credentials got leaked from exchange, attacker still not be able to login/sendRequests because he doesn't know passphrase, which only user knows.
* [etorox](https://etorox.com/etorox-fix-api/#FIX-Session-Level-Messages) - here we are using `HMAC` to sign randomly generated payload
* [cme](https://www.cmegroup.com/confluence/display/EPICSANDBOX/Session+Layer+-+Logon#SessionLayerLogon-Step2-CreateSignatureusingSecretKeyandCanonicalFIXMessage) - here we use `HMAC` to sign whole body
As you see there is no standard across the ecosystem, different exchanges uses different approach. The most consistent in my opinion is cme case, where you have body and sign it.
Also best practice is to have key rotation logic, but it should be handled by different component, where basically you urge your clients to rotate keys every year, and if clients is not dot doing this, you disable such keys after expiration date.