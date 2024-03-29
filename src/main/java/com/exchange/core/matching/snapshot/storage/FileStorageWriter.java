package com.exchange.core.matching.snapshot.storage;

import com.exchange.core.exceptions.AppException;
import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.util.Arrays;

public class FileStorageWriter implements StorageWriter {

  @Override
  public void write(String path, String data) {
    try {
      BufferedWriter writer = new BufferedWriter(new FileWriter(path));
      writer.write(data);
      writer.close();
    } catch (IOException ex) {
      throw new AppException("Failed to write: path=" + path);
    }
  }

  @Override
  public String read(String path) {
    try {
      BufferedReader reader = new BufferedReader(new FileReader(path));
      StringBuilder sb = new StringBuilder();
      for (String line; (line = reader.readLine()) != null; ) {
        sb.append(line);
      }
      return sb.toString();
    } catch (IOException ex) {
      throw new AppException("Failed to read: path=" + path);
    }
  }

  @Override
  public String getLastModifiedFilename(String path) {
    File[] files = new File(path).listFiles();
    if (files == null || files.length == 0) {
      return null;
    }
    return Arrays.stream(files)
        .filter(File::isFile)
        .sorted((f1, f2) -> Math.toIntExact(f2.lastModified() - f1.lastModified()))
        .map(File::getName)
        .findFirst()
        .get();
  }
}
