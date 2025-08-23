package com.womensafety.alertsystem.manager;

import com.womensafety.alertsystem.model.Responder;
import com.womensafety.alertsystem.security.RBACManager;
import com.womensafety.alertsystem.util.SystemLogger;
import com.womensafety.alertsystem.util.Constants;
import com.womensafety.alertsystem.service.AuthenticationHelper;
import com.womensafety.alertsystem.model.Role;
import java.sql.*;
import java.util.HashMap;
import java.util.Random;

public class ResponderManager{
    private HashMap<Integer,Responder> resp;
    private int nextResponderId;
    private Random random;

    public ResponderManager(){
        resp=new HashMap<>();
        random=new Random();
        initializeNextResponderId();
    }

    private void initializeNextResponderId() {
        try {
            String dburl = "jdbc:mysql://localhost:3306/WomenSafetyDB";
            String dbuser = "root";
            String dbpass = "";

            Connection con = DriverManager.getConnection(dburl, dbuser, dbpass);

            String query = "SELECT MAX(Responder_id) as max_id FROM responder_details";
            PreparedStatement pst = con.prepareStatement(query);
            ResultSet rs = pst.executeQuery();

            if (rs.next()) {
                int maxId = rs.getInt("max_id");
                nextResponderId = maxId + 1;
            } else {
                nextResponderId = 1;
            }

            rs.close();
            pst.close();
            con.close();

            SystemLogger.info("Next Responder ID initialized to: " + nextResponderId);

        } catch (Exception e) {
            SystemLogger.error("Error initializing Responder ID: " + e.getMessage());
            nextResponderId = 1;
        }
    }

    public Responder registerResponder(String Name, String Phone, String Email, String Zone, boolean Available, String Password){
        Responder newResponder=new Responder(nextResponderId, Name, Phone, Email, Zone, Available, Password);

        double[] coords = generateZoneBasedCoordinates(Zone);
        newResponder.setX(coords[1]);
        newResponder.setY(coords[0]);

        resp.put(nextResponderId, newResponder);

        try {
            String dburl = "jdbc:mysql://localhost:3306/WomenSafetyDB";
            String dbuser = "root";
            String dbpass = "";

            Connection con = DriverManager.getConnection(dburl, dbuser, dbpass);

            if(con!=null){
                SystemLogger.success("Database connection established successfully.");
            }else{
                SystemLogger.error("Failed to connect to the database.");
                return null;
            }

            String insertResponder = "INSERT INTO responder_details (Responder_id, Name, Phone_no, Email, Zone, Availability, Password, X_coordinate, Y_coordinate) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?)";
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
                SystemLogger.success("User saved to database successfully!");
                nextResponderId++;
            }else{
                SystemLogger.error("Failed to save user to database.");
                resp.remove(nextResponderId);
                return null;
            }

            pst.close();
            con.close();

        } catch (Exception e) {
            SystemLogger.error("Database error: " + e.getMessage());
            resp.remove(nextResponderId);
            return null;
        }

        return newResponder;
    }

    public Responder authenticateResponderLogin(int responderId, String password) {
        Responder responder = resp.get(responderId);
        if (responder!=null && AuthenticationHelper.authenticateResponder(responder, password)) {
            responder.setRole(Role.RESPONDER);
            RBACManager.setCurrentUser(responder);
            return responder;
        }

        try {
            String dburl = "jdbc:mysql://localhost:3306/WomenSafetyDB";
            String dbuser = "root";
            String dbpass = "";

            Connection con = DriverManager.getConnection(dburl, dbuser, dbpass);

            String query = "SELECT * FROM responder_details WHERE Responder_id = ? AND Password = ?";
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

                Responder dbResponder = new Responder(responderId, name, phone, email, zone, availability, password);
                dbResponder.setX(x);
                dbResponder.setY(y);
                dbResponder.setRole(Role.RESPONDER);
                resp.put(responderId, dbResponder);

                RBACManager.setCurrentUser(dbResponder);

                rs.close();
                pst.close();
                con.close();

                return dbResponder;
            }

            rs.close();
            pst.close();
            con.close();

        } catch (Exception e) {
            SystemLogger.error("Database authentication error: " + e.getMessage());
        }
        return null;
    }

    private double[] generateRandomCoordinates() {
        double lat = Constants.INDIA_MIN_LAT + (Constants.INDIA_MAX_LAT - Constants.INDIA_MIN_LAT) * random.nextDouble();
        double lng = Constants.INDIA_MIN_LNG + (Constants.INDIA_MAX_LNG - Constants.INDIA_MIN_LNG) * random.nextDouble();
        return new double[]{lat, lng};
    }

    private double[] generateZoneBasedCoordinates(String zone) {
        double baseLat, baseLng;
        double offset = 2.0;

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
                return generateRandomCoordinates();
        }
        double lat = baseLat + (random.nextDouble() - 0.5) * 2 * offset;
        double lng = baseLng + (random.nextDouble() - 0.5) * 2 * offset;

        lat = Math.max(Constants.INDIA_MIN_LAT, Math.min(Constants.INDIA_MAX_LAT, lat));
        lng = Math.max(Constants.INDIA_MIN_LNG, Math.min(Constants.INDIA_MAX_LNG, lng));

        return new double[]{lat, lng};
    }

    public boolean updateResponderCoordinates(int responderId, double x, double y){
        Responder responder = resp.get(responderId);
        if(responder != null){
            responder.setX(x);
            responder.setY(y);
            SystemLogger.success("Coordinates updated for responder: " + responder.getName());
            return true;
        }
        return false;
    }

    public boolean updateResponderInDatabase(int responderId, String fieldName,String oldValue, String newValue) {
        try {
            String dburl = "jdbc:mysql://localhost:3306/WomenSafetyDB";
            String dbuser = "root";
            String dbpass = "";
            Connection con = DriverManager.getConnection(dburl, dbuser, dbpass);

            String updateQuery = "";
            PreparedStatement pst = null;

            switch (fieldName.toLowerCase()) {
                case "name":
                    updateQuery = "UPDATE responder_details SET Name = ? WHERE Responder_id = ?";
                    break;
                case "phone":
                    updateQuery = "UPDATE responder_details SET Phone_no = ? WHERE Responder_id = ?";
                    break;
                case "email":
                    updateQuery = "UPDATE responder_details SET Email = ? WHERE Responder_id = ?";
                    break;
                case "password":
                    updateQuery = "UPDATE responder_details SET Password = ? WHERE Responder_id = ?";
                    break;
                case "zone":
                    updateQuery = "UPDATE responder_details SET Zone = ?, X_coordinate = ?, Y_coordinate = ? WHERE Responder_id = ?";
                    break;
                case "available":
                    updateQuery = "UPDATE responder_details SET Availability = ? WHERE Responder_id = ?";
                    break;
                default:
                    SystemLogger.error("Invalid field: " + fieldName);
                    return false;
            }

            pst = con.prepareStatement(updateQuery);

            if (fieldName.equalsIgnoreCase("zone")) {
                double[] coords = generateZoneBasedCoordinates(newValue);
                pst.setString(1, newValue);
                pst.setDouble(2, coords[1]); // X
                pst.setDouble(3, coords[0]); // Y
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
                SystemLogger.success("Responder updated successfully in DB for field: " + fieldName);
                pst.close();
                con.close();
                return true;
            }

            pst.close();
            con.close();
        } catch (Exception e) {
            SystemLogger.error("Database update error: " + e.getMessage());
        }
        return false;
    }


    public boolean updateResponderPassword(int responderId, String oldPassword, String newPassword) {
        Responder responder = resp.get(responderId);
        if (responder == null) {
            SystemLogger.error("Responder not found.");
            return false;
        }

        if (!responder.getPassword().equals(oldPassword)) {
            SystemLogger.error("Current password is incorrect.");
            return false;
        }

        if (!AuthenticationHelper.isValidPassword(newPassword)) {
            SystemLogger.error("New password must be at least 6 characters long.");
            return false;
        }

        responder.setPassword(newPassword);

        try {
            String dburl = "jdbc:mysql://localhost:3306/WomenSafetyDB";
            String dbuser = "root";
            String dbpass = "";
            Connection con = DriverManager.getConnection(dburl, dbuser, dbpass);

            if (con != null) {
                String updateQuery = "UPDATE responder_details SET Password = ? WHERE Responder_id = ?";
                PreparedStatement pst = con.prepareStatement(updateQuery);
                pst.setString(1, newPassword);
                pst.setInt(2, responderId);

                int result = pst.executeUpdate();

                if (result > 0) {
                    SystemLogger.success("Password updated successfully!");
                    pst.close();
                    con.close();
                    return true;
                }

                pst.close();
                con.close();
            }

        } catch (Exception e) {
            SystemLogger.error("Database update error: " + e.getMessage());
            responder.setPassword(oldPassword);
        }
        return false;
    }


    public void displayAllResponders(){
        System.out.println(Constants.CYAN + "\n--- All Registered Responders ---" + Constants.RESET);
        System.out.println("=" .repeat(85));

        try {
            String dburl = "jdbc:mysql://localhost:3306/WomenSafetyDB";
            String dbuser = "root";
            String dbpass = "";

            Connection con = DriverManager.getConnection(dburl, dbuser, dbpass);

            String query = "SELECT * FROM responder_details ORDER BY Responder_id";
            PreparedStatement pst = con.prepareStatement(query);
            ResultSet rs = pst.executeQuery();

            boolean hasResponders = false;
            int responderCount = 0;
            int availableCount = 0;
            int busyCount = 0;

            while (rs.next()) {
                hasResponders = true;
                responderCount++;

                int responderId = rs.getInt("Responder_id");
                String name = rs.getString("Name");
                String phone = rs.getString("Phone_no");
                String email = rs.getString("Email");
                String zone = rs.getString("Zone");
                boolean availability = rs.getBoolean("Availability");
                double x = rs.getDouble("X_coordinate");
                double y = rs.getDouble("Y_coordinate");

                if (availability) {
                    availableCount++;
                } else {
                    busyCount++;
                }

                System.out.println(Constants.BLUE + "Responder ID: " + Constants.RESET + responderId);
                System.out.println(Constants.BLUE + "Name: " + Constants.RESET + name);
                System.out.println(Constants.BLUE + "Phone: " + Constants.RESET + "+91 " + phone);
                System.out.println(Constants.BLUE + "Email: " + Constants.RESET + email);
                System.out.println(Constants.BLUE + "Zone: " + Constants.RESET + zone);

                String availabilityText = availability ?
                    Constants.SUCCESS + "AVAILABLE" + Constants.RESET :
                    Constants.ERROR + "BUSY" + Constants.RESET;
                System.out.println(Constants.BLUE + "Status: " + Constants.RESET + availabilityText);

                System.out.println(Constants.COORDINATES + "Coordinates: " + Constants.RESET +
                    "(" + String.format("%.4f", x) + ", " + String.format("%.4f", y) + ")");
                System.out.println("-".repeat(45));
            }

            if (!hasResponders) {
                SystemLogger.info("No responders found in database.");
            } else {
                System.out.println(Constants.SUCCESS + "Total Responders: " + responderCount + Constants.RESET);
                System.out.println(Constants.SUCCESS + "Available: " + availableCount + Constants.RESET +
                    " | " + Constants.WARNING + "Busy: " + busyCount + Constants.RESET);
            }

            rs.close();
            pst.close();
            con.close();

        } catch (Exception e) {
            SystemLogger.error("Error retrieving responders from database: " + e.getMessage());

            System.out.println("\nFalling back to in-memory data:");
            if(resp.isEmpty()){
                SystemLogger.info("No responders registered in memory either.");
            } else {
                for(Responder responder: resp.values()){
                    System.out.println(responder);
                    System.out.println("");
                }
            }
        }
        System.out.println("=" .repeat(85));
    }

    public boolean isPhoneExists(String phone) {
        String dbUrl = "jdbc:mysql://localhost:3306/WomenSafetyDB";
        String dbUser = "root";
        String dbPass = "";

        String query = "SELECT COUNT(*) FROM responder_details WHERE Phone_no = ?";
        try (Connection conn = DriverManager.getConnection(dbUrl, dbUser, dbPass);
             PreparedStatement stmt = conn.prepareStatement(query)) {
            stmt.setString(1, phone);
            ResultSet rs = stmt.executeQuery();
            if (rs.next()) {
                return rs.getInt(1) > 0;
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
     return false;
    }
}