package com.exchange.core.matching.counter;

import java.util.concurrent.atomic.AtomicLong;

/**
 * Since we are building single-threaded matching, where all orders from all pairs would be handled
 * by single thread simple counters would do the job. If we plan to split our matching thread into
 * several thread, then atomic counter should be implemented
 */
public class AtomicGlobalCounter implements GlobalCounter {

  private final AtomicLong orderId = new AtomicLong();
  private final AtomicLong executionId = new AtomicLong();

  @Override
  public long getNextOrderId() {
    return orderId.incrementAndGet();
  }

  @Override
  public long getNextExecutionId() {
    return executionId.incrementAndGet();
  }
}