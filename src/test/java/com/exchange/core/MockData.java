package com.exchange.core;

import com.exchange.core.model.enums.OrderSide;
import com.exchange.core.model.enums.OrderType;
import com.exchange.core.model.msg.InstrumentConfig;
import com.exchange.core.model.msg.Order;
import com.exchange.core.model.msg.UserBalance;
import java.math.BigDecimal;

public class MockData {

  public final static String SYMBOL = "BTC/USDT";
  public final static int BUY_ACCOUNT = 1;

  public static InstrumentConfig getInstrument() {
    InstrumentConfig config = new InstrumentConfig();
    config.setSymbol(SYMBOL);
    config.setBase("BTC");
    config.setQuote("USDT");
    return config;
  }

  public static Order getLimitBuy() {
    Order order = new Order();
    order.setSymbol(SYMBOL);
    order.setType(OrderType.LIMIT);
    order.setSide(OrderSide.BUY);
    order.setPrice(new BigDecimal("100"));
    order.setOrderQty(new BigDecimal("10"));
    order.setAccount(BUY_ACCOUNT);
    return order;
  }

  public static UserBalance getUser(String asset) {
    UserBalance user = new UserBalance();
    user.setAccount(BUY_ACCOUNT);
    user.setAsset(asset);
    user.setAmount(new BigDecimal("2000"));
    return user;
  }
}
