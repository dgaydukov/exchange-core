package com.exchange.core.matching.snapshot.manager;

import java.util.List;

public interface SnapshotManager {

  String makeSnapshot();

  void loadSnapshot(String name);

  List<String> getSymbols(String name);

  long getLastOrderId();
}
