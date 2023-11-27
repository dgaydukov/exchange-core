package com.exchange.core.repository;

import com.exchange.core.model.msg.InstrumentConfig;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import java.util.List;

public class InstrumentRepositoryTest {

  @Test
  public void addInstrumentTest() {
    final InstrumentConfig config = new InstrumentConfig();
    config.setSymbol("BTC/USDT");
    config.setBase("BTC");
    config.setQuote("USDT");

    InstrumentRepository repo = new InstrumentRepositoryImpl();
    Assertions.assertNull(repo.getInstrument(config.getSymbol()));
    Assertions.assertNotNull(repo.getAssets());
    Assertions.assertEquals(0, repo.getAssets().size());
    repo.add(config);
    Assertions.assertEquals(config, repo.getInstrument(config.getSymbol()));
    Assertions.assertEquals(2, repo.getAssets().size());
    Assertions.assertEquals(List.of(config.getBase(), config.getQuote()), repo.getAssets());
  }
}