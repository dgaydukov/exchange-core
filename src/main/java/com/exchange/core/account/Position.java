package com.exchange.core.account;

import com.exchange.core.exceptions.AppException;
import lombok.Data;

import java.math.BigDecimal;

@Data
public class Position {
    private String asset;
    private BigDecimal balance;
    private BigDecimal locked;

    public Position(String asset){
        this(asset, BigDecimal.ZERO);
    }

    public Position(String asset, BigDecimal balance){
        this.asset = asset;
        this.balance = balance;
        this.locked = BigDecimal.ZERO;
    }

    public BigDecimal getAvailableBalance(){
        return balance.subtract(locked);
    }

    public void lock(BigDecimal amount){
        if (amount.compareTo(balance) > 0){
            throw new AppException("Failed to lock more than available: amount=" + amount + ", balance="+balance);
        }
        locked = locked.add(amount);
        balance = balance.subtract(amount);
    }

    public void unlock(BigDecimal amount){
        if (amount.compareTo(locked) > 0){
            throw new AppException("Failed to unlock more than locked: amount=" + amount + ", locked="+locked);
        }
        locked = locked.subtract(amount);
        balance = balance.add(amount);
    }

    public void freeLocked(BigDecimal amount){
        if (amount.compareTo(locked) > 0){
            throw new AppException("Failed to free more than locked: amount=" + amount + ", locked="+locked);
        }
        locked = locked.subtract(amount);
    }

    public void add(BigDecimal amount){
        this.balance = balance.add(amount);
    }
    public void subtract(BigDecimal amount){
        if (amount.compareTo(balance) > 0){
            throw new AppException("Failed to subtract more than available: amount=" + amount + ", balance="+balance);
        }
        balance = balance.subtract(amount);
    }
}