package com.exchange.core.matching.orderbook.ipq;

import java.util.Arrays;

/**
 * Use this as base case for IndexedPriorityQueue
 * https://algs4.cs.princeton.edu/24pq/
 */
public class IntIndexedPriorityQueue<V> implements IndexedPriorityQueue<Integer, V> {
    private final int maxPrice;
    private final int growSize;
    private final SortOrder sortOrder;
    private int[] pq;
    private V[] map;

    private int size;
    private int iterationIndex;


    public IntIndexedPriorityQueue(SortOrder sortOrder, int capacity, int growSize, int maxPrice){
        map = (V[]) new Object[maxPrice + 1];
        pq = new int[capacity];
        this.growSize = growSize;
        this.maxPrice = maxPrice;
        this.sortOrder = sortOrder;
    }

    private void grow(){
        pq = Arrays.copyOf(pq, pq.length + growSize);
    }

    private void swim(int k) {
        while (k > 1 && compare(k/2, k)) {
            swap(k/2, k);
            k = k/2;
        }
    }

    private void sink(int k) {
        while (2*k <= size) {
            int j = 2*k;
            if (j < size && compare(j, j+1)) j++;
            if (!compare(k, j)) break;
            swap(k, j);
            k = j;
        }
    }

    private boolean compare(int i, int j) {
        return sortOrder == SortOrder.ASC ? pq[i] > pq[j] : pq[i] < pq[j];
    }

    private void swap(int i, int j) {
        int swap = pq[i];
        pq[i] = pq[j];
        pq[j] = swap;
    }


    @Override
    public boolean offer(Integer key, V value) {
        if (key > maxPrice){
            throw new RuntimeException("MaxPrice exceeded");
        }
        map[key] = value;
        // add to PQ
        if (pq.length - 1 == size){
            grow();
        }
        pq[++size] = key;
        swim(size);
        return true;
    }

    @Override
    public V poll() {
        if (size == 0){
            throw new RuntimeException("Queue is empty");
        }
        int max = pq[1];
        V value = map[max];
        swap(1, size--);
        sink(1);
        pq[size+1] = 0;
        map[max] = null;
        return value;

    }

    @Override
    public V peek() {
        return map[pq[1]];
    }

    @Override
    public int size() {
        return size;
    }

    @Override
    public V getExact(Integer key) {
        return map[key];
    }


    @Override
    public void resetIterator() {
        iterationIndex = 1;
    }

    @Override
    public boolean hasNext() {
        return iterationIndex <= size;
    }

    @Override
    public V next() {
        return map[pq[iterationIndex++]];
    }

    @Override
    public void remove() {
        size--;
        sink(iterationIndex);
    }
}
