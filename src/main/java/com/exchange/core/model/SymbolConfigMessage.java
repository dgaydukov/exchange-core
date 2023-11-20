package com.exchange.core.model;

import lombok.Data;

@Data
public class SymbolConfigMessage implements Message{
    private String symbol;
    private String base;
    private String quote;
}
