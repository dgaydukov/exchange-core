package com.exchange.core.matching.orderbook.ipq;

import java.util.Arrays;

public class IntIndexedPriorityQueue<V> implements IndexedPriorityQueue<Integer, V> {
    private final int maxPrice;
    private final int growSize;
    private V[] pq;
    private V[] map;
    private int size;

    public IntIndexedPriorityQueue(SortOrder sortOrder, int maxPrice){
        this(sortOrder, 1000, 100, maxPrice);
    }

    public IntIndexedPriorityQueue(SortOrder sortOrder, int capacity, int growSize, int maxPrice){
        pq = (V[]) new Object[capacity];
        map = (V[]) new Object[maxPrice];
        this.growSize = growSize;
        this.maxPrice = maxPrice;
    }

    private void grow(){
        pq = Arrays.copyOf(pq, pq.length + growSize);
    }


    @Override
    public boolean offer(Integer key, V value) {
        if (key > maxPrice){
            throw new RuntimeException("MaxPrice exceeded");
        }
        map[maxPrice] = value;
        return false;
    }

    @Override
    public V poll() {
        return null;
    }

    @Override
    public V peek() {
        return null;
    }

    @Override
    public int size() {
        return size;
    }

    @Override
    public V getExact(Integer key) {
        return null;
    }

    @Override
    public V getNearestLeft(Integer key) {
        return null;
    }

    @Override
    public void resetIterator() {

    }

    @Override
    public boolean hasNext() {
        return false;
    }

    @Override
    public V next() {
        return null;
    }

    @Override
    public void remove() {

    }
}
