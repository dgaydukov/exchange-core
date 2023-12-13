package com.exchange.core;

import com.exchange.core.config.AppConstants;
import java.math.BigDecimal;
import java.math.RoundingMode;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

public class PrecisionLossTest {

  @Test
  public void bigDecimalTest(){
    BigDecimal amount = new BigDecimal("111");
    BigDecimal price = new BigDecimal("219");
    BigDecimal tradeQty = amount.divide(price, AppConstants.ROUNDING_SCALE, RoundingMode.DOWN);
    BigDecimal tradeAmount = tradeQty.multiply(price);
    System.out.println("tradeQty="+tradeQty+", tradeAmount="+tradeAmount);
    Assertions.assertNotEquals(amount, tradeAmount, "values shouldn't match");
  }
}
