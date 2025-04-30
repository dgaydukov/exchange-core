package com.exchange.core.integration;

import com.exchange.core.MockData;
import com.exchange.core.TestUtils;
import com.exchange.core.matching.engine.MatchingEngine;
import com.exchange.core.matching.engine.SpotMatchingEngine;
import com.exchange.core.model.SnapshotItem;
import com.exchange.core.model.enums.OrderSide;
import com.exchange.core.model.enums.OrderStatus;
import com.exchange.core.model.enums.SnapshotType;
import com.exchange.core.model.msg.ExecutionReport;
import com.exchange.core.model.msg.InstrumentConfig;
import com.exchange.core.model.msg.MarketData;
import com.exchange.core.model.msg.Message;
import com.exchange.core.model.msg.Order;
import com.exchange.core.model.msg.SnapshotMessage;
import com.exchange.core.model.msg.UserBalance;
import com.exchange.core.user.Account;
import com.exchange.core.user.Position;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.math.BigDecimal;
import java.util.LinkedList;
import java.util.List;
import java.util.Queue;

public class MatchingEngineSnapshotTest {

  private final String SNAPSHOT_BASE_DIR = System.getProperty("user.dir") + "/snapshots";

  @Test
  public void makeSnapshotTest() throws InterruptedException, IOException {
    File baseDir = new File(SNAPSHOT_BASE_DIR);
    TestUtils.deleteDirectory(baseDir);
    Assertions.assertFalse(baseDir.exists());

    Queue<Message> inbound = new LinkedList<>();
    Queue<Message> outbound = new LinkedList<>();
    MatchingEngine me = new SpotMatchingEngine(inbound, outbound);
    me.start();
    // add instrument config
    InstrumentConfig inst = MockData.getInstrument();
    inbound.add(inst);
    // add balance
    UserBalance ub = MockData.getUser(inst.getQuote());
    inbound.add(ub);
    UserBalance ub2 = new UserBalance();
    ub2.setAccount(2);
    ub2.setAsset(inst.getBase());
    ub2.setAmount(new BigDecimal("15"));
    inbound.add(ub2);
    // add order
    Order buy = MockData.getLimitBuy();
    inbound.add(buy);

    // validate snapshot directory created after me start
    Assertions.assertTrue(baseDir.exists());
    // validate that directory is empty
    Assertions.assertEquals(0, baseDir.listFiles().length, "directory should be empty");
    inbound.add(new SnapshotMessage());
    Thread.sleep(200);
    File[] files = baseDir.listFiles();
    Assertions.assertEquals(1, files.length, "1 file should be inside directory");
    // manually read file and see if all data is there
    File snapshotFile = files[0];
    BufferedReader reader = new BufferedReader(new FileReader(snapshotFile));
    String content = reader.readLine();
    reader.close();
    final ObjectMapper mapper = new ObjectMapper()
        .configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);
    List<SnapshotItem> snapshotItems = mapper.readValue(content, new TypeReference<>() {
    });
    Assertions.assertEquals(3, snapshotItems.size(), "should be 3 items in the snapshot");
    // validate instrument
    Object instrumentData = snapshotItems
        .stream()
        .filter(s -> s.getType() == SnapshotType.INSTRUMENT)
        .map(SnapshotItem::getData)
        .findFirst()
        .orElse(null);
    Assertions.assertNotNull(instrumentData);
    List<InstrumentConfig> instruments = mapper.readValue(mapper.writeValueAsString(instrumentData),
        new TypeReference<>() {
        });
    Assertions.assertEquals(1, instruments.size(), "should be 1 instrument");
    Assertions.assertEquals(inst, instruments.get(0), "instrument mismatch");
    // validate account
    Object accountData = snapshotItems
        .stream()
        .filter(s -> s.getType() == SnapshotType.ACCOUNT)
        .map(SnapshotItem::getData)
        .findFirst()
        .orElse(null);
    Assertions.assertNotNull(accountData);
    List<Account> accounts = mapper.readValue(mapper.writeValueAsString(accountData),
        new TypeReference<>() {
        });
    Assertions.assertEquals(2, accounts.size(), "should be 2 account");
    Account account = accounts
        .stream()
        .filter(a -> a.getAccountId() == ub.getAccount())
        .findFirst()
        .orElse(null);
    Assertions.assertNotNull(account);
    Assertions.assertEquals(ub.getAccount(), account.getAccountId(), "accountId mismatch");
    Assertions.assertEquals(1, account.getPositions().size(), "should be 1 position");
    Position position = new Position(ub.getAsset(), ub.getAmount());
    // inside ME would lock balance once order got received
    position.lock(buy.getOrderQty().multiply(buy.getPrice()));
    Assertions.assertEquals(position, account.getPosition(ub.getAsset()), "position mismatch");
    // validate order book
    Object orderBookData = snapshotItems
        .stream()
        .filter(s -> s.getType() == SnapshotType.ORDER_BOOK)
        .map(SnapshotItem::getData)
        .findFirst()
        .orElse(null);
    Assertions.assertNotNull(orderBookData);
    List<Order> orders = mapper.readValue(mapper.writeValueAsString(orderBookData),
        new TypeReference<>() {
        });
    Assertions.assertEquals(1, orders.size(), "should be 1 order");
    Order order = orders.get(0);
    Assertions.assertEquals(buy, order, "order mismatch");

    // cleanup
    TestUtils.deleteDirectory(baseDir);
  }

  @Test
  public void loadSnapshotTest() throws IOException, InterruptedException {
    String content = "[{\"type\":\"ACCOUNT\",\"data\":[{\"accountId\":1,\"positions\":{\"USDT\":{\"symbol\":\"USDT\",\"balance\":1000,\"locked\":1000,\"totalBalance\":2000}}},{\"accountId\":2,\"positions\":{\"BTC\":{\"symbol\":\"BTC\",\"balance\":15,\"locked\":0,\"totalBalance\":15}}}]},{\"type\":\"INSTRUMENT\",\"data\":[{\"symbol\":\"BTC/USDT\",\"base\":\"BTC\",\"quote\":\"USDT\"}]},{\"type\":\"ORDER_BOOK\",\"data\":[{\"symbol\":\"BTC/USDT\",\"orderId\":1,\"clOrdId\":null,\"account\":1,\"side\":\"BUY\",\"type\":\"LIMIT\",\"orderQty\":10,\"leavesQty\":10,\"quoteOrderQty\":null,\"price\":100}]}]";
    File baseDir = new File(SNAPSHOT_BASE_DIR);
    TestUtils.deleteDirectory(baseDir);
    Assertions.assertFalse(baseDir.exists());
    baseDir.mkdir();
    File snapshotFile = new File(SNAPSHOT_BASE_DIR + "/snap_" + System.currentTimeMillis());
    BufferedWriter writer = new BufferedWriter(new FileWriter(snapshotFile));
    writer.write(content);
    writer.close();

    // ME should pick-up the snapshot and load it into memory
    Queue<Message> inbound = new LinkedList<>();
    Queue<Message> outbound = new LinkedList<>();
    MatchingEngine me = new SpotMatchingEngine(inbound, outbound);
    me.start();

    // we can send counter order for account=2 and see trade results
    Order sell = MockData.getLimitBuy();
    sell.setAccount(2);
    sell.setOrderQty(new BigDecimal("15"));
    sell.setSide(OrderSide.SELL);
    inbound.add(sell);
    Thread.sleep(200);

    final int takerOrderId = 2, makerOrderId = 1;
    final BigDecimal sellLeavesQty = new BigDecimal("5");
    ExecutionReport takerNew = (ExecutionReport) outbound.poll();
    Assertions.assertEquals(takerOrderId, takerNew.getOrderId(), "orderId mismatch");
    Assertions.assertEquals(1, takerNew.getExecId(), "execId mismatch");
    Assertions.assertEquals(OrderStatus.NEW, takerNew.getStatus(), "status should be new");

    ExecutionReport takerFilled = (ExecutionReport) outbound.poll();
    Assertions.assertEquals(takerOrderId, takerFilled.getOrderId(), "orderId mismatch");
    Assertions.assertEquals(2, takerFilled.getExecId(), "execId mismatch");
    Assertions.assertEquals(sell.getSymbol(), takerFilled.getSymbol(), "symbol mismatch");
    Assertions.assertEquals(sell.getOrderQty(), takerFilled.getOrderQty(), "orderQty mismatch");
    Assertions.assertEquals(sellLeavesQty, takerFilled.getLeavesQty(), "leavesQty mismatch");
    Assertions.assertEquals(sell.getPrice(), takerFilled.getPrice(), "price mismatch");
    Assertions.assertEquals(OrderStatus.PARTIALLY_FILLED, takerFilled.getStatus(),
        "status should be partially_filled");

    ExecutionReport makerFilled = (ExecutionReport) outbound.poll();
    Assertions.assertEquals(makerOrderId, makerFilled.getOrderId(), "orderId mismatch");
    Assertions.assertEquals(3, makerFilled.getExecId(), "execId mismatch");
    Assertions.assertEquals(sell.getSymbol(), makerFilled.getSymbol(), "symbol mismatch");
    Assertions.assertEquals(new BigDecimal("10"), makerFilled.getOrderQty(), "orderQty mismatch");
    Assertions.assertEquals(new BigDecimal("0"), makerFilled.getLeavesQty(), "leavesQty mismatch");
    Assertions.assertEquals(sell.getPrice(), makerFilled.getPrice(), "price mismatch");
    Assertions.assertEquals(OrderStatus.FILLED, makerFilled.getStatus(), "status should be new");

    MarketData md = (MarketData) outbound.poll();
    Assertions.assertEquals(1, md.getDepth(), "depth should be 1");
    Assertions.assertEquals(0, md.getBids().length, "bids size should be 0");
    Assertions.assertEquals(1, md.getAsks().length, "asks size should be 1");
    BigDecimal[][] asks = new BigDecimal[][]{
        {sell.getPrice(), sellLeavesQty},
    };
    Assertions.assertArrayEquals(asks, md.getAsks(), "asks mismatch");

    // confirm there is no more messages in the outbound queue
    Assertions.assertNull(outbound.poll());

    // cleanup
    TestUtils.deleteDirectory(baseDir);
  }
}