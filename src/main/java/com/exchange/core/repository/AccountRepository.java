package com.exchange.core.repository;

import com.exchange.core.model.msg.UserBalance;
import com.exchange.core.user.Account;
import com.exchange.core.user.Position;
import java.util.List;

public interface AccountRepository {

  Account getAccount(int accountId);

  List<Account> getAllAccounts();

  Position getAccountPosition(int accountId, String asset);

  void addBalance(UserBalance msg);
}