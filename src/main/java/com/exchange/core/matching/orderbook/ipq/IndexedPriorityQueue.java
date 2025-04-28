package com.exchange.core.matching.orderbook.ipq;

public interface IndexedPriorityQueue<K, V> {
    void offer(K key, V value);

    V poll();

    int size();

    V get(K key);

    V getNearest(K key);
}