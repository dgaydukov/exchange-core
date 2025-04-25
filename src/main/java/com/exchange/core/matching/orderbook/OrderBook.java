package com.exchange.core.matching.orderbook;

import com.exchange.core.model.Trade;
import com.exchange.core.model.msg.MarketData;
import com.exchange.core.model.msg.Order;
import java.util.List;

/**
 * Base interface to design order book
 * Include 3 main methods to work with orders: add/update/cancel
 */
public interface OrderBook {

  /**
   * Match order against current order book (if it's buy we match against asks, if it's sell - against bids)
   * @param order - taker order to be matched against current order book
   * @return list of trades produced by matching
   */
  List<Trade> match(Order order);

  /**
   * Add order directly into order book
   * @param  order - order object
   * @return true if order was added, false - if order was rejected by the system
   */
  boolean add(Order order);

  /**
   * Modify existing order
   * @param order - order object
   * @return true if modification was successful, false - if order already matched or doesn't exist
   */
  boolean update(Order order);

  /**
   * remove order by orderId from order book
   * @param orderId - unique system-generated ID of type long
   * @return true if order successfully cancelled, false - if order not found
   */
  boolean remove(long orderId);

  /**
   * Get current market data
   * @return MarketData object
   */
  MarketData buildMarketData();
}