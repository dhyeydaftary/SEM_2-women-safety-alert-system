package com.womensafety.alertsystem.model;

// Enum defining user roles within the system
public enum Role {
    USER("User", "Can raise alerts and manage personal profile"), // Role for regular users
    RESPONDER("Responder", "Can process alerts and manage availability"), // Role for responders
    ADMIN("Administrator", "System administration and monitoring"); // Role for administrators
    
    private final String displayName;
    private final String description;
    
    // Constructor for Role enum
    Role(String displayName, String description) {
        this.displayName = displayName;
        this.description = description;
    }
    
    // Gets the display name of the role
    public String getDisplayName() {
        return displayName;
    }
    
    // Gets the description of the role
    public String getDescription() {
        return description;
    }
}
