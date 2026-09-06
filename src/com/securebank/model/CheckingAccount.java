package com.securebank.model;

import com.securebank.service.BankingException;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Instant;

/**
 * Concrete implementation of a Checking Account demonstrating Inheritance and Polymorphism.
 * Supports configurable overdraft protection and overdraft penalty fees.
 *
 * @author Tinevimbo Musingadi (H250125B)
 */
public class CheckingAccount extends Account {

    private final BigDecimal overdraftLimit;
    private static final BigDecimal OVERDRAFT_FEE = new BigDecimal("5.00");

    public CheckingAccount(String accountNumber, String userId, BigDecimal initialBalance,
                           BigDecimal overdraftLimit, Instant createdAt, boolean active) {
        super(accountNumber, userId, initialBalance, createdAt, active);
        this.overdraftLimit = overdraftLimit != null ?
                overdraftLimit.setScale(2, RoundingMode.HALF_UP) :
                new BigDecimal("100.00");
    }

    public BigDecimal getOverdraftLimit() {
        return overdraftLimit;
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
        BigDecimal maxAllowedWithdrawal = this.balance.add(this.overdraftLimit);

        if (normalizedAmount.compareTo(maxAllowedWithdrawal) > 0) {
            throw new BankingException(String.format(
                    "Transaction declined. Exceeds available balance ($%s) plus overdraft protection ($%s).",
                    balance, overdraftLimit));
        }

        this.balance = this.balance.subtract(normalizedAmount);

        // If balance dips into overdraft, apply standard overdraft fee
        if (this.balance.compareTo(BigDecimal.ZERO) < 0) {
            this.balance = this.balance.subtract(OVERDRAFT_FEE);
        }
    }

    @Override
    public synchronized void applyPeriodicUpdate() {
        // Checking account maintenance hook
    }

    @Override
    public String getAccountType() {
        return "CHECKING";
    }

    @Override
    public String getSpecialAttribute() {
        return overdraftLimit.toPlainString();
    }
}
