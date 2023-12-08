package com.exchange.core.matching.snapshot.converter;

import com.exchange.core.MockData;
import com.exchange.core.exceptions.AppException;
import com.exchange.core.matching.snapshot.storage.FileStorageWriter;
import com.exchange.core.model.msg.InstrumentConfig;
import com.fasterxml.jackson.core.type.TypeReference;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

public class ObjectConverterTest {

  private ObjectConverter converter;

  @BeforeEach
  public void initNewInstance(){
    converter = new JsonObjectConverter();
  }

  @Test
  public void nullTest() {
    String str = converter.objToString(null);
    Assertions.assertNull(str);
    Object obj = converter.stringToObj(null, null);
    Assertions.assertNull(obj);
  }

  @Test
  public void instrumentConfigTest() {
    InstrumentConfig instrument = MockData.getInstrument();
    String instrumentStr = converter.objToString(instrument);
    Assertions.assertEquals("{\"symbol\":\"BTC/USDT\",\"base\":\"BTC\",\"quote\":\"USDT\"}",
        instrumentStr, "InstrumentConfig string mismatch");
    InstrumentConfig obj = converter.stringToObj(instrumentStr,
        new TypeReference<>() {
        });
    Assertions.assertEquals(instrument, obj, "InstrumentConfig object mismatch");
  }

  @Test
  public void instrumentConfigListTest() {
    InstrumentConfig inst1 = MockData.getInstrument();
    InstrumentConfig inst2 = new InstrumentConfig();
    inst2.setSymbol("ETH-USDT");
    inst2.setBase("ETH");
    inst2.setQuote("USDT");
    InstrumentConfig inst3 = new InstrumentConfig();
    inst3.setSymbol("BTC-ETH");
    inst3.setBase("BTC");
    inst3.setQuote("ETH");
    List<InstrumentConfig> list = new ArrayList<>();
    list.add(inst1);
    list.add(inst2);
    list.add(inst3);
    String listStr = converter.objToString(list);
    Assertions.assertEquals("[{\"symbol\":\"BTC/USDT\",\"base\":\"BTC\",\"quote\":\"USDT\"},{\"symbol\":\"ETH-USDT\",\"base\":\"ETH\",\"quote\":\"USDT\"},{\"symbol\":\"BTC-ETH\",\"base\":\"BTC\",\"quote\":\"ETH\"}]",
        listStr, "InstrumentConfig string mismatch");
    List<InstrumentConfig> recovered = converter.stringToObj(listStr, new TypeReference<>(){});
    Assertions.assertNotSame(list, recovered, "Original list and recovered should be different objects");
    Assertions.assertEquals(list, recovered, "InstrumentConfig object mismatch");
  }

  @Test
  public void stringToObjExceptionTest(){
    String str = "hello world";
    AppException exception = Assertions.assertThrows(AppException.class,
        () ->  converter.stringToObj(str, new TypeReference<>() {}), "Exception should be thrown");
    Assertions.assertEquals("Failed to read: str=hello world", exception.getMessage(), "Exception message mismatch");
  }
}