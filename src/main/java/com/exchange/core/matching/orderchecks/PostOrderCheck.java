package com.exchange.core.matching.orderchecks;

import com.exchange.core.model.msg.MarketData;
import com.exchange.core.model.msg.Order;

import java.math.BigDecimal;

public interface PostOrderCheck {

  void sendExecReportNew(Order order);

  void sendExecReportTrade(Order taker, Order maker, BigDecimal tradeQty, BigDecimal tradePrice);

  void sendMarketData(MarketData marketData);

  void settleTrade(Order taker, Order maker, BigDecimal tradeQty, BigDecimal tradeAmount);

  void cancelOrder(Order order);
}