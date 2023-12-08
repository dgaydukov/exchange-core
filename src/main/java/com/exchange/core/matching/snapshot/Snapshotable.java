package com.exchange.core.matching.snapshot;

public interface Snapshotable {
  SnapshotType getType();
  SnapshotItem create();
  void load(SnapshotItem data);
}