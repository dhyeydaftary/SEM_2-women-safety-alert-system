package com.womensafety.alertsystem.model;

import com.womensafety.alertsystem.util.Constants;
import com.womensafety.alertsystem.util.SystemLogger;
import java.time.LocalDateTime;
import java.sql.*;

// Contains alert details, status tracking, and database persistence methods
public class Alert {
    private int alertId;
    private User user;
    private LocalDateTime timestamp;
    private String status;
    private Responder assignedResponder;

    // Constructor creates a new alert with default Active status
    public Alert(User user) {
        this.user = user;
        this.timestamp = LocalDateTime.now();
        this.status = Constants.STATUS_ACTIVE;
    }

    // Constructor creates an alert with all fields specified
    public Alert(int alertId, User user, LocalDateTime timestamp, String status, Responder assignedResponder) {
        this.alertId = alertId;
        this.user = user;
        this.timestamp = timestamp;
        this.status = status;
        this.assignedResponder = assignedResponder;
    }

    // Gets the next available alert ID from the database
    // Returns: next available alert ID
    private int getNextAlertIdFromDatabase() {
        int nextId = 1;
        try {
            String dburl = "jdbc:mysql://localhost:3306/WomenSafetyDB";
            String dbuser = "root";
            String dbpass = "";

            Connection con = DriverManager.getConnection(dburl, dbuser, dbpass);

            String query = "SELECT IFNULL(MAX(Alert_id), 0) AS max_id FROM alert_details";
            PreparedStatement pst = con.prepareStatement(query);
            ResultSet rs = pst.executeQuery();

            if (rs.next()) {
                nextId = rs.getInt("max_id") + 1;
            }

            rs.close();
            pst.close();
            con.close();

            SystemLogger.info("Next Alert ID initialized to: " + nextId);

        } catch (Exception e) {
            SystemLogger.error("Error getting next alert ID: " + e.getMessage());
        }
        return nextId;
    }

    // Saves the alert to the database with status history tracking
    // Returns: true if save successful, false if failed
    public boolean saveToDatabase() {
        try {
            String dburl = "jdbc:mysql://localhost:3306/WomenSafetyDB";
            String dbuser = "root";
            String dbpass = "";
            Connection con = DriverManager.getConnection(dburl, dbuser, dbpass);
            
            // Get the next available Alert_id using the same pattern as User/Responder
            this.alertId = getNextAlertIdFromDatabase();
            
            String insertQuery = "INSERT INTO alert_details (Alert_id, User_id, Responder_id, Status, Alert_time, X_coordinate, Y_coordinate) VALUES (?, ?, ?, ?, ?, ?, ?)";
            PreparedStatement pst = con.prepareStatement(insertQuery);
            pst.setInt(1, this.alertId);
            pst.setInt(2, this.user.getId());
            
            if (this.assignedResponder != null) {
                pst.setInt(3, this.assignedResponder.getId());
            } else {
                pst.setNull(3, Types.INTEGER);
            }
            
            pst.setString(4, this.status);
            pst.setTimestamp(5, Timestamp.valueOf(this.timestamp));
            pst.setDouble(6, this.user.getX());
            pst.setDouble(7, this.user.getY());
            
            int result = pst.executeUpdate();
            pst.close();
            
            if (result > 0) {
                String historySQL = "INSERT INTO alert_status_history (Alert_id, Previous_status, Current_status, Responder_id) VALUES (?, ?, ?, NULL)";
                
                try (PreparedStatement historyPst = con.prepareStatement(historySQL)) {
                    historyPst.setInt(1, this.alertId);
                    historyPst.setString(2, "NEW");
                    historyPst.setString(3, this.status);
                    historyPst.executeUpdate();
                    SystemLogger.info("Alert status history initialized for Alert ID: " + this.alertId);
                } catch (Exception e) {
                    SystemLogger.warning("Alert saved but failed to create status history: " + e.getMessage());
                }
                
                SystemLogger.success("Alert saved to database successfully!");
                con.close();
                return true;
            }
            
            con.close();
            return false;
            
        } catch (Exception e) {
            SystemLogger.error("Error saving alert to database: " + e.getMessage());
            return false;
        }
    }

    // Updates the alert in the database and tracks status changes
    // Returns: true if update successful, false if failed
    public boolean updateInDatabase() {
        try {
            String dburl = "jdbc:mysql://localhost:3306/WomenSafetyDB";
            String dbuser = "root";
            String dbpass = "";
            
            Connection con = DriverManager.getConnection(dburl, dbuser, dbpass);

            String getCurrentStatusSQL = "SELECT Status FROM alert_details WHERE Alert_id = ?";
            String previousStatus = "NEW";
            try (PreparedStatement getCurrentPst = con.prepareStatement(getCurrentStatusSQL)) {
                getCurrentPst.setInt(1, this.alertId);
                ResultSet rs = getCurrentPst.executeQuery();
                if (rs.next()) {
                    previousStatus = rs.getString("Status");
                }
                rs.close();
            }
            
            String updateQuery = "UPDATE alert_details SET Status = ?, Responder_id = ? WHERE Alert_id = ?";
            PreparedStatement pst = con.prepareStatement(updateQuery);
            
            pst.setString(1, this.status);
            if (this.assignedResponder != null) {
                pst.setInt(2, this.assignedResponder.getId());
            } else {
                pst.setNull(2, Types.INTEGER);
            }
            pst.setInt(3, this.alertId);

            int result = pst.executeUpdate();
            
            pst.close();

            if (result > 0 && previousStatus != null && !previousStatus.equals(this.status)) {
                String historySQL = "INSERT INTO alert_status_history (Alert_id, Previous_status, Current_status, Responder_id) VALUES (?, ?, ?, ?)";
                try (PreparedStatement historyPst = con.prepareStatement(historySQL)) {
                    historyPst.setInt(1, this.alertId);
                    historyPst.setString(2, previousStatus);
                    historyPst.setString(3, this.status);

                    if (this.assignedResponder != null) {
                        historyPst.setInt(4, this.assignedResponder.getId());
                    } else {
                        historyPst.setNull(4, Types.INTEGER);
                    }

                    historyPst.executeUpdate();
                    SystemLogger.info("Status history updated: " + previousStatus + " -> " + this.status + " for Alert ID: " + this.alertId);
                } catch (Exception e) {
                    SystemLogger.warning("Alert updated but failed to create status history: " + e.getMessage());
                }
            }

            con.close();
            
            return result > 0;
            
        } catch (Exception e) {
            SystemLogger.error("Error updating alert in database: " + e.getMessage());
            return false;
        }
    }

    // Sets the alert status
    public void setStatus(String status){
        this.status = status;
    }
    
    // Sets the assigned responder
    public void setResponder(Responder responder){
        this.assignedResponder = responder;
    }

    
    // Gets the alert ID
    int getId(){
        return alertId;
    }
    // Gets the alert ID (public accessor)
    public int getAlertId(){
        return alertId;
    }
    // Gets the user who triggered the alert
    public User getUser(){
        return user;
    }
    // Gets the alert creation timestamp
    public LocalDateTime getTimestamp(){
        return timestamp;
    }
    // Gets the current alert status
    public String getStatus(){
        return status;
    }
    // Gets the assigned responder (alias for getAssignedResponder)
    public Responder getResponder(){
        return assignedResponder;
    }
    // Gets the assigned responder
    public Responder getAssignedResponder(){
        return assignedResponder;
    }

    // Returns string representation of the alert
    @Override
    public String toString(){
        return "Alert ID:" +alertId+"\nUser: "+user.getName()+"\nTime: "+timestamp+"\nStatus: "+status+
                (assignedResponder!=null?("\nResponder: "+assignedResponder.getName()): "\nResponder: No responder assigned");
    }
}
