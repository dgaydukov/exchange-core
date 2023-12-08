package com.exchange.core.matching.snapshot.manager;

import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.exchange.core.MockData;
import com.exchange.core.TestUtils;
import com.exchange.core.exceptions.AppException;
import com.exchange.core.matching.snapshot.Snapshotable;
import com.exchange.core.matching.snapshot.converter.JsonObjectConverter;
import com.exchange.core.matching.snapshot.converter.ObjectConverter;
import com.exchange.core.matching.snapshot.storage.FileStorageWriter;
import com.exchange.core.matching.snapshot.storage.StorageWriter;
import com.exchange.core.model.SnapshotItem;
import com.exchange.core.model.enums.SnapshotType;
import com.exchange.core.model.msg.InstrumentConfig;
import com.exchange.core.model.msg.Order;
import com.exchange.core.user.Account;
import com.exchange.core.user.Position;
import java.io.File;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.Mockito;

public class SnapshotManagerTest {

  private final static String BASE_PATH =
      System.getProperty("user.dir") + "/test_snapshots_" + System.currentTimeMillis();

  @BeforeAll
  public static void init() {
    new File(BASE_PATH).mkdir();
  }

  @AfterAll
  public static void cleanup() {
    TestUtils.deleteDirectory(new File(BASE_PATH));
  }

  @Test
  public void loadSnapshotTest() {
    List<Snapshotable> snapshotables = new ArrayList<>();
    ObjectConverter converter = new JsonObjectConverter();
    StorageWriter storageWriter = new FileStorageWriter();
    AppException initException = Assertions.assertThrows(AppException.class, () ->
            new SnapshotManagerImpl(snapshotables, converter, storageWriter, BASE_PATH),
        "Exception should be thrown");
    Assertions.assertEquals("List of Snapshotable should be provided", initException.getMessage(),
        "Exception message mismatch");
    Snapshotable instrumentRepo = Mockito.mock(Snapshotable.class);
    Snapshotable accountRepo = Mockito.mock(Snapshotable.class);
    snapshotables.add(instrumentRepo);
    snapshotables.add(accountRepo);
    SnapshotManager snapshotManager = new SnapshotManagerImpl(snapshotables, converter,
        storageWriter, BASE_PATH);

    SnapshotItem instrumentItem = new SnapshotItem();
    instrumentItem.setType(SnapshotType.INSTRUMENT);
    List<InstrumentConfig> instruments = new ArrayList<>();
    instruments.add(MockData.getInstrument());
    instrumentItem.setData(instruments);

    SnapshotItem accountItem = new SnapshotItem();
    accountItem.setType(SnapshotType.ACCOUNT);
    List<Account> accounts = new ArrayList<>();
    Account account = new Account(1);
    account.getPositions().put("USDT", new Position("USDT", new BigDecimal("1000")));
    accounts.add(account);
    accountItem.setData(accounts);

    // update instrument mock
    when(instrumentRepo.getType()).thenReturn(SnapshotType.INSTRUMENT);
    when(instrumentRepo.create()).thenReturn(instrumentItem);
    // update account mock
    when(accountRepo.getType()).thenReturn(SnapshotType.ACCOUNT);
    when(accountRepo.create()).thenReturn(accountItem);

    String filename = snapshotManager.makeSnapshot();
    String path = BASE_PATH + "/" + filename;
    Assertions.assertTrue(new File(path).isFile(), "file should exist");
    snapshotManager.loadSnapshot(filename);

    ArgumentCaptor<SnapshotItem> instrumentArgument = ArgumentCaptor.forClass(SnapshotItem.class);
    verify(instrumentRepo).load(instrumentArgument.capture());
    SnapshotItem argumentInstrumentItem = instrumentArgument.getValue();
    Assertions.assertNotNull(argumentInstrumentItem.getType());
    Assertions.assertEquals(SnapshotType.INSTRUMENT, argumentInstrumentItem.getType(),
        "type mismatch");
    Assertions.assertEquals(instruments, argumentInstrumentItem.getData(),
        "instrument list mismatch");

    ArgumentCaptor<SnapshotItem> accountArgument = ArgumentCaptor.forClass(SnapshotItem.class);
    verify(accountRepo).load(accountArgument.capture());
    SnapshotItem argumentAccountItem = accountArgument.getValue();
    Assertions.assertNotNull(argumentAccountItem);
    Assertions.assertEquals(SnapshotType.ACCOUNT, argumentAccountItem.getType(), "type mismatch");
    Assertions.assertEquals(accounts, argumentAccountItem.getData(), "account list mismatch");
  }

  @Test
  public void lastOrderIdTest(){
    List<Snapshotable> snapshotables = new ArrayList<>();
    ObjectConverter converter = new JsonObjectConverter();
    StorageWriter storageWriter = new FileStorageWriter();
    Snapshotable ob = Mockito.mock(Snapshotable.class);
    snapshotables.add(ob);
    SnapshotManager snapshotManager = new SnapshotManagerImpl(snapshotables, converter,
        storageWriter, BASE_PATH);
    // add 5 orders
    List<Order> orders = new ArrayList<>();
    long orderId = 0;
    String symbol = "";
    for (int i = 1; i <= 5; i++){
      Order buy = MockData.getLimitBuy();
      symbol = buy.getSymbol();
      orderId = i;
      buy.setOrderId(orderId);
      orders.add(buy);

    }
    SnapshotItem item = new SnapshotItem();
    item.setType(SnapshotType.ORDER_BOOK);
    item.setData(orders);
    // update instrument mock
    when(ob.getType()).thenReturn(item.getType());
    when(ob.create()).thenReturn(item);

    String filename = snapshotManager.makeSnapshot();
    List<String> symbols = snapshotManager.getSymbols(filename);
    Assertions.assertEquals(1, symbols.size(), "should be 1 symbol");
    Assertions.assertEquals(symbol, symbols.get(0), "symbol mismatch");

    // validate lastOrderId
    Assertions.assertEquals(0, snapshotManager.getLastOrderId(), "lastOrderId should be 0");
    snapshotManager.loadSnapshot(filename);
    Assertions.assertEquals(orderId, snapshotManager.getLastOrderId(), "lastOrderId should be "+orderId);
  }
}
