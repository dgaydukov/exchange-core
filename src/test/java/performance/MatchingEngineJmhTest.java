package performance;

import com.exchange.core.MockData;
import com.exchange.core.matching.MatchingEngine;
import com.exchange.core.model.enums.OrderBookType;
import com.exchange.core.model.enums.OrderSide;
import com.exchange.core.model.enums.OrderType;
import com.exchange.core.model.msg.InstrumentConfig;
import com.exchange.core.model.msg.Message;
import com.exchange.core.model.msg.Order;
import com.exchange.core.model.msg.UserBalance;
import java.math.BigDecimal;
import java.util.Random;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.TimeUnit;
import org.openjdk.jmh.annotations.Benchmark;
import org.openjdk.jmh.annotations.BenchmarkMode;
import org.openjdk.jmh.annotations.Fork;
import org.openjdk.jmh.annotations.Level;
import org.openjdk.jmh.annotations.Measurement;
import org.openjdk.jmh.annotations.Mode;
import org.openjdk.jmh.annotations.OutputTimeUnit;
import org.openjdk.jmh.annotations.Scope;
import org.openjdk.jmh.annotations.Setup;
import org.openjdk.jmh.annotations.State;
import org.openjdk.jmh.annotations.Warmup;
import org.openjdk.jmh.infra.Blackhole;
import org.openjdk.jmh.runner.Runner;
import org.openjdk.jmh.runner.RunnerException;
import org.openjdk.jmh.runner.options.Options;
import org.openjdk.jmh.runner.options.OptionsBuilder;

@BenchmarkMode(Mode.AverageTime)
@OutputTimeUnit(TimeUnit.MILLISECONDS)
@State(Scope.Benchmark)
@Fork(value = 2, jvmArgs = {"-Xms2G", "-Xmx2G"})
@Warmup(iterations = 5, time = 5)
@Measurement(iterations = 5, time = 5)
public class MatchingEngineJmhTest {

  private final static Random random = new Random();
  private final BlockingQueue<Message> inbound = new LinkedBlockingQueue<>();
  private final BlockingQueue<Message> outbound = new LinkedBlockingQueue<>();
  private final MatchingEngine me = new MatchingEngine(inbound, outbound, OrderBookType.MAP, false);

  public static void main(String[] args) throws RunnerException {
    Options opt = new OptionsBuilder()
        .include(MatchingEngineJmhTest.class.getSimpleName())
        .forks(1)
        .build();
    new Runner(opt).run();
  }

  @Setup(Level.Iteration)
  public void setUp() {
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

    // add 1M orders
    for (int i = 0; i < 1_000_000; i++) {
      Order buy = buyLimitUser1();
      buy.setClOrdId("buy_" + i);
      inbound.add(buy);
      Order sell = sellLimitUser2();
      sell.setClOrdId("sell_" + i);
      inbound.add(sell);
    }
  }


  @Benchmark
  public void latency(Blackhole blackhole) {
    me.start();
    blackhole.consume(outbound.poll());
  }

  private Order buyLimitUser1() {
    Order order = new Order();
    order.setSymbol(MockData.SYMBOL);
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

  private BigDecimal getPrice() {
    int next = random.nextInt(1, 1000);
    return new BigDecimal(next);
  }
}