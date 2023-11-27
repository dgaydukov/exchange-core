package com.exchange.core.model.msg;

import lombok.Data;

@Data
public class ErrorMessage implements Message {

  private String error;
  private Object payload;

  public ErrorMessage(String error, Object payload) {
    this.error = error;
    this.payload = payload;
  }
}