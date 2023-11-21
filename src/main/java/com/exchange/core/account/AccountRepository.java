package com.exchange.core.account;

import com.exchange.core.model.msg.AccountBalance;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class AccountRepository {
    private final Map<Integer, Account> accounts;

    public AccountRepository() {
        accounts = new HashMap<>();
    }

    public Account getAccount(int accountId) {
        return accounts.compute(accountId, (k, v) -> v == null ? new Account(accountId) : v);
    }

    public Position getAccPosition(int accountId, String asset) {
        Account account = getAccount(accountId);
        return account.getPosition(asset);
    }

    public void addBalance(AccountBalance ab) {
        Position position = getAccPosition(ab.getAccount(), ab.getAsset());
        position.add(ab.getAmount());
    }

    public List<Account> getAllAccounts() {
        return accounts.values().stream().toList();
    }
}