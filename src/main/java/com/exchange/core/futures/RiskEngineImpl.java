package com.exchange.core.futures;

import com.exchange.core.user.Account;
import com.exchange.core.repository.AccountRepository;

import java.math.BigDecimal;
import java.util.List;

public class RiskEngineImpl implements RiskEngine{
    private final AccountRepository accountRepository;

    public RiskEngineImpl(AccountRepository accountRepository){
        this.accountRepository = accountRepository;
    }

    /**
     * We take lastPrice for BTC/USDT on binance
     * ideally we should take median price from top N exchanges
     */
    @Override
    public BigDecimal getIndexPrice() {
        return null;
    }

    public void start(){
        System.out.println("Starting risk engine...");
        new Thread(this::run, "RiskThread").start();
    }

    private void run(){
        List<Account> accounts = accountRepository.getAllAccounts();


        /**
         * how to share state between 2 objects
         * we can make TAM values of user object volative field
         * then changes in ME would be immediately visible in RE, but since we anyway will do liquidation in ME all other fields don't matter
         * so object is not thread-safely, but it's field TAM it is
         */
    }
}