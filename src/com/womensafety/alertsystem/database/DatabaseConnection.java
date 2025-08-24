package com.womensafety.alertsystem.database;
import java.sql.*;

// Database connection class
class DatabaseConnection{
    public static void main(String[] args) throws Exception{
        // Database connection parameters
        String dburl="jdbc:mysql://localhost:3306/WomenSafetyDB"; 
        String dbuser="root";
        String dbpass=""; 

        Connection con=DriverManager.getConnection(dburl,dbuser,dbpass);

        // Check if connection was successful
        if(con!=null){
            System.out.println("Connection is successful.");
        }else{
            System.out.println("Connection failed.");
        }
    }
}
