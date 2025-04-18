package com.exchange.core.matching.orderbook.array;

import com.exchange.core.exceptions.AppException;
import com.exchange.core.model.msg.Order;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import lombok.Data;
import lombok.Getter;

@Data
public class PriceLevelImpl {

  @Getter
  private BigDecimal price;
  private List<Order> orders;
  private int index;

  public PriceLevelImpl(Order order) {
    price = order.getPrice();
    orders = new ArrayList<>();
    addOrder(order);
  }

  public void addOrder(Order order) {
    if (price.compareTo(order.getPrice()) != 0) {
      throw new AppException("Fail to add order: price mismatch");
    }
    orders.add(order);
  }

  public List<Order> getOrders() {
    return orders;
  }
}