package com.exchange.core.matching;

import com.exchange.core.exceptions.AppException;
import com.exchange.core.matching.counter.GlobalCounter;
import com.exchange.core.matching.counter.SimpleGlobalCounter;
import com.exchange.core.matching.orderbook.MapOrderBook;
import com.exchange.core.matching.orderbook.OrderBook;
import com.exchange.core.matching.orderbook.array.ArrayOrderBook;
import com.exchange.core.matching.orderchecks.PostOrderCheck;
import com.exchange.core.matching.orderchecks.PostOrderCheckImpl;
import com.exchange.core.matching.orderchecks.PreOrderCheck;
import com.exchange.core.matching.orderchecks.PreOrderCheckImpl;
import com.exchange.core.matching.waitstrategy.SleepWaitStrategy;
import com.exchange.core.matching.waitstrategy.WaitStrategy;
import com.exchange.core.model.Trade;
import com.exchange.core.model.enums.OrderBookType;
import com.exchange.core.model.enums.OrderType;
import com.exchange.core.model.msg.*;
import com.exchange.core.repository.AccountRepository;
import com.exchange.core.repository.AccountRepositoryImpl;
import com.exchange.core.repository.InstrumentRepository;
import com.exchange.core.repository.InstrumentRepositoryImpl;

import java.math.BigDecimal;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Queue;

public class MatchingEngine {

  private final Map<String, OrderBook> orderBooks;
  private final AccountRepository accountRepository;
  private final InstrumentRepository instrumentRepository;
  private final PreOrderCheck preOrderCheck;
  private final PostOrderCheck postOrderCheck;
  private final Queue<Message> inbound;
  private final Queue<Message> outbound;
  private final OrderBookType orderBookType;
  private final boolean printInboundMsg;

  public MatchingEngine(Queue<Message> inbound, Queue<Message> outbound) {
    this(inbound, outbound, OrderBookType.MAP, true);
  }

  public MatchingEngine(Queue<Message> inbound, Queue<Message> outbound, OrderBookType orderBookType, boolean printInboundMsg) {
    orderBooks = new HashMap<>();
    accountRepository = new AccountRepositoryImpl();
    instrumentRepository = new InstrumentRepositoryImpl();
    GlobalCounter counter = new SimpleGlobalCounter();
    preOrderCheck = new PreOrderCheckImpl(counter, accountRepository, instrumentRepository,
        outbound);
    postOrderCheck = new PostOrderCheckImpl(counter, accountRepository, instrumentRepository,
        outbound);
    this.inbound = inbound;
    this.outbound = outbound;
    this.orderBookType = orderBookType;
    this.printInboundMsg = printInboundMsg;
  }

  public void start() {
    System.out.println("Starting matching engine...");
    new Thread(this::run, "MatchingThread").start();
  }

  private void run() {
    WaitStrategy wait = new SleepWaitStrategy();
    while (true) {
      Message msg = inbound.poll();
      if (msg != null) {
        if(printInboundMsg)
          System.out.println("inbound => " + msg);
        try {
          process(msg);
        } catch (Exception ex) {
          outbound.add(new ErrorMessage(ex.getMessage(), msg));
        }
      }
      wait.idle();
    }
  }

  private void process(Message msg) {
    if (msg instanceof InstrumentConfig symbol) {
      addInstrument(symbol);
    } else if (msg instanceof Order order) {
      addOrder(order);
    } else if (msg instanceof UserBalance ab) {
      addBalance(ab);
    } else {
      throw new AppException("Undefined message: msg=" + msg);
    }
  }

  private void addInstrument(InstrumentConfig msg) {
    instrumentRepository.add(msg);
    final String symbol = msg.getSymbol();
    orderBooks.put(symbol, createNewOrderBook(symbol));
  }

  private OrderBook createNewOrderBook(String symbol){
    return switch (orderBookType){
      case MAP -> new MapOrderBook(symbol);
      case ARRAY -> new ArrayOrderBook(symbol);
    };
  }

  private void addOrder(Order order) {
    final String symbol = order.getSymbol();
    if (symbol == null) {
      throw new AppException("Symbol of new order can't be null");
    }
    OrderBook ob = orderBooks.get(symbol);
    if (ob == null) {
      throw new AppException("OrderBook not found for symbol=" + symbol);
    }
    handleOrder(ob, order);
  }

  private void handleOrder(OrderBook ob, Order order) {
    if (!preOrderCheck.validateOrder(order)) {
      return;
    }
    preOrderCheck.updateNewOrder(order);
    preOrderCheck.lockBalance(order);
    postOrderCheck.sendExecReportNew(order);
    List<Trade> trades = ob.match(order);
    trades.forEach(trade -> {
      Order taker = trade.getTaker();
      Order maker = trade.getMaker();
      BigDecimal tradeQty = trade.getTradeQty();
      BigDecimal tradePrice = trade.getTradePrice();
      BigDecimal tradeAmount = trade.getTradeAmount();
      postOrderCheck.settleTrade(taker, maker, tradeQty, tradeAmount);
      postOrderCheck.sendExecReportTrade(taker, maker, tradeQty, tradePrice);
    });
    // if order not fully matched we should either add to order book or cancel if it's market order
    if (order.getLeavesQty().compareTo(BigDecimal.ZERO) > 0) {
      if (order.getType() == OrderType.MARKET) {
        postOrderCheck.cancelOrder(order);
      } else {
        ob.add(order);
      }
    }
    postOrderCheck.sendMarketData(ob.buildMarketData());
  }

  private void addBalance(UserBalance ab) {
    if (!instrumentRepository.getAssets().contains(ab.getAsset())) {
      throw new AppException("Asset not found: msg=" + ab);
    }
    accountRepository.addBalance(ab);
  }
}