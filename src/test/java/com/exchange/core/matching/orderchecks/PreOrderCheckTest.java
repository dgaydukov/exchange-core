package com.exchange.core.matching.orderchecks;

import com.exchange.core.MockData;
import com.exchange.core.matching.counter.GlobalCounter;
import com.exchange.core.model.enums.OrderType;
import com.exchange.core.model.msg.InstrumentConfig;
import com.exchange.core.model.msg.Message;
import com.exchange.core.model.msg.Order;
import com.exchange.core.repository.AccountRepository;
import com.exchange.core.repository.InstrumentRepository;
import com.exchange.core.user.Account;
import com.exchange.core.user.Position;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.util.Queue;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

public class PreOrderCheckTest {

  @Test
  public void validateOrderTest() {
    GlobalCounter counter = mock(GlobalCounter.class);
    AccountRepository accountRepository = mock(AccountRepository.class);
    InstrumentRepository instrumentRepository = mock(InstrumentRepository.class);
    Queue<Message> outbound = mock(Queue.class);
    PreOrderCheck preCheck = new PreOrderCheckImpl(counter, accountRepository, instrumentRepository,
        outbound);
    Order order = MockData.getLimitBuy();
    Assertions.assertFalse(preCheck.validateOrder(order), "limit order should fail");

    InstrumentConfig config = MockData.getInstrument();
    when(accountRepository.getAccount(order.getAccount())).thenReturn(
        new Account(order.getAccount()));
    when(accountRepository.getAccountPosition(order.getAccount(), config.getQuote()))
        .thenReturn(new Position(config.getQuote(), new BigDecimal("1000")));
    when(instrumentRepository.getInstrument(order.getSymbol())).thenReturn(config);
    Assertions.assertTrue(preCheck.validateOrder(order), "limit order should pass validation");

    Order market = MockData.getLimitBuy();
    market.setType(OrderType.MARKET);
    Assertions.assertFalse(preCheck.validateOrder(market), "market order should fail");
    market.setPrice(null);
    market.setOrderQty(null);
    market.setQuoteOrderQty(new BigDecimal("1000"));
    Assertions.assertTrue(preCheck.validateOrder(market), "market order should pass");
    // amount more than available
    market.setQuoteOrderQty(new BigDecimal("1001"));
    Assertions.assertFalse(preCheck.validateOrder(market), "market should fail cause 1001>1000");
  }

  @Test
  public void updateNewOrderTest() {
    GlobalCounter counter = mock(GlobalCounter.class);
    AccountRepository accountRepository = mock(AccountRepository.class);
    InstrumentRepository instrumentRepository = mock(InstrumentRepository.class);
    Queue<Message> outbound = mock(Queue.class);
    PreOrderCheck preCheck = new PreOrderCheckImpl(counter, accountRepository, instrumentRepository,
        outbound);

    Order buy = MockData.getLimitBuy();
    final long orderId = 100;
    when(counter.getNextOrderId()).thenReturn(orderId);
    preCheck.updateNewOrder(buy);
    Assertions.assertEquals(orderId, buy.getOrderId(), "orderId mismatch");
    Assertions.assertEquals(buy.getOrderQty(), buy.getLeavesQty(), "orderQty mismatch");

    Order market = MockData.getLimitBuy();
    market.setType(OrderType.MARKET);
    market.setQuoteOrderQty(new BigDecimal("1000"));
    preCheck.updateNewOrder(market);
    Assertions.assertEquals(market.getQuoteOrderQty(), market.getLeavesQty(), "orderQty mismatch");
  }

  @Test
  public void lockBalanceTest() {
    GlobalCounter counter = mock(GlobalCounter.class);
    AccountRepository accountRepository = mock(AccountRepository.class);
    InstrumentRepository instrumentRepository = mock(InstrumentRepository.class);
    Queue<Message> outbound = mock(Queue.class);
    PreOrderCheck preCheck = new PreOrderCheckImpl(counter, accountRepository, instrumentRepository,
        outbound);

    Order order = MockData.getLimitBuy();
    InstrumentConfig config = MockData.getInstrument();
    when(instrumentRepository.getInstrument(order.getSymbol())).thenReturn(config);
    Position position = new Position(config.getQuote(), new BigDecimal("1500"));
    when(accountRepository.getAccountPosition(order.getAccount(), config.getQuote()))
        .thenReturn(position);
    Assertions.assertEquals(new BigDecimal("0"), position.getLocked(), "locked mismatch");
    preCheck.lockBalance(order);
    Assertions.assertEquals(new BigDecimal("1000"), position.getLocked(), "locked mismatch");
  }
}
