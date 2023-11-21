package com.exchange.core.account;

import com.exchange.core.model.msg.AccountBalance;

import java.util.List;

public interface AccountRepository {
    Account getAccount(int accountId);
    List<Account> getAllAccounts();
    Position getAccountPosition(int accountId, String asset);
    void addBalance(AccountBalance msg);
}