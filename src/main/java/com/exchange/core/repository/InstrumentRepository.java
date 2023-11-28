package com.exchange.core.repository;

import com.exchange.core.model.msg.InstrumentConfig;

import java.util.List;

public interface InstrumentRepository {

  void add(InstrumentConfig msg);

  InstrumentConfig getInstrument(String symbol);

  List<InstrumentConfig> getInstruments();

  List<String> getAssets();
}