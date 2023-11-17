package com.exchange.core.account;

import lombok.Data;

import java.math.BigDecimal;

@Data
public class Balance {
    private String asset;
    private BigDecimal balance;
    private BigDecimal locked;
}
