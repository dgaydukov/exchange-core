package performance;

import com.exchange.core.MockData;
import com.exchange.core.matching.orderbook.book.MapOrderBook;
import com.exchange.core.matching.orderbook.OrderBook;
import com.exchange.core.matching.orderbook.book.ArrayOrderBook;
import com.exchange.core.model.Trade;
import java.util.List;
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
public class OrderBookPerformanceTest {

  private OrderBook arrayOrderBook;
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
    mapOrderBook = new MapOrderBook(MockData.SYMBOL);
    arrayOrderBook = new ArrayOrderBook(MockData.SYMBOL);
  }

  @Benchmark
  public void measureMapOrderBook(Blackhole blackhole) {
    mapOrderBook.add(RandomOrder.buyLimitUser1());
    List<Trade> trades = mapOrderBook.match(RandomOrder.sellLimitUser2());
    blackhole.consume(trades);
    blackhole.consume(mapOrderBook.buildMarketData());
  }

  @Benchmark
  public void measureArrayOrderBook(Blackhole blackhole) {
    arrayOrderBook.add(RandomOrder.buyLimitUser1());
    List<Trade> trades = arrayOrderBook.match(RandomOrder.sellLimitUser2());
    blackhole.consume(trades);
    blackhole.consume(arrayOrderBook.buildMarketData());
  }
}