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
import java.util.Queue;
import java.util.Random;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.LinkedTransferQueue;
import org.agrona.collections.Int2ObjectHashMap;
import org.agrona.concurrent.ManyToManyConcurrentArrayQueue;
import org.junit.jupiter.api.Test;

public class MatchingEnginePerformanceTest {

  private final static String BASE = "BTC";
  private final static String QUOTE = "USDT";
  private final static String SYMBOL = BASE + "/" + QUOTE;
  private final static Random random = new Random();

  @Test
  public void mapOrderBookTest() throws InterruptedException {
    final int QUEUE_SIZE = 5_000;

    Queue<Message> inbound = new ManyToManyConcurrentArrayQueue<>(QUEUE_SIZE*2);
    Queue<Message> outbound = new ManyToManyConcurrentArrayQueue<>(QUEUE_SIZE*10);
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

    long start = System.currentTimeMillis();

    for (int i = 0; i < QUEUE_SIZE; i++) {
      inbound.add(buyLimitUser1());
      inbound.add(sellLimitUser2());
    }


    long count = 0, maxExecId = 0;
    while (!outbound.isEmpty()){
      System.out.println(outbound.size());
      count++;
      Message msg = outbound.poll();
      if (msg instanceof ExecutionReport exec) {
        long execId = exec.getExecId();
        if (execId > maxExecId){
          maxExecId = execId;
        }
      }
    }

    Thread.sleep(100);
    Message msg = outbound.poll();
    while (msg != null){
      System.out.println(outbound.size());
      if (msg instanceof ExecutionReport exec) {

      }
      msg = outbound.poll();
    }

    long timeTaken = System.currentTimeMillis() - start;
    System.out.println("timeTaken=" + timeTaken);
  }

  @Test
  public void latencyTest(){
    Queue<Message> queue = new ManyToManyConcurrentArrayQueue<>(1000000);
  }

  private BigDecimal getPrice() {
    int next = random.nextInt(100, 200);
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