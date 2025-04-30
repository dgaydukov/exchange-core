package performance;

import com.exchange.core.MockData;
import com.exchange.core.matching.orderbook.OrderBook;
import com.exchange.core.matching.orderbook.book.ArrayOrderBook;
import com.exchange.core.matching.orderbook.book.IpqOrderBook;
import com.exchange.core.matching.orderbook.book.LinkedListOrderBook;
import com.exchange.core.matching.orderbook.book.MapOrderBook;
import com.exchange.core.model.msg.Order;
import org.openjdk.jmh.annotations.*;
import org.openjdk.jmh.infra.Blackhole;
import org.openjdk.jmh.runner.Runner;
import org.openjdk.jmh.runner.RunnerException;
import org.openjdk.jmh.runner.options.Options;
import org.openjdk.jmh.runner.options.OptionsBuilder;

import java.math.BigDecimal;
import java.util.concurrent.TimeUnit;

@BenchmarkMode(Mode.AverageTime)
@OutputTimeUnit(TimeUnit.MILLISECONDS)
@State(Scope.Benchmark)
@Fork(value = 2, jvmArgs = {"-Xms2G", "-Xmx2G"})
@Warmup(iterations = 5, time = 5)
@Measurement(iterations = 5, time = 5)
public class OrderBookPerformanceTest {

  private OrderBook arrayOrderBook;
  private OrderBook ipqOrderBook;
  private OrderBook linkedListOrderBook;
  private OrderBook mapOrderBook;

  public static void main(String[] args) throws RunnerException {
    Options opt = new OptionsBuilder()
        .include(OrderBookPerformanceTest.class.getSimpleName())
        .forks(1)
        .build();
    new Runner(opt).run();
  }

  @Setup(Level.Iteration)
  public void setUp() {
    // set big array as 100k, to have enough space for different price levels
    arrayOrderBook = new ArrayOrderBook(MockData.SYMBOL, 100_000);
    ipqOrderBook = new IpqOrderBook(MockData.SYMBOL);
    linkedListOrderBook = new LinkedListOrderBook(MockData.SYMBOL);
    mapOrderBook = new MapOrderBook(MockData.SYMBOL);
  }

  /**
   * List of actions:
   * 1. add 2 buy orders
   * 2. match against sell
   * 3. build market data
   * 4. remove second order
   * 5. update first order
   * 6. build market data
   */

  private void __orderBookTest(OrderBook ob, Blackhole blackhole){
    Order buy = RandomOrder.buyLimitUser1();
    Order sell = RandomOrder.buyLimitUser1();
    ob.match(buy);
    if
    blackhole.consume(ob.match(RandomOrder.sellLimitUser2()));
    blackhole.consume(ob.match(RandomOrder.sellMarkettUser2()));
    blackhole.consume(ob.buildMarketData());
    Order fetch1 = ob.getOrder(buy1.getOrderId());
    if (fetch1 != null){
      blackhole.consume(ob.remove(fetch1.getOrderId()));
      blackhole.consume(ob.buildMarketData());
    }
    Order fetch2 = ob.getOrder(buy2.getOrderId());
    if (fetch2 != null){
      fetch2.setLeavesQty(fetch2.getLeavesQty().subtract(new BigDecimal(1)));
      fetch2.setPrice(fetch2.getLeavesQty().subtract(new BigDecimal(1)));
      blackhole.consume(ob.update(fetch2));
      blackhole.consume(ob.buildMarketData());
    }
  }
  private void orderBookTest(OrderBook ob, Blackhole blackhole){
    try{
      __orderBookTest(ob, blackhole);
    } catch (Exception ex){}
  }

  @Benchmark
  public void measureArrayOrderBook(Blackhole blackhole) {
    orderBookTest(arrayOrderBook, blackhole);
  }

  @Benchmark
  public void measureIpqOrderBook(Blackhole blackhole) {
    orderBookTest(ipqOrderBook, blackhole);
  }

  @Benchmark
  public void measureLinkedListOrderBook(Blackhole blackhole) {
    orderBookTest(linkedListOrderBook, blackhole);
  }

  @Benchmark
  public void measureMapOrderBook(Blackhole blackhole) {
    orderBookTest(mapOrderBook, blackhole);
  }
}