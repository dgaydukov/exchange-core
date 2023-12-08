package com.exchange.core.matching.snapshot;

import com.exchange.core.MockData;
import com.exchange.core.exceptions.AppException;
import com.exchange.core.matching.snapshot.converter.JsonObjectConverter;
import com.exchange.core.matching.snapshot.converter.ObjectConverter;
import com.exchange.core.matching.snapshot.storage.FileStorageWriter;
import com.exchange.core.matching.snapshot.storage.StorageWriter;
import com.exchange.core.model.SnapshotItem;
import com.exchange.core.model.enums.SnapshotType;
import com.exchange.core.model.msg.InstrumentConfig;
import com.exchange.core.user.Account;
import com.exchange.core.user.Position;
import com.fasterxml.jackson.core.type.TypeReference;
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

import static org.mockito.Mockito.*;

public class SnapshotManagerTest {
  private final static String BASE_PATH = System.getProperty("user.dir") + "/test_snapshots_" + System.currentTimeMillis();

  @BeforeAll
  public static void init(){
    new File(BASE_PATH).mkdir();
  }

  @AfterAll
  public static void cleanup(){
    deleteDirectory(new File(BASE_PATH));
  }

  private static boolean deleteDirectory(File directoryToBeDeleted) {
    File[] allContents = directoryToBeDeleted.listFiles();
    if (allContents != null) {
      for (File file : allContents) {
        deleteDirectory(file);
      }
    }
    return directoryToBeDeleted.delete();
  }
  @Test
  public void loadSnapshotTest(){
    List<Snapshotable> snapshotables = new ArrayList<>();
    ObjectConverter converter = new JsonObjectConverter();
    StorageWriter storageWriter = new FileStorageWriter();
    AppException initException = Assertions.assertThrows(AppException.class, () ->
        new SnapshotManager(snapshotables, converter, storageWriter, BASE_PATH), "Exception should be thrown");
    Assertions.assertEquals("List of Snapshotable should be provided", initException.getMessage(), "Exception message mismatch");
    Snapshotable instrumentRepo = Mockito.mock(Snapshotable.class);
    Snapshotable accountRepo = Mockito.mock(Snapshotable.class);
    snapshotables.add(instrumentRepo);
    snapshotables.add(accountRepo);
    SnapshotManager snapshotManager = new SnapshotManager(snapshotables, converter, storageWriter, BASE_PATH);

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
    Assertions.assertEquals(SnapshotType.INSTRUMENT, argumentInstrumentItem.getType(), "type mismatch");
    Assertions.assertEquals(instruments, argumentInstrumentItem.getData(), "instrument list mismatch");


    ArgumentCaptor<SnapshotItem> accountArgument = ArgumentCaptor.forClass(SnapshotItem.class);
    verify(accountRepo).load(accountArgument.capture());
    SnapshotItem argumentAccountItem = accountArgument.getValue();
    Assertions.assertNotNull(argumentAccountItem);
    Assertions.assertEquals(SnapshotType.ACCOUNT, argumentAccountItem.getType(), "type mismatch");
    Assertions.assertEquals(accounts, argumentAccountItem.getData(), "account list mismatch");
  }
}
