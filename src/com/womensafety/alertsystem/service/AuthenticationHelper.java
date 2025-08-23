package com.womensafety.alertsystem.service;

import com.womensafety.alertsystem.model.User;
import com.womensafety.alertsystem.model.Responder;

public class AuthenticationHelper{
    public static boolean isValidPassword(String password) {
        if (password == null || password.trim().isEmpty()) {
            System.out.println("Password is null or empty");
            return false;
        }
        if (password.length() < 6) {
            System.out.println("Password length must be at least 6.");
            return false;
        }
        return true;
    }

    public static boolean authenticateUser(User user, String enteredPassword) {
        return user != null && user.getPassword().equals(enteredPassword);
    }

    public static boolean authenticateResponder(Responder responder, String enteredPassword) {
        return responder != null && responder.getPassword().equals(enteredPassword);
    }
}