package com.example.cashback.user.model;

/**
 * User role enumeration for the cashback system
 */
public enum Role {
    USER, // Regular customer
    MERCHANT, // Merchant/Shop owner
    ADMIN, // System administrator
    COMPLIANCE // Compliance officer (added for audit access)
}
