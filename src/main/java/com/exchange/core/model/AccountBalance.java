package com.exchange.core.model;

import lombok.Data;

import java.math.BigDecimal;

@Data
public class AccountBalance implements Message {
    private int accountId;
    private String asset;
    private BigDecimal amount;
}