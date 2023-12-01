package com.exchange.core.matching.orderbook;

import com.exchange.core.model.enums.OrderSide;
import com.exchange.core.model.enums.OrderType;
import com.exchange.core.model.msg.Order;
import java.math.BigDecimal;

public class OrderBookPerformanceTest {

  private final static String SYMBOL = "BTC/USDT";



  private Order getLimitBuy() {
    Order order = new Order();
    order.setSymbol(SYMBOL);
    order.setType(OrderType.LIMIT);
    order.setSide(OrderSide.BUY);
    order.setPrice(new BigDecimal("100"));
    order.setLeavesQty(new BigDecimal("10"));
    return order;
  }
}