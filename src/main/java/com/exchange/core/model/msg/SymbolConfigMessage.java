package com.exchange.core.model.msg;

import com.exchange.core.model.msg.Message;
import lombok.Data;

@Data
public class SymbolConfigMessage implements Message {
    private String symbol;
    private String base;
    private String quote;
}
