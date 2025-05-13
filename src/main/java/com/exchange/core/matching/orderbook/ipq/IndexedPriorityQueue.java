package com.exchange.core.matching.orderbook.ipq;

/**
 * Interface for indexed priority queue
 * Designed to store a list of entries with format <Price, PriceLevel>, where we can fetch bestBid/bestAsk,
 * because it's priority queue and the best values would be polled first. Also you can fetch the PriceLevel to which you append your order
 *
 * @param <K> - key value, would be stored price
 * @param <V> - value, would store PriceLevel
 */
public interface IndexedPriorityQueue<K extends Comparable<K>, V> {
    /**
     * Add new element into queue, It would sort automatically
     * @param key - key
     * @param value - value
     *
     * @return true - if element added to queue, false - if failed to add
     */
    boolean offer(K key, V value);

    /**
     * Poll from the top of the queue
     * @return V - value
     */
    V poll();

    /**
     * View, but not remove first element from the queue
     * @return V - value
     */
    V peek();

    /**
     * Get current size of the queue
     * @return int size
     */
    int size();

    /**
     * Fetch exact value by key. Usually you fetch PriceLevel by price.
     * If PriceLevel doesn't exist for such price, null should be returned
     * @param key - key
     * @return V - if value exists for key, null if no such value for key
     */
    V getExact(K key);
    

    /*
     * *****************************ITERATOR**********************************************
     */

    /**
     * Reset iterator sequence and start iterating from the first element
     */
    void resetIterator();

    /**
     *
     * @return true - if next element exists, false - if iteration is over and no new elements
     */
    boolean hasNext();

    /**
     * Get next element in iteration sequence
     * @return next value in iteration, null if no next value
     */
    V next();

    /**
     * remove currently iterating element
     */
    void remove();
}