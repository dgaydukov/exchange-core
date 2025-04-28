package com.exchange.core.matching.orderbook.ipq;

public interface IndexedPriorityQueue<K extends Comparable<K>, V> {
    void offer(K key, V value);

    V poll();

    int size();

    V getExact(K key);

    V getNearestLeft(K key);
}