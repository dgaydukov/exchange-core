package com.exchange.core.matching.snapshot;

import com.exchange.core.exceptions.AppException;
import com.exchange.core.matching.snapshot.converter.ObjectConverter;
import com.exchange.core.matching.snapshot.storage.StorageWriter;
import com.exchange.core.model.SnapshotItem;
import com.exchange.core.model.enums.SnapshotType;
import com.exchange.core.model.msg.InstrumentConfig;
import com.exchange.core.model.msg.Order;
import com.exchange.core.user.Account;
import com.fasterxml.jackson.core.type.TypeReference;
import java.util.ArrayList;
import java.util.List;

public class SnapshotManager {

  private final List<Snapshotable> snapshotables;
  private final ObjectConverter converter;
  private final StorageWriter storageWriter;
  private final String basePath;

  public SnapshotManager(List<Snapshotable> snapshotables, ObjectConverter objectConverter,
      StorageWriter storageWriter, String basePath) {
    if (snapshotables.size() == 0){
      throw new AppException("List of Snapshotable should be provided");
    }
    this.snapshotables = snapshotables;
    this.converter = objectConverter;
    this.storageWriter = storageWriter;
    this.basePath = basePath;
  }

  public String makeSnapshot() {
    List<SnapshotItem> snapshots = new ArrayList<>();
    for (Snapshotable s : snapshotables) {
      snapshots.add(s.create());
    }
    String snapshotStr = converter.objToString(snapshots);
    String filename = "snap_"+System.currentTimeMillis();
    String path = basePath + "/" + filename;
    storageWriter.write(path, snapshotStr);
    return filename;
  }

  public void loadSnapshot(String name) {
    String path = basePath + "/" + name;
    String snapshotStr = storageWriter.read(path);
    List<SnapshotItem> snapshots = converter.stringToObj(snapshotStr, new TypeReference<>() {
    });
    for (Snapshotable s : snapshotables) {
      for (SnapshotItem i : snapshots) {
        if (i.getType() == s.getType()) {
          i.setData(cast(i));
          s.load(i);
          break;
        }
      }
    }
  }

  private Object cast(SnapshotItem item){
    return switch (item.getType()){
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