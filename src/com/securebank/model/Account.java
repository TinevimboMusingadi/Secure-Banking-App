package com.securebank.model;

import com.securebank.service.BankingException;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Instant;
import java.util.Objects;

/**
 * Abstract base class representing a generic bank account.
 * Demonstrates Abstraction and Encapsulation.
 *
 * Secure Coding Practices:
 * 1. Monetary representation using java.math.BigDecimal with fixed scale (2) and RoundingMode.HALF_UP
 *    to prevent arithmetic rounding errors and precision exploits.
 * 2. Thread-safe balance mutations using synchronization.
 * 3. Immutable account identity.
 *
 * @author Tinevimbo Musingadi (H250125B)
 */
public abstract class Account {

    protected final String accountNumber;
    protected final String userId;
    protected BigDecimal balance;
    protected final Instant createdAt;
    protected boolean active;

    public Account(String accountNumber, String userId, BigDecimal initialBalance, Instant createdAt, boolean active) {
        this.accountNumber = Objects.requireNonNull(accountNumber, "accountNumber cannot be null");
        this.userId = Objects.requireNonNull(userId, "userId cannot be null");
        this.balance = initialBalance != null ?
                initialBalance.setScale(2, RoundingMode.HALF_UP) :
                BigDecimal.ZERO.setScale(2, RoundingMode.HALF_UP);
        this.createdAt = createdAt != null ? createdAt : Instant.now();
        this.active = active;
    }

    public String getAccountNumber() {
        return accountNumber;
    }

    public String getUserId() {
        return userId;
    }

    public synchronized BigDecimal getBalance() {
        return balance.setScale(2, RoundingMode.HALF_UP);
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

    public boolean isActive() {
        return active;
    }

    public void setActive(boolean active) {
        this.active = active;
    }

    /**
     * Deposits money into the account in a thread-safe manner.
     *
     * @param amount positive monetary amount
     * @throws BankingException if amount is non-positive or account is inactive
     */
    public synchronized void deposit(BigDecimal amount) throws BankingException {
        if (!active) {
            throw new BankingException("Cannot deposit into an inactive account");
        }
        if (amount == null || amount.compareTo(BigDecimal.ZERO) <= 0) {
            throw new BankingException("Deposit amount must be strictly positive");
        }
        BigDecimal normalizedAmount = amount.setScale(2, RoundingMode.HALF_UP);
        this.balance = this.balance.add(normalizedAmount);
    }

    /**
     * Polymorphic withdrawal method implemented specifically by account subclasses.
     *
     * @param amount positive monetary amount to withdraw
     * @throws BankingException if balance is insufficient or limits are breached
     */
    public abstract void withdraw(BigDecimal amount) throws BankingException;

    /**
     * Polymorphic method to apply periodic interest or maintenance fees.
     */
    public abstract void applyPeriodicUpdate();

    /**
     * Returns the polymorphic account type name.
     */
    public abstract String getAccountType();

    /**
     * Returns any type-specific attribute (interest rate or overdraft limit) for persistence.
     */
    public abstract String getSpecialAttribute();

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        Account account = (Account) o;
        return Objects.equals(accountNumber, account.accountNumber);
    }

    @Override
    public int hashCode() {
        return Objects.hash(accountNumber);
    }
}
