package com.womensafety.alertsystem.database;

import java.sql.*;

class DatabaseConnection{
    public static void main(String[] args) throws Exception{
        String dburl="jdbc:mysql://localhost:3306/WomenSafetyDB";
        String dbuser="root";
        String dbpass="";

        Connection con=DriverManager.getConnection(dburl,dbuser,dbpass);

        if(con!=null){
            System.out.println("Connection is successful.");
        }else{
            System.out.println("Connection failed.");
        }
    }
}
