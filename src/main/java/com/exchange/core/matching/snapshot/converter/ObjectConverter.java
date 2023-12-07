package com.exchange.core.matching.snapshot.converter;

import com.fasterxml.jackson.core.JsonProcessingException;

public interface ObjectConverter {
  String objToString(Object obj) throws JsonProcessingException;
  Object stringToObj(String str);
}
