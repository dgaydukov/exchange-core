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
     * Check if there is next order in the sequence
     * @return true - if next order exist in iteration
     */
    boolean hasNext();

    /**
     * Get current order inside current iteration
     * Can throw NPE, so make sure to check if next element exists before calling it
     * @return order - current iteration order
     */
    Order next();

    /**
     * reset iterator for hasNext/next method. Always call it before you want to iterate over the list of orders
     */
    void resetIterator();
}