package com.exchange.core.matching.snapshot.storage;

import java.util.List;

public interface StorageWriter {

  void write(String path, String data);

  String read(String path);

  String getLastModifiedFilename(String path);
}
