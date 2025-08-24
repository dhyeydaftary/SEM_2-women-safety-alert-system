package com.womensafety.alertsystem.model;

import com.womensafety.alertsystem.util.Constants;
import java.util.regex.Pattern;

// Inherits from Person class
public class Responder extends Person{
    private int id;
    private boolean available;
    private double x=0.0,y=0.0;

    // Constructor for Responder
    public Responder(int Id, String Name, String Phone, String Email, String Zone, boolean Available, String Password){
        super(Name, Phone, Email, Zone, Password);
        this.id=Id;
        this.password=Password;
        this.role = Role.RESPONDER; // Set role to RESPONDER

        // Validation checks
        if (this.id <= 0)
            throw new IllegalArgumentException("Responder ID must be positive");
        if (name==null || name.trim().isEmpty())
            throw new IllegalArgumentException("Responder name cannot be empty");

        if (!Pattern.matches(Constants.PHONE_PATTERN, Phone))
            throw new IllegalArgumentException("Invalid phone number. It must be 10 digits.");
        if (!Pattern.matches(Constants.EMAIL_PATTERN, Email))
            throw new IllegalArgumentException("Invalid email format.");

        if(!Pattern.matches(Constants.ZONE_PATTERN, Zone))
            throw new IllegalArgumentException("Invalid zone. It must be North, South, East, or West.");
        if(Password==null || Password.trim().length() < 6)
            throw new IllegalArgumentException("Password must be at least 6 characters long.");

        this.available=Available; // Set availability status
    }

    // Sets the responder's availability status
    public void setAvailable(boolean available){
        this.available=available;
    }
    // Sets the responder's X coordinate
    public void setX(double x){
        this.x=x;
    }
    // Sets the responder's Y coordinate
    public void setY(double y){
        this.y=y;
    }


    // Gets the responder's ID
    public int getId(){
        return id;
    }
    // Checks if the responder is available
    public boolean isAvailable(){
        return available;
    }
    // Gets the responder's X coordinate
    public double getX(){
        return x;
    }
    // Gets the responder's Y coordinate
    public double getY(){
        return y;
    }

    // Returns a string representation of the responder
    @Override
    public String toString() {
        return "Responder Id: " + id +
                "\nName: " + name +
                "\nPhone No: " + phone +
                "\nEmail: " + email +
                "\nZone: " + zone +
                "\nAvailable: " + available +
                "\nLocation: " + Constants.COORDINATES + "(" +
                String.format("%.4f", x) + ", " +
                String.format("%.4f", y) + ")" + Constants.RESET;
    }

    // Notifies the user when an alert is assigned to the responder
    public void notifyUserAssigned(User user) {
        System.out.println(Constants.INFO + "\n=== NEW ALERT ASSIGNED TO YOU ===" + Constants.RESET);
        System.out.println(Constants.BLUE + "Responder ID: " + Constants.RESET + this.getId());
        System.out.println(Constants.BLUE + "Responder name: "  + Constants.RESET + this.getName());
        System.out.println("");

        System.out.println("User details:");
        System.out.println(Constants.BLUE + "Name: " + Constants.RESET + user.getName());
        System.out.println(Constants.BLUE + "Phone: +91 " + Constants.RESET + user.getPhone());
        System.out.println(Constants.BLUE +"Email: " + Constants.RESET + user.getEmail());
        System.out.println(Constants.BLUE + "Location: " + Constants.RESET + user.getLocation());
        System.out.println(Constants.BLUE + "Zone: " + Constants.RESET + user.getZone());
        System.out.println(Constants.COORDINATES + "Coordinates: " + Constants.RESET + "(" +String.format("%.4f", user.getX()) +
                ", " +String.format("%.4f", user.getY()) + ")");
        System.out.println("=".repeat(40));
        System.out.flush();
    }
}
