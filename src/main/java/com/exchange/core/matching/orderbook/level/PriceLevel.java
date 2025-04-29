package com.exchange.core.matching.orderbook.level;

import com.exchange.core.model.msg.Order;
import java.math.BigDecimal;

/**
 * Holder for list of orders for particular price
 */
public interface PriceLevel {
    /**
     * Get level price
     * @return price - current price for all orders for this particular level
     */
    BigDecimal getPrice();

    void add(Order order);

    /**
     * Remove current Order from the list of orders
     */
    void remove();

    /**
     * remove order with O(1)
     * This should be used outside iteration to avoid ConcurrentModificationException
     * Only use if you want to remove some order without any iteration
     */
    void remove(Order order);

    /**
     * Check if there is next order in the sequence
     * @return true - if next order exist in iteration
     */
    boolean hasNext();

    /**
     * Get current order inside current iteration
     * Can throw NPE, so make sure to check if next element exists before calling it
     * @return next value in iteration, null if no next value
     */
    Order next();

    /**
     * reset iterator for hasNext/next method. Always call it before you want to iterate over the list of orders
     */
    void resetIterator();
}