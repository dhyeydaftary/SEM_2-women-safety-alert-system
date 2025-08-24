package com.womensafety.alertsystem.security;

import com.womensafety.alertsystem.model.*;
import com.womensafety.alertsystem.util.SystemLogger;

// Role-Based Access Control (RBAC) manager for user authentication and permission checking
public class RBACManager {
    private static Person currentLoggedInPerson = null; // Currently logged-in user
    
    // Sets the current logged-in user
    // Logs the login event
    public static void setCurrentUser(Person person) {
        currentLoggedInPerson = person; // Set current user
        if (person != null) {
            SystemLogger.info("User logged in: " + person.getName() + " (Role: " + person.getRole().getDisplayName() + ")"); // Log login event
        }
    }
    
    // Gets the current logged-in user
    // Returns: the current user, or null if no user is logged in
    public static Person getCurrentUser() {
        return currentLoggedInPerson; // Return current user
    }
    
    // Logs out the current user
    public static void logout() {
        if (currentLoggedInPerson != null) {
            SystemLogger.info("User logged out: " + currentLoggedInPerson.getName()); // Log logout event
            currentLoggedInPerson = null; // Clear current user
        }
    }
    
    // Checks if a user is currently logged in
    // Returns: true if a user is logged in, false otherwise
    public static boolean isLoggedIn() {
        return currentLoggedInPerson != null; // Check if user exists
    }
    
    // Checks if the current user has a specific permission
    // Returns: true if user has permission, false otherwise
    public static boolean hasPermission(Permission permission) {
        if (currentLoggedInPerson == null) {
            return false; // No user logged in, no permissions
        }
        return RolePermissionManager.hasPermission(
            currentLoggedInPerson.getRole(), permission); // Delegate to RolePermissionManager
    }
    
    // Checks if the current user has a specific role
    // Returns: true if user has the role, false otherwise
    public static boolean hasRole(Role role) {
        return currentLoggedInPerson != null && 
               currentLoggedInPerson.getRole() == role; // Direct role comparison
    }
    
    // Validates that the current user has a specific permission
    // Throws: SecurityException if user doesn't have the permission
    public static void checkPermission(Permission permission) throws SecurityException {
        if (!hasPermission(permission)) {
            throw new SecurityException("Access denied: " + permission.getDescription()); // Throw security exception
        }
    }
    
    // Gets the role of the current user
    // Returns: the current user's role, or null if no user is logged in
    public static Role getCurrentRole() {
        return currentLoggedInPerson != null ? currentLoggedInPerson.getRole() : null; // Return role or null
    }
}
