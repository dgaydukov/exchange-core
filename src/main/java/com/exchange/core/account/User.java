package com.exchange.core.account;

import lombok.Data;

import java.util.Map;

@Data
public class User {
    private int userId;
    private Map<String, Balance> balances;
}
