package com.exchange.core;

import java.math.BigDecimal;
import java.math.RoundingMode;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

public class PrecisionLossTest {

  @Test
  public void bigDecimalDivisionTest() {
    BigDecimal amount = new BigDecimal("111");
    BigDecimal price = new BigDecimal("219");
    BigDecimal tradeQty = amount.divide(price, 16, RoundingMode.HALF_EVEN);
    BigDecimal tradeAmount = tradeQty.multiply(price);
    System.out.println("tradeQty=" + tradeQty + ", tradeAmount=" + tradeAmount);
    Assertions.assertEquals(new BigDecimal("0.5068493150684932"), tradeQty, "tradeQty mismatch");
    Assertions.assertNotEquals(amount, tradeAmount, "values shouldn't match");
  }

  @Test
  public void doubleDivisionTest() {
    double amount = 111;
    double price = 219;
    double tradeQty = amount / price;
    double tradeAmount = tradeQty * price;
    System.out.println("tradeQty=" + tradeQty + ", tradeAmount=" + tradeAmount);
    Assertions.assertEquals(0.5068493150684932, tradeQty, "tradeQty mismatch");
    Assertions.assertEquals(amount, tradeAmount, "values shouldn't match");
  }
}
