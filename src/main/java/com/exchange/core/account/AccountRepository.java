package com.exchange.core.account;

import java.math.BigDecimal;
import java.util.HashMap;
import java.util.Map;

public class AccountRepository {
    private final Map<Integer, User> users;

    public AccountRepository() {
        users = new HashMap<>();
    }

    public void addBalance(int userId, String asset, BigDecimal qty) {
        User user = users.compute(userId, (k, v) -> {
            if (v == null) {
                v = new User();
                v.setUserId(userId);
            }
            return v;
        });
        user.getBalances().compute(asset, (k, v) -> {
            if (v == null) {

            }
            return v;
        });
    }
}