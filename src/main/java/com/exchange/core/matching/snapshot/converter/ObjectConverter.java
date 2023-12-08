package com.exchange.core.matching.snapshot.converter;

import com.fasterxml.jackson.core.type.TypeReference;

public interface ObjectConverter {

  <T> String objToString(T obj);

  <T> T stringToObj(String str, TypeReference<T> typeRef);
}
