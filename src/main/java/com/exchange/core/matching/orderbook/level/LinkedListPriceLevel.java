package com.exchange.core.matching.orderbook.level;

import com.exchange.core.exceptions.AppException;
import com.exchange.core.model.msg.Order;

import java.math.BigDecimal;
import java.util.ConcurrentModificationException;

/**
 * This class has 2 versions of use: List and Iterator
 * As List you can remove item by object
 * But if you are iterating over it, and remove by object, you may have broken iteration
 * That's why we throw exception, just like Iterator in java
 */
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
    } else{
      last.next = order;
    }
    order.prev = last;
    last = order;
    order.level = this;
  }

  @Override
  public void remove() {
    if (order == null){
      return;
    }
    delete(order);
  }

  @Override
  public void remove(Order o) {
    if (this.order != first){
      throw new ConcurrentModificationException("You can't remove by object during iteration");
    }
    delete(o);
  }

  private void delete(Order o){
    Order prev = o.prev;
    Order next = o.next;

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
    if (!hasNext()){
      return null;
    }
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