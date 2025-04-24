package com.exchange.core.matching.orderbook.array;

import com.exchange.core.MockData;
import com.exchange.core.exceptions.AppException;
import com.exchange.core.model.msg.Order;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

public class PriceLevelTest {

  @Test
  public void priceMismatchErrorTest() {
    Order buy = MockData.getLimitBuy();
    PriceLevel level = new LinkedListPriceLevel(buy);
    buy.setPrice(new BigDecimal("200"));
    AppException lock = Assertions.assertThrows(AppException.class,
            () -> level.add(buy), "Exception should be thrown");
    Assertions.assertEquals("Fail to add order: price mismatch", lock.getMessage());
  }

  @Test
  public void nullOrderConstructorErrorTest() {
    AppException lock = Assertions.assertThrows(AppException.class,
            () -> new LinkedListPriceLevel(null), "Exception should be thrown");
    Assertions.assertEquals("Fail to add order: order is null", lock.getMessage());
  }

  @Test
  public void nullOrderErrorTest() {
    Order buy = MockData.getLimitBuy();
    PriceLevel level = new LinkedListPriceLevel(buy);
    AppException lock = Assertions.assertThrows(AppException.class,
            () -> level.add(null), "Exception should be thrown");
    Assertions.assertEquals("Fail to add order: order is null", lock.getMessage());
  }

  @Test
  public void addOrderTest() {
    List<Order> expected = new ArrayList<>();
    Order buy = MockData.getLimitBuy();
    expected.add(buy);
    PriceLevel level = new LinkedListPriceLevel(buy);
    for (int i = 0; i < 5; i++) {
      Order _buy = MockData.getLimitBuy();
      _buy.setQuoteOrderQty(new BigDecimal(i));
      expected.add(_buy);
      level.add(_buy);
    }
    List<Order> orders = new ArrayList<>();
    level.resetIterator();
    while (level.hasNext()) {
      orders.add(level.next());
    }
    Assertions.assertEquals(6, orders.size(), "Should be 6 iterations");
    Assertions.assertIterableEquals(expected, orders, "level order mismatch");
  }

  @Test
  public void getNextTest() {
    Order first = MockData.getLimitBuy();
    PriceLevel level = new LinkedListPriceLevel(first);
    Order second = MockData.getLimitBuy();
    second.setQuoteOrderQty(new BigDecimal("20"));
    level.add(second);
    Order third = MockData.getLimitBuy();
    third.setQuoteOrderQty(new BigDecimal("30"));
    level.add(third);

    Assertions.assertTrue(level.hasNext(), "should have next");
    Assertions.assertEquals(first, level.next(), "first order mismatch");
    Assertions.assertTrue(level.hasNext(), "should have next");
    Assertions.assertEquals(second, level.next(), "second order mismatch");
    Assertions.assertTrue(level.hasNext(), "should have next");
    Assertions.assertEquals(third, level.next(), "third order mismatch");
    Assertions.assertFalse(level.hasNext(), "should not have next");

    // reset and retest
    level.resetIterator();
    Assertions.assertTrue(level.hasNext(), "should have next");
    Assertions.assertEquals(first, level.next(), "first order mismatch");
    Assertions.assertTrue(level.hasNext(), "should have next");
    Assertions.assertEquals(second, level.next(), "second order mismatch");
    Assertions.assertTrue(level.hasNext(), "should have next");
    Assertions.assertEquals(third, level.next(), "third order mismatch");
    Assertions.assertFalse(level.hasNext(), "should not have next");
  }

  @Test
  public void getNextAndRemoveTest() {
    Order first = MockData.getLimitBuy();
    PriceLevel level = new LinkedListPriceLevel(first);
    Order second = MockData.getLimitBuy();
    second.setOrderId(2);
    level.add(second);
    Order third = MockData.getLimitBuy();
    second.setOrderId(3);
    level.add(third);

    Assertions.assertTrue(level.hasNext(), "should have next");
    Assertions.assertEquals(first, level.next(), "first order mismatch");
    level.remove();
    Assertions.assertTrue(level.hasNext(), "should have next");
    Assertions.assertEquals(second, level.next(), "second order mismatch");
    level.remove();
    Assertions.assertTrue(level.hasNext(), "should have next");
    Assertions.assertEquals(third, level.next(), "third order mismatch");
    level.remove();
    Assertions.assertFalse(level.hasNext(), "should not have next");
  }
}