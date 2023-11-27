package com.exchange.core.matching.orderbook.array;

import com.exchange.core.MockData;
import com.exchange.core.exceptions.AppException;
import com.exchange.core.model.msg.Order;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;

public class PriceLevelTest {

  @Test
  public void priceMismatchErrorTest() {
    Order buy = MockData.getLimitBuy();
    PriceLevel level = new PriceLevel(buy);
    buy.setPrice(new BigDecimal("200"));
    AppException lock = Assertions.assertThrows(AppException.class,
        () -> level.addOrder(buy), "Exception should be thrown");
    Assertions.assertEquals(lock.getMessage(), "Fail to add order: price mismatch");
  }

  @Test
  public void addOrderTest() {
    Order buy = MockData.getLimitBuy();
    PriceLevel level = new PriceLevel(buy);
    for (int i = 0; i < 5; i++) {
      level.addOrder(buy);
    }
    Assertions.assertEquals(6, level.getOrders().size(), "Should be 6 iterations");
  }
}
