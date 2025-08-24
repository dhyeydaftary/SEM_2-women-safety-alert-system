package com.womensafety.alertsystem.model;

// Extends the Person base class
public class Admin extends Person {
    
    private int id; // Admin ID

    // Constructor for creating an Admin instance
    public Admin(int id, String name, String phone, String email, String password) {
        super(id, name, phone, email, password);
        this.id=id;
        this.role = Role.ADMIN; // Set role to ADMIN
    }
    
    // Overridden toString method
    @Override
    public String toString() {
        return "Admin ID: " + id +
               "\nName: " + name +
               "\nPhone: +91 " + phone +
               "\nEmail: " + email +
               "\nRole: " + role.getDisplayName();
    }
}
