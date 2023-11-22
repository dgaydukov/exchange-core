package com.exchange.core.model.msg;

import lombok.Data;

import java.math.BigDecimal;

@Data
public class UserBalance implements Message {
    private int account;
    private String asset;
    private BigDecimal amount;
}