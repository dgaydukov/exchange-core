package com.exchange.core.exceptions;

import com.exchange.core.App;

public class AppException extends RuntimeException{
    public AppException(String msg){
        super(msg);
    }
}
