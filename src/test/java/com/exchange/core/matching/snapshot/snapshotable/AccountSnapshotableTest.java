package com.exchange.core.matching.snapshot.snapshotable;

import com.exchange.core.MockData;
import com.exchange.core.matching.snapshot.Snapshotable;
import com.exchange.core.model.SnapshotItem;
import com.exchange.core.model.enums.SnapshotType;
import com.exchange.core.model.msg.UserBalance;
import com.exchange.core.repository.AccountRepository;
import com.exchange.core.repository.AccountRepositoryImpl;
import com.exchange.core.user.Account;
import com.exchange.core.user.Position;
import java.util.ArrayList;
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
    UserBalance ub = MockData.getUser("BTC");
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

  @Test
  public void loadSnapshotTest(){
    SnapshotItem item = new SnapshotItem();
    item.setType(SnapshotType.ACCOUNT);
    List<Account> accounts = new ArrayList<>();
    UserBalance ub = MockData.getUser("BTC");
    Account account = new Account(ub.getAccount());
    account.getPositions().put(ub.getAsset(), new Position(ub.getAsset(), ub.getAmount()));
    accounts.add(account);
    item.setData(accounts);
    snapshotable.load(item);

    AccountRepository repo = (AccountRepository) snapshotable;
    Assertions.assertEquals(1, repo.getAllAccounts().size(), "size should be 1");
    Account acc = repo.getAccount(ub.getAccount());
    Assertions.assertNotNull(acc);
    Assertions.assertEquals(ub.getAccount(), acc.getAccountId(), "accountId mismatch");
    Assertions.assertEquals(new Position(ub.getAsset(), ub.getAmount()), acc.getPosition(ub.getAsset()), "accountId mismatch");
  }
}
