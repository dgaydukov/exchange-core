package com.exchange.core.matching.snapshot;

public interface Snapshotable {
  SnapshotItem download();
  void upload(SnapshotItem data);
}
