package com.exchange.core.matching.snapshot.manager;

import com.exchange.core.exceptions.AppException;
import com.exchange.core.matching.snapshot.Snapshotable;
import com.exchange.core.matching.snapshot.converter.ObjectConverter;
import com.exchange.core.matching.snapshot.storage.StorageWriter;
import com.exchange.core.model.SnapshotItem;
import com.exchange.core.model.enums.SnapshotType;
import com.exchange.core.model.msg.InstrumentConfig;
import com.exchange.core.model.msg.Order;
import com.exchange.core.user.Account;
import com.fasterxml.jackson.core.type.TypeReference;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import lombok.Getter;

public class SnapshotManagerImpl implements SnapshotManager {

  private final List<Snapshotable> snapshotables;
  private final ObjectConverter converter;
  private final StorageWriter storageWriter;
  private final String basePath;

  @Getter
  private long lastOrderId;

  public SnapshotManagerImpl(List<Snapshotable> snapshotables, ObjectConverter objectConverter,
      StorageWriter storageWriter, String basePath) {
    if (snapshotables.size() == 0) {
      throw new AppException("List of Snapshotable should be provided");
    }
    this.snapshotables = snapshotables;
    this.converter = objectConverter;
    this.storageWriter = storageWriter;
    this.basePath = basePath;
  }

  @Override
  public String makeSnapshot() {
    List<SnapshotItem> snapshots = new ArrayList<>();
    for (Snapshotable s : snapshotables) {
      snapshots.add(s.create());
    }
    String snapshotStr = converter.objToString(snapshots);
    String filename = "snap_" + System.currentTimeMillis();
    String path = basePath + "/" + filename;
    storageWriter.write(path, snapshotStr);
    return filename;
  }

  @Override
  public void loadSnapshot(String name) {
    List<SnapshotItem> snapshots = loadSnapshots(name);
    for (Snapshotable s : snapshotables) {
      for (SnapshotItem i : snapshots) {
        if (i.getType() == s.getType()) {
          i.setData(cast(i));
          if (i.getType() == SnapshotType.ORDER_BOOK) {
            updateLastOrderId(i.getData());
          }
          s.load(i);
          break;
        }
      }
    }
  }


  @Override
  public List<String> getSymbols(String name) {
    List<String> symbols = new ArrayList<>();
    Object instruments = loadSnapshots(name)
        .stream()
        .filter(s -> s.getType() == SnapshotType.INSTRUMENT)
        .findFirst()
        .map(this::cast)
        .orElse(null);
    if (instruments != null) {
      ((List<InstrumentConfig>) instruments)
          .forEach(i -> symbols.add(i.getSymbol()));
    }
    return symbols;
  }

  public void updateLastOrderId(Object orders) {
    if (orders != null) {
      lastOrderId = ((List<Order>) orders)
          .stream()
          .map(Order::getOrderId)
          .max(Comparator.naturalOrder())
          .get();
    }
  }

  private List<SnapshotItem> loadSnapshots(String name) {
    String path = basePath + "/" + name;
    String snapshotStr = storageWriter.read(path);
    return converter.stringToObj(snapshotStr, new TypeReference<>() {
    });
  }

  private Object cast(SnapshotItem item) {
    return switch (item.getType()) {
      case ACCOUNT -> converter.stringToObj(
          converter.objToString(item.getData()),
          new TypeReference<List<Account>>() {
          });
      case INSTRUMENT -> converter.stringToObj(
          converter.objToString(item.getData()),
          new TypeReference<List<InstrumentConfig>>() {
          });
      case ORDER_BOOK -> converter.stringToObj(
          converter.objToString(item.getData()),
          new TypeReference<List<Order>>() {
          });
    };
  }
}