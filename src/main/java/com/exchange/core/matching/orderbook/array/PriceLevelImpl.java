package com.exchange.core.matching.orderbook.array;

import com.exchange.core.exceptions.AppException;
import com.exchange.core.model.msg.Order;
import java.math.BigDecimal;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;

public class PriceLevelImpl implements PriceLevel{
  private final BigDecimal price;
  private final List<Order> orders;

  public PriceLevelImpl(Order order){
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
    if (price.compareTo(order.getPrice()) != 0) {
      throw new AppException("Fail to add order: price mismatch");
    }
    orders.add(order);
  }

  @Override
  public boolean remove(Order order) {
    return orders.remove(order);
  }

  @Override
  public Order getFirst() {
    return orders.isEmpty() ? null : orders.get(0);
  }

  @Override
  public Iterator<Order> getOrders() {
    return orders.iterator();
  }
}