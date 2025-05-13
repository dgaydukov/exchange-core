package com.exchange.core.matching.orderbook.ipq;

import java.util.Arrays;

public class IntIndexedPriorityQueue<V> implements IndexedPriorityQueue<Integer, V> {
    private final int maxPrice;
    private final int growSize;
    private final SortOrder sortOrder;
    private V[] pq;
    private V[] map;
    private int size;
    private int iterationIndex;


    public IntIndexedPriorityQueue(SortOrder sortOrder, int capacity, int growSize, int maxPrice){
        pq = (V[]) new Object[capacity];
        map = (V[]) new Object[maxPrice];
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
        if (sortOrder == SortOrder.ASC){
            return i < j;
        }
        return i > j;
    }

    private void swap(int i, int j) {
        V swap = pq[i];
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
        pq[++size] = value;
        swim(size);
        return true;
    }

    @Override
    public V poll() {
        if (size == 0){
            throw new RuntimeException("Queue is empty");
        }
        V max = pq[1];
        swap(1, size--);
        sink(1);
        pq[size+1] = null;
        return max;
    }

    @Override
    public V peek() {
        return pq[1];
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
        return iterationIndex < size;
    }

    @Override
    public V next() {
        return pq[iterationIndex++];
    }

    @Override
    public void remove() {
        size--;
        sink(iterationIndex);
    }
}
