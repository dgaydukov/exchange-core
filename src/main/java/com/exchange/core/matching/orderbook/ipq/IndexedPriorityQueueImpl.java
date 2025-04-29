package com.exchange.core.matching.orderbook.ipq;

import java.util.Comparator;
import java.util.NavigableMap;
import java.util.TreeMap;

public class IndexedPriorityQueueImpl<K extends Comparable<K>, V> implements IndexedPriorityQueue<K, V> {
    private int size;
    private int capacity;
    private final int growSize;
    private final SortOrder sortOrder;
    private final NavigableMap<K, V> map;

    public IndexedPriorityQueueImpl (SortOrder sortOrder){
        this(sortOrder, 1000, 100);
    }
    public IndexedPriorityQueueImpl(SortOrder sortOrder, int capacity, int growSize){
        this.capacity = capacity;
        this.growSize = growSize;
        this.sortOrder = sortOrder;
        Comparator<K> comparator = sortOrder == SortOrder.ASC ? Comparator.naturalOrder() : Comparator.reverseOrder();
        map = new TreeMap<>(comparator);
    }

    @Override
    public void offer(K key, V value) {
        map.put(key, value);
    }

    @Override
    public V poll() {
        if (map.size() == 0){
            return null;
        }
        return map.pollFirstEntry().getValue();
    }

    @Override
    public int size() {
        return map.size();
    }

    @Override
    public V getExact(K key) {
        return map.get(key);
    }

    @Override
    public V getNearestLeft(K key) {
        return map.get(getNearestLeftKey(key));
    }

    private K getNearestLeftKey(K key){
        K before = map.floorKey(key);
        K after = map.ceilingKey(key);
        if (before == null) return after;
        if (after == null) return before;
        return before.compareTo(after) > 0 ? before : after;
    }
}
