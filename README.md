# Spot matching-engine

### Content
* [Description](#description)
* [Matching engine architecture](#matching-engine-architecture)
* [Snapshot](#snapshot)
* [Test coverage](#test-coverage)
  * [Unit test](#unit-test)
  * [Integration test](#integration-test)
  * [Performance test](#performance-test)
  * [Architectural test](#architectural-test)
* [Precision loss problem](#precision-loss-problem)

### Description
This is the public part of my [derivative-matching-engine](/derivative-matching-engine.md) project that can serve 2 purposes:
* educational - you can see what is matching engine and how it's supposed to work
* backbone to your own matching-engine - you can take this project and work on top of it to create fully-operational exchange system
This public part include only `spot matching-engine`. The full end-to-end project is private.

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
* [MapOrderBook](/src/main/java/com/exchange/core/matching/orderbook/OrderBook.java) - naive implementation, here we are using default JDK implementations `java.utils.TreeMap` to store internal state of order book like bids/asks. Since these JDK implementations are not designed for low latency, they won't perform well under high load due to: gc problems or primitive unboxing/auto-boxing. Such implementation is good for educational purpose, but completely useless for real-world apps.
* [ArrayOrderBook](/src/main/java/com/exchange/core/matching/orderbook/array/ArrayOrderBook.java) - advanced version of order book, compared to `MapOrderBook` order book, here we can achieve faster performance by directly manipulating the underlying array. In `TreeMap` there is also underlying array and each time we insert/remove, it's sorted internally/implicitly. But here we are adding/removing from order book explicitly

### Snapshot
Snapshot is the crucial concept in ME design. Since our ME using internal memory to store the state that means that order book, user balances and other state stored in internal memory while our java app is running. But imagine is something like this happens:
* app crash
* planned downtime
* deployment with downtime
Any of these 3 would require to restart (stop & start) our java app, and once we do this, our app would start from 0, and nothing would be there. So we need some logic that:
* once-in-a-while => system take snapshot of in-memory data and store it
* once we start => system would read latest snapshot and update internal state
This concept is implemented in all matching-engines, cause otherwise it won't work properly. Below is a set of rules how we implement it here:
* storage: store snapshot in files
* format: use raw json as format
Pay attention to development process:
1. We first build `snapshot` package
2. then we covered it by tests, but since we haven't implemented `Snapshotable` we imitate this behavior inside our test. Such approach allows us to develop separately. One can create snapshot code that would be responsible: creating snapshot, storing it into file, recover it from the file, load into the system. And another can actually implement `Snapshotable` into different classes (instruments, accounts, orders). By doing this we can split the work with the help of java interfaces.
   * Take a look into [SnapshotManagerTest](/src/test/java/com/exchange/core/matching/snapshot/SnapshotManagerTest.java) - here you can see that although we don't have any implementation of `Snapshotable` yes, it doesn't stop us from actually test snapshot manager, by mocking this interface. So again this is a nice example of how you can collaborate & distribute tasks within a team, by using java interfaces as a contract between different developers.
3. actually implement `Snapshotable` interface with already existing classes, and add tests for this new interface for already existing tests. Pay attention this step can be done separately from `step#2`. All you need is to take 3 already implemented classes `InstrumentRepositoryImpl/AccountRepositoryImpl/MapOrderBook` and add implementation of `Snapshotable` interface. Now each of these 3 classes would be implementing 2 interfaces. The great thing about interfaces that we can create separate tests for each. So we have 2 test per each class with separate interface. For example take a look at these 2 tests for same class that implements 2 interfaces:
  * [AccountRepositoryTest](/src/test/java/com/exchange/core/repository/AccountRepositoryTest.java) - test of `AccountRepository` interface and it's main functions
  * [AccountSnapshotableTest](/src/test/java/com/exchange/core/matching/snapshotable/AccountSnapshotableTest.java) - test of `Snapshotable` interface with it's main functions. See how here we cast our object back to `AccountRepository` to actually validate how snapshoting works, when we create snapshot & load snapshot.
As you see you can use different test classes for each interface that you class implements.

### Test coverage
Test coverage is the most important thing in any app, so here we covered our application with all test cases. 
There are 4 types of test coverage:
* unit test - we test source code here
* integration test - we test running app here
* performance test - we test performance of running app (we can test performance of code, but mostly we are interested in the end-to-end performance of running app)
* architectural test - we test enforcement of architectural rules

#### Unit test
In unit tests we test only source code. Most of your code should be covered by unit test. They are fast to run, cause we don't need to start the app, we test the code. They take like 80% of overall test coverage. In this app all code except `MatchingEngine` covered by unit tests. And we can't test `MatchingEngine` class with unit test, cause it's running app that start threads and read messages, this is not a class that you can mock. So we have to write integration test for it. The basic rule, if class can be easily mocked, we should use unit test for it.
Since we have 2 order books - and both implement same interface and are expected to behave the same way, I've created [parametrized test](/src/test/java/com/exchange/core/matching/orderbook/OrderBookTest.java) where I run all the tests at the same time for 2 order books, using Junit `@ParameterizedTest` annotation.

#### Integration test
In integration tests we actually run the app, send incoming messages & listen for outgoing. So it's like black-box testing. We run out app, send incoming requests (messages to the queue in our case) and listen for response (read messages from outbound queue in our case). This is similar to actually real user using our app. Good candidate here is `MatchingEngine` class. Cause to test it you have to run it, provide 2 queues and send messages into `inbound` queue and verify that you receive messages in the `outbound` queue.

#### Performance test
Performance testing - this is integration test that measures system performance overall. It should be integration, cause you need to actually run your system end-to-end, and then put extreme load into it and measure performance. I've created [MatchingEnginePerformanceTest](/src/test/java/performance/MatchingEnginePerformanceTest.java) that measures 2 things:
* TPS - how many messages/orders system can handle per second
* latency - what is average latency per single request. Here we measure end-to-end latency from the moment user send his order to our system and to the point when he received message back (in case of order - message would be execution report). 
If you dig into latency test you would notice that 90% of time is taken by adding and matching the order. If you run `latencyTest` you will notice that `ArrayOrderBook` performs way faster then `MapOrderBook`. If you look into test results it shows, that array-based order book performs on average better, due to native manipulation with data in array, where in `MapOrderBook` we are using java `TreeMap` which on average performs slower then array. See test results below for comparison (we send 1 million orders at once through the loop):
```
# Test results for TPS:
tpsAndThroughputTest: orderBookType=MAP, size=500000
Starting matching engine...
writing done: time=751
reading done: time=47821, TPS=10455, messagesRead=1781876
tpsAndThroughputTest: orderBookType=ARRAY, size=500000
Starting matching engine...
writing done: time=223
reading done: time=5391, TPS=92747, messagesRead=1782574

# Test results for latency:
runLatencyTest: size=500000, type=MAP
Starting matching engine...
writing done: time=1457
reading done: time=45611
latency for 50% is below 10111
latency for 90% is below 35381
latency for 99% is below 42995
runLatencyTest: size=500000, type=ARRAY
Starting matching engine...
writing done: time=445
reading done: time=9080
latency for 50% is below 5681
latency for 90% is below 7974
latency for 99% is below 8563
```
There are 2 ways you can write performance test:
* naive way - just write test, save startTime, and endTime. Then calculate the difference. There is so much wrong with thsi approach:
  * you need to run your code at least several times to find the average time. But if we write custom loop, then the loop itself would affect performance.
  * JVM can compile or interpret your code during compilation which may significantly affect measurement
  * JVM/CPU can optimize your code to improve performance, usually knows as dead code optimization, then part of your code would never be called
To avoid all such problems, it's better to use some framework for such testing.
* advanced way - using some kind of framework that specifically designed to handle all problems from naive approach. Once of such frameworks is [Java Microbenchmark Harness](https://github.com/openjdk/jmh). This tool was developed by the team who works on JVM, so developers utilized all their knowledge on JVM internals to create a tool that can take into account all JVM nuances. You can utilize it and measure your code performance. JHM should only be used for code performance. It's own name `micro-benchmark` suggest this. If we are talking about end-to-end performance with apiServer=>matchingEngine=>webSocket. We can use other tools, cause in such cases we are not care about code performance, but rather end-to-end performance. Yet for code performance measurement JHM is the best thing available. 

#### Architectural test
Here we can actually create a test that would enforce our architecture. We are using [ArchUnit library](https://www.archunit.org/userguide/html/000_Index.html) to enforce such tests. One good example if you are using `spring boot`, there is a good practice that you don't use `Repository` inside your controllers. So you can add a test to validate this. And next time, some new junior developer will add new API endpoint, and use repository directly inside controller method, the test will fail and he would have to rewrite it and move logic into `Service`. If you don't have such test cases, your only hope is code review, where senior devs would notice pattern breaking. But it's always better to have such tests in the first place.
Here we implement [ArchitecturalTest](/src/test/java/archetecture/ArchitecturalTest.java) where we validate that `orderchecks` can't be used inside `orderbook`. Since we decided to separate validation, matching and settlement. those 3 shouldn't mixed together. If you create new instance of `OrderBook` and pass either `PreOrderCheck` or `PostOrderCheck`, such tests would fail.
By default all arch tests are passing, but if you add this orderbook class, it would fail. As you see here, we created OrderBook implementation that depends on `PreOrderCheck/PostOrderCheck` which is again our architecture rules.
```java
import com.exchange.core.matching.orderchecks.PostOrderCheckImpl;
import com.exchange.core.matching.orderchecks.PreOrderCheck;
import com.exchange.core.model.Trade;
import com.exchange.core.model.msg.MarketData;
import com.exchange.core.model.msg.Order;
import java.util.List;

public class FailedOrderBook implements OrderBook {

  private final PreOrderCheck preOrderCheck;
  private final PostOrderCheckImpl postOrderCheck;

  public FailedOrderBook(PreOrderCheck preOrderCheck, PostOrderCheckImpl postOrderCheck) {
    this.preOrderCheck = preOrderCheck;
    this.postOrderCheck = postOrderCheck;
  }

  @Override
  public List<Trade> match(Order order) {
    return null;
  }

  @Override
  public void add(Order order) {

  }

  @Override
  public MarketData buildMarketData() {
    return null;
  }
}
```
To complicate things we add one dependency as interface and another as concrete implementation. But our test written in such a way that it can detect it, and if you run it, you will get error. There are 4 errors, cause 4 total dependencies, 2 declared as `private final` amd others 2 are passed as arguments into constructor. Yet if we remove all 4 dependencies, test would pass again.
```
java.lang.AssertionError: Architecture Violation [Priority: MEDIUM] - Rule 'classes that implement com.exchange.core.matching.orderbook.OrderBook should OrderBook shouldn't implement OrderChecks' was violated (4 times):
com.exchange.core.matching.orderbook.FailedOrderBook shouldn't implement PreOrderCheck/PostOrderCheck interfaces
com.exchange.core.matching.orderbook.FailedOrderBook shouldn't implement PreOrderCheck/PostOrderCheck interfaces
com.exchange.core.matching.orderbook.FailedOrderBook shouldn't implement PreOrderCheck/PostOrderCheck interfaces
com.exchange.core.matching.orderbook.FailedOrderBook shouldn't implement PreOrderCheck/PostOrderCheck interfaces
```

### Precision loss problem
This is the most common problem in finance where we deal with floating point.
There are 3 ways you can store price/quantity inside you system:
* double - good and cheap way to store data"
  * pros: simple and straightforward, take 8 bytes of memory
  * cons: precision loss almost in all cases, bad for finance
* BigDecimal - very high precision, can store any big number. but limitation on space:
  * pros: high precision, almost no loss
  * cons: require more bytes to store object, and it takes harder to serialize and transfer over the network. you may still encounter precision loss when you divide/multiply at the same time, take a look at [PrecisionLossTest](/src/test/java/com/exchange/core/PrecisionLossTest.java). No matter what rounding method are you using you will never get back original amount value. Yet if you look into same example for double, you would notice that when we multiply back double we can get correct value. Yet with `BigDecimal` it's behaved differently. But don't worry there is plenty of cases where `double` fails too.
* value/scale - where for each price/quantity you store 2 fields: `long value` & `byte scale`. By doing so you can pass any arbitrary number. For example, you work with bitcoin, and you want to operate with up to 6 digits after the comma. Then you can set `scale=6`. so your values would be:
```
1 BTC = 1000000
0.001 BTC = 100
```
But inside your matching-engine you should have special class to operate and round such values.
In my opinion this is the most cost-effective way to store numeric data in real-world production system.