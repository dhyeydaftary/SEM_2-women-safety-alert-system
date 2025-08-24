package com.womensafety.alertsystem.model;

import com.womensafety.alertsystem.util.Constants;
import java.util.regex.Pattern;

// Inherits from Person class
public class User extends Person{
    private int id;
    private String location;
    private double x=0.0,y=0.0;

    // Constructor for User
    public User(int Id, String Name, String Phone, String Email, String Location, String Zone, String Password){
        super(Name, Phone, Email, Zone, Password);
        this.id=Id;
        this.location=Location;
        this.password=Password;
        this.role=Role.USER; // Set role to USER

        // Validation checks
        if (this.id <= 0)
            throw new IllegalArgumentException("User ID must be positive");
        if (name==null || name.trim().isEmpty())
            throw new IllegalArgumentException("User name cannot be empty");

        if (!Pattern.matches(Constants.PHONE_PATTERN, Phone))
            throw new IllegalArgumentException("Invalid phone number. It must be 10 digits.");
        if (!Pattern.matches(Constants.EMAIL_PATTERN, Email))
            throw new IllegalArgumentException("Invalid email format.");

        if(this.location==null || this.location.trim().isEmpty())
            throw new IllegalArgumentException("Location cannot be empty");
        if(!Pattern.matches(Constants.ZONE_PATTERN, Zone))
            throw new IllegalArgumentException("Invalid zone. It must be North, South, East, or West.");
        if(Password==null || Password.trim().length() < 6)
            throw new IllegalArgumentException("Password must be at least 6 characters long.");
    }

    // Sets the user's location description
    public void setLocation(String location){
        this.location=location;
    }
    // Sets the user's X coordinate
    public void setX(double x){
        this.x=x;
    }
    // Sets the user's Y coordinate
    public void setY(double y){
        this.y=y;
    }

    
    // Gets the user's ID
    public int getId(){
        return id;
    }
    // Gets the user's location description
    public String getLocation(){
        return location;
    }
    // Gets the user's X coordinate
    public double getX(){
        return x;
    }
    // Gets the user's Y coordinate
    public double getY(){
        return y;
    }

    // Returns a string representation of the user
    @Override
    public String toString() {
        return "User Id: "+ id+
                "\nName: " + name +
                "\nLocation: " + location + " " + Constants.COORDINATES + "(" +
                String.format("%.4f", x) + ", " +
                String.format("%.4f", y) + ")" + Constants.RESET +
                "\nZone: " + zone;
    }
}
