package com.securebank.model;

/**
 * Enumeration of supported banking transaction operations.
 *
 * @author Tinevimbo Musingadi (H250125B)
 */
public enum TransactionType {
    DEPOSIT("Deposit", true),
    WITHDRAWAL("Withdrawal", false),
    TRANSFER_IN("Transfer Received", true),
    TRANSFER_OUT("Transfer Sent", false),
    INTEREST("Interest Credited", true),
    FEE("Service Fee", false);

    private final String label;
    private final boolean isCredit;

    TransactionType(String label, boolean isCredit) {
        this.label = label;
        this.isCredit = isCredit;
    }

    public String getLabel() {
        return label;
    }

    public boolean isCredit() {
        return isCredit;
    }

    public static TransactionType fromString(String typeStr) {
        if (typeStr == null) {
            return DEPOSIT;
        }
        for (TransactionType t : values()) {
            if (t.name().equalsIgnoreCase(typeStr.trim())) {
                return t;
            }
        }
        return DEPOSIT;
    }
}
