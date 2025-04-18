package com.exchange.core.matching.orderbook.array;

import com.exchange.core.MockData;
import com.exchange.core.exceptions.AppException;
import com.exchange.core.model.msg.Order;
import java.math.BigDecimal;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

public class PriceLevelTest {

  @Test
  public void priceMismatchErrorTest() {
    Order buy = MockData.getLimitBuy();
    PriceLevelImpl level = new PriceLevelImpl(buy);
    buy.setPrice(new BigDecimal("200"));
    AppException lock = Assertions.assertThrows(AppException.class,
        () -> level.add(buy), "Exception should be thrown");
    Assertions.assertEquals(lock.getMessage(), "Fail to add order: price mismatch");
  }

  @Test
  public void addOrderTest() {
    Order buy = MockData.getLimitBuy();
    PriceLevelImpl level = new PriceLevelImpl(buy);
    for (int i = 0; i < 5; i++) {
      level.add(buy);
    }
    Assertions.assertEquals(6, level.getOrders(), "Should be 6 iterations");
  }
}
