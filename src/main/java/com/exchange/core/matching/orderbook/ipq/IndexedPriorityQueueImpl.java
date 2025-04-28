package com.exchange.core.matching.orderbook.ipq;

import java.util.TreeMap;

public class IndexedPriorityQueueImpl<K extends Comparable<K>, V> implements IndexedPriorityQueue<K, V> {


    public IndexedPriorityQueueImpl (){
        this(SortOrder.ASC);
    }
    public IndexedPriorityQueueImpl (SortOrder sortOrder){
        this(sortOrder, 1000, 100);
    }
    public IndexedPriorityQueueImpl(SortOrder sortOrder, int capacity, int growSize){
    }

    @Override
    public void offer(K key, V value) {

    }

    @Override
    public V poll() {
        return null;
    }

    @Override
    public int size() {
        return 0;
    }

    @Override
    public V getExact(K key) {
        return null;
    }

    @Override
    public V getNearestLeft(K key) {
        return null;
    }
}
