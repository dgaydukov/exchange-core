package com.exchange.core.model;

import lombok.Data;

@Data
public class ErrorMessage implements Message{
    private String error;
    private Object payload;

    public ErrorMessage(String error){
        this.error = error;
    }
}