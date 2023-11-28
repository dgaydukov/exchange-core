package com.exchange.core.futures.risk;

import com.exchange.core.repository.AccountRepository;
import com.exchange.core.user.Account;

import java.math.BigDecimal;


public class FundingEngine implements Engine {

  private final AccountRepository accountRepository;

  public FundingEngine(AccountRepository accountRepository) {
    this.accountRepository = accountRepository;
  }

  public BigDecimal getIndexPrice() {
    return BigDecimal.ZERO;
  }

  @Override
  public void start() {
    System.out.println("Starting FundingEngine...");
    new Thread(this::run, "FundingEngine").start();
  }

  private void run() {
    for (Account account : accountRepository.getAllAccounts()) {

    }
  }
}