package com.exchange.core.matching.snapshot;

import com.exchange.core.matching.snapshot.storage.StorageWriter;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.ArrayList;
import java.util.List;

public class SnapshotManager {
  private final List<Snapshotable> snapshotables;
  private final StorageWriter storageWriter;
  private final StorageWriter storageWriter;

  public SnapshotManager(List<Snapshotable> snapshotables){
    this.snapshotables = snapshotables;
  }

  public void makeSnapshot(){
    List<SnapshotItem> snapshots = new ArrayList<>();
    for (Snapshotable s : snapshotables) {
      snapshots.add(s.download());
    }
  }

  public void loadSnapshot(){

  }
}
