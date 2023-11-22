package com.exchange.core.repository;

import com.exchange.core.user.Account;
import com.exchange.core.user.Position;
import com.exchange.core.model.msg.UserBalance;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class AccountRepositoryImpl implements AccountRepository {
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
}