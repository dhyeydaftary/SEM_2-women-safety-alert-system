package com.womensafety.alertsystem.security;

import java.util.*;
import com.womensafety.alertsystem.model.Role;
import com.womensafety.alertsystem.model.Permission;

// Role-Permission Manager for managing permissions associated with different roles
public class RolePermissionManager {
    private static final Map<Role, Set<Permission>> rolePermissions = new HashMap<>(); // Map to store role-permission mappings
    
    static {
        // User permissions
        rolePermissions.put(Role.USER, Set.of( // Assign permissions to USER role
            Permission.REGISTER_USER,
            Permission.LOGIN_USER,
            Permission.RAISE_ALERT,
            Permission.UPDATE_USER_DETAILS,
            Permission.EXIT_SYSTEM
        ));
        
        // Responder permissions
        rolePermissions.put(Role.RESPONDER, Set.of( // Assign permissions to RESPONDER role
            Permission.REGISTER_RESPONDER,
            Permission.LOGIN_RESPONDER,
            Permission.PROCESS_NEXT_ALERT,
            Permission.COMPLETE_ALERT,
            Permission.SHOW_PENDING_ALERTS,
            Permission.UPDATE_RESPONDER_DETAILS,
            Permission.EXIT_SYSTEM
        ));
        
        // Admin permissions
        rolePermissions.put(Role.ADMIN, Set.of( // Assign permissions to ADMIN role
            Permission.CREATE_ADMIN,
            Permission.LOGIN_ADMIN,
            Permission.DISPLAY_ALL_USERS,
            Permission.DISPLAY_ALL_RESPONDERS,
            Permission.VIEW_PENDING_ALERTS,
            Permission.VIEW_SYSTEM_STATISTICS,
            Permission.EXIT_SYSTEM
        ));
    }
    
    // Checks if a role has a specific permission
    // Returns: true if the role has the permission, false otherwise
    public static boolean hasPermission(Role role, Permission permission) {
        Set<Permission> permissions = rolePermissions.get(role); // Get permissions for the role
        return permissions != null && permissions.contains(permission); // Check if permission is present
    }
    
    // Gets all permissions associated with a role
    // Returns: a set of permissions for the role, or an empty set if none
    public static Set<Permission> getPermissions(Role role) {
        return rolePermissions.getOrDefault(role, Collections.emptySet()); // Return permissions or empty set
    }
}
