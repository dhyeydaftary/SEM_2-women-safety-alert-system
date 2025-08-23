package com.womensafety.alertsystem.manager;

import com.womensafety.alertsystem.model.User;
import com.womensafety.alertsystem.security.RBACManager;
import com.womensafety.alertsystem.util.SystemLogger;
import com.womensafety.alertsystem.util.Constants;
import com.womensafety.alertsystem.service.AuthenticationHelper;
import com.womensafety.alertsystem.model.Role;
import java.sql.*;
import java.util.HashMap;
import java.util.Collection;
import java.util.Random;

public class UserManager{
    private HashMap <Integer,User> users;
    private int nextUserId;
    private Random random;

    public UserManager(){
        users=new HashMap<>();
        random=new Random();
        initializeNextUserId();
    }

    private void initializeNextUserId() {
        try {
            String dburl = "jdbc:mysql://localhost:3306/WomenSafetyDB";
            String dbuser = "root";
            String dbpass = "";

            Connection con = DriverManager.getConnection(dburl, dbuser, dbpass);
            
            String query = "SELECT MAX(User_id) as max_id FROM user_details";
            PreparedStatement pst = con.prepareStatement(query);
            ResultSet rs = pst.executeQuery();
            
            if (rs.next()) {
                int maxId = rs.getInt("max_id");
                nextUserId = maxId + 1;
            } else {
                nextUserId = 1;
            }
            
            rs.close();
            pst.close();
            con.close();
            
            SystemLogger.info("Next User ID initialized to: " + nextUserId);
            
        } catch (Exception e) {
            SystemLogger.error("Error initializing User ID: " + e.getMessage());
            nextUserId = 1;
        }
    }

    public User registerUser(String Name, String Phone, String Email, String Location, String Zone, String Password){
        User newUser=new User(nextUserId, Name, Phone, Email, Location, Zone, Password);

        double[] coords = generateZoneBasedCoordinates(Zone);
        newUser.setX(coords[1]);
        newUser.setY(coords[0]);

        users.put(nextUserId, newUser);

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

            String insertUser = "INSERT INTO user_details (User_id, Name, Phone_no, Email, Location, Zone, Password, X_coordinate, Y_coordinate) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?)";
            PreparedStatement pst = con.prepareStatement(insertUser);

            pst.setInt(1, nextUserId);
            pst.setString(2, Name);
            pst.setString(3, Phone);
            pst.setString(4, Email);
            pst.setString(5, Location);
            pst.setString(6, Zone);
            pst.setString(7, Password);
            pst.setDouble(8, newUser.getX());
            pst.setDouble(9, newUser.getY());

            int result = pst.executeUpdate();

            if (result > 0) {
                SystemLogger.success("User saved to database successfully!");
                nextUserId++;
            }else{
                SystemLogger.error("Failed to save user to database.");
                users.remove(nextUserId);
                return null;
            }

            pst.close();
            con.close();

        } catch (Exception e) {
            SystemLogger.error("Database error: " + e.getMessage());
            users.remove(nextUserId);
            return null;
        }

        return newUser;
    }

    public User authenticateUserLogin(int userId, String password) {
        User user = users.get(userId);
        if (user!=null && AuthenticationHelper.authenticateUser(user, password)) {
            user.setRole(Role.USER);
            RBACManager.setCurrentUser(user);
            return user;
        }

        try {
            String dburl = "jdbc:mysql://localhost:3306/WomenSafetyDB";
            String dbuser = "root";
            String dbpass = "";
            
            Connection con = DriverManager.getConnection(dburl, dbuser, dbpass);
            
            String query = "SELECT * FROM user_details WHERE User_id = ? AND Password = ?";
            PreparedStatement pst = con.prepareStatement(query);
            pst.setInt(1, userId);
            pst.setString(2, password);
            
            ResultSet rs = pst.executeQuery();
            
            if (rs.next()) {
                String name = rs.getString("Name");
                String phone = rs.getString("Phone_no");
                String email = rs.getString("Email");
                String location = rs.getString("Location");
                String zone = rs.getString("Zone");
                double x = rs.getDouble("X_coordinate");
                double y = rs.getDouble("Y_coordinate");
                
                User dbUser = new User(userId, name, phone, email, location, zone, password);
                dbUser.setX(x);
                dbUser.setY(y);
                dbUser.setRole(Role.USER);
                users.put(userId, dbUser);

                RBACManager.setCurrentUser(dbUser);
                
                rs.close();
                pst.close();
                con.close();
                
                return dbUser;
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

    public boolean updateUserCoordinates(int userId, double x, double y){
        User user = users.get(userId);
        if(user != null) {
            user.setX(x);
            user.setY(y);
            SystemLogger.success("Coordinates updated for user: " + user.getName());
            return true;
        }
        return false;
    }



    public boolean updateUserInDatabase(int userId, String fieldName, String oldValue, String newValue) {
        try {
            String dburl = "jdbc:mysql://localhost:3306/WomenSafetyDB";
            String dbuser = "root";
            String dbpass = "";
            Connection con = DriverManager.getConnection(dburl, dbuser, dbpass);
            
            if (con != null) {
                PreparedStatement ps1 = con.prepareStatement("SET @current_user_id = ?");
                ps1.setInt(1, userId);
                ps1.execute();
                ps1.close();
                String updateQuery = "";
                PreparedStatement pst = null;
                
                switch (fieldName.toLowerCase()) {
                    case "name":
                        updateQuery = "UPDATE user_details SET Name = ? WHERE User_id = ?";
                        break;
                    case "phone":
                        updateQuery = "UPDATE user_details SET Phone_no = ? WHERE User_id = ?";
                        break;
                    case "email":
                        updateQuery = "UPDATE user_details SET Email = ? WHERE User_id = ?";
                        break;
                    case "location":
                        updateQuery = "UPDATE user_details SET Location = ? WHERE User_id = ?";
                        break;
                    case "zone":
                        updateQuery = "UPDATE user_details SET Zone = ?, X_coordinate = ?, Y_coordinate = ? WHERE User_id = ?";
                        break;
                }
                
                pst = con.prepareStatement(updateQuery);
                
                if (fieldName.equalsIgnoreCase("zone")) {
                    User user = getUserById(userId);
                    pst.setString(1, newValue);
                    pst.setDouble(2, user.getX());
                    pst.setDouble(3, user.getY());
                    pst.setInt(4, userId);
                } else {
                    pst.setString(1, newValue);
                    pst.setInt(2, userId);
                }
                
                int result = pst.executeUpdate();
                
               if (result > 0) {
                    //logUserUpdate(con, userId, fieldName, oldValue, newValue, userId);
                    SystemLogger.success("Database updated successfully for " + fieldName);
                    pst.close();
                    con.close();
                    return true;
                }

                pst.close();
                con.close();
            }
        } catch (Exception e) {
            SystemLogger.error("Database update error: " + e.getMessage());
        }
        return false;
    }

    public boolean updateUserPassword(int userId, String oldPassword, String newPassword) {
        User user = users.get(userId);
        if (user == null) {
            SystemLogger.error("User not found.");
            return false;
        }
        
        if (!user.getPassword().equals(oldPassword)) {
            SystemLogger.error("Current password is incorrect.");
            return false;
        }
        
        if (!AuthenticationHelper.isValidPassword(newPassword)) {
            SystemLogger.error("New password must be at least 6 characters long.");
            return false;
        }
        
        user.setPassword(newPassword);
        
        try {
            String dburl = "jdbc:mysql://localhost:3306/WomenSafetyDB";
            String dbuser = "root";
            String dbpass = "";
            Connection con = DriverManager.getConnection(dburl, dbuser, dbpass);
            
            if (con != null) {
                String updateQuery = "UPDATE user_details SET Password = ? WHERE User_id = ?";
                PreparedStatement pst = con.prepareStatement(updateQuery);
                pst.setString(1, newPassword);
                pst.setInt(2, userId);
                
                int result = pst.executeUpdate();
                
                if (result > 0) {
                    //logUserUpdate(con, userId, "password", "****", "****", userId);
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
            user.setPassword(oldPassword);
        }
        return false;
    }



    public User getUserById(int Id){
        return users.get(Id);
    }

    Collection<User>getAllUsers(){
        return users.values();
    }

    public void displayAllUsers(){
        System.out.println(Constants.CYAN + "\n--- All Registered Users ---" + Constants.RESET);
        System.out.println("=" .repeat(80));
            
        try {
            String dburl = "jdbc:mysql://localhost:3306/WomenSafetyDB";
            String dbuser = "root";
            String dbpass = "";
                
            Connection con = DriverManager.getConnection(dburl, dbuser, dbpass);
                
            String query = "SELECT * FROM user_details ORDER BY User_id";
            PreparedStatement pst = con.prepareStatement(query);
            ResultSet rs = pst.executeQuery();
                
            boolean hasUsers = false;
            int userCount = 0;
                
            while (rs.next()) {
                hasUsers = true;
                userCount++;
                    
                int userId = rs.getInt("User_id");
                String name = rs.getString("Name");
                String phone = rs.getString("Phone_no");
                String email = rs.getString("Email");
                String location = rs.getString("Location");
                String zone = rs.getString("Zone");
                double x = rs.getDouble("X_coordinate");
                double y = rs.getDouble("Y_coordinate");
                    
                System.out.println(Constants.BLUE + "User ID: " + Constants.RESET + userId);
                System.out.println(Constants.BLUE + "Name: " + Constants.RESET + name);
                System.out.println(Constants.BLUE + "Phone: " + Constants.RESET + "+91 " + phone);
                System.out.println(Constants.BLUE + "Email: " + Constants.RESET + email);
                System.out.println(Constants.BLUE + "Location: " + Constants.RESET + location);
                System.out.println(Constants.COORDINATES + "Zone: " + Constants.RESET + zone);
                System.out.println(Constants.BLUE + "Coordinates: " + Constants.RESET + 
                    "(" + String.format("%.4f", x) + ", " + String.format("%.4f", y) + ")");
                System.out.println("-".repeat(40));
            }
                
            if (!hasUsers) {
                SystemLogger.info("No users found in database.");
            } else {
                System.out.println(Constants.SUCCESS + "Total Users: " + userCount + Constants.RESET);
            }
                
            rs.close();
            pst.close();
            con.close();
                
        } catch (Exception e) {
            SystemLogger.error("Error retrieving users from database: " + e.getMessage());
                
            System.out.println("\nFalling back to in-memory data:");
            if(users.isEmpty()){
                SystemLogger.info("No users registered in memory either.");
            } else {
                for(User user: users.values()){
                    System.out.println(user);
                    System.out.println("");
                }
            }
        }
        System.out.println("=" .repeat(80));
    }

    public boolean isPhoneExists(String phone) {
        String dbUrl = "jdbc:mysql://localhost:3306/WomenSafetyDB";
        String dbUser = "root";
        String dbPass = "";

        String query = "SELECT COUNT(*) FROM user_details WHERE Phone_no = ?";
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