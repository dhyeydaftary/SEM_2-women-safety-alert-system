package com.womensafety.alertsystem.manager; // Manager classes for business logic and operations

import com.womensafety.alertsystem.model.Responder;
import com.womensafety.alertsystem.security.RBACManager;
import com.womensafety.alertsystem.util.SystemLogger;
import com.womensafety.alertsystem.util.Constants;
import com.womensafety.alertsystem.service.AuthenticationHelper;
import com.womensafety.alertsystem.model.Role;
import java.sql.*;
import java.util.HashMap;
import java.util.Random;

// ResponderManager class handles responder registration, authentication, and management
// Manages responder data in both memory and database with coordinate generation
public class ResponderManager{
    private HashMap<Integer,Responder> resp; // In-memory storage of responders by ID
    private int nextResponderId; // Next available responder ID
    private Random random; // Random generator for coordinate generation

    // Constructor initializes data structures and loads next responder ID from database
    public ResponderManager(){
        resp = new HashMap<>(); // Initialize empty responder map
        random = new Random(); // Initialize random number generator
        initializeNextResponderId(); // Load next ID from database
    }

    // Initializes the next responder ID by querying the database for maximum ID
    private void initializeNextResponderId() {
        try {
            String dburl = "jdbc:mysql://localhost:3306/WomenSafetyDB";
            String dbuser = "root";
            String dbpass = "";

            Connection con = DriverManager.getConnection(dburl, dbuser, dbpass);

            String query = "SELECT MAX(Responder_id) as max_id FROM responder_details"; // Query max ID
            PreparedStatement pst = con.prepareStatement(query);
            ResultSet rs = pst.executeQuery();

            if (rs.next()) {
                int maxId = rs.getInt("max_id"); // Get maximum ID from database
                nextResponderId = maxId + 1; // Set next ID to max + 1
            } else {
                nextResponderId = 1; // Default to 1 if no responders exist
            }

            rs.close();
            pst.close();
            con.close();

            SystemLogger.info("Next Responder ID initialized to: " + nextResponderId); // Log initialization

        } catch (Exception e) {
            SystemLogger.error("Error initializing Responder ID: " + e.getMessage()); // Log error
            nextResponderId = 1; // Fallback to ID 1 on error
        }
    }

    // Registers a new responder with the system
    // Returns: Registered Responder object or null if registration fails
    public Responder registerResponder(String Name, String Phone, String Email, String Zone, boolean Available, String Password){
        Responder newResponder = new Responder(nextResponderId, Name, Phone, Email, Zone, Available, Password); // Create responder object

        double[] coords = generateZoneBasedCoordinates(Zone); // Generate coordinates based on zone
        newResponder.setX(coords[1]);
        newResponder.setY(coords[0]);

        resp.put(nextResponderId, newResponder); // Store responder in memory

        try {
            String dburl = "jdbc:mysql://localhost:3306/WomenSafetyDB";
            String dbuser = "root";
            String dbpass = "";

            Connection con = DriverManager.getConnection(dburl, dbuser, dbpass);

            if(con != null){
                SystemLogger.success("Database connection established successfully."); // Log connection success
            } else {
                SystemLogger.error("Failed to connect to the database."); // Log connection failure
                return null; // Return null on connection failure
            }

            String insertResponder = "INSERT INTO responder_details (Responder_id, Name, Phone_no, Email, Zone, Availability, Password, X_coordinate, Y_coordinate) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?)"; // Insert query
            PreparedStatement pst = con.prepareStatement(insertResponder);

            pst.setInt(1, nextResponderId);
            pst.setString(2, Name);
            pst.setString(3, Phone);
            pst.setString(4, Email);
            pst.setString(5, Zone);
            pst.setBoolean(6, Available);
            pst.setString(7, Password);
            pst.setDouble(8, newResponder.getX());
            pst.setDouble(9, newResponder.getY());

            int result = pst.executeUpdate();

            if (result > 0) {
                SystemLogger.success("User saved to database successfully!"); // Log success
                nextResponderId++; // Increment ID for next responder
            } else {
                SystemLogger.error("Failed to save user to database."); // Log failure
                resp.remove(nextResponderId); // Remove from memory on failure
                return null; // Return null on database failure
            }

            pst.close();
            con.close();

        } catch (Exception e) {
            SystemLogger.error("Database error: " + e.getMessage()); // Log error
            resp.remove(nextResponderId); // Remove from memory on error
            return null; // Return null on exception
        }

        return newResponder; // Return created responder
    }

    // Authenticates a responder login using both memory and database
    // Returns: Authenticated Responder object or null if authentication fails
    public Responder authenticateResponderLogin(int responderId, String password) {
        Responder responder = resp.get(responderId); // Check memory first
        if (responder != null && AuthenticationHelper.authenticateResponder(responder, password)) {
            responder.setRole(Role.RESPONDER); // Set responder role
            RBACManager.setCurrentUser(responder); // Set current user in RBAC
            return responder; // Return authenticated responder
        }

        try {
            String dburl = "jdbc:mysql://localhost:3306/WomenSafetyDB";
            String dbuser = "root";
            String dbpass = "";

            Connection con = DriverManager.getConnection(dburl, dbuser, dbpass);

            String query = "SELECT * FROM responder_details WHERE Responder_id = ? AND Password = ?"; // Auth query
            PreparedStatement pst = con.prepareStatement(query);
            pst.setInt(1, responderId);
            pst.setString(2, password);

            ResultSet rs = pst.executeQuery();

            if (rs.next()) {
                String name = rs.getString("Name");
                String phone = rs.getString("Phone_no");
                String email = rs.getString("Email");
                String zone = rs.getString("Zone");
                boolean availability = rs.getBoolean("Availability");
                double x = rs.getDouble("X_coordinate");
                double y = rs.getDouble("Y_coordinate");

                Responder dbResponder = new Responder(responderId, name, phone, email, zone, availability, password); // Create responder
                dbResponder.setX(x);
                dbResponder.setY(y);
                dbResponder.setRole(Role.RESPONDER); // Set responder role
                resp.put(responderId, dbResponder); // Store in memory

                RBACManager.setCurrentUser(dbResponder); // Set current user in RBAC

                rs.close();
                pst.close();
                con.close();

                return dbResponder; // Return authenticated responder
            }

            rs.close();
            pst.close();
            con.close();

        } catch (Exception e) {
            SystemLogger.error("Database authentication error: " + e.getMessage()); // Log error
        }
        return null; // Return null if authentication fails
    }

    // Generates random coordinates within India's boundaries
    // Returns: Array containing [latitude, longitude]
    private double[] generateRandomCoordinates() {
        double lat = Constants.INDIA_MIN_LAT + (Constants.INDIA_MAX_LAT - Constants.INDIA_MIN_LAT) * random.nextDouble(); // Random latitude
        double lng = Constants.INDIA_MIN_LNG + (Constants.INDIA_MAX_LNG - Constants.INDIA_MIN_LNG) * random.nextDouble(); // Random longitude
        return new double[]{lat, lng}; // Return coordinates
    }

    // Generates coordinates based on specified zone within India
    // Returns: Array containing [latitude, longitude] for the zone
    private double[] generateZoneBasedCoordinates(String zone) {
        double baseLat, baseLng; // Base coordinates for the zone
        double offset = 2.0; // Coordinate offset range

        switch (zone.toLowerCase()) {
            case "north":
                baseLat = 28.0;
                baseLng = 77.0;
                break;
            case "south":
                baseLat = 12.0;
                baseLng = 78.0;
                break;
            case "east":
                baseLat = 22.0;
                baseLng = 88.0;
                break;
            case "west":
                baseLat = 19.0;
                baseLng = 73.0;
                break;
            default:
                return generateRandomCoordinates(); // Fallback to random coordinates
        }
        double lat = baseLat + (random.nextDouble() - 0.5) * 2 * offset; // Random latitude within zone
        double lng = baseLng + (random.nextDouble() - 0.5) * 2 * offset; // Random longitude within zone

        // Ensure coordinates stay within India boundaries
        lat = Math.max(Constants.INDIA_MIN_LAT, Math.min(Constants.INDIA_MAX_LAT, lat));
        lng = Math.max(Constants.INDIA_MIN_LNG, Math.min(Constants.INDIA_MAX_LNG, lng));

        return new double[]{lat, lng}; // Return zone-based coordinates
    }

    // Updates responder coordinates in memory
    // Returns: true if update successful, false if responder not found
    public boolean updateResponderCoordinates(int responderId, double x, double y){
        Responder responder = resp.get(responderId); // Get responder from memory
        if(responder != null){
            responder.setX(x);
            responder.setY(y);
            SystemLogger.success("Coordinates updated for responder: " + responder.getName()); // Log success
            return true; // Return success
        }
        return false; // Return failure if responder not found
    }

    // Updates responder information in the database for various fields
    // Returns: true if update successful, false if failed
    public boolean updateResponderInDatabase(int responderId, String fieldName, String oldValue, String newValue) {
        try {
            String dburl = "jdbc:mysql://localhost:3306/WomenSafetyDB";
            String dbuser = "root";
            String dbpass = "";
            Connection con = DriverManager.getConnection(dburl, dbuser, dbpass);

            String updateQuery = "";
            PreparedStatement pst = null;

            switch (fieldName.toLowerCase()) {
                case "name":
                    updateQuery = "UPDATE responder_details SET Name = ? WHERE Responder_id = ?"; // Name update
                    break;
                case "phone":
                    updateQuery = "UPDATE responder_details SET Phone_no = ? WHERE Responder_id = ?"; // Phone update
                    break;
                case "email":
                    updateQuery = "UPDATE responder_details SET Email = ? WHERE Responder_id = ?"; // Email update
                    break;
                case "password":
                    updateQuery = "UPDATE responder_details SET Password = ? WHERE Responder_id = ?"; // Password update
                    break;
                case "zone":
                    updateQuery = "UPDATE responder_details SET Zone = ?, X_coordinate = ?, Y_coordinate = ? WHERE Responder_id = ?"; // Zone update
                    break;
                case "available":
                    updateQuery = "UPDATE responder_details SET Availability = ? WHERE Responder_id = ?"; // Availability update
                    break;
                default:
                    SystemLogger.error("Invalid field: " + fieldName); // Log invalid field
                    return false; // Return failure
            }

            pst = con.prepareStatement(updateQuery);

            if (fieldName.equalsIgnoreCase("zone")) {
                double[] coords = generateZoneBasedCoordinates(newValue);
                pst.setString(1, newValue);
                pst.setDouble(2, coords[1]);
                pst.setDouble(3, coords[0]);
                pst.setInt(4, responderId);
            } else if (fieldName.equalsIgnoreCase("available")) {
                pst.setBoolean(1, Boolean.parseBoolean(newValue));
                pst.setInt(2, responderId);
            } else {
                pst.setString(1, newValue);
                pst.setInt(2, responderId);
            }

            int result = pst.executeUpdate();
            if (result > 0) {
                SystemLogger.success("Responder updated successfully in DB for field: " + fieldName); // Log success
                pst.close();
                con.close();
                return true; // Return success
            }

            pst.close();
            con.close();
        } catch (Exception e) {
            SystemLogger.error("Database update error: " + e.getMessage()); // Log error
        }
        return false; // Return failure
    }

    // Updates responder password with validation
    // Returns: true if password update successful, false if failed
    public boolean updateResponderPassword(int responderId, String oldPassword, String newPassword) {
        Responder responder = resp.get(responderId); // Get responder from memory
        if (responder == null) {
            SystemLogger.error("Responder not found."); // Log error
            return false; // Return failure
        }

        if (!responder.getPassword().equals(oldPassword)) {
            SystemLogger.error("Current password is incorrect."); // Log error
            return false; // Return failure
        }

        if (!AuthenticationHelper.isValidPassword(newPassword)) {
            SystemLogger.error("New password must be at least 6 characters long."); // Log error
            return false; // Return failure
        }

        responder.setPassword(newPassword); // Update password in memory

        try {
            String dburl = "jdbc:mysql://localhost:3306/WomenSafetyDB";
            String dbuser = "root";
            String dbpass = "";
            Connection con = DriverManager.getConnection(dburl, dbuser, dbpass);

            if (con != null) {
                String updateQuery = "UPDATE responder_details SET Password = ? WHERE Responder_id = ?"; // Password update query
                PreparedStatement pst = con.prepareStatement(updateQuery);
                pst.setString(1, newPassword);
                pst.setInt(2, responderId);

                int result = pst.executeUpdate();

                if (result > 0) {
                    SystemLogger.success("Password updated successfully!"); // Log success
                    pst.close();
                    con.close();
                    return true;
                }

                pst.close();
                con.close();
            }

        } catch (Exception e) {
            SystemLogger.error("Database update error: " + e.getMessage()); // Log error
            responder.setPassword(oldPassword); // Revert password change on error
        }
        return false; // Return failure
    }

    // Displays all responders from database with formatted output
    public void displayAllResponders(){
        System.out.println(Constants.CYAN + "\n--- All Registered Responders ---" + Constants.RESET);
        System.out.println("=" .repeat(85));

        try {
            String dburl = "jdbc:mysql://localhost:3306/WomenSafetyDB";
            String dbuser = "root";
            String dbpass = "";

            Connection con = DriverManager.getConnection(dburl, dbuser, dbpass);

            String query = "SELECT * FROM responder_details ORDER BY Responder_id"; // Select all responders
            PreparedStatement pst = con.prepareStatement(query);
            ResultSet rs = pst.executeQuery();

            boolean hasResponders = false; // Flag for responders found
            int responderCount = 0; // Total responder count
            int availableCount = 0; // Available responder count
            int busyCount = 0; // Busy responder count

            while (rs.next()) {
                hasResponders = true; // Set flag
                responderCount++; // Increment total count

                int responderId = rs.getInt("Responder_id");
                String name = rs.getString("Name");
                String phone = rs.getString("Phone_no");
                String email = rs.getString("Email");
                String zone = rs.getString("Zone");
                boolean availability = rs.getBoolean("Availability");
                double x = rs.getDouble("X_coordinate");
                double y = rs.getDouble("Y_coordinate");

                if (availability) {
                    availableCount++; // Increment available count
                } else {
                    busyCount++; // Increment busy count
                }

                System.out.println(Constants.BLUE + "Responder ID: " + Constants.RESET + responderId);
                System.out.println(Constants.BLUE + "Name: " + Constants.RESET + name);
                System.out.println(Constants.BLUE + "Phone: " + Constants.RESET + "+91 " + phone);
                System.out.println(Constants.BLUE + "Email: " + Constants.RESET + email);
                System.out.println(Constants.BLUE + "Zone: " + Constants.RESET + zone);

                String availabilityText = availability ? // Format availability text
                    Constants.SUCCESS + "AVAILABLE" + Constants.RESET :
                    Constants.ERROR + "BUSY" + Constants.RESET;
                System.out.println(Constants.BLUE + "Status: " + Constants.RESET + availabilityText); // Print status

                System.out.println(Constants.COORDINATES + "Coordinates: " + Constants.RESET + // Print coordinates
                    "(" + String.format("%.4f", x) + ", " + String.format("%.4f", y) + ")");
                System.out.println("-".repeat(45));
            }

            if (!hasResponders) {
                SystemLogger.info("No responders found in database."); // Log no responders
            } else {
                System.out.println(Constants.SUCCESS + "Total Responders: " + responderCount + Constants.RESET); // Print total
                System.out.println(Constants.SUCCESS + "Available: " + availableCount + Constants.RESET + // Print available
                    " | " + Constants.WARNING + "Busy: " + busyCount + Constants.RESET); // Print busy
            }

            rs.close();
            pst.close();
            con.close();

        } catch (Exception e) {
            SystemLogger.error("Error retrieving responders from database: " + e.getMessage()); // Log error

            System.out.println("\nFalling back to in-memory data:"); // Fallback message
            if(resp.isEmpty()){
                SystemLogger.info("No responders registered in memory either."); // Log no in-memory responders
            } else {
                for(Responder responder: resp.values()){
                    System.out.println(responder); // Print responder details
                    System.out.println("");
                }
            }
        }
        System.out.println("=" .repeat(85));
    }

    // Checks if a phone number already exists in the responder database
    // Returns: true if phone exists, false otherwise
    public boolean isPhoneExists(String phone) {
        String dbUrl = "jdbc:mysql://localhost:3306/WomenSafetyDB";
        String dbUser = "root";
        String dbPass = "";

        String query = "SELECT COUNT(*) FROM responder_details WHERE Phone_no = ?"; // Phone existence query
        try (Connection conn = DriverManager.getConnection(dbUrl, dbUser, dbPass); 
             PreparedStatement stmt = conn.prepareStatement(query)) {
            stmt.setString(1, phone);
            ResultSet rs = stmt.executeQuery();
            if (rs.next()) {
                return rs.getInt(1) > 0; // Return true if count > 0
            }
        } catch (SQLException e) {
            e.printStackTrace(); // Print stack trace on error
        }
        return false; // Return false if phone doesn't exist or error occurs
    }
}
