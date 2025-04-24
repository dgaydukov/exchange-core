package com.exchange.core.matching.orderbook.array;

import com.exchange.core.exceptions.AppException;
import com.exchange.core.model.msg.Order;

import java.math.BigDecimal;

public class LinkedListPriceLevel implements PriceLevel{
  private final BigDecimal price;

  // keep track of first order
  private Order first;
  // always add items to the last
  private Order last;
  // current order used in iteration
  private Order order;

  public LinkedListPriceLevel(Order order){
    if (order == null){
      throw new AppException("Fail to add order: order is null");
    }
    price = order.getPrice();
    add(order);
  }

  @Override
  public BigDecimal getPrice() {
    return price;
  }

  @Override
  public void add(Order order) {
    if (order == null){
      throw new AppException("Fail to add order: order is null");
    }
    if (price.compareTo(order.getPrice()) != 0) {
      throw new AppException("Fail to add order: price mismatch");
    }
    // add to the last
    if (first == null){
      first = order;
      last = order;
      resetIterator();
    } else {
      
    }
  }

  @Override
  public void remove() {
    Order prev = order.prev;
    Order next = order.next;
    if (prev == null){
      first = null;
      resetIterator();
    } else {
      prev.next = next;
    }
  }

  @Override
  public Order next() {
    order = order.next;
    return order;
  }

  @Override
  public boolean hasNext() {
    return order.next != null;
  }

  @Override
  public void resetIterator() {
    order = first;
  }
}