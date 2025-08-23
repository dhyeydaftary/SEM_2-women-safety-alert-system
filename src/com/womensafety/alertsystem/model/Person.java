package com.womensafety.alertsystem.model;

public class Person{
    protected int id;
    protected String name;
    protected String phone;
    protected String email;
    protected String zone;
    protected String password;
    protected Role role;

    public Person(int id, String Name, String Phone, String Email, String password){
        this.id = id;
        this.name=Name;
        this.phone=Phone;
        this.email=Email;
        this.password=password;
    }
    
    public Person(String Name, String Phone, String Email, String Zone, String password){
        this.name=Name;
        this.phone=Phone;
        this.email=Email;
        this.zone=Zone;
        this.password=password;
    }

    public int getId() { 
        return id; 
    }
    public String getName(){ 
        return name; 
    }
    public String getPhone(){ 
        return phone; 
    }
    public String getEmail(){ 
        return email; 
    }
    public String getZone(){ 
        return zone; 
    }
    public String getPassword(){ 
        return password; 
    }
    public Role getRole() { 
        return role; 
    }

    public void setName(String name){ 
        this.name=name; 
    }
    public void setPhone(String phone){ 
        this.phone=phone; 
    }
    public void setEmail(String email){ 
        this.email=email; 
    }
    public void setZone(String zone){ 
        this.zone=zone; 
    }
    public void setPassword(String password){ 
        this.password=password; 
    }
    public void setRole(Role role) { 
        this.role = role; 
    }
}
