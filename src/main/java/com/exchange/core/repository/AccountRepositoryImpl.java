package com.exchange.core.repository;

import com.exchange.core.matching.snapshot.Snapshotable;
import com.exchange.core.model.SnapshotItem;
import com.exchange.core.model.enums.SnapshotType;
import com.exchange.core.model.msg.InstrumentConfig;
import com.exchange.core.user.Account;
import com.exchange.core.user.Position;
import com.exchange.core.model.msg.UserBalance;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class AccountRepositoryImpl implements AccountRepository, Snapshotable {

  private final Map<Integer, Account> accounts;

  public AccountRepositoryImpl() {
    accounts = new HashMap<>();
  }

  @Override
  public Account getAccount(int accountId) {
    return accounts.get(accountId);
  }

  @Override
  public List<Account> getAllAccounts() {
    return accounts.values().stream().toList();
  }

  @Override
  public Position getAccountPosition(int accountId, String asset) {
    Account account = getAccount(accountId);
    if (account == null) {
      return null;
    }
    return account.getPosition(asset);
  }

  @Override
  public void addBalance(UserBalance ub) {
    final int accountId = ub.getAccount();
    Account account = getAccount(accountId);
    if (account == null) {
      accounts.put(accountId, new Account(accountId));
    }
    getAccountPosition(accountId, ub.getAsset()).add(ub.getAmount());
  }

  @Override
  public SnapshotType getType() {
    return SnapshotType.ACCOUNT;
  }

  @Override
  public SnapshotItem create() {
    SnapshotItem item = new SnapshotItem();
    item.setType(getType());
    item.setData(getAllAccounts());
    return item;
  }

  @Override
  public void load(SnapshotItem data) {
    ((List<Account>) data.getData()).forEach(a -> {
      accounts.put(a.getAccountId(), a);
    });
  }
}