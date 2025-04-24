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
      resetIterator();
    } else {
      last.prev = last;
      last.prev.next = order;
    }
    last = order;
  }

  @Override
  public void remove() {
    if (order == null){
      return;
    }
    Order prev = order.prev;
    Order next = order.next;
    if (prev == null){
      first = null;
      resetIterator();
    } else {
      prev.next = next;
      if (next != null){
        next.prev = prev;
      }
    }
  }

  @Override
  public Order next() {
    Order current = order;
    order = order.next;
    return current;
  }

  @Override
  public boolean hasNext() {
    return order != null;
  }

  @Override
  public void resetIterator() {
    order = first;
  }
}