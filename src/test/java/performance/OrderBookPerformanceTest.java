package performance;

import com.exchange.core.MockData;
import com.exchange.core.matching.orderbook.OrderBook;
import com.exchange.core.matching.orderbook.book.ArrayOrderBook;
import com.exchange.core.matching.orderbook.book.IpqOrderBook;
import com.exchange.core.matching.orderbook.book.LinkedListOrderBook;
import com.exchange.core.matching.orderbook.book.MapOrderBook;
import com.exchange.core.model.Trade;
import org.openjdk.jmh.annotations.*;
import org.openjdk.jmh.infra.Blackhole;
import org.openjdk.jmh.runner.Runner;
import org.openjdk.jmh.runner.RunnerException;
import org.openjdk.jmh.runner.options.Options;
import org.openjdk.jmh.runner.options.OptionsBuilder;

import java.util.List;
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
    arrayOrderBook = new ArrayOrderBook(MockData.SYMBOL, 100_000);
    ipqOrderBook = new IpqOrderBook(MockData.SYMBOL);
    linkedListOrderBook = new LinkedListOrderBook(MockData.SYMBOL);
    mapOrderBook = new MapOrderBook(MockData.SYMBOL);
  }

  @Benchmark
  public void measureArrayOrderBook(Blackhole blackhole) {
    arrayOrderBook.add(RandomOrder.buyLimitUser1());
    List<Trade> trades = arrayOrderBook.match(RandomOrder.sellLimitUser2());
    blackhole.consume(trades);
    blackhole.consume(arrayOrderBook.buildMarketData());
  }

  @Benchmark
  public void measureIpqOrderBook(Blackhole blackhole) {
    ipqOrderBook.add(RandomOrder.buyLimitUser1());
    List<Trade> trades = ipqOrderBook.match(RandomOrder.sellLimitUser2());
    blackhole.consume(trades);
    blackhole.consume(ipqOrderBook.buildMarketData());
  }

  @Benchmark
  public void measureLinkedListOrderBook(Blackhole blackhole) {
    linkedListOrderBook.add(RandomOrder.buyLimitUser1());
    List<Trade> trades = linkedListOrderBook.match(RandomOrder.sellLimitUser2());
    blackhole.consume(trades);
    blackhole.consume(linkedListOrderBook.buildMarketData());
  }

  @Benchmark
  public void measureMapOrderBook(Blackhole blackhole) {
    mapOrderBook.add(RandomOrder.buyLimitUser1());
    List<Trade> trades = mapOrderBook.match(RandomOrder.sellLimitUser2());
    blackhole.consume(trades);
    blackhole.consume(mapOrderBook.buildMarketData());
  }
}