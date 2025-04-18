package com.exchange.core.matching.orderbook.array;

import com.exchange.core.MockData;
import com.exchange.core.exceptions.AppException;
import com.exchange.core.model.msg.Order;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

public class PriceLevelTest {

  @Test
  public void priceMismatchErrorTest() {
    Order buy = MockData.getLimitBuy();
    PriceLevel level = new PriceLevelImpl(buy);
    buy.setPrice(new BigDecimal("200"));
    AppException lock = Assertions.assertThrows(AppException.class,
            () -> level.add(buy), "Exception should be thrown");
    Assertions.assertEquals(lock.getMessage(), "Fail to add order: price mismatch");
  }

  @Test
  public void addOrderTest() {
    List<Order> expected = new ArrayList<>();
    Order buy = MockData.getLimitBuy();
    expected.add(buy);
    PriceLevel level = new PriceLevelImpl(buy);
    for (int i = 0; i < 5; i++) {
      Order _buy = MockData.getLimitBuy();
      _buy.setQuoteOrderQty(new BigDecimal(i));
      expected.add(_buy);
      level.add(_buy);
    }
    List<Order> orders = new ArrayList<>();
    Iterator<Order> iterator = level.getOrders();
    while (iterator.hasNext()) {
      orders.add(iterator.next());
    }
    Assertions.assertEquals(6, orders.size(), "Should be 6 iterations");
    Assertions.assertIterableEquals(expected, orders, "level order mismatch");
  }



  @Test
  public void getFirstAndRemove(){
    Order first = MockData.getLimitBuy();
    PriceLevel level = new PriceLevelImpl(first);
    Order second = MockData.getLimitBuy();
    second.setQuoteOrderQty(new BigDecimal("20"));
    level.add(second);
    Order third = MockData.getLimitBuy();
    third.setQuoteOrderQty(new BigDecimal("30"));
    level.add(third);

    Assertions.assertEquals(first, level.getFirst(), "first order mismatch");
    Assertions.assertTrue(level.remove(first), "first order should be removed successfully");
    Assertions.assertFalse(level.remove(first), "first order already removed");
    Assertions.assertEquals(second, level.getFirst(), "second order should be removed");
    Assertions.assertTrue(level.remove(second), "second order should be removed successfully");
    Assertions.assertEquals(third, level.getFirst(), "third order mismatch");
    Assertions.assertTrue(level.remove(third), "third order should be removed successfully");

    Iterator<Order> iterator = level.getOrders();
    Assertions.assertFalse(iterator.hasNext(), "Iterator should be empty");
  }
}