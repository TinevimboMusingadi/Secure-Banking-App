package com.securebank.model;

/**
 * Role-Based Access Control (RBAC) definitions.
 * Separates administrative operations from normal customer banking actions,
 * adhering to the Principle of Least Privilege.
 *
 * @author Tinevimbo Musingadi (H250125B)
 */
public enum Role {
    CUSTOMER("Customer", "Standard banking privileges"),
    ADMIN("Administrator", "Privileged access for user management and audit analysis");

    private final String displayName;
    private final String description;

    Role(String displayName, String description) {
        this.displayName = displayName;
        this.description = description;
    }

    public String getDisplayName() {
        return displayName;
    }

    public String getDescription() {
        return description;
    }

    public static Role fromString(String roleStr) {
        if (roleStr == null) {
            return CUSTOMER;
        }
        for (Role role : values()) {
            if (role.name().equalsIgnoreCase(roleStr.trim())) {
                return role;
            }
        }
        return CUSTOMER;
    }
}
