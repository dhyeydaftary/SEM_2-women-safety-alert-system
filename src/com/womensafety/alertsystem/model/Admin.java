package com.womensafety.alertsystem.model;

public class Admin extends Person {
    
    private int id;

    public Admin(int id, String name, String phone, String email, String password) {
        super(id, name, phone, email, password);
        this.id=id;
        this.role = Role.ADMIN;
    }
    
    @Override
    public String toString() {
        return "Admin ID: " + id +
               "\nName: " + name +
               "\nPhone: +91 " + phone +
               "\nEmail: " + email +
               "\nRole: " + role.getDisplayName();
    }
}
