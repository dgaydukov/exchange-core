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
   * 1. match & add buy limit order
   * 2. match & add sell limit order
   * 3. match buy market (we don't add market orders to OrderBook)
   * 4. match sell market (we don't add market orders to OrderBook)
   * 5. fetch & update buy limit order
   * 6. fetch & remove sell limit order
   */
  private void __orderBookTest(OrderBook ob, Blackhole blackhole){
    Order buy, sell;

    buy = RandomOrder.buyLimitUser1();
    blackhole.consume(ob.match(buy));
    if (buy.getLeavesQty().compareTo(BigDecimal.ZERO) > 0) {
      blackhole.consume(ob.add(buy));
      blackhole.consume(ob.buildMarketData());
    }
    sell = RandomOrder.sellLimitUser2();
    blackhole.consume(ob.match(sell));
    if (sell.getLeavesQty().compareTo(BigDecimal.ZERO) > 0) {
      blackhole.consume(ob.add(sell));
      blackhole.consume(ob.buildMarketData());
    }

    blackhole.consume(ob.match(RandomOrder.buyMarketUser1()));
    blackhole.consume(ob.buildMarketData());
    blackhole.consume(ob.match(RandomOrder.sellMarketUser1()));
    blackhole.consume(ob.buildMarketData());

    buy = RandomOrder.buyLimitUser1();
    blackhole.consume(ob.match(buy));
    if (buy.getLeavesQty().compareTo(BigDecimal.ZERO) > 0) {
      blackhole.consume(ob.add(buy));
      blackhole.consume(ob.buildMarketData());
    }
    sell = RandomOrder.sellLimitUser2();
    blackhole.consume(ob.match(sell));
    if (sell.getLeavesQty().compareTo(BigDecimal.ZERO) > 0) {
      blackhole.consume(ob.add(sell));
      blackhole.consume(ob.buildMarketData());
    }

    Order fetchBuy = ob.getOrder(buy.getOrderId());
    if (fetchBuy != null){
      blackhole.consume(ob.remove(fetchBuy.getOrderId()));
      blackhole.consume(ob.buildMarketData());
    }

    Order fetchSell = ob.getOrder(sell.getOrderId());
    if (fetchSell != null){
      fetchSell.setLeavesQty(fetchSell.getLeavesQty().subtract(new BigDecimal(1)));
      fetchSell.setPrice(fetchSell.getLeavesQty().subtract(new BigDecimal(1)));
      blackhole.consume(ob.update(fetchSell));
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