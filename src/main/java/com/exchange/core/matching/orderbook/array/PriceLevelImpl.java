package com.exchange.core.matching.orderbook.array;

import com.exchange.core.exceptions.AppException;
import com.exchange.core.model.msg.Order;
import java.math.BigDecimal;
import java.util.LinkedList;
import java.util.List;

public class PriceLevelImpl implements PriceLevel{
  private final BigDecimal price;
  private final List<Order> orders;
  private int index;

  public PriceLevelImpl(Order order){
    if (order == null){
      throw new AppException("Fail to add order: order is null");
    }
    price = order.getPrice();
    orders = new LinkedList<>();
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
    orders.add(order);
  }

  @Override
  public void remove() {
    orders.remove(index-1);
    index--;
  }

  @Override
  public Order next() {
    return orders.get(index++);
  }

  @Override
  public boolean hasNext() {
    return index < orders.size();
  }

  @Override
  public void resetIterator() {
    index = 0;
  }
}