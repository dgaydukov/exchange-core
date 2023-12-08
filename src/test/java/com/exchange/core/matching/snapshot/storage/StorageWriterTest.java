package com.exchange.core.matching.snapshot.storage;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

public class StorageWriterTest {
  private StorageWriter storageWriter;
  private final static String BASE_PATH = System.getProperty("user.dir") + "/test_snapshots_" + System.currentTimeMillis();

  @BeforeAll
  public static void init(){
    new File(BASE_PATH).mkdir();
  }

  @BeforeEach
  public void initNewInstance(){
    storageWriter = new FileStorageWriter();
  }

  @AfterAll
  public static void cleanup(){
    deleteDirectory(new File(BASE_PATH));
  }

  private static boolean deleteDirectory(File directoryToBeDeleted) {
    File[] allContents = directoryToBeDeleted.listFiles();
    if (allContents != null) {
      for (File file : allContents) {
        deleteDirectory(file);
      }
    }
    return directoryToBeDeleted.delete();
  }

  @Test
  public void writeTest() throws IOException {
    String content = "hello world";
    String filepath = BASE_PATH + "/simpleFile";
    storageWriter.write(filepath, content);
    BufferedReader reader = new BufferedReader(new FileReader(filepath));
    String read = reader.readLine();
    Assertions.assertEquals(content, read, "file content mismatch");
  }

  @Test
  public void readTest() {
    String content = "hello world";
    String filepath = BASE_PATH + "/simpleFile";
    storageWriter.write(filepath, content);
    String read = storageWriter.read(filepath);
    Assertions.assertEquals(content, read, "file content mismatch");
  }

  @Test
  public void getAllFileNamesTest() throws IOException {
    List<String> fileNames = new ArrayList<>();
    for (int i = 0; i < 10; i++){
      String name = "file_"+i;
      fileNames.add(name);
      new File(BASE_PATH+"/"+name).createNewFile();
    }
    List<String> list = storageWriter.getAllFileNames(BASE_PATH)
            .stream().sorted().toList();
    Assertions.assertEquals(10, list.size(), "size should be 10");
    Assertions.assertIterableEquals(fileNames, list, "names mismatch");
  }
}
