package com.exchange.core.performance;

import com.exchange.core.matching.orderbook.MapOrderBook;
import com.exchange.core.matching.orderbook.OrderBook;
import com.exchange.core.model.enums.OrderSide;
import com.exchange.core.model.enums.OrderType;
import com.exchange.core.model.msg.Order;
import java.math.BigDecimal;
import org.junit.jupiter.api.Test;

public class MatchingEnginePerformanceTest {

  private final static String SYMBOL = "BTC/USDT";

  @Test
  public void mapOrderBookTest(){
    OrderBook ob = new MapOrderBook(SYMBOL);
  }



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