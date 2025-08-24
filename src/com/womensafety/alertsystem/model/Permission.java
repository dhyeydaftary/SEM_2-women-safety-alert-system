package com.womensafety.alertsystem.model;

// Enum defining all system permissions with descriptions
public enum Permission {
    // User permissions
    REGISTER_USER("Register as new user"),
    LOGIN_USER("Login as user"),
    RAISE_ALERT("Raise emergency alert"),
    UPDATE_USER_DETAILS("Update user profile details"),
    
    // Responder permissions
    REGISTER_RESPONDER("Register as new responder"),
    LOGIN_RESPONDER("Login as responder"),
    PROCESS_NEXT_ALERT("Process pending alerts"),
    COMPLETE_ALERT("Complete assigned alerts"),
    SHOW_PENDING_ALERTS("View pending alerts"),
    UPDATE_RESPONDER_DETAILS("Update responder profile details"),
    
    // Admin permissions
    CREATE_ADMIN("Create new admin account"),
    LOGIN_ADMIN("Login as admin"),
    DISPLAY_ALL_USERS("View all registered users"),
    DISPLAY_ALL_RESPONDERS("View all registered responders"),
    VIEW_PENDING_ALERTS("View pending alerts for processing"),
    VIEW_SYSTEM_STATISTICS("View system statistics"),
    
    // Common permissions
    EXIT_SYSTEM("Exit the system");
    
    private final String description; // Description of the permission
    
    // Constructor for Permission enum
    Permission(String description) {
        this.description = description;
    }
    
    // Gets the description of the permission
    public String getDescription() {
        return description;
    }
}
