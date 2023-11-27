package com.exchange.core.matching.orderchecks;

import com.exchange.core.MockData;
import com.exchange.core.matching.counter.GlobalCounter;
import com.exchange.core.model.enums.OrderSide;
import com.exchange.core.model.enums.OrderStatus;
import com.exchange.core.model.msg.*;
import com.exchange.core.repository.AccountRepository;
import com.exchange.core.repository.InstrumentRepository;
import com.exchange.core.user.Position;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

import java.math.BigDecimal;
import java.util.List;
import java.util.Queue;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static org.mockito.Mockito.*;

public class PostOrderCheckTest {

  @Test
  public void sendExecReportNewTest() {
    GlobalCounter counter = mock(GlobalCounter.class);
    AccountRepository accountRepository = mock(AccountRepository.class);
    InstrumentRepository instrumentRepository = mock(InstrumentRepository.class);
    Queue<Message> outbound = mock(Queue.class);
    PostOrderCheck postCheck = new PostOrderCheckImpl(counter, accountRepository,
        instrumentRepository, outbound);

    Order order = MockData.getLimitBuy();
    order.setOrderId(99);
    final long execId = 100;
    when(counter.getNextExecutionId()).thenReturn(execId);
    postCheck.sendExecReportNew(order);
    ArgumentCaptor<ExecutionReport> argument = ArgumentCaptor.forClass(ExecutionReport.class);
    verify(outbound).add(argument.capture());
    ExecutionReport exec = argument.getValue();
    Assertions.assertEquals(order.getSymbol(), exec.getSymbol(), "symbol mismatch");
    Assertions.assertEquals(execId, exec.getExecId(), "execId mismatch");
    Assertions.assertEquals(order.getOrderId(), exec.getOrderId(), "orderId mismatch");
    Assertions.assertEquals(order.getOrderQty(), exec.getOrderQty(), "orderQty mismatch");
    Assertions.assertEquals(order.getPrice(), exec.getPrice(), "price mismatch");
    Assertions.assertEquals(OrderStatus.NEW, exec.getStatus(), "status should be new");
  }

  @Test
  public void sendExecReportTradeTest() {
    GlobalCounter counter = mock(GlobalCounter.class);
    AccountRepository accountRepository = mock(AccountRepository.class);
    InstrumentRepository instrumentRepository = mock(InstrumentRepository.class);
    Queue<Message> outbound = mock(Queue.class);
    PostOrderCheck postCheck = new PostOrderCheckImpl(counter, accountRepository,
        instrumentRepository, outbound);

    Order orderTaker = MockData.getLimitBuy();
    orderTaker.setLeavesQty(BigDecimal.ZERO);
    orderTaker.setOrderId(11);
    Order orderMaker = MockData.getLimitBuy();
    orderMaker.setOrderId(22);
    orderMaker.setSide(OrderSide.SELL);
    orderMaker.setOrderQty(new BigDecimal("20"));
    orderMaker.setLeavesQty(orderMaker.getOrderQty());
    BigDecimal tradeQty = orderTaker.getOrderQty();
    BigDecimal tradePrice = orderMaker.getPrice();

    final long execId = 100;
    when(counter.getNextExecutionId()).thenReturn(execId);
    postCheck.sendExecReportTrade(orderTaker, orderMaker, tradeQty, tradePrice);
    ArgumentCaptor<ExecutionReport> argument = ArgumentCaptor.forClass(ExecutionReport.class);
    verify(outbound, times(2)).add(argument.capture());
    List<ExecutionReport> list = argument.getAllValues();
    Assertions.assertEquals(2, list.size(), "should be 2 executionReports for a trade");

    ExecutionReport taker = list.get(0);
    Assertions.assertEquals(orderTaker.getSymbol(), taker.getSymbol(), "symbol mismatch");
    Assertions.assertEquals(execId, taker.getExecId(), "execId mismatch");
    Assertions.assertEquals(orderTaker.getOrderId(), taker.getOrderId(), "orderId mismatch");
    Assertions.assertEquals(orderMaker.getOrderId(), taker.getCounterOrderId(),
        "counterOrderId mismatch");
    Assertions.assertEquals(orderTaker.getOrderQty(), taker.getOrderQty(), "orderQty mismatch");
    Assertions.assertEquals(orderMaker.getPrice(), taker.getPrice(), "price mismatch");
    Assertions.assertEquals(OrderStatus.FILLED, taker.getStatus(), "status should be filled");
    ExecutionReport maker = list.get(1);
    Assertions.assertEquals(orderMaker.getSymbol(), maker.getSymbol(), "symbol mismatch");
    Assertions.assertEquals(execId, maker.getExecId(), "execId mismatch");
    Assertions.assertEquals(orderMaker.getOrderId(), maker.getOrderId(), "orderId mismatch");
    Assertions.assertEquals(orderTaker.getOrderId(), maker.getCounterOrderId(),
        "counterOrderId mismatch");
    Assertions.assertEquals(orderMaker.getOrderQty(), maker.getOrderQty(), "orderQty mismatch");
    Assertions.assertEquals(orderMaker.getPrice(), maker.getPrice(), "price mismatch");
    Assertions.assertEquals(OrderStatus.PARTIALLY_FILLED, maker.getStatus(),
        "status should be partially filled");
  }

  @Test
  public void sendMarketDataTest() {
    GlobalCounter counter = mock(GlobalCounter.class);
    AccountRepository accountRepository = mock(AccountRepository.class);
    InstrumentRepository instrumentRepository = mock(InstrumentRepository.class);
    Queue<Message> outbound = mock(Queue.class);
    PostOrderCheck postCheck = new PostOrderCheckImpl(counter, accountRepository,
        instrumentRepository, outbound);

    MarketData md = new MarketData();
    postCheck.sendMarketData(md);
    ArgumentCaptor<MarketData> argument = ArgumentCaptor.forClass(MarketData.class);
    verify(outbound).add(argument.capture());
    Assertions.assertEquals(md, argument.getValue(), "MarketData mismatch");
  }

  @Test
  public void settleTradeTrade() {
    GlobalCounter counter = mock(GlobalCounter.class);
    AccountRepository accountRepository = mock(AccountRepository.class);
    InstrumentRepository instrumentRepository = mock(InstrumentRepository.class);
    Queue<Message> outbound = mock(Queue.class);
    PostOrderCheck postCheck = new PostOrderCheckImpl(counter, accountRepository,
        instrumentRepository, outbound);

    BigDecimal tradeQty = new BigDecimal("5");
    BigDecimal tradeAmount = new BigDecimal("500");
    Order takerBuy = MockData.getLimitBuy();
    takerBuy.setAccount(1);
    takerBuy.setLeavesQty(takerBuy.getOrderQty());
    Order makerSell = MockData.getLimitBuy();
    makerSell.setAccount(2);
    makerSell.setSide(OrderSide.SELL);
    makerSell.setOrderQty(tradeQty);
    makerSell.setLeavesQty(makerSell.getOrderQty());
    InstrumentConfig config = MockData.getInstrument();
    when(instrumentRepository.getInstrument(takerBuy.getSymbol())).thenReturn(config);
    Position takerBasePosition = new Position(config.getQuote());
    Position takerQuotePosition = new Position(config.getQuote(), new BigDecimal("700"));
    Position makerBasePosition = new Position(config.getBase(), new BigDecimal("10"));
    Position makerQuotePosition = new Position(config.getQuote());
    takerQuotePosition.lock(tradeAmount);
    makerBasePosition.lock(tradeQty);
    when(accountRepository.getAccountPosition(takerBuy.getAccount(), config.getBase())).thenReturn(
        takerBasePosition);
    when(accountRepository.getAccountPosition(takerBuy.getAccount(), config.getQuote())).thenReturn(
        takerQuotePosition);
    when(accountRepository.getAccountPosition(makerSell.getAccount(), config.getBase())).thenReturn(
        makerBasePosition);
    when(
        accountRepository.getAccountPosition(makerSell.getAccount(), config.getQuote())).thenReturn(
        makerQuotePosition);
    postCheck.settleTrade(takerBuy, makerSell, tradeQty, tradeAmount);
    // taker buy 5 BTC for 500 USDT, and exchange those funds
    Assertions.assertEquals(new BigDecimal("5"), takerBasePosition.getBalance(),
        "taker base should be 5");
    Assertions.assertEquals(new BigDecimal("200"), takerQuotePosition.getBalance(),
        "taker base should be 200");
    Assertions.assertEquals(new BigDecimal("5"), makerBasePosition.getBalance(),
        "maker base should be 5");
    Assertions.assertEquals(new BigDecimal("500"), makerQuotePosition.getBalance(),
        "maker quote should be 500");
  }

  @Test
  public void cancelOrderTest() {
    GlobalCounter counter = mock(GlobalCounter.class);
    AccountRepository accountRepository = mock(AccountRepository.class);
    InstrumentRepository instrumentRepository = mock(InstrumentRepository.class);
    Queue<Message> outbound = mock(Queue.class);
    PostOrderCheck postCheck = new PostOrderCheckImpl(counter, accountRepository,
        instrumentRepository, outbound);

    Order order = MockData.getLimitBuy();
    order.setLeavesQty(order.getOrderQty());
    InstrumentConfig config = MockData.getInstrument();
    when(instrumentRepository.getInstrument(order.getSymbol())).thenReturn(config);
    Position position = new Position(config.getQuote(), new BigDecimal("1000"));
    position.lock(order.getOrderQty().multiply(order.getPrice()));
    when(accountRepository.getAccountPosition(order.getAccount(), config.getQuote()))
        .thenReturn(position);
    final long execId = 100;
    when(counter.getNextExecutionId()).thenReturn(execId);
    postCheck.cancelOrder(order);
    // check locked
    Assertions.assertEquals(new BigDecimal("1000"), position.getBalance(),
        "balance should be 1000");
    Assertions.assertEquals(new BigDecimal("0"), position.getLocked(), "locked should be 0");
    // check ExecutionReport=CANCEL
    ArgumentCaptor<ExecutionReport> argument = ArgumentCaptor.forClass(ExecutionReport.class);
    verify(outbound).add(argument.capture());
    ExecutionReport exec = argument.getValue();
    Assertions.assertEquals(order.getSymbol(), exec.getSymbol(), "symbol mismatch");
    Assertions.assertEquals(execId, exec.getExecId(), "execId mismatch");
    Assertions.assertEquals(order.getOrderId(), exec.getOrderId(), "orderId mismatch");
    Assertions.assertEquals(order.getOrderQty(), exec.getOrderQty(), "orderQty mismatch");
    Assertions.assertEquals(order.getPrice(), exec.getPrice(), "price mismatch");
    Assertions.assertEquals(OrderStatus.CANCELLED, exec.getStatus(), "status should be cancelled");

  }
}
