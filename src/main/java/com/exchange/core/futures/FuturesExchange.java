package com.exchange.core.futures;

import com.exchange.core.futures.calculators.MarkPriceCalculatorImpl;
import java.math.BigDecimal;

public class FuturesExchange {

  public static void main(String[] args) {
    MarkPriceCalculatorImpl markPriceCalculatorImpl = new MarkPriceCalculatorImpl(new BigDecimal("10000"), 100);
  }
}
