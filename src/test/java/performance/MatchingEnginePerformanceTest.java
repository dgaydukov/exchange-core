package performance;

import com.exchange.core.MockData;
import com.exchange.core.matching.engine.MatchingEngine;
import com.exchange.core.matching.engine.SpotMatchingEngine;
import com.exchange.core.model.enums.OrderBookType;
import com.exchange.core.model.enums.OrderStatus;
import com.exchange.core.model.msg.ExecutionReport;
import com.exchange.core.model.msg.InstrumentConfig;
import com.exchange.core.model.msg.Message;
import com.exchange.core.model.msg.Order;
import com.exchange.core.model.msg.UserBalance;
import java.math.BigDecimal;
import java.util.List;
import java.util.Map;
import java.util.Queue;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.stream.Stream;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

public class MatchingEnginePerformanceTest {

  private static Stream<Arguments> getOrderBookTypes() {
    final int size = 500_000;
    return Stream.of(
        Arguments.of(size, OrderBookType.MAP),
        Arguments.of(size, OrderBookType.ARRAY)
    );
  }

  /**
   * Simple test to validate JDK Queue performance to understand is it a good structure to measure
   * latency and TPS Since we are using pure in-memory tests, any structure including JDK would
   * work, cause we don't waste time on serialize/decerialize. The conclusion, JDK Queue
   * implementation is good enough to validate
   */
  @Test
  public void javaQueueLatencyTest() throws InterruptedException {
    int QUEUE_SIZE = 1_000_000;
    final String lastClOrdId = "sell_" + (QUEUE_SIZE - 1);
    // use thread-safe queue instead of LinkedList cause we read it from another repo
    Queue<Order> queue = new LinkedBlockingQueue<>();
    long start = System.currentTimeMillis();
    Map<String, Long> latencyMap = new ConcurrentHashMap<>();

    Runnable reader = () -> {
      long readerStart = System.currentTimeMillis();
      int counter = 0;
      while (true) {
        Message msg = queue.poll();
        if (msg instanceof Order exec) {
          counter++;
          latencyMap.compute(exec.getClOrdId(), (k, v) -> System.currentTimeMillis() - v);
          if (exec.getClOrdId().equals(lastClOrdId)) {
            break;
          }
        }
      }
      System.out.println(
          "time to process read: " + (System.currentTimeMillis() - readerStart) + ", counter="
              + counter);
      List<Long> latencyList = latencyMap.values()
          .stream()
          .sorted()
          .toList();
      System.out.println(
          "latency for 50% is below " + latencyList.get((int) (latencyList.size() * .5)));
      System.out.println(
          "latency for 90% is below " + latencyList.get((int) (latencyList.size() * .9)));
      System.out.println(
          "latency for 99% is below " + latencyList.get((int) (latencyList.size() * .99)));
    };

    Thread t1 = new Thread(reader);
    t1.start();

    for (int i = 0; i < QUEUE_SIZE; i++) {
      long timestamp = System.currentTimeMillis();
      Order buy = RandomOrder.buyLimitUser1();
      buy.setClOrdId("buy_" + i);
      latencyMap.put(buy.getClOrdId(), timestamp);
      queue.add(buy);
      Order sell = RandomOrder.sellLimitUser2();
      long sellTimestamp = System.currentTimeMillis();
      sell.setClOrdId("sell_" + i);
      latencyMap.put(sell.getClOrdId(), sellTimestamp);
      queue.add(sell);
    }

    System.out.println("time to process write: " + (System.currentTimeMillis() - start));
    t1.join();
  }

  @ParameterizedTest
  @MethodSource("getOrderBookTypes")
  public void tpsAndThroughputTest(int queueSize, OrderBookType orderBookType)
      throws InterruptedException {
    System.out.println(
        "tpsAndThroughputTest: orderBookType=" + orderBookType + ", size=" + queueSize);
    final String lastClOrdId = "sell_" + (queueSize - 1);

    Queue<Message> inbound = new LinkedBlockingQueue<>();
    Queue<Message> outbound = new LinkedBlockingQueue<>();
    MatchingEngine me = new SpotMatchingEngine(inbound, outbound, orderBookType, false);
    me.start();

    // adding instrument
    InstrumentConfig symbolMsg = new InstrumentConfig();
    symbolMsg.setBase(MockData.BASE);
    symbolMsg.setQuote(MockData.QUOTE);
    symbolMsg.setSymbol(MockData.SYMBOL);
    inbound.add(symbolMsg);

    // adding 2 users with balances
    UserBalance userBalance1 = new UserBalance();
    userBalance1.setAccount(1);
    userBalance1.setAsset(MockData.QUOTE);
    userBalance1.setAmount(new BigDecimal("10000000000"));
    inbound.add(userBalance1);
    UserBalance userBalance2 = new UserBalance();
    userBalance2.setAccount(2);
    userBalance2.setAsset(MockData.BASE);
    userBalance2.setAmount(new BigDecimal("10000000000"));
    inbound.add(userBalance2);

    Runnable reader = () -> {
      long readerStart = System.currentTimeMillis();
      long count = 0;
      while (true) {
        Message msg = outbound.poll();
        if (msg instanceof ExecutionReport exec) {
          count++;
          if (exec.getClOrdId().equals(lastClOrdId)) {
            break;
          }
        }
      }
      long timeTaken = System.currentTimeMillis() - readerStart;
      double tps = queueSize / (double) timeTaken * 1000;
      System.out.println(
          "reading done: time=" + timeTaken + ", TPS=" + (long) tps + ", messagesRead="
              + count);
    };
    Thread t1 = new Thread(reader);
    t1.start();

    long start = System.currentTimeMillis();
    for (int i = 0; i < queueSize; i++) {
      Order buy = RandomOrder.buyLimitUser1();
      buy.setClOrdId("buy_" + i);
      inbound.add(buy);
      Order sell = RandomOrder.sellLimitUser2();
      sell.setClOrdId("sell_" + i);
      inbound.add(sell);
    }

    System.out.println("writing done: time=" + (System.currentTimeMillis() - start));
    t1.join();
  }

  @ParameterizedTest
  @MethodSource("getOrderBookTypes")
  public void latencyTest(int queueSize, OrderBookType orderBookType) throws InterruptedException {
    System.out.println("runLatencyTest: size=" + queueSize + ", type=" + orderBookType);
    Map<String, Long> latencyMap = new ConcurrentHashMap<>();
    String lastClOrdId = "sell_" + (queueSize - 1);

    Queue<Message> inbound = new LinkedBlockingQueue<>();
    Queue<Message> outbound = new LinkedBlockingQueue<>();
    MatchingEngine me = new SpotMatchingEngine(inbound, outbound, orderBookType, false);
    me.start();
    long start = System.currentTimeMillis();

    // adding instrument
    InstrumentConfig symbolMsg = new InstrumentConfig();
    symbolMsg.setBase(MockData.BASE);
    symbolMsg.setQuote(MockData.QUOTE);
    symbolMsg.setSymbol(MockData.SYMBOL);
    inbound.add(symbolMsg);

    // adding 2 users with balances
    UserBalance userBalance1 = new UserBalance();
    userBalance1.setAccount(1);
    userBalance1.setAsset(MockData.QUOTE);
    userBalance1.setAmount(new BigDecimal("10000000000"));
    inbound.add(userBalance1);
    UserBalance userBalance2 = new UserBalance();
    userBalance2.setAccount(2);
    userBalance2.setAsset(MockData.BASE);
    userBalance2.setAmount(new BigDecimal("10000000000"));
    inbound.add(userBalance2);

    Runnable reader = () -> {
      while (true) {
        Message msg = outbound.poll();
        if (msg instanceof ExecutionReport exec) {
          if (exec.getStatus() == OrderStatus.NEW) {
            latencyMap.compute(exec.getClOrdId(), (k, v) -> System.currentTimeMillis() - v);
          }
          if (exec.getClOrdId().equals(lastClOrdId)) {
            break;
          }
        }
      }
      System.out.println("reading done: time=" + (System.currentTimeMillis() - start));
      List<Long> latencyList = latencyMap.values()
          .stream()
          .sorted()
          .toList();
      System.out.println(
          "latency for 50% is below " + latencyList.get((int) (latencyList.size() * .5)));
      System.out.println(
          "latency for 90% is below " + latencyList.get((int) (latencyList.size() * .9)));
      System.out.println(
          "latency for 99% is below " + latencyList.get((int) (latencyList.size() * .99)));
    };
    Thread t1 = new Thread(reader);
    t1.start();

    for (int i = 0; i < queueSize; i++) {
      long timestamp = System.currentTimeMillis();
      Order buy = RandomOrder.buyLimitUser1();
      buy.setClOrdId("buy_" + i);
      inbound.add(buy);
      Order sell = RandomOrder.sellLimitUser2();
      long sellTimestamp = System.currentTimeMillis();
      sell.setClOrdId("sell_" + i);
      inbound.add(sell);
      latencyMap.put(buy.getClOrdId(), timestamp);
      latencyMap.put(sell.getClOrdId(), sellTimestamp);
    }
    System.out.println("writing done: time=" + (System.currentTimeMillis() - start));
    t1.join();
  }
}