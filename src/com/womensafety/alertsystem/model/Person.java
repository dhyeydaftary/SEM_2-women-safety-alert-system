package com.womensafety.alertsystem.model;

// Base Person class representing a generic user in the system
// Serves as parent class for User, Responder, and Admin classes
public class Person{
    protected int id;
    protected String name;
    protected String phone;
    protected String email;
    protected String zone;
    protected String password;
    protected Role role;

    // Constructor for Person with ID
    public Person(int id, String Name, String Phone, String Email, String password){
        this.id = id;
        this.name=Name;
        this.phone=Phone;
        this.email=Email;
        this.password=password;
    }
    
    // Constructor for Person without ID (for new registrations)
    public Person(String Name, String Phone, String Email, String Zone, String password){
        this.name=Name;
        this.phone=Phone;
        this.email=Email;
        this.zone=Zone;
        this.password=password;
    }

    // Gets the person's ID
    public int getId() { 
        return id; 
    }
    // Gets the person's name
    public String getName(){ 
        return name; 
    }
    // Gets the person's phone number
    public String getPhone(){ 
        return phone; 
    }
    // Gets the person's email address
    public String getEmail(){ 
        return email; 
    }
    // Gets the person's geographic zone
    public String getZone(){ 
        return zone; 
    }
    // Gets the person's password
    public String getPassword(){ 
        return password; 
    }
    // Gets the person's role
    public Role getRole() { 
        return role; 
    }


    // Sets the person's name
    public void setName(String name){ 
        this.name=name; 
    }
    // Sets the person's phone number
    public void setPhone(String phone){ 
        this.phone=phone; 
    }
    // Sets the person's email address
    public void setEmail(String email){ 
        this.email=email; 
    }
    // Sets the person's geographic zone
    public void setZone(String zone){ 
        this.zone=zone; 
    }
    // Sets the person's password
    public void setPassword(String password){ 
        this.password=password; 
    }
    // Sets the person's role
    public void setRole(Role role) { 
        this.role = role; 
    }
}
