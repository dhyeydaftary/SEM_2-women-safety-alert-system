package com.womensafety.alertsystem.service;

import com.womensafety.alertsystem.model.Alert;
import com.womensafety.alertsystem.model.User;
import com.womensafety.alertsystem.model.Responder;
import com.womensafety.alertsystem.manager.LocationManager;
import com.womensafety.alertsystem.manager.UserManager;
import com.womensafety.alertsystem.manager.ResponderManager;
import com.womensafety.alertsystem.util.EscalationLogger;
import com.womensafety.alertsystem.util.SystemLogger;
import com.womensafety.alertsystem.util.Constants;
import java.sql.*;
import java.util.*;
import java.time.format.DateTimeFormatter;

// Dispatcher service handles alert processing, responder assignment, and database operations
// Manages alert queue and coordinates between users and responders
public class Dispatcher{
    private Queue<Alert> alertQueue; // Queue for pending alerts
    private LocationManager locationManager; // Manager for location-based operations
    private EscalationLogger escalationLogger; // Logger for escalation events
    private UserManager userManager; // Manager for user operations
    private ResponderManager responderManager; // Manager for responder operations

    // Constructor initializes dispatcher with required managers
    public Dispatcher(LocationManager locationManager, UserManager userManager, ResponderManager responderManager){
        this.alertQueue = new LinkedList<>();
        this.locationManager = locationManager;
        this.escalationLogger = new EscalationLogger();
        this.userManager = userManager;
        this.responderManager = responderManager;
        loadPendingAlertsFromDatabase(); // Load existing pending alerts on initialization
    }

    // Loads pending alerts (ACTIVE and WAITING status) from database into memory queue
    // Query: Retrieves alert details with user and responder information for pending alerts
    private void loadPendingAlertsFromDatabase() {
        try {
            String dburl = "jdbc:mysql://localhost:3306/WomenSafetyDB";
            String dbuser = "root";
            String dbpass = "";
            
            Connection con = DriverManager.getConnection(dburl, dbuser, dbpass);
            
            // Query: Join alert_details with user_details and responder_details to get complete alert information
            String query = "SELECT a.*, u.Name as user_name, u.Phone_no as user_phone, u.Email as user_email, " +
                "u.Location as user_location, u.Zone as user_zone, u.Password as user_password,u.X_coordinate as user_x,"+
                " u.Y_coordinate as user_y,r.Name as resp_name, r.Phone_no as resp_phone, r.Email as resp_email, " +
                "r.Zone as resp_zone, r.Availability as resp_availability, r.Password as resp_password, " +
                "r.X_coordinate as resp_x, r.Y_coordinate as resp_y " +
                "FROM alert_details a " +
                "JOIN user_details u ON a.User_id = u.User_id " +
                "LEFT JOIN responder_details r ON a.Responder_id = r.Responder_id " +
                "WHERE a.Status IN (?, ?) " + // Filter for ACTIVE and WAITING status alerts
                "ORDER BY a.Alert_time ASC"; // Process oldest alerts first
            
            PreparedStatement pst = con.prepareStatement(query);
            pst.setString(1, Constants.STATUS_ACTIVE);
            pst.setString(2, Constants.STATUS_WAITING);
            
            ResultSet rs = pst.executeQuery();
            
            while (rs.next()) {
                User user = new User(
                    rs.getInt("User_id"),
                    rs.getString("user_name"),
                    rs.getString("user_phone"),
                    rs.getString("user_email"),
                    rs.getString("user_location"),
                    rs.getString("user_zone"),
                    rs.getString("user_password")
                );
                user.setX(rs.getDouble("user_x"));
                user.setY(rs.getDouble("user_y"));
                
                Responder responder = null;
                if (rs.getInt("Responder_id") != 0) {
                    responder = new Responder(
                        rs.getInt("Responder_id"),
                        rs.getString("resp_name"),
                        rs.getString("resp_phone"),
                        rs.getString("resp_email"),
                        rs.getString("resp_zone"),
                        rs.getBoolean("resp_availability"),
                        rs.getString("resp_password")
                    );
                    responder.setX(rs.getDouble("resp_x"));
                    responder.setY(rs.getDouble("resp_y"));
                }
                
                Alert alert = new Alert(
                    rs.getInt("Alert_id"),
                    user,
                    rs.getTimestamp("Alert_time").toLocalDateTime(),
                    rs.getString("Status"),
                    responder
                );
                
                alertQueue.add(alert);
            }
            
            rs.close();
            pst.close();
            con.close();
            
            SystemLogger.info("Loaded " + alertQueue.size() + " pending alerts from database.");
            
        } catch (Exception e) {
            SystemLogger.error("Error loading pending alerts from database: " + e.getMessage());
        }
    }

    // Adds a new alert to the system and database
    public void addAlert(Alert alert){
        if (alert.saveToDatabase()) {
            alertQueue.add(alert);
            SystemLogger.info("New alert added by " + alert.getUser().getName() + " at " + alert.getTimestamp().format(DateTimeFormatter.ofPattern("HH:mm:ss")));
        } else {
            SystemLogger.error("Failed to save alert to database. Alert not added to queue.");
        }
    }

    // Processes the next alert in the queue, attempting to assign a responder
    public void processNextAlert(){
        if(alertQueue.isEmpty()){
            SystemLogger.info("[INFO] No pending alerts to process.");
            return;
        }

        Alert alert=alertQueue.peek();
        String userZone=alert.getUser().getZone();
        Responder responder = findAvailableResponderFromDatabase(userZone);

        if(responder != null){
            alertQueue.poll();
            alert.setResponder(responder);
            responder.setAvailable(false);
            alert.setStatus(Constants.STATUS_ASSIGNED);

            double distanceKm = NearestResponderFinder.calculateDistance(
                alert.getUser().getX(), alert.getUser().getY(),
                responder.getX(), responder.getY()
            );

            try {
                String dburl = "jdbc:mysql://localhost:3306/WomenSafetyDB";
                String dbuser = "root";
                String dbpass = "";
                
                Connection conn = DriverManager.getConnection(dburl, dbuser, dbpass);
                
                // Query: Create dispatch record tracking responder assignment and distance
                String dispatchSQL = "INSERT INTO dispatches (Alert_id, Responder_id, Distance_km) VALUES (?, ?, ?)";
                try (PreparedStatement pstmt = conn.prepareStatement(dispatchSQL)) {
                    pstmt.setInt(1, alert.getAlertId());
                    pstmt.setInt(2, responder.getId());
                    pstmt.setDouble(3, distanceKm);
                    pstmt.executeUpdate();
                    SystemLogger.info("Dispatch record created for Alert ID: " + alert.getAlertId());
                }

                // Query: Update alert status to ASSIGNED and set responder ID
                String updateAlertSQL = "UPDATE alert_details SET Status = ?, Responder_id = ? WHERE Alert_id = ?";
                try (PreparedStatement pstmt = conn.prepareStatement(updateAlertSQL)) {
                    pstmt.setString(1, Constants.STATUS_ASSIGNED);
                    pstmt.setInt(2, responder.getId());
                    pstmt.setInt(3, alert.getAlertId());
                    pstmt.executeUpdate();
                    SystemLogger.info("Alert status updated to ASSIGNED for Alert ID: " + alert.getAlertId());
                }

                // Query: Record status change history for audit trail
                String historySQL = "INSERT INTO alert_status_history (Alert_id, Previous_status, Current_status, Responder_id) VALUES (?, ?, ?, ?)";
                try (PreparedStatement pstmt = conn.prepareStatement(historySQL)) {
                    pstmt.setInt(1, alert.getAlertId());
                    pstmt.setString(2, Constants.STATUS_ACTIVE);
                    pstmt.setString(3, Constants.STATUS_ASSIGNED);
                    pstmt.setInt(4, responder.getId());
                    pstmt.executeUpdate();
                    SystemLogger.info("Status history recorded: Active -> Assigned for Alert ID: " + alert.getAlertId());
                }
                
                // Query: Update responder availability to false (busy)
                String updateResponderSQL = "UPDATE responder_details SET Availability = ? WHERE Responder_id = ?";
                try (PreparedStatement pstmt = conn.prepareStatement(updateResponderSQL)) {
                    pstmt.setBoolean(1, false);
                    pstmt.setInt(2, responder.getId());
                    pstmt.executeUpdate();
                    SystemLogger.info("Responder availability updated to false for Responder ID: " + responder.getId());
                }
                
                conn.close();
                
            } catch (Exception e) {
                SystemLogger.error("Database error during alert processing: " + e.getMessage());
                alert.setResponder(null);
                responder.setAvailable(true);
                alert.setStatus(Constants.STATUS_ACTIVE);
                alertQueue.offer(alert);
                return;
            }

            try{
                responder.notifyUserAssigned(alert.getUser());
            }catch(Exception e){
                SystemLogger.error("Error in notifications: "+e.getMessage());
            }
            SystemLogger.success("Alert assigned to " +responder.getName()+ " for user " + alert.getUser().getName());

        }else{
            alert.setStatus(Constants.STATUS_WAITING);
        
            try {
                String dburl = "jdbc:mysql://localhost:3306/WomenSafetyDB";
                String dbuser = "root";
                String dbpass = "";
                
                Connection conn = DriverManager.getConnection(dburl, dbuser, dbpass);
                
                // Query: Update alert status to WAITING when no responder available
                String updateAlertSQL = "UPDATE alert_details SET Status = ? WHERE Alert_id = ?";
                try (PreparedStatement pstmt = conn.prepareStatement(updateAlertSQL)) {
                    pstmt.setString(1, Constants.STATUS_WAITING);
                    pstmt.setInt(2, alert.getAlertId());
                    pstmt.executeUpdate();
                }
                
                // Query: Record status change to WAITING in history
                String historySQL = "INSERT INTO alert_status_history (Alert_id, Previous_status, Current_status, Responder_id) VALUES (?, ?, ?, NULL)";
                try (PreparedStatement pstmt = conn.prepareStatement(historySQL)) {
                    pstmt.setInt(1, alert.getAlertId());
                    pstmt.setString(2, Constants.STATUS_ACTIVE);
                    pstmt.setString(3, Constants.STATUS_WAITING);
                    pstmt.executeUpdate();
                }
                
                conn.close();
                
            } catch (Exception e) {
                SystemLogger.error("Database error while updating waiting status: " + e.getMessage());
            }
            SystemLogger.warning("No available responder in "+userZone+" for user "+alert.getUser().getName());
        }
    }

    // Finds an available responder in the specified zone from database
    // Query: Selects random available responder in the specified zone
    private Responder findAvailableResponderFromDatabase(String zone) {
        try {
            String dburl = "jdbc:mysql://localhost:3306/WomenSafetyDB";
            String dbuser = "root";
            String dbpass = "";
            Connection con = DriverManager.getConnection(dburl, dbuser, dbpass);
            
            // Query: Find available responder in specified zone, ordered randomly for load balancing
            String query = "SELECT * FROM responder_details WHERE Zone = ? AND Availability = true ORDER BY RAND() LIMIT 1";
            PreparedStatement pst = con.prepareStatement(query);
            pst.setString(1, zone);
            ResultSet rs = pst.executeQuery();
            
            if (rs.next()) {
                try {
                    Responder responder = new Responder(
                        rs.getInt("Responder_id"),
                        rs.getString("Name"),
                        rs.getString("Phone_no"),
                        rs.getString("Email"),
                        rs.getString("Zone"),
                        rs.getBoolean("Availability"),
                        rs.getString("Password")
                    );
                    responder.setX(rs.getDouble("X_coordinate"));
                    responder.setY(rs.getDouble("Y_coordinate"));
                    rs.close();
                    pst.close();
                    con.close();
                    return responder;
                } catch (IllegalArgumentException e) {
                    SystemLogger.error("Skipping invalid responder ID " + rs.getInt("Responder_id") + ": " + e.getMessage());
                    rs.close();
                    pst.close();
                    con.close();
                    return null;
                }
            }
            
            rs.close();
            pst.close();
            con.close();
        } catch (Exception e) {
            SystemLogger.error("Error finding available responder from database: "+ e.getMessage());
        }
        
        return null;
    }

    // Displays all pending alerts with detailed information
    public void showPendingAlerts(){
        loadPendingAlertsForDisplay();
        
        if(alertQueue.isEmpty()){
            SystemLogger.info("No pending alerts.");
            return;
        }

        System.out.println(Constants.CYAN + "\n--- Pending Alerts ---" + Constants.RESET);
        System.out.println("=" .repeat(80));
        System.out.println("Total pending alerts: " + alertQueue.size());
        System.out.println("-".repeat(80));
        
        for(Alert alert : alertQueue){
            System.out.println(Constants.BLUE + "Alert ID: " + Constants.RESET + alert.getAlertId());
            System.out.println(Constants.BLUE + "User: " + Constants.RESET + alert.getUser().getName());
            System.out.println(Constants.BLUE + "Phone: " + Constants.RESET + "+91 " + alert.getUser().getPhone());
            System.out.println(Constants.BLUE + "Location: " + Constants.RESET + alert.getUser().getLocation());
            System.out.println(Constants.BLUE + "Zone: " + Constants.RESET + alert.getUser().getZone());
            System.out.println(Constants.BLUE + "Status: " + Constants.RESET + getStatusWithColor(alert.getStatus()));
            System.out.println(Constants.TIMESTAMP + "Time: " + Constants.RESET + alert.getTimestamp().format(DateTimeFormatter.ofPattern("dd-MM-yyyy HH:mm:ss")));
            
            if (alert.getAssignedResponder() != null) {
                System.out.println(Constants.BLUE + "Assigned Responder: " + Constants.RESET + alert.getAssignedResponder().getName());
                System.out.println(Constants.BLUE + "Responder Phone: " + Constants.RESET + "+91 " + alert.getAssignedResponder().getPhone());
            } else {
                System.out.println(Constants.BLUE + "Assigned Responder: " + Constants.RESET + Constants.WARNING + "None" + Constants.RESET);
            }
            System.out.println("-".repeat(50));
        }
        System.out.println("=" .repeat(80));
    }

    // Reloads pending alerts from database for display purposes
    private void loadPendingAlertsForDisplay() {
        alertQueue.clear();
        loadPendingAlertsFromDatabase();
    }

    // Returns status string with appropriate color formatting
    private String getStatusWithColor(String status) {
        switch (status) {
            case Constants.STATUS_ACTIVE:
                return Constants.MAGENTA_ITALIC_BOLD + status + Constants.RESET;
            case Constants.STATUS_ASSIGNED:
                return Constants.SUCCESS + status + Constants.RESET;
            case Constants.STATUS_WAITING:
                return Constants.WARNING + status + Constants.RESET;
            case Constants.STATUS_RESOLVED:
                return Constants.SUCCESS + status + Constants.RESET;
            default:
                return status;
        }
    }

    // Processes all pending alerts in the queue
    public void processAllPendingAlerts() {
        if(alertQueue.isEmpty()){
            SystemLogger.info("No pending alerts to process.");
            return;
        }

        int originalSize = alertQueue.size();
        int processed = 0;
        int assigned = 0;
        int waiting = 0;

        while (!alertQueue.isEmpty() && processed < originalSize) {
            Alert alert = alertQueue.peek();
            String userZone = alert.getUser().getZone();
            Responder responder = locationManager.findRandomAvailableResponderInZone(userZone);

            if(responder != null){
                alertQueue.poll();
                alert.setResponder(responder);
                responder.setAvailable(false);
                alert.setStatus(Constants.STATUS_ASSIGNED);

                // Update database for this assignment
                updateAssignmentInDatabase(alert, responder);
                
                responder.notifyUserAssigned(alert.getUser());
                SystemLogger.success("Alert assigned to "+responder.getName()+" for user "+ alert.getUser().getName());
                assigned++;
            } else {
                Alert unassignedAlert = alertQueue.poll();
                unassignedAlert.setStatus(Constants.STATUS_WAITING);
                alertQueue.offer(unassignedAlert);

                // Update database for waiting status
                updateWaitingStatusInDatabase(unassignedAlert);
                
                SystemLogger.warning("No available responder in "+userZone+" zone for user "+unassignedAlert.getUser().getName());
                escalationLogger.logToFile(unassignedAlert, "No available responder in zone "+userZone);
                waiting++;
            }
            processed++;
        }

        if (assigned > 0) {
            SystemLogger.info(assigned + " alert(s) successfully assigned to responders.");
        }
        if (waiting > 0) {
            SystemLogger.info(waiting + " alert(s) remain in queue waiting for responders.");
        }
    }

    // Updates database records for alert assignment
    // Queries: Update alert status, record history, and update responder availability
    private void updateAssignmentInDatabase(Alert alert, Responder responder) {
        try {
            String dburl = "jdbc:mysql://localhost:3306/WomenSafetyDB";
            String dbuser = "root";
            String dbpass = "";
            Connection con = DriverManager.getConnection(dburl, dbuser, dbpass);
            
            // Query: Update alert status and assign responder
            String updateAlertSQL = "UPDATE alert_details SET Status = ?, Responder_id = ? WHERE Alert_id = ?";
            try (PreparedStatement pst = con.prepareStatement(updateAlertSQL)) {
                pst.setString(1, Constants.STATUS_ASSIGNED);
                pst.setInt(2, responder.getId());
                pst.setInt(3, alert.getAlertId());
                pst.executeUpdate();
            }
            
            // Query: Log status change history
            String historySQL = "INSERT INTO alert_status_history (Alert_id, Previous_status, Current_status, Responder_id) VALUES (?, ?, ?, ?)";
            try (PreparedStatement pst = con.prepareStatement(historySQL)) {
                pst.setInt(1, alert.getAlertId());
                pst.setString(2, Constants.STATUS_ACTIVE);
                pst.setString(3, Constants.STATUS_ASSIGNED);
                pst.setInt(4, responder.getId());
                pst.executeUpdate();
            }
            
            // Query: Update responder availability to false
            String updateResponderSQL = "UPDATE responder_details SET Availability = ? WHERE Responder_id = ?";
            try (PreparedStatement pst = con.prepareStatement(updateResponderSQL)) {
                pst.setBoolean(1, false);
                pst.setInt(2, responder.getId());
                pst.executeUpdate();
            }
            
            con.close();
            
        } catch (Exception e) {
            SystemLogger.error("Database error during assignment update: " + e.getMessage());
        }
    }

    // Updates database for alerts that need to wait for responders
    // Queries: Update alert status and record waiting status history
    private void updateWaitingStatusInDatabase(Alert alert) {
        try {
            String dburl = "jdbc:mysql://localhost:3306/WomenSafetyDB";
            String dbuser = "root";
            String dbpass = "";
            Connection con = DriverManager.getConnection(dburl, dbuser, dbpass);
            
            // Query: Update alert status to WAITING
            String updateAlertSQL = "UPDATE alert_details SET Status = ? WHERE Alert_id = ?";
            try (PreparedStatement pst = con.prepareStatement(updateAlertSQL)) {
                pst.setString(1, Constants.STATUS_WAITING);
                pst.setInt(2, alert.getAlertId());
                pst.executeUpdate();
            }
            
            // Query: Record status change to WAITING in history
            String historySQL = "INSERT INTO alert_status_history (Alert_id, Previous_status, Current_status, Responder_id) VALUES (?, ?, ?, NULL)";
            try (PreparedStatement pst = con.prepareStatement(historySQL)) {
                pst.setInt(1, alert.getAlertId());
                pst.setString(2, Constants.STATUS_ACTIVE);
                pst.setString(3, Constants.STATUS_WAITING);
                pst.executeUpdate();
            }
            
            con.close();
            
        } catch (Exception e) {
            SystemLogger.error("Database error during waiting status update: " + e.getMessage());
        }
    }

    // Completes an alert by marking it as resolved and freeing up the responder
    // Queries: Update dispatch completion, alert status, history, and responder availability
    public void completeAlert(Alert alert){
        Responder responder = alert.getResponder();
        if(responder != null){
            try {
                String dburl = "jdbc:mysql://localhost:3306/WomenSafetyDB";
                String dbuser = "root";
                String dbpass = "";
                
                Connection con = DriverManager.getConnection(dburl, dbuser, dbpass);
                
                // Query: Record dispatch completion time
                String updateDispatchSQL = "UPDATE dispatches SET Completion_time = CURRENT_TIMESTAMP WHERE Alert_id = ? AND Responder_id = ?";
                try (PreparedStatement pst = con.prepareStatement(updateDispatchSQL)) {
                    pst.setInt(1, alert.getAlertId());
                    pst.setInt(2, responder.getId());
                    pst.executeUpdate();
                    SystemLogger.info("Dispatch completed for Alert ID: " + alert.getAlertId());
                }
                
                // Query: Update alert status to RESOLVED
                String updateAlertSQL = "UPDATE alert_details SET Status = ? WHERE Alert_id = ?";
                try (PreparedStatement pst = con.prepareStatement(updateAlertSQL)) {
                    pst.setString(1, Constants.STATUS_RESOLVED);
                    pst.setInt(2, alert.getAlertId());
                    pst.executeUpdate();
                }
                
                // Query: Record status change to RESOLVED in history
                String historySQL = "INSERT INTO alert_status_history (Alert_id, Previous_status, Current_status, Responder_id) VALUES (?, ?, ?, ?)";
                try (PreparedStatement pst = con.prepareStatement(historySQL)) {
                    pst.setInt(1, alert.getAlertId());
                    pst.setString(2, Constants.STATUS_ASSIGNED);
                    pst.setString(3, Constants.STATUS_RESOLVED);
                    pst.setInt(4, responder.getId());
                    pst.executeUpdate();
                }

                // Query: Update responder availability to true (available)
                String updateResponderSQL = "UPDATE responder_details SET Availability = ? WHERE Responder_id = ?";
                try (PreparedStatement pst = con.prepareStatement(updateResponderSQL)) {
                    pst.setBoolean(1, true);
                    pst.setInt(2, responder.getId());
                    pst.executeUpdate();
                }
                
                con.close();
                
            } catch (Exception e) {
                SystemLogger.error("Database error during alert completion: " + e.getMessage());
            }
            
            updateResponderAvailabilityInDatabase(responder.getId(), true);
            responder.setAvailable(true);
            
            alert.setStatus(Constants.STATUS_RESOLVED);
            SystemLogger.success("Alert ID " + alert.getAlertId() + " resolved by " + responder.getName());

            alertQueue.removeIf(a -> a.getAlertId() == alert.getAlertId());

            if(!alertQueue.isEmpty()){
                SystemLogger.info("Checking if pending alerts can now be processed...");
                processAllPendingAlerts();
            }
        } else {
            SystemLogger.warning("No responder assigned to this alert.");
        }
    }

    // Marks an alert as complete with validation checks
    // Queries: Verify assignment, update status, record completion, and free responder
    public boolean markAlertComplete(int alertId, int responderId) {
        try {
            String dburl = "jdbc:mysql://localhost:3306/WomenSafetyDB";
            String dbuser = "root";
            String dbpass = "";
            
            Connection con = DriverManager.getConnection(dburl, dbuser, dbpass);
            
            // Query: Verify alert is assigned to this responder and in ASSIGNED status
            String verifySQL = "SELECT Status FROM alert_details WHERE Alert_id = ? AND Responder_id = ?";
            try (PreparedStatement pst = con.prepareStatement(verifySQL)) {
                pst.setInt(1, alertId);
                pst.setInt(2, responderId);
                ResultSet rs = pst.executeQuery();
                
                if (!rs.next()) {
                    SystemLogger.error("Alert not found or not assigned to this responder");
                    con.close();
                    return false;
                }
                
                String currentStatus = rs.getString("Status");
                if (!Constants.STATUS_ASSIGNED.equals(currentStatus)) {
                    SystemLogger.error("Alert is not in ASSIGNED status");
                    con.close();
                    return false;
                }
            }
            
            // Query: Update alert status to RESOLVED
            String updateAlertSQL = "UPDATE alert_details SET Status = ? WHERE Alert_id = ?";
            try (PreparedStatement pst = con.prepareStatement(updateAlertSQL)) {
                pst.setString(1, Constants.STATUS_RESOLVED);
                pst.setInt(2, alertId);
                pst.executeUpdate();
            }
            
            // Query: Record dispatch completion time
            String updateDispatchSQL = "UPDATE dispatches SET Completion_time = CURRENT_TIMESTAMP WHERE Alert_id = ? AND Responder_id = ?";
            try (PreparedStatement pst = con.prepareStatement(updateDispatchSQL)) {
                pst.setInt(1, alertId);
                pst.setInt(2, responderId);
                pst.executeUpdate();
            }
            
            // Query: Record status change to RESOLVED in history
            String historySQL = "INSERT INTO alert_status_history (Alert_id, Previous_status, Current_status, Responder_id) VALUES (?, ?, ?, ?)";
            try (PreparedStatement pst = con.prepareStatement(historySQL)) {
                pst.setInt(1, alertId);
                pst.setString(2, Constants.STATUS_ASSIGNED);
                pst.setString(3, Constants.STATUS_RESOLVED);
                pst.setInt(4, responderId);
                pst.executeUpdate();
            }
            
            // Query: Update responder availability to true
            String updateResponderSQL = "UPDATE responder_details SET Availability = ? WHERE Responder_id = ?";
            try (PreparedStatement pst = con.prepareStatement(updateResponderSQL)) {
                pst.setBoolean(1, true);
                pst.setInt(2, responderId);
                pst.executeUpdate();
            }
            
            con.close();
            
            SystemLogger.success("Alert ID " + alertId + " marked as complete by Responder ID " + responderId);
            return true;
            
        } catch (Exception e) {
            SystemLogger.error("Database error during alert completion: " + e.getMessage());
            return false;
        }
    }

    // Updates responder availability in database
    // Query: Update availability status for a specific responder
    private void updateResponderAvailabilityInDatabase(int responderId, boolean available) {
        try {
            String dburl = "jdbc:mysql://localhost:3306/WomenSafetyDB";
            String dbuser = "root";
            String dbpass = "";
            
            Connection con = DriverManager.getConnection(dburl, dbuser, dbpass);
            
            // Query: Update responder availability status
            String updateQuery = "UPDATE responder_details SET Availability = ? WHERE Responder_id = ?";
            PreparedStatement pst = con.prepareStatement(updateQuery);
            
            pst.setBoolean(1, available);
            pst.setInt(2, responderId);
            
            pst.executeUpdate();
            
            pst.close();
            con.close();
            
        } catch (Exception e) {
            SystemLogger.error("Error updating responder availability: " + e.getMessage());
        }
    }

    // Reassigns a different responder to an alert
    // Queries: Complete old dispatch, create new dispatch, update records
    public boolean reassignResponder(Alert alert){
        String zone = alert.getUser().getZone();
        Responder current = alert.getResponder();

        Responder newResponder = findAlternateResponderFromDatabase(zone, current != null ? current.getId() : -1);
        
        if (newResponder != null) {
            try {
                String dburl = "jdbc:mysql://localhost:3306/WomenSafetyDB";
                String dbuser = "root";
                String dbpass = "";
                
                Connection conn = DriverManager.getConnection(dburl, dbuser, dbpass);
                
                if(current != null){
                    // Query: Mark old dispatch as completed
                    String updateOldDispatchSQL = "UPDATE dispatches SET Completion_time = CURRENT_TIMESTAMP WHERE Alert_id = ? AND Responder_id = ?";
                    try (PreparedStatement pstmt = conn.prepareStatement(updateOldDispatchSQL)) {
                        pstmt.setInt(1, alert.getAlertId());
                        pstmt.setInt(2, current.getId());
                        pstmt.executeUpdate();
                    }
                    
                    // Query: Free up old responder
                    String updateOldResponderSQL = "UPDATE responder_details SET Availability = ? WHERE Responder_id = ?";
                    try (PreparedStatement pstmt = conn.prepareStatement(updateOldResponderSQL)) {
                        pstmt.setBoolean(1, true);
                        pstmt.setInt(2, current.getId());
                        pstmt.executeUpdate();
                    }
                    
                    current.setAvailable(true);
                    updateResponderAvailabilityInDatabase(current.getId(), true);
                }

                double distanceKm = NearestResponderFinder.calculateDistance(
                    alert.getUser().getX(), alert.getUser().getY(),
                    newResponder.getX(), newResponder.getY()
                );
                
                // Query: Create new dispatch record for reassignment
                String dispatchSQL = "INSERT INTO dispatches (Alert_id, Responder_id, Distance_km) VALUES (?, ?, ?)";
                try (PreparedStatement pstmt = conn.prepareStatement(dispatchSQL)) {
                    pstmt.setInt(1, alert.getAlertId());
                    pstmt.setInt(2, newResponder.getId());
                    pstmt.setDouble(3, distanceKm);
                    pstmt.executeUpdate();
                }

                // Query: Update alert with new responder
                String updateAlertSQL = "UPDATE alert_details SET Responder_id = ?, Status = ? WHERE Alert_id = ?";
                try (PreparedStatement pstmt = conn.prepareStatement(updateAlertSQL)) {
                    pstmt.setInt(1, newResponder.getId());
                    pstmt.setString(2, Constants.STATUS_ASSIGNED);
                    pstmt.setInt(3, alert.getAlertId());
                    pstmt.executeUpdate();
                }

                // Query: Record reassignment in history
                String historySQL = "INSERT INTO alert_status_history (Alert_id, Previous_status, Current_status, Responder_id) VALUES (?, ?, ?, ?)";
                try (PreparedStatement pstmt = conn.prepareStatement(historySQL)) {
                    pstmt.setInt(1, alert.getAlertId());
                    pstmt.setString(2, Constants.STATUS_ASSIGNED);
                    pstmt.setString(3, Constants.STATUS_ASSIGNED);
                    pstmt.setInt(4, newResponder.getId());
                    pstmt.executeUpdate();
                }
                
                // Query: Mark new responder as unavailable
                String updateNewResponderSQL = "UPDATE responder_details SET Availability = ? WHERE Responder_id = ?";
                try (PreparedStatement pstmt = conn.prepareStatement(updateNewResponderSQL)) {
                    pstmt.setBoolean(1, false);
                    pstmt.setInt(2, newResponder.getId());
                    pstmt.executeUpdate();
                }
                
                conn.close();
                
            } catch (Exception e) {
                SystemLogger.error("Database error during responder reassignment: " + e.getMessage());
                return false;
            }
            
            alert.setResponder(newResponder);
            newResponder.setAvailable(false);
            alert.setStatus(Constants.STATUS_ASSIGNED);

            newResponder.notifyUserAssigned(alert.getUser());
            SystemLogger.success("Responder reassigned to "+newResponder.getName()+" for Alert ID "+alert.getAlertId());
            return true;
        }
        
        SystemLogger.warning("No alternate responder available for Alert ID " + alert.getAlertId());
        escalationLogger.logToFile(alert, "No alternate responder available in zone "+zone);
        return false;
    }

    // Finds an alternate responder excluding the current one
    // Query: Find available responder in zone excluding specified responder ID
    private Responder findAlternateResponderFromDatabase(String zone, int excludeResponderId) {
        try {
            String dburl = "jdbc:mysql://localhost:3306/WomenSafetyDB";
            String dbuser = "root";
            String dbpass = "";
            
            Connection con = DriverManager.getConnection(dburl, dbuser, dbpass);
            
            // Query: Find available responder in zone excluding current responder
            String query = "SELECT * FROM responder_details WHERE Zone = ? AND Availability = true AND Responder_id != ? ORDER BY RAND() LIMIT 1";
            PreparedStatement pst = con.prepareStatement(query);
            pst.setString(1, zone);
            pst.setInt(2, excludeResponderId);
            
            ResultSet rs = pst.executeQuery();
            
            if (rs.next()) {
                Responder responder = new Responder(
                    rs.getInt("Responder_id"),
                    rs.getString("Name"),
                    rs.getString("Phone_no"),
                    rs.getString("Email"),
                    rs.getString("Zone"),
                    rs.getBoolean("Availability"),
                    rs.getString("Password")
                );
                responder.setX(rs.getDouble("X_coordinate"));
                responder.setY(rs.getDouble("Y_coordinate"));
                
                rs.close();
                pst.close();
                con.close();
                
                return responder;
            }
            
            rs.close();
            pst.close();
            con.close();
            
        } catch (Exception e) {
            SystemLogger.error("Error finding alternate responder from database: " + e.getMessage());
        }
        
        return null;
    }

    public void checkUnassignedAlerts(){
        if(Constants.ENABLE_BACKGROUND_LOGGING) {
            System.out.println("\n[Background] Checking for unassigned alerts...");
        }

        boolean foundUnassigned = false;
        for(Alert alert : alertQueue) {
            if(alert.getStatus().equals(Constants.STATUS_WAITING)) {
                foundUnassigned = true;
                if(Constants.ENABLE_BACKGROUND_LOGGING) {
                    System.out.println("[Background] Attempting to reassign Alert ID " + alert.getAlertId());
                }
                reassignResponder(alert);
            }
        }

        if(!foundUnassigned && Constants.ENABLE_BACKGROUND_LOGGING) {
            System.out.println("[Background] No unassigned alerts found.");
        }
    }
}