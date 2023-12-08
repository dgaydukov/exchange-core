package com.exchange.core.model;

import com.exchange.core.model.enums.SnapshotType;
import lombok.Data;

@Data
public class SnapshotItem {

  private SnapshotType type;
  private Object data;
}
