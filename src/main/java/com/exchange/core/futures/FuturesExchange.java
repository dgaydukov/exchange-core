package com.exchange.core.futures;

import com.exchange.core.futures.model.MarkPrice;
import java.math.BigDecimal;

public class FuturesExchange {

  public static void main(String[] args) {
    MarkPrice markPrice = new MarkPrice(new BigDecimal("10000"), 100);
  }
}
