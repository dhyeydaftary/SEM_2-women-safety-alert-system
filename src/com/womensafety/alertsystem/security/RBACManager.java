package com.womensafety.alertsystem.security;

import com.womensafety.alertsystem.model.*;
import com.womensafety.alertsystem.util.SystemLogger;

public class RBACManager {
    private static Person currentLoggedInPerson = null;
    
    public static void setCurrentUser(Person person) {
        currentLoggedInPerson = person;
        if (person != null) {
            SystemLogger.info("User logged in: " + person.getName() + " (Role: " + person.getRole().getDisplayName() + ")");
        }
    }
    
    public static Person getCurrentUser() {
        return currentLoggedInPerson;
    }
    
    public static void logout() {
        if (currentLoggedInPerson != null) {
            SystemLogger.info("User logged out: " + currentLoggedInPerson.getName());
            currentLoggedInPerson = null;
        }
    }
    
    public static boolean isLoggedIn() {
        return currentLoggedInPerson != null;
    }
    
    public static boolean hasPermission(Permission permission) {
        if (currentLoggedInPerson == null) {
            return false;
        }
        return RolePermissionManager.hasPermission(
            currentLoggedInPerson.getRole(), permission);
    }
    
    public static boolean hasRole(Role role) {
        return currentLoggedInPerson != null && 
               currentLoggedInPerson.getRole() == role;
    }
    
    public static void checkPermission(Permission permission) throws SecurityException {
        if (!hasPermission(permission)) {
            throw new SecurityException("Access denied: " + permission.getDescription());
        }
    }
    
    public static Role getCurrentRole() {
        return currentLoggedInPerson != null ? currentLoggedInPerson.getRole() : null;
    }
}
