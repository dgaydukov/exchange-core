package performance;

import com.exchange.core.matching.MatchingEngine;
import com.exchange.core.model.enums.OrderBookType;
import com.exchange.core.model.enums.OrderSide;
import com.exchange.core.model.enums.OrderStatus;
import com.exchange.core.model.enums.OrderType;
import com.exchange.core.model.msg.ExecutionReport;
import com.exchange.core.model.msg.InstrumentConfig;
import com.exchange.core.model.msg.Message;
import com.exchange.core.model.msg.Order;
import com.exchange.core.model.msg.UserBalance;
import java.math.BigDecimal;
import java.util.List;
import java.util.Map;
import java.util.Queue;
import java.util.Random;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.LinkedBlockingQueue;
import org.junit.jupiter.api.Test;

public class MatchingEnginePerformanceTest {

  private final static String BASE = "BTC";
  private final static String QUOTE = "USDT";
  private final static String SYMBOL = BASE + "/" + QUOTE;
  private final static Random random = new Random();

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
      Order buy = buyLimitUser1();
      buy.setClOrdId("buy_" + i);
      latencyMap.put(buy.getClOrdId(), timestamp);
      queue.add(buy);
      Order sell = sellLimitUser2();
      long sellTimestamp = System.currentTimeMillis();
      sell.setClOrdId("sell_" + i);
      latencyMap.put(sell.getClOrdId(), sellTimestamp);
      queue.add(sell);
    }

    System.out.println("time to process write: " + (System.currentTimeMillis() - start));
    t1.join();
  }

  @Test
  public void tpsAndThroughputTest() throws InterruptedException {
    final int QUEUE_SIZE = 100_000;
    final String lastClOrdId = "sell_" + (QUEUE_SIZE - 1);

    Queue<Message> inbound = new LinkedBlockingQueue<>();
    Queue<Message> outbound = new LinkedBlockingQueue<>();
    MatchingEngine me = new MatchingEngine(inbound, outbound, OrderBookType.MAP, false);
    me.start();

    // adding instrument
    InstrumentConfig symbolMsg = new InstrumentConfig();
    symbolMsg.setBase(BASE);
    symbolMsg.setQuote(QUOTE);
    symbolMsg.setSymbol(SYMBOL);
    inbound.add(symbolMsg);

    // adding 2 users with balances
    UserBalance userBalance1 = new UserBalance();
    userBalance1.setAccount(1);
    userBalance1.setAsset(QUOTE);
    userBalance1.setAmount(new BigDecimal("10000000000"));
    inbound.add(userBalance1);
    UserBalance userBalance2 = new UserBalance();
    userBalance2.setAccount(2);
    userBalance2.setAsset(BASE);
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
      double tps = QUEUE_SIZE / (double) timeTaken * 1000;
      System.out.println(
          "time to process read=" + timeTaken + ", TPS=" + (long) tps + ", outboundMessagesRead="
              + count);
    };
    Thread t1 = new Thread(reader);
    t1.start();

    long start = System.currentTimeMillis();
    for (int i = 0; i < QUEUE_SIZE; i++) {
      Order buy = buyLimitUser1();
      buy.setClOrdId("buy_" + i);
      inbound.add(buy);
      Order sell = sellLimitUser2();
      sell.setClOrdId("sell_" + i);
      inbound.add(sell);
    }

    System.out.println("time to process write: " + (System.currentTimeMillis() - start));
    t1.join();
  }

  @Test
  public void latencyTest() throws InterruptedException {
    final int QUEUE_SIZE = 100_000;
    runLatencyTest(QUEUE_SIZE, OrderBookType.MAP);
    System.out.println();
    runLatencyTest(QUEUE_SIZE, OrderBookType.ARRAY);
  }

  private void runLatencyTest(int queueSize, OrderBookType type) throws InterruptedException {
    System.out.println("runLatencyTest: size="+queueSize+", type="+type);
    Map<String, Long> latencyMap = new ConcurrentHashMap<>();
    String lastClOrdId = "sell_" + (queueSize - 1);

    Queue<Message> inbound = new LinkedBlockingQueue<>();
    Queue<Message> outbound = new LinkedBlockingQueue<>();
    MatchingEngine me = new MatchingEngine(inbound, outbound, type, false);
    me.start();
    long start = System.currentTimeMillis();

    // adding instrument
    InstrumentConfig symbolMsg = new InstrumentConfig();
    symbolMsg.setBase(BASE);
    symbolMsg.setQuote(QUOTE);
    symbolMsg.setSymbol(SYMBOL);
    inbound.add(symbolMsg);

    // adding 2 users with balances
    UserBalance userBalance1 = new UserBalance();
    userBalance1.setAccount(1);
    userBalance1.setAsset(QUOTE);
    userBalance1.setAmount(new BigDecimal("10000000000"));
    inbound.add(userBalance1);
    UserBalance userBalance2 = new UserBalance();
    userBalance2.setAccount(2);
    userBalance2.setAsset(BASE);
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
      System.out.println("time to process read: " + (System.currentTimeMillis() - start));
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
      Order buy = buyLimitUser1();
      buy.setClOrdId("buy_" + i);
      inbound.add(buy);
      Order sell = sellLimitUser2();
      long sellTimestamp = System.currentTimeMillis();
      sell.setClOrdId("sell_" + i);
      inbound.add(sell);
      latencyMap.put(buy.getClOrdId(), timestamp);
      latencyMap.put(sell.getClOrdId(), sellTimestamp);
    }
    System.out.println("time to process write: " + (System.currentTimeMillis() - start));
    t1.join();
  }


  private BigDecimal getPrice() {
    int next = random.nextInt(1, 1000);
    return new BigDecimal(next);
  }

  private Order buyLimitUser1() {
    Order order = new Order();
    order.setSymbol(SYMBOL);
    order.setType(OrderType.LIMIT);
    order.setSide(OrderSide.BUY);
    order.setAccount(1);
    order.setOrderQty(new BigDecimal("1"));
    order.setPrice(getPrice());
    return order;
  }

  private Order sellLimitUser2() {
    Order order = buyLimitUser1();
    order.setAccount(2);
    order.setSide(OrderSide.SELL);
    return order;
  }
}