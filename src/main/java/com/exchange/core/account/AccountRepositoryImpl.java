package com.exchange.core.account;

import com.exchange.core.model.msg.AccountBalance;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class AccountRepositoryImpl implements AccountRepository{
    private final Map<Integer, Account> accounts;

    public AccountRepositoryImpl() {
        accounts = new HashMap<>();
    }

    @Override
    public Account getAccount(int accountId) {
        return accounts.compute(accountId, (k, v) -> v == null ? new Account(accountId) : v);
    }

    @Override
    public List<Account> getAllAccounts() {
        return accounts.values().stream().toList();
    }

    @Override
    public Position getAccountPosition(int accountId, String asset) {
        return getAccount(accountId).getPosition(asset);
    }

    @Override
    public void addBalance(AccountBalance ab) {
        getAccountPosition(ab.getAccount(), ab.getAsset()).add(ab.getAmount());
    }
}