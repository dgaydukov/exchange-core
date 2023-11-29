package com.exchange.core.config;

import java.math.BigDecimal;

public interface AppConstants {

  int DEFAULT_DEPTH = 20;
  int ROUNDING_SCALE = 8;
  String FUTURES_SETTLE_ASSET = "USDT";
  BigDecimal MARGIN_LIQUIDATION_TRIGGER = new BigDecimal("0.4");
}
