package com.exchange.core.exceptions;

public class AppException extends RuntimeException {

  public AppException(String msg) {
    super(msg);
  }

  public AppException(String msg, Exception ex) {
    super(msg, ex);
  }
}
