package com.exchange.core.model;

import lombok.Data;

@Data
public class Message implements Cloneable {
    private long sequenceId;

    @Override
    public Object clone() throws CloneNotSupportedException {
        return super.clone();
    }
}