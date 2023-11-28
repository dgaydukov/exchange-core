package com.exchange.core.repository;

import com.exchange.core.model.msg.InstrumentConfig;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.stream.Collectors;

public class InstrumentRepositoryImpl implements InstrumentRepository {

  private final Map<String, InstrumentConfig> instruments = new HashMap<>();

  @Override
  public void add(InstrumentConfig msg) {
    instruments.put(msg.getSymbol(), msg);
  }

  @Override
  public InstrumentConfig getInstrument(String symbol) {
    return instruments.get(symbol);
  }

  @Override
  public List<String> getAssets() {
    final List<String> assets = new ArrayList<>();
    instruments.forEach((symbol, config) -> {
      assets.add(config.getBase());
      assets.add(config.getQuote());
    });
    return assets;
  }

  @Override
  public List<InstrumentConfig> getInstruments() {
    return instruments.entrySet()
        .stream()
        .map(Entry::getValue)
        .collect(Collectors.toList());
  }
}