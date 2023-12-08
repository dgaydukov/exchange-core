package com.exchange.core.matching.snapshot.snapshotable;

import com.exchange.core.matching.snapshot.Snapshotable;
import com.exchange.core.model.SnapshotItem;
import com.exchange.core.model.enums.SnapshotType;
import com.exchange.core.model.msg.UserBalance;
import com.exchange.core.repository.AccountRepository;
import com.exchange.core.repository.AccountRepositoryImpl;
import com.exchange.core.user.Account;
import com.exchange.core.user.Position;
import java.math.BigDecimal;
import java.util.List;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

public class AccountSnapshotableTest {
  private Snapshotable snapshotable;

  @BeforeEach
  public void initNewInstance(){
    snapshotable = new AccountRepositoryImpl();
  }
  @Test
  public void snapshotTypeTest(){
    Assertions.assertEquals(SnapshotType.ACCOUNT, snapshotable.getType(), "snapshot type mismatch");
  }

  @Test
  public void createSnapshotTest(){
    AccountRepository repo = (AccountRepository) snapshotable;
    UserBalance ub = new UserBalance();
    ub.setAccount(1);
    ub.setAsset("BTC");
    ub.setAmount(new BigDecimal("100"));
    repo.addBalance(ub);
    SnapshotItem item = snapshotable.create();
    Assertions.assertEquals(SnapshotType.ACCOUNT, item.getType(), "snapshot type mismatch");
    Assertions.assertTrue(item.getData() instanceof List);
    List<Account> accounts = (List<Account>) item.getData();
    Assertions.assertEquals(1, accounts.size(), "should be 1 account");
    Account account = new Account(ub.getAccount());
    account.getPositions().put(ub.getAsset(), new Position(ub.getAsset(), ub.getAmount()));
    Assertions.assertEquals(account, accounts.get(0), "account mismatch");
  }
}
