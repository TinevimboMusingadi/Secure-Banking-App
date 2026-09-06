package com.securebank.model;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Instant;
import java.util.Objects;
import java.util.UUID;

/**
 * Immutable transaction audit model representing financial movements.
 * Adheres to secure design principles:
 * - Immutability prevents tampering of in-memory transaction logs.
 * - Exact financial precision with BigDecimal.
 *
 * @author Tinevimbo Musingadi (H250125B)
 */
public final class Transaction {

    private final String transactionId;
    private final Instant timestamp;
    private final String sourceAccountNumber;
    private final String destinationAccountNumber;
    private final TransactionType type;
    private final BigDecimal amount;
    private final BigDecimal resultingBalance;
    private final String description;
    private final String status; // SUCCESS or FAILED

    public Transaction(String transactionId, Instant timestamp, String sourceAccountNumber,
                       String destinationAccountNumber, TransactionType type, BigDecimal amount,
                       BigDecimal resultingBalance, String description, String status) {
        this.transactionId = transactionId != null ? transactionId : UUID.randomUUID().toString();
        this.timestamp = timestamp != null ? timestamp : Instant.now();
        this.sourceAccountNumber = Objects.requireNonNull(sourceAccountNumber, "sourceAccountNumber required");
        this.destinationAccountNumber = destinationAccountNumber != null ? destinationAccountNumber : "N/A";
        this.type = Objects.requireNonNull(type, "TransactionType required");
        this.amount = amount != null ? amount.setScale(2, RoundingMode.HALF_UP) : BigDecimal.ZERO;
        this.resultingBalance = resultingBalance != null ? resultingBalance.setScale(2, RoundingMode.HALF_UP) : BigDecimal.ZERO;
        this.description = description != null ? description.trim() : "";
        this.status = status != null ? status : "SUCCESS";
    }

    public static Transaction createSuccessful(String sourceAccount, String destAccount,
                                              TransactionType type, BigDecimal amount,
                                              BigDecimal balanceAfter, String desc) {
        return new Transaction(UUID.randomUUID().toString(), Instant.now(), sourceAccount,
                destAccount, type, amount, balanceAfter, desc, "SUCCESS");
    }

    public static Transaction createFailed(String sourceAccount, String destAccount,
                                           TransactionType type, BigDecimal amount,
                                           BigDecimal currentBalance, String reason) {
        return new Transaction(UUID.randomUUID().toString(), Instant.now(), sourceAccount,
                destAccount, type, amount, currentBalance, "FAILED: " + reason, "FAILED");
    }

    public String getTransactionId() {
        return transactionId;
    }

    public Instant getTimestamp() {
        return timestamp;
    }

    public String getSourceAccountNumber() {
        return sourceAccountNumber;
    }

    public String getDestinationAccountNumber() {
        return destinationAccountNumber;
    }

    public TransactionType getType() {
        return type;
    }

    public BigDecimal getAmount() {
        return amount;
    }

    public BigDecimal getResultingBalance() {
        return resultingBalance;
    }

    public String getDescription() {
        return description;
    }

    public String getStatus() {
        return status;
    }

    @Override
    public String toString() {
        return String.format("[%s] %s | %s | %s$%s | Bal: $%s | Status: %s | %s",
                timestamp, transactionId, type.getLabel(),
                type.isCredit() ? "+" : "-", amount, resultingBalance, status, description);
    }
}
