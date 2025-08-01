package com.exchange.core.matching.engine;

import com.exchange.core.exceptions.AppException;
import com.exchange.core.matching.counter.GlobalCounter;
import com.exchange.core.matching.counter.SimpleGlobalCounter;
import com.exchange.core.matching.orderbook.book.MapOrderBook;
import com.exchange.core.matching.orderbook.OrderBook;
import com.exchange.core.matching.orderbook.book.ArrayOrderBook;
import com.exchange.core.matching.orderchecks.PostOrderCheck;
import com.exchange.core.matching.orderchecks.PostOrderCheckImpl;
import com.exchange.core.matching.orderchecks.PreOrderCheck;
import com.exchange.core.matching.orderchecks.PreOrderCheckImpl;
import com.exchange.core.matching.snapshot.Snapshotable;
import com.exchange.core.matching.snapshot.converter.JsonObjectConverter;
import com.exchange.core.matching.snapshot.manager.SnapshotManager;
import com.exchange.core.matching.snapshot.manager.SnapshotManagerImpl;
import com.exchange.core.matching.snapshot.storage.FileStorageWriter;
import com.exchange.core.matching.snapshot.storage.StorageWriter;
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
import lombok.extern.slf4j.Slf4j;

import java.io.File;
import java.math.BigDecimal;
import java.util.*;

@Slf4j
public class SpotMatchingEngine implements MatchingEngine {

  private final Map<String, OrderBook> orderBooks;
  private final AccountRepository accountRepository;
  private final InstrumentRepository instrumentRepository;
  private final GlobalCounter counter;
  private final PreOrderCheck preOrderCheck;
  private final PostOrderCheck postOrderCheck;
  private final Queue<Message> inbound;
  private final Queue<Message> outbound;
  private final OrderBookType orderBookType;
  private final SnapshotManager snapshotManager;
  private final List<Snapshotable> snapshotables;
  private final StorageWriter storageWriter;
  private final boolean printInboundMsg;
  private final String SNAPSHOT_BASE_DIR = System.getProperty("user.dir") + "/snapshots";
  private final WaitStrategy waitStrategy;

  public SpotMatchingEngine(Queue<Message> inbound, Queue<Message> outbound) {
    this(inbound, outbound, OrderBookType.MAP, true);
  }

  public SpotMatchingEngine(Queue<Message> inbound, Queue<Message> outbound,
                            OrderBookType orderBookType, boolean printInboundMsg) {
    orderBooks = new HashMap<>();
    accountRepository = new AccountRepositoryImpl();
    instrumentRepository = new InstrumentRepositoryImpl();
    counter = new SimpleGlobalCounter();
    preOrderCheck = new PreOrderCheckImpl(counter, accountRepository, instrumentRepository,
        outbound);
    postOrderCheck = new PostOrderCheckImpl(counter, accountRepository, instrumentRepository,
        outbound);
    this.inbound = inbound;
    this.outbound = outbound;
    this.orderBookType = orderBookType;
    this.printInboundMsg = printInboundMsg;
    snapshotables = new ArrayList<>();
    snapshotables.add((Snapshotable) accountRepository);
    snapshotables.add((Snapshotable) instrumentRepository);
    storageWriter = new FileStorageWriter();
    snapshotManager = new SnapshotManagerImpl(snapshotables,
        new JsonObjectConverter(), storageWriter, SNAPSHOT_BASE_DIR);
    waitStrategy = new SleepWaitStrategy();
  }

  public void start() {
    loadSnapshot();
    log.info("Starting matching engine...");
    new Thread(this::run, "MatchingThread").start();
  }

  private void loadSnapshot() {
    File file = new File(SNAPSHOT_BASE_DIR);
    if (!file.exists()) {
      log.info("Creating snapshot directory: path={}", file);
      if (!file.mkdir()){
        throw new AppException("Failed to create snapshot directory: path=" + file);
      }
    }
    String filename = storageWriter.getLastModifiedFilename(SNAPSHOT_BASE_DIR);
    if (filename != null) {
      // create order books
      log.info("Loading snapshot: name={}", filename);
      snapshotManager.getSymbols(filename).forEach(symbol -> {
        log.info("Adding order book: symbol={}", symbol);
        addOrderBook(symbol);
      });
      // load snapshots
      snapshotManager.loadSnapshot(filename);
      // update counter for next orderId
      long lastOrderId = snapshotManager.getLastOrderId();
      log.info("Updating counter: lastOrderId={}", lastOrderId);
      while (lastOrderId != counter.getNextOrderId()) {
        waitStrategy.idle();
      }
      log.info("Loaded snapshot: name={}", filename);
    }
  }

  private void run() {
    WaitStrategy wait = new SleepWaitStrategy();
    while (true) {
      Message msg = inbound.poll();
      if (msg == null) {
        wait.idle();
        continue;
      }
      if (printInboundMsg) {
        log.info("Get inbound message: {}", msg);
      }
      try {
        process(msg);
      } catch (Exception ex) {
        outbound.add(new ErrorMessage(ex.getMessage(), msg));
      }
    }
  }

  private void process(Message msg) {
      switch (msg) {
          case InstrumentConfig symbol -> addInstrument(symbol);
          case Order order -> addOrder(order);
          case UserBalance userBalance -> addBalance(userBalance);
          case SnapshotMessage snapshotMessage -> snapshotManager.makeSnapshot();
          case null, default -> throw new AppException("Undefined message: msg=" + msg);
      }
  }

  private void addInstrument(InstrumentConfig msg) {
    instrumentRepository.add(msg);
    final String symbol = msg.getSymbol();
    addOrderBook(symbol);
  }

  private void addOrderBook(String symbol) {
    OrderBook ob = createNewOrderBook(symbol);
    orderBooks.put(symbol, ob);
    snapshotables.add((Snapshotable) ob);
  }

  private OrderBook createNewOrderBook(String symbol) {
    return switch (orderBookType) {
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