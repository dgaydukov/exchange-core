package com.exchange.core.user;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

public class PositionTest {
    @Test
    public void notNullBalanceTest(){
        Position position = new Position("BTC");
        Assertions.assertNotNull(position.getAsset());
        Assertions.assertNotNull(position.getBalance());
        Assertions.assertNotNull(position.getLocked());
        Assertions.assertNotNull(position.getAvailableBalance());
    }

    @Test
    public void lockBalanceTest(){
        Position position = new Position("BTC");
    }
}
