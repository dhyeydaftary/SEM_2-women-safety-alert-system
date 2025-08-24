package com.womensafety.alertsystem.util; // Utility classes for the Women's Safety Alert System

// Constants class containing all application-wide constant values
public class Constants {
    // Validation pattern constants
    public static final String EMAIL_PATTERN = "^[A-Za-z0-9+_.-]+@gmail\\.com$"; // Regex pattern for Gmail validation
    public static final String PHONE_PATTERN = "^(7|8|9)\\d{9}$"; // Regex pattern for Indian phone numbers (10 digits starting with 7,8,9)
    public static final String ZONE_PATTERN = "(?i)^(North|South|East|West)$"; // Regex pattern for zone validation (case insensitive)

    // Alert status constants
    public static final String STATUS_ACTIVE = "Active";
    public static final String STATUS_ASSIGNED = "ASSIGNED";
    public static final String STATUS_WAITING = "WAITING";
    public static final String STATUS_RESOLVED = "Resolved";

    // Timing and configuration constants
    public static final int RESPONDER_STATUS_CHECKER_INTERVAL = 60000; // 60 seconds interval for responder status checking
    public static final int PENDING_ALERT_CHECKER_INTERVAL= 45000; // 45 seconds interval for pending alert checking
    public static boolean ENABLE_BACKGROUND_LOGGING = false; // Flag to enable/disable background logging

    // Geographic boundary constants for India
    public static final double INDIA_MIN_LAT = 8.4;
    public static final double INDIA_MAX_LAT = 37.6;
    public static final double INDIA_MIN_LNG = 68.7;
    public static final double INDIA_MAX_LNG = 97.25;

    // ANSI color codes for console output
    public static final String RESET ="\u001b[0m"; 
    public static final String ERROR = "\u001B[31m"; 
    public static final String SUCCESS = "\u001B[32m"; 
    public static final String WARNING = "\u001B[33m"; 
    public static final String INFO = "\u001B[37m"; 
    public static final String CYAN = "\u001B[1;36m"; 
    public static final String BLUE = "\u001B[34m";
    public static final String MAGENTA_ITALIC_BOLD = "\u001B[35m\u001B[3m\u001B[1m"; 
    public static final String COORDINATES = "\u001B[35m\u001B[3m\u001B[1m"; 
    public static final String TIMESTAMP = "\u001B[35m\u001B[3m\u001B[1m"; 
}