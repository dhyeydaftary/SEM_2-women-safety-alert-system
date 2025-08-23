package com.womensafety.alertsystem.model;

public enum Role {
    USER("User", "Can raise alerts and manage personal profile"),
    RESPONDER("Responder", "Can process alerts and manage availability"),
    ADMIN("Administrator", "System administration and monitoring");
    
    private final String displayName;
    private final String description;
    
    Role(String displayName, String description) {
        this.displayName = displayName;
        this.description = description;
    }
    
    public String getDisplayName() { return displayName; }
    public String getDescription() { return description; }
}
