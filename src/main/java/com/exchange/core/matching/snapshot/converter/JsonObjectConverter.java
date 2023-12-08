package com.exchange.core.matching.snapshot.converter;

import com.exchange.core.exceptions.AppException;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;

public class JsonObjectConverter implements ObjectConverter{
  private final ObjectMapper mapper = new ObjectMapper()
      .configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);

  @Override
  public <T> String objToString(T obj) {
    if (obj == null){
      return null;
    }
    try{
      return mapper.writeValueAsString(obj);
    } catch (JsonProcessingException ex){
      throw new AppException("Failed to write: obj="+obj, ex);
    }
  }

  @Override
  public <T> T stringToObj(String str, TypeReference<T> typeRef) {
    if (str == null){
      return null;
    }
    try{
      return mapper.readValue(str, typeRef);
    } catch (JsonProcessingException ex){
      throw new AppException("Failed to read: str="+str, ex);
    }
  }
}
