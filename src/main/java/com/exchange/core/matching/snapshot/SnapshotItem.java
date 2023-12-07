package com.exchange.core.matching.snapshot;

import lombok.Data;

@Data
public class SnapshotItem {
  private SnapshotType type;
  private Object data;
}
