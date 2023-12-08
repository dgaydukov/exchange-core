package com.exchange.core.matching.snapshot;

import com.exchange.core.matching.snapshot.converter.ObjectConverter;
import com.exchange.core.matching.snapshot.storage.StorageWriter;
import java.util.ArrayList;
import java.util.List;

public class SnapshotManager {

  private final List<Snapshotable> snapshotables;
  private final ObjectConverter objectConverter;
  private final StorageWriter storageWriter;
  private final String basePath;

  public SnapshotManager(List<Snapshotable> snapshotables,
      ObjectConverter objectConverter, StorageWriter storageWriter, String basePath) {
    this.snapshotables = snapshotables;
    this.objectConverter = objectConverter;
    this.storageWriter = storageWriter;
    this.basePath = basePath;
  }

  public void makeSnapshot() {
    List<SnapshotItem> snapshots = new ArrayList<>();
    for (Snapshotable s : snapshotables) {
      snapshots.add(s.create());
    }
    String snapshotStr = objectConverter.objToString(snapshots);
    String path = basePath + "/" + System.currentTimeMillis();
    storageWriter.write(path, snapshotStr);
  }

  public void loadSnapshot(String name) {
    String path = basePath + "/" + name;
    String snapshotStr = storageWriter.read(path);
    List<SnapshotItem> snapshots = (List<SnapshotItem>) objectConverter.stringToObj(snapshotStr);
    for (Snapshotable s : snapshotables) {
      for (SnapshotItem i : snapshots) {
        if (i.getType() == s.getType()) {
          s.load(i);
          break;
        }
      }
    }
  }

  public void loadLatestSnapshot() {
    List<String> fileNames = storageWriter.getAllFileNames(basePath);
    if (fileNames.size() > 0){
      String name = fileNames
          .stream()
          .map(s -> Long.parseLong(s))
          .sorted()
          .findFirst()
          .map(l -> Long.toString(l))
          .get();
      loadSnapshot(name);
    }
  }
}
