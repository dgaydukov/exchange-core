package com.exchange.core.matching.counter;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class GlobalCounterTest {

  @Test
  public void simpleCounterTest() {
    GlobalCounter counter = new SimpleGlobalCounter();
    Assertions.assertEquals(1, counter.getNextOrderId());
    Assertions.assertEquals(1, counter.getNextExecutionId());
    for (int i = 0; i < 10; i++) {
      counter.getNextOrderId();
    }
    Assertions.assertEquals(12, counter.getNextOrderId());
    Assertions.assertEquals(2, counter.getNextExecutionId());
  }

  @Test
  public void atomicCounterTest() throws InterruptedException {
    GlobalCounter counter = new AtomicGlobalCounter();
    int numberOfThreads = 10;
    ExecutorService service = Executors.newFixedThreadPool(numberOfThreads);
    CountDownLatch latch = new CountDownLatch(numberOfThreads);
    for (int i = 0; i < numberOfThreads; i++) {
      service.submit(() -> {
        counter.getNextOrderId();
        latch.countDown();
      });
    }
    latch.await();
    Assertions.assertEquals(numberOfThreads + 1, counter.getNextOrderId());
  }
}
