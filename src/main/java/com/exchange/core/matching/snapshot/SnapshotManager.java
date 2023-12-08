package com.exchange.core.matching.snapshot;

import com.exchange.core.exceptions.AppException;
import com.exchange.core.matching.snapshot.converter.ObjectConverter;
import com.exchange.core.matching.snapshot.storage.StorageWriter;
import com.fasterxml.jackson.core.type.TypeReference;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

public class SnapshotManager {

  private final List<Snapshotable> snapshotables;
  private final ObjectConverter objectConverter;
  private final StorageWriter storageWriter;
  private final String basePath;

  public SnapshotManager(List<Snapshotable> snapshotables, ObjectConverter objectConverter,
      StorageWriter storageWriter, String basePath) {
    if (snapshotables.size() == 0){
      throw new AppException("List of Snapshotable should be provided");
    }
    this.snapshotables = snapshotables;
    this.objectConverter = objectConverter;
    this.storageWriter = storageWriter;
    this.basePath = basePath;
  }

  public String makeSnapshot() {
    List<SnapshotItem> snapshots = new ArrayList<>();
    for (Snapshotable s : snapshotables) {
      snapshots.add(s.create());
    }
    String snapshotStr = objectConverter.objToString(snapshots);
    String filename = "snap_"+System.currentTimeMillis();
    String path = basePath + "/" + filename;
    storageWriter.write(path, snapshotStr);
    return filename;
  }

  public void loadSnapshot(String name) {
    String path = basePath + "/" + name;
    String snapshotStr = storageWriter.read(path);
    List<SnapshotItem> snapshots = objectConverter.stringToObj(snapshotStr, new TypeReference<>() {
    });
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
          .map(Long::parseLong)
          .max(Comparator.naturalOrder())
          .map(l -> Long.toString(l))
          .get();
      loadSnapshot(name);
    }
  }
}