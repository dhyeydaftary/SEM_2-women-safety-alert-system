package com.womensafety.alertsystem.model;

import com.womensafety.alertsystem.util.Constants;
import java.util.regex.Pattern;

public class User extends Person{
    private int id;
    private String location;
    private double x=0.0,y=0.0;

    public User(int Id, String Name, String Phone, String Email, String Location, String Zone, String Password){
        super(Name, Phone, Email, Zone, Password);
        this.id=Id;
        this.location=Location;
        this.password=Password;
        this.role=Role.USER;

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

    public int getId(){
        return id;
    }
    public String getLocation(){
        return location;
    }
    public double getX(){
        return x;
    }
    public double getY(){
        return y;
    }


    public void setLocation(String location){
        this.location=location;
    }
    public void setX(double x){
        this.x=x;
    }
    public void setY(double y){
        this.y=y;
    }

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