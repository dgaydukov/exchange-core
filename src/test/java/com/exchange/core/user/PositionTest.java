package com.exchange.core.user;

import com.exchange.core.exceptions.AppException;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;

public class PositionTest {
    final BigDecimal b200 = new BigDecimal("200");
    final BigDecimal b100 = new BigDecimal("100");
    final BigDecimal b0 = BigDecimal.ZERO;

    @Test
    public void notNullBalanceTest(){
        Position position = new Position("BTC");
        Assertions.assertNotNull(position.getAsset());
        Assertions.assertNotNull(position.getBalance());
        Assertions.assertNotNull(position.getLocked());
        Assertions.assertNotNull(position.getTotalBalance());
    }

    @Test
    public void availableBalanceTest(){
        Position position = new Position("BTC");
        Assertions.assertEquals(b0, position.getBalance(), "balance should be " + b0);
        Assertions.assertEquals(b0, position.getLocked(), "locked should be " + b0);
        Assertions.assertEquals(b0, position.getTotalBalance(), "totalBalance should be " + b0);
        position.add(b200);
        Assertions.assertEquals(b200, position.getBalance(), "balance should be " + b200);
        Assertions.assertEquals(b0, position.getLocked(), "locked should be " + b0);
        Assertions.assertEquals(b200, position.getTotalBalance(), "totalBalance should be " + b200);
        position.lock(b200);
        Assertions.assertEquals(b0, position.getBalance(), "balance should be " + b0);
        Assertions.assertEquals(b200, position.getLocked(), "locked should be " + b200);
        Assertions.assertEquals(b200, position.getTotalBalance(), "totalBalance should be " + b200);
        position.freeLocked(b200);
        Assertions.assertEquals(b0, position.getBalance(), "balance should be " + b0);
        Assertions.assertEquals(b0, position.getLocked(), "locked should be " + b0);
        Assertions.assertEquals(b0, position.getTotalBalance(), "totalBalance should be " + b0);
    }

    @Test
    public void lockBalanceTest(){
        Position position = new Position("BTC");
        position.add(b200);
        position.lock(b200);
        position.freeLocked(b100);
        Assertions.assertEquals(b0, position.getBalance(), "balance should be " + b0);
        Assertions.assertEquals(b100, position.getLocked(), "locked should be " + b100);
        Assertions.assertEquals(b100, position.getTotalBalance(), "totalBalance should be " + b100);
        position.unlock(b100);
        Assertions.assertEquals(b100, position.getBalance(), "balance should be " + b100);
        Assertions.assertEquals(b0, position.getLocked(), "locked should be " + b0);
        Assertions.assertEquals(b100, position.getTotalBalance(), "totalBalance should be " + b100);
    }

    @Test
    public void exceptionTest(){
        Position position = new Position("BTC");
        AppException lock = Assertions.assertThrows(AppException.class,
                ()-> position.lock(b200), "Exception should be thrown");
        Assertions.assertEquals(lock.getMessage(), "Failed to lock more than available: amount=200, balance=0");

        AppException unlock = Assertions.assertThrows(AppException.class,
                ()-> position.unlock(b200), "Exception should be thrown");
        Assertions.assertEquals(unlock.getMessage(), "Failed to unlock more than locked: amount=200, locked=0");

        AppException freeLock = Assertions.assertThrows(AppException.class,
                ()-> position.freeLocked(b200), "Exception should be thrown");
        Assertions.assertEquals(freeLock.getMessage(), "Failed to free more than locked: amount=200, locked=0");
    }
}