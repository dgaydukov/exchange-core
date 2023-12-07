package com.exchange.core.matching.snapshot.converter;

import com.exchange.core.exceptions.AppException;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;

public class JsonObjectConverter implements ObjectConverter{
  private final ObjectMapper mapper = new ObjectMapper();

  @Override
  public String objToString(Object obj) {
    try{
      return mapper.writeValueAsString(obj);
    } catch (JsonProcessingException ex){
      throw new AppException("Failed to write object: error=" + ex.getMessage());
    }
  }

  @Override
  public Object stringToObj(String str) {
    try{
      return mapper.readValue(str, Object.class);
    } catch (JsonProcessingException ex){
      throw new AppException("Failed to read object: error=" + ex.getMessage());
    }
  }
}
