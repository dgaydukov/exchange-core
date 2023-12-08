package com.exchange.core.matching.snapshot;

import com.exchange.core.model.SnapshotItem;
import com.exchange.core.model.enums.SnapshotType;

public interface Snapshotable {
  SnapshotType getType();
  SnapshotItem create();
  void load(SnapshotItem data);
}