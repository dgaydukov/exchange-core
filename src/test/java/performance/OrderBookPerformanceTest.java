package performance;

import com.exchange.core.MockData;
import com.exchange.core.matching.orderbook.OrderBook;
import com.exchange.core.matching.orderbook.book.ArrayOrderBook;
import com.exchange.core.matching.orderbook.book.IpqOrderBook;
import com.exchange.core.matching.orderbook.book.LinkedListOrderBook;
import com.exchange.core.matching.orderbook.book.MapOrderBook;
import com.exchange.core.model.enums.OrderType;
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
@Fork(value = 2, jvmArgs = {"-Xms2G", "-Xmx4G"})
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
    arrayOrderBook = new ArrayOrderBook(MockData.SYMBOL);
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
   * 5. match & add buy limit order
   * 6. get/update/remove buy limit order
   * 7. match & add sell limit order
   * 8. get/update/remove sell limit order
   */
  private void orderBookTest(OrderBook ob, Blackhole blackhole) {
    matchAndAdd(RandomOrder.buyLimitUser1(), ob, blackhole);
    matchAndAdd(RandomOrder.sellLimitUser2(), ob, blackhole);
    matchAndAdd(RandomOrder.buyMarketUser1(), ob, blackhole);
    matchAndAdd(RandomOrder.sellMarketUser1(), ob, blackhole);
    Order buy = matchAndAdd(RandomOrder.buyLimitUser1(), ob, blackhole);
    getUpdateRemove(buy.getOrderId(), ob, blackhole);
    Order sell = matchAndAdd(RandomOrder.sellLimitUser2(), ob, blackhole);
    getUpdateRemove(sell.getOrderId(), ob, blackhole);
  }
  private Order matchAndAdd(Order order, OrderBook ob, Blackhole blackhole){
    blackhole.consume(ob.match(order));
    if (order.getType() == OrderType.LIMIT && order.getLeavesQty().compareTo(BigDecimal.ZERO) > 0) {
      blackhole.consume(ob.add(order));
    }
    blackhole.consume(ob.buildMarketData());
    return order;
  }
  private void getUpdateRemove(long orderId, OrderBook ob, Blackhole blackhole){
    Order order = ob.getOrder(orderId);
    if (order != null) {
      BigDecimal price = new BigDecimal(100);
      BigDecimal qty = new BigDecimal(1);
      if (order.getPrice().compareTo(price) > 0){
        price = order.getPrice().subtract(price);
      }
      if (order.getLeavesQty().compareTo(qty) > 0){
        qty = order.getLeavesQty().subtract(qty);
      }
      order.setLeavesQty(price);
      order.setPrice(qty);
      blackhole.consume(ob.update(order));
      blackhole.consume(ob.buildMarketData());
      blackhole.consume(ob.remove(orderId));
      blackhole.consume(ob.buildMarketData());
    }
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