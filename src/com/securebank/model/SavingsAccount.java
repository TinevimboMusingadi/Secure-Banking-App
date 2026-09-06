package com.securebank.model;

import com.securebank.service.BankingException;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Instant;

/**
 * Concrete implementation of a Savings Account demonstrating Inheritance and Polymorphism.
 * Implements minimum balance requirements and interest calculation.
 *
 * @author Tinevimbo Musingadi (H250125B)
 */
public class SavingsAccount extends Account {

    private final BigDecimal interestRate; // e.g. 0.0350 (3.5% APY)
    private static final BigDecimal MINIMUM_BALANCE = new BigDecimal("25.00");

    public SavingsAccount(String accountNumber, String userId, BigDecimal initialBalance,
                          BigDecimal interestRate, Instant createdAt, boolean active) {
        super(accountNumber, userId, initialBalance, createdAt, active);
        this.interestRate = interestRate != null ?
                interestRate.setScale(4, RoundingMode.HALF_UP) :
                new BigDecimal("0.0350");
    }

    public BigDecimal getInterestRate() {
        return interestRate;
    }

    public static BigDecimal getMinimumBalance() {
        return MINIMUM_BALANCE;
    }

    @Override
    public synchronized void withdraw(BigDecimal amount) throws BankingException {
        if (!active) {
            throw new BankingException("Account " + accountNumber + " is inactive.");
        }
        if (amount == null || amount.compareTo(BigDecimal.ZERO) <= 0) {
            throw new BankingException("Withdrawal amount must be positive.");
        }

        BigDecimal normalizedAmount = amount.setScale(2, RoundingMode.HALF_UP);
        BigDecimal projectedBalance = this.balance.subtract(normalizedAmount);

        if (projectedBalance.compareTo(MINIMUM_BALANCE) < 0) {
            throw new BankingException(String.format(
                    "Insufficient funds. Savings account requires a minimum balance of $%s. Current: $%s, Requested: $%s",
                    MINIMUM_BALANCE, balance, normalizedAmount));
        }

        this.balance = projectedBalance;
    }

    @Override
    public synchronized void applyPeriodicUpdate() {
        if (!active) return;
        // Monthly interest = Balance * (annual rate / 12)
        BigDecimal monthlyRate = interestRate.divide(new BigDecimal("12"), 6, RoundingMode.HALF_UP);
        BigDecimal interestEarned = this.balance.multiply(monthlyRate).setScale(2, RoundingMode.HALF_UP);
        if (interestEarned.compareTo(BigDecimal.ZERO) > 0) {
            this.balance = this.balance.add(interestEarned);
        }
    }

    @Override
    public String getAccountType() {
        return "SAVINGS";
    }

    @Override
    public String getSpecialAttribute() {
        return interestRate.toPlainString();
    }
}
