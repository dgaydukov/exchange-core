package com.exchange.core.matching.counter;

/**
 * Since we are building single-threaded matching, where all orders from all pairs would be handled by single thread simple counters would do the job.
 * If we plan to split our matching thread into several thread, then atomic counter should be implemented
 */
public class SimpleGlobalCounter implements GlobalCounter {
    private long orderId;
    private long executionId;

    @Override
    public long getNextOrderId() {
        return ++orderId;
    }

    @Override
    public long getNextExecutionId() {
        return ++executionId;
    }
}