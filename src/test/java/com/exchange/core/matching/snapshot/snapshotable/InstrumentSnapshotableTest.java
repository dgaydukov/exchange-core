package com.exchange.core.matching.snapshot.snapshotable;

import com.exchange.core.MockData;
import com.exchange.core.matching.snapshot.Snapshotable;
import com.exchange.core.model.SnapshotItem;
import com.exchange.core.model.enums.SnapshotType;
import com.exchange.core.model.msg.InstrumentConfig;
import com.exchange.core.repository.InstrumentRepository;
import com.exchange.core.repository.InstrumentRepositoryImpl;
import java.util.ArrayList;
import java.util.List;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

public class InstrumentSnapshotableTest {

  private Snapshotable snapshotable;

  @BeforeEach
  public void initNewInstance(){
    snapshotable = new InstrumentRepositoryImpl();
  }


  @Test
  public void snapshotTypeTest(){
    Assertions.assertEquals(SnapshotType.INSTRUMENT, snapshotable.getType(), "snapshot type mismatch");
  }

  @Test
  public void createSnapshotTest(){
    InstrumentRepository repo = (InstrumentRepository) snapshotable;
    InstrumentConfig config = MockData.getInstrument();
    repo.add(config);

    SnapshotItem item = snapshotable.create();
    Assertions.assertEquals(SnapshotType.INSTRUMENT, item.getType(), "snapshot type mismatch");
    Assertions.assertTrue(item.getData() instanceof List);
    List<InstrumentConfig> instruments = (List<InstrumentConfig>) item.getData();
    Assertions.assertEquals(1, instruments.size(), "should be 1 instrument");
    Assertions.assertEquals(config, instruments.get(0), "account mismatch");
  }

  @Test
  public void loadSnapshotTest(){
    InstrumentConfig config = MockData.getInstrument();
    SnapshotItem item = new SnapshotItem();
    item.setType(SnapshotType.INSTRUMENT);
    List<InstrumentConfig> instruments = new ArrayList<>();
    instruments.add(config);
    item.setData(instruments);
    snapshotable.load(item);

    InstrumentRepository repo = (InstrumentRepository) snapshotable;
    Assertions.assertEquals(1, repo.getInstruments().size(), "size should be 1");
    Assertions.assertEquals(config, repo.getInstrument(config.getSymbol()), "instrument mismatch");
  }
}
