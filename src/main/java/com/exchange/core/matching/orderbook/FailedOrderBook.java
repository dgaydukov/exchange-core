package com.exchange.core.matching.orderbook;

import com.exchange.core.matching.orderchecks.PostOrderCheckImpl;
import com.exchange.core.matching.orderchecks.PreOrderCheck;
import com.exchange.core.model.Trade;
import com.exchange.core.model.msg.MarketData;
import com.exchange.core.model.msg.Order;
import java.util.List;

public class FailedOrderBook implements OrderBook {

  private final PreOrderCheck preOrderCheck;
  private final PostOrderCheckImpl postOrderCheck;

  public FailedOrderBook(PreOrderCheck preOrderCheck, PostOrderCheckImpl postOrderCheck) {
    this.preOrderCheck = preOrderCheck;
    this.postOrderCheck = postOrderCheck;
  }

  @Override
  public List<Trade> match(Order order) {
    return null;
  }

  @Override
  public void add(Order order) {

  }

  @Override
  public MarketData buildMarketData() {
    return null;
  }
}