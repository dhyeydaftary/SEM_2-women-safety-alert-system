package com.womensafety.alertsystem.security;

import java.util.*;
import com.womensafety.alertsystem.model.Role;
import com.womensafety.alertsystem.model.Permission;

public class RolePermissionManager {
    private static final Map<Role, Set<Permission>> rolePermissions = new HashMap<>();
    
    static {
        // User permissions
        rolePermissions.put(Role.USER, Set.of(
            Permission.REGISTER_USER,
            Permission.LOGIN_USER,
            Permission.RAISE_ALERT,
            Permission.UPDATE_USER_DETAILS,
            Permission.EXIT_SYSTEM
        ));
        
        // Responder permissions
        rolePermissions.put(Role.RESPONDER, Set.of(
            Permission.REGISTER_RESPONDER,
            Permission.LOGIN_RESPONDER,
            Permission.PROCESS_NEXT_ALERT,
            Permission.COMPLETE_ALERT,
            Permission.SHOW_PENDING_ALERTS,
            Permission.UPDATE_RESPONDER_DETAILS,
            Permission.EXIT_SYSTEM
        ));
        
        // Admin permissions
        rolePermissions.put(Role.ADMIN, Set.of(
            Permission.CREATE_ADMIN,
            Permission.LOGIN_ADMIN,
            Permission.DISPLAY_ALL_USERS,
            Permission.DISPLAY_ALL_RESPONDERS,
            Permission.VIEW_PENDING_ALERTS,
            Permission.VIEW_SYSTEM_STATISTICS,
            Permission.EXIT_SYSTEM
        ));
    }
    
    public static boolean hasPermission(Role role, Permission permission) {
        Set<Permission> permissions = rolePermissions.get(role);
        return permissions != null && permissions.contains(permission);
    }
    
    public static Set<Permission> getPermissions(Role role) {
        return rolePermissions.getOrDefault(role, Collections.emptySet());
    }
}
