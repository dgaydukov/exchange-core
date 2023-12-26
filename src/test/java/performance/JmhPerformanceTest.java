package performance;

import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicLong;
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
import org.openjdk.jmh.annotations.TearDown;
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
public class JmhPerformanceTest {

  private AtomicLong atomic = new AtomicLong();

  public static void main(String[] args) throws RunnerException {
    Options opt = new OptionsBuilder()
        .include(JmhPerformanceTest.class.getSimpleName())
        .forks(1)
        .build();
    new Runner(opt).run();
  }

  @Setup(Level.Iteration)
  public void setUp() {
    System.out.println("setup");
    atomic.set(0);
  }

  @TearDown(Level.Iteration)
  public void tearDown() {
    System.out.println("tearDown => " + atomic.get());
  }

  /**
   * This is example test to show how JMH works and what it doing under the hood
   */
  @Benchmark
  public void latency(Blackhole blackhole) {
    atomic.incrementAndGet();
  }
}