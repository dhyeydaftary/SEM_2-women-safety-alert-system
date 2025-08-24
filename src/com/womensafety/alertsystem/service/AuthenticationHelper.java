package com.womensafety.alertsystem.service;

import com.womensafety.alertsystem.model.User;
import com.womensafety.alertsystem.model.Responder;

// Authentication helper class for password validation and user authentication
public class AuthenticationHelper{
    // Validates if a password meets the required criteria
    public static boolean isValidPassword(String password) {
        if (password == null || password.trim().isEmpty()) {
            System.out.println("Password is null or empty");
            return false; // Invalid password
        }
        if (password.length() < 6) {
            System.out.println("Password length must be at least 6.");
            return false; // Invalid password
        }
        return true; // Password is valid
    }

    // Authenticates a user by comparing entered password with stored password
    public static boolean authenticateUser(User user, String enteredPassword) {
        return user != null && user.getPassword().equals(enteredPassword); // Check user exists and passwords match
    }

    // Authenticates a responder by comparing entered password with stored password
    public static boolean authenticateResponder(Responder responder, String enteredPassword) {
        return responder != null && responder.getPassword().equals(enteredPassword); // Check responder exists and passwords match
    }
}