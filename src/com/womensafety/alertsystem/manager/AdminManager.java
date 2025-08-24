package com.womensafety.alertsystem.manager;

import com.womensafety.alertsystem.model.*;
import com.womensafety.alertsystem.security.RBACManager;
import com.womensafety.alertsystem.util.SystemLogger;
import java.sql.*;

// Manages admin creation, authentication, system statistics, and validation
public class AdminManager {
    
    // Creates a new admin user in the system
    public Admin createAdmin(String name, String phone, String email, String password) {
        try {
            String dburl = "jdbc:mysql://localhost:3306/WomenSafetyDB";
            String dbuser = "root";
            String dbpass = "";
            Connection con = DriverManager.getConnection(dburl, dbuser, dbpass);
            
            // Get next admin ID
            String getIdQuery = "SELECT MAX(Admin_id) as max_id FROM admin_details"; // Query to get max admin ID
            PreparedStatement getIdPst = con.prepareStatement(getIdQuery);
            ResultSet rs = getIdPst.executeQuery();
            int adminId = 1; // Default admin ID
            if (rs.next()) {
                adminId = rs.getInt("max_id") + 1; // Increment max ID for new admin
            }
            
            String sql = "INSERT INTO admin_details (Admin_id, Name, Phone_no, Email, Password, Role) VALUES (?, ?, ?, ?, ?, ?)"; // Insert query
            PreparedStatement pst = con.prepareStatement(sql);
            pst.setInt(1, adminId);
            pst.setString(2, name);
            pst.setString(3, phone);
            pst.setString(4, email);
            pst.setString(5, password);
            pst.setString(6, Role.ADMIN.toString());
            
            int result = pst.executeUpdate();
            
            rs.close();
            getIdPst.close();
            pst.close();
            con.close();
            
            if (result > 0) {
                Admin admin = new Admin(adminId, name, phone, email, password); // Create Admin object
                SystemLogger.success("Admin created successfully with ID: " + adminId); // Log success
                return admin; // Return created admin
            }
        } catch (Exception e) {
            SystemLogger.error("Failed to create admin: " + e.getMessage()); // Log error
        }
        return null; // Return null if creation failed
    }
    
    // Authenticates admin login credentials
    public Admin authenticateAdminLogin(int adminId, String password) {
        try {
            String dburl = "jdbc:mysql://localhost:3306/WomenSafetyDB";
            String dbuser = "root";
            String dbpass = "";
            Connection con = DriverManager.getConnection(dburl, dbuser, dbpass);
            
            String query = "SELECT * FROM admin_details WHERE Admin_id = ? AND Password = ?"; // Authentication query
            PreparedStatement pst = con.prepareStatement(query);
            pst.setInt(1, adminId);
            pst.setString(2, password);
            
            ResultSet rs = pst.executeQuery(); // Execute query
            
            if (rs.next()) {
                Admin admin = new Admin( // Create Admin object from result set
                    rs.getInt("Admin_id"),
                    rs.getString("Name"),
                    rs.getString("Phone_no"),
                    rs.getString("Email"),
                    rs.getString("Password")
                );
                
                rs.close();
                pst.close();
                con.close();
                
                RBACManager.setCurrentUser(admin); // Set current user in RBAC system
                return admin; // Return authenticated admin
            }
            
            rs.close();
            pst.close();
            con.close();
        } catch (Exception e) {
            SystemLogger.error("Database authentication error: " + e.getMessage()); // Log error
        }
        return null; // Return null if authentication failed
    }
    
    // Displays system statistics including user count, responder count, and alert status
    public void displaySystemStatistics() {
        try {
            String dburl = "jdbc:mysql://localhost:3306/WomenSafetyDB";
            String dbuser = "root";
            String dbpass = "";
            Connection con = DriverManager.getConnection(dburl, dbuser, dbpass);
            
            // Get user count
            String userQuery = "SELECT COUNT(*) as count FROM user_details"; // User count query
            PreparedStatement userPst = con.prepareStatement(userQuery);
            ResultSet userRs = userPst.executeQuery();
            int userCount = 0; // Initialize user count
            if (userRs.next()) userCount = userRs.getInt("count"); // Get user count
            
            // Get responder count
            String respQuery = "SELECT COUNT(*) as count FROM responder_details"; // Responder count query
            PreparedStatement respPst = con.prepareStatement(respQuery);
            ResultSet respRs = respPst.executeQuery();
            int responderCount = 0; // Initialize responder count
            if (respRs.next()) responderCount = respRs.getInt("count"); // Get responder count
            
            // Get alert statistics
            String alertQuery = "SELECT Status, COUNT(*) as count FROM alert_details GROUP BY Status"; // Alert statistics query
            PreparedStatement alertPst = con.prepareStatement(alertQuery);
            ResultSet alertRs = alertPst.executeQuery();
            
            System.out.println("\n=== SYSTEM STATISTICS ===");
            System.out.println("Total Users: " + userCount); // Print user count
            System.out.println("Total Responders: " + responderCount); // Print responder count
            System.out.println("\nAlert Statistics:");
            while (alertRs.next()) {
                System.out.println("- " + alertRs.getString("Status") + ": " + alertRs.getInt("count")); // Print each alert status count
            }
            
            userRs.close(); 
            userPst.close();
            respRs.close(); 
            respPst.close();
            alertRs.close(); 
            alertPst.close();
            con.close();
            
        } catch (Exception e) {
            SystemLogger.error("Error retrieving system statistics: " + e.getMessage()); // Log error
        }
    }

    // Checks if a phone number already exists in the admin database
    public boolean isPhoneExists(String phone) {
        String dbUrl = "jdbc:mysql://localhost:3306/WomenSafetyDB";
        String dbUser = "root";
        String dbPass = "";

        String query = "SELECT COUNT(*) FROM admin_details WHERE Phone_no = ?"; // Phone existence query
        try (Connection conn = DriverManager.getConnection(dbUrl, dbUser, dbPass);
             PreparedStatement stmt = conn.prepareStatement(query)) {
            stmt.setString(1, phone);
            ResultSet rs = stmt.executeQuery();
            if (rs.next()) {
                return rs.getInt(1) > 0;
            }
        } catch (SQLException e) {
            e.printStackTrace(); // Print stack trace on error
        }
        return false; // Return false if phone doesn't exist or error occurs
    }

}