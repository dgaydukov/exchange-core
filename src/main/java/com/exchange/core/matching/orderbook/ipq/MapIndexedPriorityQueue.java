package com.exchange.core.matching.orderbook.ipq;

import java.util.Comparator;
import java.util.Iterator;
import java.util.NavigableMap;
import java.util.TreeMap;

/**
 * This is Map-based implementation that underneath using TreeMap as primary data structure
 * This would work from business logic (you can check tests), but it's not best for low-latency.
 * Ideally you should rewrite to use raw array as underlying data structure for quick access
 *
 * @param <K> - key
 * @param <V> - value
 */
public class MapIndexedPriorityQueue<K extends Comparable<K>, V> implements IndexedPriorityQueue<K, V> {
    private int size;
    private int capacity;
    private final int growSize;
    private final SortOrder sortOrder;
    private final NavigableMap<K, V> map;

    public MapIndexedPriorityQueue(SortOrder sortOrder, int capacity, int growSize){
        this.capacity = capacity;
        this.growSize = growSize;
        this.sortOrder = sortOrder;
        Comparator<K> comparator = sortOrder == SortOrder.ASC ? Comparator.naturalOrder() : Comparator.reverseOrder();
        map = new TreeMap<>(comparator);
    }

    @Override
    public boolean offer(K key, V value) {
        map.put(key, value);
        return true;
    }

    @Override
    public V poll() {
        if (map.isEmpty()){
            return null;
        }
        return map.pollFirstEntry().getValue();
    }

    @Override
    public V peek() {
        if (map.isEmpty()){
            return null;
        }
        return map.firstEntry().getValue();
    }

    @Override
    public int size() {
        return map.size();
    }

    @Override
    public V getExact(K key) {
        return map.get(key);
    }

    private Iterator<V> iterator;
    @Override
    public void resetIterator() {
        iterator = map.values().iterator();
    }

    @Override
    public boolean hasNext() {
        return iterator.hasNext();
    }

    @Override
    public V next() {
        if (!iterator.hasNext()){
            return null;
        }
        return iterator.next();
    }

    @Override
    public void remove() {
        iterator.remove();
    }
}
