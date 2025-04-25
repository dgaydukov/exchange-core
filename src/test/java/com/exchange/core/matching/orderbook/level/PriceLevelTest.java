package com.exchange.core.matching.orderbook.level;

import com.exchange.core.MockData;
import com.exchange.core.exceptions.AppException;
import com.exchange.core.model.msg.Order;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.ConcurrentModificationException;
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
  public void nextNLPException(){
    Order buy = MockData.getLimitBuy();
    PriceLevel level = new LinkedListPriceLevel(buy);
    Assertions.assertEquals(buy, level.next(), "next mismatch");
    Assertions.assertThrows(NullPointerException.class, level::next);
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
  public void validateInsertionOrderTest() {
    Order first = MockData.getLimitBuy();
    first.setOrderId(1);
    PriceLevel level = new LinkedListPriceLevel(first);
    Order second = MockData.getLimitBuy();
    second.setOrderId(2);
    level.add(second);
    Order third = MockData.getLimitBuy();
    third.setOrderId(3);
    level.add(third);
    Order fourth = MockData.getLimitBuy();
    fourth.setOrderId(4);
    level.add(fourth);

    Assertions.assertNull(first.prev, "first.prev mismatch");
    Assertions.assertEquals(second, first.next, "first.next mismatch");
    Assertions.assertEquals(first, second.prev, "second.prev mismatch");
    Assertions.assertEquals(third, second.next, "second.next mismatch");
    Assertions.assertEquals(second, third.prev, "third.prev mismatch");
    Assertions.assertEquals(fourth, third.next, "third.next mismatch");
    Assertions.assertEquals(third, fourth.prev, "fourth.prev mismatch");
    Assertions.assertNull(fourth.next, "fourth.next mismatch");
  }

  @Test
  public void getNextTest() {
    Order first = MockData.getLimitBuy();
    first.setOrderId(1);
    PriceLevel level = new LinkedListPriceLevel(first);
    Order second = MockData.getLimitBuy();
    second.setOrderId(2);
    level.add(second);
    Order third = MockData.getLimitBuy();
    third.setOrderId(3);
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
    first.setOrderId(1);
    PriceLevel level = new LinkedListPriceLevel(first);
    Order second = MockData.getLimitBuy();
    second.setOrderId(2);
    level.add(second);
    Order third = MockData.getLimitBuy();
    third.setOrderId(3);
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

  @Test
  public void removeRandomOrderTest(){
    Order first = MockData.getLimitBuy();
    first.setOrderId(1);
    PriceLevel level = new LinkedListPriceLevel(first);
    Order second = MockData.getLimitBuy();
    second.setOrderId(2);
    level.add(second);
    Order third = MockData.getLimitBuy();
    third.setOrderId(3);
    level.add(third);

    Assertions.assertTrue(level.hasNext(), "should have next");
    level.remove(third);
    level.remove(second);
    level.remove(first);
    Assertions.assertFalse(level.hasNext(), "should not have next");
  }

  @Test
  public void removeAndIterateTest(){
    Order first = MockData.getLimitBuy();
    first.setOrderId(1);
    PriceLevel level = new LinkedListPriceLevel(first);
    Order second = MockData.getLimitBuy();
    second.setOrderId(2);
    level.add(second);
    Order third = MockData.getLimitBuy();
    third.setOrderId(3);
    level.add(third);

    level.resetIterator();
    Assertions.assertEquals(first, level.next(), "first order mismatch");
    ConcurrentModificationException ex = Assertions.assertThrows(ConcurrentModificationException.class, () -> {
      level.remove(second);
    });
    Assertions.assertEquals("You can't remove by object during iteration", ex.getMessage(), "error message mismatch");

  }
}