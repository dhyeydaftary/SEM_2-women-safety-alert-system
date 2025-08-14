import java.io.*;
import java.time.*;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.regex.Pattern;
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



class Constants {
    public static final String EMAIL_PATTERN = "^[A-Za-z0-9+_.-]+@gmail\\.com$";
    public static final String PHONE_PATTERN = "^(7|8|9)\\d{9}$";
    public static final String ZONE_PATTERN = "(?i)^(North|South|East|West)$";

    public static final String STATUS_ACTIVE = "Active";
    public static final String STATUS_ASSIGNED = "ASSIGNED";
    public static final String STATUS_WAITING = "WAITING";
    public static final String STATUS_RESOLVED = "Resolved";

    public static final int RESPONDER_STATUS_CHECKER_INTERVAL = 60000;
    public static final int PENDING_ALERT_CHECKER_INTERVAL= 45000;
    public static boolean ENABLE_BACKGROUND_LOGGING = false;

    public static final double INDIA_MIN_LAT = 8.4;
    public static final double INDIA_MAX_LAT = 37.6;
    public static final double INDIA_MIN_LNG = 68.7;
    public static final double INDIA_MAX_LNG = 97.25;

    static final String RESET ="\u001b[0m";
    static final String ERROR = "\u001B[31m";
    static final String SUCCESS = "\u001B[32m";
    static final String WARNING = "\u001B[33m";
    static final String INFO = "\u001B[37m";
    static final String CYAN = "\u001B[1;36m";
    static final String BLUE = "\u001B[34m";
}





class CoordinateGenerator{
    public static final double INDIA_MIN_LAT =8.4;
    public static final double INDIA_MAX_LAT = 37.6;
    public static final double INDIA_MIN_LNG = 68.7;
    public static final double INDIA_MAX_LNG = 97.25;

    private static  Random random = new Random();

    public static double generateRandomLatitude(){
        return INDIA_MIN_LAT+(INDIA_MAX_LAT - INDIA_MIN_LAT)*random.nextDouble();
    }

    public static double generateRandomLongitude() {
        return INDIA_MIN_LNG + (INDIA_MAX_LNG - INDIA_MIN_LNG) * random.nextDouble();
    }

    public static double[] generateRandomCoordinates(){
        return new double[] {
                generateRandomLatitude(),
                generateRandomLongitude()
        };
    }

    public static double[] generateZoneBasedCoordinates(String zone){
        double baseLat,baseLng;
        double offset =2.0;

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

        lat = Math.max(INDIA_MIN_LAT, Math.min(INDIA_MAX_LAT, lat));
        lng = Math.max(INDIA_MIN_LNG, Math.min(INDIA_MAX_LNG, lng));

        return new double[] {lat, lng};
    }

    public static void main(String[] args) {
        System.out.println("Random coordinates in India:");


        for (int i = 0; i < 5; i++) {
            double[] coords = generateRandomCoordinates();
            System.out.printf("Coordinate %d: Lat=%.4f, Lng=%.4f%n",
                    i+1, coords[0], coords[1]);
        }

        System.out.println("\nZone-based coordinates:");
        String[] zones = {"North", "South", "East", "West"};

        for (String zone : zones) {
            double[] coords = generateZoneBasedCoordinates(zone);
            System.out.printf("%s Zone: Lat=%.4f, Lng=%.4f%n",
                    zone, coords[0], coords[1]);
        }
    }
}



class SystemLogger {
    private static final String LOG_FILE = "system_log.txt";
    private static final DateTimeFormatter TIMESTAMP_FORMAT = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

    public enum LogLevel {
        INFO, WARNING, ERROR, SUCCESS
    }

    public static void log(LogLevel level, String message) {
        String timestamp = LocalDateTime.now().format(TIMESTAMP_FORMAT);
        String logEntry = String.format("[%s] %s: %s", timestamp, level, message);

        printToConsole(level, logEntry);
        writeToFile(logEntry);
    }

    private static void printToConsole(LogLevel level, String message) {
        String color = switch (level) {
            case INFO -> "\u001B[37m"+"[INFO] ";
            case SUCCESS -> "\u001B[32m"+"[SUCCESS] ";
            case WARNING -> "\u001B[33m"+"[WARNING] ";
            case ERROR -> "\u001B[31m"+"[ERROR] ";
        };
        System.out.println(color + message + "\u001b[0m");
    }

    private static void writeToFile(String logEntry) {
        try (FileWriter fw = new FileWriter(LOG_FILE, true);
             PrintWriter pw = new PrintWriter(fw)) {
            pw.println(logEntry);
        } catch (IOException e) {
            System.err.println("Failed to write to log file: " + e.getMessage());
        }
    }

    public static void info(String message) {
        log(LogLevel.INFO, message);
    }
    public static void warning(String message) {
        log(LogLevel.WARNING, message);
    }
    public static void error(String message) {
        log(LogLevel.ERROR, message);
    }
    public static void success(String message) {
        log(LogLevel.SUCCESS, message);
    }
}



/*     PHASE-II     */


class Person{
    protected String name;
    protected String phone;
    protected String email;
    protected String zone;
    protected String password;

    public Person(String Name, String Phone, String Email, String Zone, String password){
        this.name=Name;
        this.phone=Phone;
        this.email=Email;
        this.zone=Zone;
        this.password=password;
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
}



class User extends Person{
    private int id;
    private String location;
    private double x=0.0,y=0.0;

    public User(int Id, String Name, String Phone, String Email, String Location, String Zone, String Password){
        super(Name, Phone, Email, Zone, Password);
        this.id=Id;
        this.location=Location;
        this.password=Password;

        if (this.id <= 0)
            throw new IllegalArgumentException("User ID must be positive");
        if (name==null || name.trim().isEmpty())
            throw new IllegalArgumentException("User name cannot be empty");

        if (!Pattern.matches(Constants.PHONE_PATTERN, Phone))
            throw new IllegalArgumentException("Invalid phone number. It must be 10 digits.");
        if (!Pattern.matches(Constants.EMAIL_PATTERN, Email))
            throw new IllegalArgumentException("Invalid email format.");

        if(this.location==null || this.location.trim().isEmpty())
            throw new IllegalArgumentException("Location cannot be empty");
        if(!Pattern.matches(Constants.ZONE_PATTERN, Zone))
            throw new IllegalArgumentException("Invalid zone. It must be North, South, East, or West.");
        if(Password==null || Password.trim().length() < 6)
            throw new IllegalArgumentException("Password must be at least 6 characters long.");
    }

    public int getId(){
        return id;
    }
    public String getLocation(){
        return location;
    }
    public double getX(){
        return x;
    }
    public double getY(){
        return y;
    }


    public void setLocation(String location){
        this.location=location;
    }
    public void setX(double x){
        this.x=x;
    }
    public void setY(double y){
        this.y=y;
    }

    @Override
    public String toString() {
        return "User Id: "+ id+
                "\nName: " + name +
                "\nLocation: " + location + " (" +
                String.format("%.4f", x) + ", " +
                String.format("%.4f", y) + ")" +
                "\nZone: " + zone;
    }
}






class Responder extends Person{
    private int id;
    private boolean available;
    private double x=0.0,y=0.0;

    Responder(int Id, String Name, String Phone, String Email, String Zone, boolean Available, String Password){
        super(Name, Phone, Email, Zone, Password);
        this.id=Id;
        this.password=Password;

        if (this.id <= 0)
            throw new IllegalArgumentException("Responder ID must be positive");
        if (name==null || name.trim().isEmpty())
            throw new IllegalArgumentException("Responder name cannot be empty");

        if (!Pattern.matches(Constants.PHONE_PATTERN, Phone))
            throw new IllegalArgumentException("Invalid phone number. It must be 10 digits.");
        if (!Pattern.matches(Constants.EMAIL_PATTERN, Email))
            throw new IllegalArgumentException("Invalid email format.");

        if(!Pattern.matches(Constants.ZONE_PATTERN, Zone))
            throw new IllegalArgumentException("Invalid zone. It must be North, South, East, or West.");
        if(Password==null || Password.trim().length() < 6)
            throw new IllegalArgumentException("Password must be at least 6 characters long.");

        this.available=Available;
    }

    public int getId(){
        return id;
    }
    public boolean isAvailable(){
        return available;
    }
    public double getX(){
        return x;
    }
    public double getY(){
        return y;
    }


    void setAvailable(boolean available){
        this.available=available;
    }
    public void setX(double x){
        this.x=x;
    }
    public void setY(double y){
        this.y=y;
    }

    @Override
    public String toString() {
        return "Responder Id: " + id +
                "\nName: " + name +
                "\nPhone No: " + phone +
                "\nEmail: " + email +
                "\nZone: " + zone +
                "\nAvailable: " + available +
                "\nLocation: (" +
                String.format("%.4f", x) + ", " +
                String.format("%.4f", y) + ")";
    }

    public void notifyUserAssigned(User user) {
        System.out.println(Constants.INFO + "\n=== NEW ALERT ASSIGNED TO YOU ===" + Constants.RESET);
        System.out.println(Constants.BLUE + "Responder ID: " + Constants.RESET + this.getId());
        System.out.println(Constants.BLUE + "Responder name: "  + Constants.RESET + this.getName());
        System.out.println("");

        System.out.println("User details:");
        System.out.println(Constants.BLUE + "Name: " + Constants.RESET + user.getName());
        System.out.println(Constants.BLUE + "Phone: +91 " + Constants.RESET + user.getPhone());
        System.out.println(Constants.BLUE +"Email: " + Constants.RESET + user.getEmail());
        System.out.println(Constants.BLUE + "Location: " + Constants.RESET + user.getLocation());
        System.out.println(Constants.BLUE + "Zone: " + Constants.RESET + user.getZone());
        System.out.println(Constants.BLUE + "Coordinates: " + Constants.RESET + "(" +String.format("%.4f", user.getX()) +
                ", " +String.format("%.4f", user.getY()) + ")");
        System.out.println("=".repeat(40));
        System.out.flush();
    }
}





class AuthenticationHelper{
    public static boolean isValidPassword(String password) {
        if (password == null || password.trim().isEmpty()) {
            System.out.println("Password is null or empty");
            return false;
        }
        if (password.length() < 6) {
            System.out.println("Password length must be at least 6.");
            return false;
        }
        return true;
    }

    public static boolean authenticateUser(User user, String enteredPassword) {
        return user != null && user.getPassword().equals(enteredPassword);
    }

    public static boolean authenticateResponder(Responder responder, String enteredPassword) {
        return responder != null && responder.getPassword().equals(enteredPassword);
    }
}



class UserManager{
    private HashMap <Integer,User> users;
    private int nextUserId;
    private Random random;

    UserManager(){
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
                
                users.put(userId, dbUser);
                
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

    /*public boolean removeUser(int id){
        if(users.containsKey(id)){
            users.remove(id);
            SystemLogger.success("User with ID "+id+" removed.");
            return true;
        }
        return false;
    }*/

    public boolean updateUserInDatabase(int userId, String fieldName, String oldValue, String newValue) {
        try {
            String dburl = "jdbc:mysql://localhost:3306/WomenSafetyDB";
            String dbuser = "root";
            String dbpass = "";
            Connection con = DriverManager.getConnection(dburl, dbuser, dbpass);
            
            if (con != null) {
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
                    logUserUpdate(con, userId, fieldName, oldValue, newValue, userId);
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
                    logUserUpdate(con, userId, "password", "****", "****", userId);
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


    private void logUserUpdate(Connection con, int userId, String fieldChanged, String oldValue, String newValue, int updatedBy) {
        try {
            String logQuery = "INSERT INTO user_update_logs (User_id, Field_changed, Old_value, New_value, Updated_by) VALUES (?, ?, ?, ?, ?)";
            PreparedStatement logPst = con.prepareStatement(logQuery);
            logPst.setInt(1, userId);
            logPst.setString(2, fieldChanged);
            logPst.setString(3, oldValue);
            logPst.setString(4, newValue);
            logPst.setInt(5, updatedBy);
            
            int logResult = logPst.executeUpdate();
            if (logResult > 0) {
                SystemLogger.info("Update logged successfully");
            }
            
            logPst.close();
        } catch (Exception e) {
            SystemLogger.error("Error logging update: " + e.getMessage());
        }
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
                System.out.println(Constants.BLUE + "Zone: " + Constants.RESET + zone);
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
}





class ResponderManager{
    private HashMap<Integer,Responder> resp;
    private int nextResponderId;
    private Random random;

    ResponderManager(){
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

                resp.put(responderId, dbResponder);
                
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

    /*public boolean removeResponder(int id){
        if(resp.containsKey(id)){
            resp.remove(id);
            SystemLogger.success("Responder with ID "+id+" removed.");
            return true;
        }
        return false;
    }*/

    public boolean updateResponderInDatabase(int responderId, String fieldName, String oldValue, String newValue) {
        try {
            String dburl = "jdbc:mysql://localhost:3306/WomenSafetyDB";
            String dbuser = "root";
            String dbpass = "";
            Connection con = DriverManager.getConnection(dburl, dbuser, dbpass);
            
            if (con != null) {
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
                    case "zone":
                        updateQuery = "UPDATE responder_details SET Zone = ?, X_coordinate = ?, Y_coordinate = ? WHERE Responder_id = ?";
                        break;
                    case "availability":
                        updateQuery = "UPDATE responder_details SET Availability = ? WHERE Responder_id = ?";
                        break;
                }
                
                pst = con.prepareStatement(updateQuery);
                
                if (fieldName.equalsIgnoreCase("zone")) {
                    Responder responder = getResponderById(responderId);
                    pst.setString(1, newValue);
                    pst.setDouble(2, responder.getX());
                    pst.setDouble(3, responder.getY());
                    pst.setInt(4, responderId);
                } else if (fieldName.equalsIgnoreCase("availability")) {
                    pst.setBoolean(1, Boolean.parseBoolean(newValue));
                    pst.setInt(2, responderId);
                } else {
                    pst.setString(1, newValue);
                    pst.setInt(2, responderId);
                }
                
                int result = pst.executeUpdate();
                
                if (result > 0) {
                    logResponderUpdate(con, responderId, fieldName, oldValue, newValue, responderId);
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
                    logResponderUpdate(con, responderId, "password", "****", "****", responderId);
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

    private void logResponderUpdate(Connection con, int responderId, String fieldChanged, String oldValue, String newValue, int updatedBy) {
        try {
            String logQuery = "INSERT INTO responder_update_logs (Responder_id, Field_changed, Old_value, New_value, Updated_by) VALUES (?, ?, ?, ?, ?)";
            PreparedStatement logPst = con.prepareStatement(logQuery);
            logPst.setInt(1, responderId);
            logPst.setString(2, fieldChanged);
            logPst.setString(3, oldValue);
            logPst.setString(4, newValue);
            logPst.setInt(5, updatedBy);
            
            int logResult = logPst.executeUpdate();
            if (logResult > 0) {
                SystemLogger.info("Update logged successfully");
            }
            
            logPst.close();
        } catch (Exception e) {
            SystemLogger.error("Error logging update: " + e.getMessage());
        }
    }

    public Responder getResponderById(int Id){
        return resp.get(Id);
    }

    Collection<Responder>getAllResponders(){
        return resp.values();
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
                
                System.out.println(Constants.BLUE + "Coordinates: " + Constants.RESET + 
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
}






class LocationManager {
    private HashMap<String, List<Responder>> zoneMap;
    private Random random;

    public LocationManager() {
        zoneMap = new HashMap<>();
        random=new Random();
    }

    public void addResponder(Responder responder) {
        if(responder==null || responder.getZone() == null || responder.getZone().trim().isEmpty()) {
            SystemLogger.error("Invalid responder or zone.");
            return;
        }
        String zone = responder.getZone();

        if (!zoneMap.containsKey(zone)) {
            zoneMap.put(zone, new ArrayList<>());
        }

        zoneMap.get(zone).add(responder);
        SystemLogger.success("Responder added to zone: "+zone);
    }

    public void removeResponder(int id) {
        for (String zone : zoneMap.keySet()) {
            List<Responder> responders = zoneMap.get(zone);
            responders.removeIf(r -> r.getId() == id);
        }
        SystemLogger.info("Responder with ID " + id + " removed.");
    }

    public List<Responder> getRespondersInZone(String zone) {
        if (zone == null)
            return new ArrayList<>();
        for (String key : zoneMap.keySet()) {
            if (key.equalsIgnoreCase(zone)) {
                return zoneMap.get(key);
            }
        }
        return new ArrayList<>();
    }

    public Responder findRandomAvailableResponderInZone(String zone) {
        List<Responder> responders = getRespondersInZone(zone);
        List<Responder> availableResponders = new ArrayList<>();

        for (Responder r : responders) {
            if (r.isAvailable()) {
                availableResponders.add(r);
            }
        }

        if (!availableResponders.isEmpty()) {
            int randomIndex = random.nextInt(availableResponders.size());
            return availableResponders.get(randomIndex);
        }

        return null;
    }

    public Responder findNearestAvailableResponder(double userX, double userY, String zone) {
        List<Responder> responders = getRespondersInZone(zone);
        Responder nearest = null;
        double minDistance = Double.MAX_VALUE;

        for (Responder r : responders){
            if (r.isAvailable()){
                double dist = Math.sqrt(Math.pow(userX - r.getX(), 2) + Math.pow(userY - r.getY(), 2));
                if(dist < minDistance){
                    minDistance = dist;
                    nearest = r;
                }
            }
        }
        return nearest;
    }

    public List<Responder> getAllResponders(){
        List<Responder> all = new ArrayList<>();
        for (List<Responder> responders : zoneMap.values()){
            all.addAll(responders);
        }
        return all;
    }

    public void printAllResponders(){
        for (String zone : zoneMap.keySet()){
            System.out.println("Zone: " + zone);
            for (Responder r : zoneMap.get(zone)){
                System.out.println("-> "+r.getName()+" (Available: "+r.isAvailable()+ ")");
            }
        }
    }
}


/*     PHASE-III     */



class Alert{
    private static int idCount = 1;
    private int alertId;
    private User user;
    private LocalDateTime timestamp;
    private String status;
    private Responder assignedResponder;

    Alert(User user){
        this.alertId = idCount++;
        this.user = user;
        this.timestamp = LocalDateTime.now();
        this.status = Constants.STATUS_ACTIVE;
    }

    int getId(){
        return alertId;
    }
    int getAlertId(){
        return alertId;
    }
    User getUser(){
        return user;
    }
    LocalDateTime getTimestamp(){
        return timestamp;
    }
    String getStatus(){
        return status;
    }
    Responder getResponder(){
        return assignedResponder;
    }
    Responder getAssignedResponder(){
        return assignedResponder;
    }


    void setStatus(String status){
        this.status = status;
    }
    void setResponder(Responder responder){
        this.assignedResponder = responder;
    }

    @Override
    public String toString(){
        return "Alert ID:" +alertId+"\nUser: "+user.getName()+"\nTime: "+timestamp+"\nStatus: "+status+
                (assignedResponder!=null?("\nResponder: "+assignedResponder.getName()): "\nResponder: No responder assigned");
    }
}






class Dispatcher{
    private Queue<Alert> alertQueue;
    private LocationManager locationManager;
    private EscalationLogger escalationLogger;

    public Dispatcher(LocationManager locationManager){
        this.alertQueue = new LinkedList<>();
        this.locationManager = locationManager;
        this.escalationLogger = new EscalationLogger();
    }

    public void addAlert(Alert alert){
        alertQueue.add(alert);
        SystemLogger.info("New alert added by " + alert.getUser().getName() + " at " + alert.getTimestamp().format(DateTimeFormatter.ofPattern("HH:mm:ss")));
    }

    public void processNextAlert(){
        if(alertQueue.isEmpty()){
            SystemLogger.info("[INFO] No pending alerts to process.");
            return;
        }

        Alert alert=alertQueue.peek();
        String userZone=alert.getUser().getZone();
        Responder responder = locationManager.findRandomAvailableResponderInZone(userZone);

        if(responder != null){
            alertQueue.poll();
            alert.setResponder(responder);
            responder.setAvailable(false);
            alert.setStatus(Constants.STATUS_ASSIGNED);

            try{
                responder.notifyUserAssigned(alert.getUser());
            }catch(Exception e){
                SystemLogger.error("Error in notifications: "+e.getMessage());
            }
            SystemLogger.success("Alert assigned to " +responder.getName()+ " for user " + alert.getUser().getName());

        }else{
            alert.setStatus(Constants.STATUS_WAITING);
            SystemLogger.warning("No available responder in "+userZone+" for user "+alert.getUser().getName());
        }


    }

    public void showPendingAlerts(){
        if(alertQueue.isEmpty()){
            SystemLogger.info("No pending alerts.");
            return;
        }

        System.out.println("Pending Alerts (" + alertQueue.size() + " total):");
        for(Alert alert : alertQueue){
            System.out.println("Alert ID: " + alert.getAlertId()+ " | User: " + alert.getUser().getName() +
                    " | Zone: " + alert.getUser().getZone()+ " | Status: " + alert.getStatus() +
                    " | Time: " + alert.getTimestamp().format(DateTimeFormatter.ofPattern("HH:mm:ss")));
            System.out.println("");
        }
    }

    public void processAllPendingAlerts() {
        if(alertQueue.isEmpty()){
            SystemLogger.info("No pending alerts to process.");
            return;
        }

        int originalSize = alertQueue.size();
        int processed = 0;

        while (!alertQueue.isEmpty() && processed < originalSize) {
            Alert alert = alertQueue.peek();
            String userZone = alert.getUser().getZone();
            Responder responder = locationManager.findRandomAvailableResponderInZone(userZone);

            if(responder != null){
                alertQueue.poll();
                alert.setResponder(responder);
                responder.setAvailable(false);
                alert.setStatus(Constants.STATUS_ASSIGNED);

                responder.notifyUserAssigned(alert.getUser());
                SystemLogger.success("Alert assigned to "+responder.getName()+" for user "+ alert.getUser().getName());
            } else {
                Alert unassignedAlert = alertQueue.poll();
                unassignedAlert.setStatus(Constants.STATUS_WAITING);
                alertQueue.offer(unassignedAlert);

                SystemLogger.warning("No available responder in "+userZone+" zone for user "+unassignedAlert.getUser().getName());
                escalationLogger.logToFile(unassignedAlert, "No available responder in zone "+userZone);
            }
            processed++;
        }
        int stillWaiting = 0;
        for(Alert alert : alertQueue) {
            if(alert.getStatus().equals(Constants.STATUS_WAITING)) {
                stillWaiting++;
            }
        }

        if(stillWaiting > 0) {
            SystemLogger.info(stillWaiting + " alert(s) remain in queue waiting for responders.");
        }
    }

    public void completeAlert(Alert alert){
        Responder responder = alert.getResponder();
        if(responder != null){
            responder.setAvailable(true);
            alert.setStatus(Constants.STATUS_RESOLVED);
            SystemLogger.success("Alert ID " + alert.getAlertId() + " resolved by " + responder.getName());

            if(!alertQueue.isEmpty()){
                SystemLogger.info("Checking if pending alerts can now be processed...");
                processAllPendingAlerts();
            }
        } else {
            SystemLogger.warning("No responder assigned to this alert.");
        }
    }

    public boolean reassignResponder(Alert alert){
        String zone = alert.getUser().getZone();
        Responder current = alert.getResponder();

        for (Responder r : locationManager.getRespondersInZone(zone)){
            if (r!=current && r.isAvailable()){
                if(current!=null){
                    current.setAvailable(true);
                }
                alert.setResponder(r);
                r.setAvailable(false);
                alert.setStatus(Constants.STATUS_ASSIGNED);

                r.notifyUserAssigned(alert.getUser());
                SystemLogger.success("Responder reassigned to "+r.getName()+" for Alert ID "+alert.getAlertId());
                return true;
            }
        }
        SystemLogger.warning("No alternate responder available for Alert ID " + alert.getAlertId());
        escalationLogger.logToFile(alert, "No alternate responder available in zone "+zone);
        return false;
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






class NearestResponderFinder{
    public static Responder findNearestResponder(User user,List<Responder> responders){
        Responder nearestResponder = null;
        double minDistance = Double.MAX_VALUE;

        for(Responder r : responders){
            if(!r.isAvailable())
                continue;
            double distance=calculateDistance(user.getX(), user.getY(), r.getX(), r.getY());

            if(distance<minDistance){
                minDistance=distance;
                nearestResponder=r;
            }
        }
        return nearestResponder;
    }

    public static double calculateDistance(double x1, double y1, double x2, double y2){
        return Math.sqrt(Math.pow(x2- x1,2) + Math.pow(y2-y1,2));
    }
}





class ResponderStatusChecker implements Runnable{
    private LocationManager locationManager;

    public ResponderStatusChecker(LocationManager locationManager){
        this.locationManager = locationManager;
    }

    public void run(){
        while(!Thread.currentThread().isInterrupted()){
            try{
                checkResponderStatuses();
                Thread.sleep(Constants.RESPONDER_STATUS_CHECKER_INTERVAL);
            }catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                System.out.println(Constants.CYAN+ "Responder status checker shutting down gracefully." + Constants.RESET);
                break;
            }
        }
    }
    private void checkResponderStatuses(){
        if (!Constants.ENABLE_BACKGROUND_LOGGING) {
            return;
        }
        System.out.println("\n[Background] Checking responder statuses...");
        List<Responder>responders = locationManager.getAllResponders();

        for(Responder r : responders){
            if(r.isAvailable()){
                System.out.println("[Background] "+r.getName()+" is AVAILABLE");
            } else {
                System.out.println("[Background] "+r.getName()+" is BUSY");
            }
        }
    }
}






class AlertLoopThread extends Thread{
    private Dispatcher dispatcher;
    private boolean running = true;

    public AlertLoopThread(Dispatcher dispatcher){
        this.dispatcher = dispatcher;
    }

    public void run(){
        while (running) {
            dispatcher.checkUnassignedAlerts();
            try {
                Thread.sleep(Constants.PENDING_ALERT_CHECKER_INTERVAL);
            }catch (InterruptedException e){
                System.out.println("Thread interrupted.");
            }
        }
    }

    public void stopThread(){
        running = false;
    }
}


class EscalationLogger {
    String FILE_NAME="escalation_log.txt";

    public void logToFile(Alert alert, String reason){
        try (FileWriter fw=new FileWriter(FILE_NAME, true)){
            DateTimeFormatter formatter= DateTimeFormatter.ofPattern("dd-MM-yyyy HH:mm:ss");
            String timestamp=LocalDateTime.now().format(formatter);
            String logEntry =  "[" + timestamp + "] ALERT ID: " + alert.getAlertId()+" | USER: "+alert.getUser().getName()+
                    " | ZONE: "+alert.getUser().getZone()+" | REASON: "+reason+"\n";

            fw.write(logEntry);
        } catch (IOException e){
            System.out.println("Error writing to log file: " + e.getMessage());
        }
    }
}





class Main{
    private static Scanner sc=new Scanner(System.in);
    private static UserManager userManager=new UserManager();
    private static ResponderManager responderManager=new ResponderManager();
    private static LocationManager locationManager=new LocationManager();
    private static Dispatcher dispatcher=new Dispatcher(locationManager);
    private static AlertLoopThread alertLoopThread=new AlertLoopThread(dispatcher);
    private static Thread responderStatusCheckerThread =new Thread(new ResponderStatusChecker(locationManager));
    private static HashMap <Integer, Alert> alertMap = new HashMap<>();
    private static User currentUser = null;
    private static Responder currentResponder = null;

    public static void main(String[] args)throws Exception {

        System.out.println(Constants.CYAN+ "\nStarting Women's Safety Alert System...." +Constants.RESET);

        boolean running = true;

        while(running){

            System.out.println("\n"+"=".repeat(45));
            System.out.println(Constants.BLUE+ "\n      --- WOMEN SAFETY SYSTEM MENU ---" +Constants.RESET);
            System.out.println("\n"+"=".repeat(45));
            System.out.println("1. Register User");
            System.out.println("2. Login User");
            System.out.println("3. Register Responder");
            System.out.println("4. Login Responder");
            System.out.println("5. Raise Alert");
            System.out.println("6. Process Next Alert");
            System.out.println("7. Complete Alert");
            System.out.println("8. Show Pending Alerts");
            System.out.println("9. Update User details");
            System.out.println("10. Update Responder details");
            System.out.println("11. Display User");
            System.out.println("12. Display Responder");
            System.out.println("13. Enable Background Logging");
            System.out.println("0. Exit");
            System.out.println("=".repeat(45));
            System.out.print("Enter choice: ");
            int choice=sc.nextInt();
            sc.nextLine();

            switch(choice){
                case 1:
                    String userName, userPhone, userEmail, userLocation, userZone, userPassword;

                    System.out.println(Constants.CYAN+ "\n--- Register User ---" +Constants.RESET);

                    while (true) {
                        System.out.print("Name: ");
                        userName = sc.nextLine().trim();
                        if(userName.isEmpty() || "null".equalsIgnoreCase(userName) || !userName.matches("[a-zA-Z ]+")) {
                            SystemLogger.error("Invalid name.");
                            System.out.println("");
                        } else {
                            break;
                        }
                    }

                    while (true){
                        System.out.print("Phone (10-digit): +91 ");
                        userPhone = sc.nextLine();
                        if (userPhone.matches(Constants.PHONE_PATTERN)) {
                            break;
                        } else {
                            SystemLogger.error("Invalid phone number.");
                            System.out.println("");
                        }
                    }

                    while (true) {
                        System.out.print("Email (must end with @gmail.com): ");
                        userEmail=sc.nextLine();
                        if(Pattern.matches(Constants.EMAIL_PATTERN, userEmail)) {
                            break;
                        } else {
                            SystemLogger.error("Invalid email format.");
                            System.out.println("");
                        }
                    }

                    while (true) {
                        System.out.print("Location (Landmark): ");
                        userLocation = sc.nextLine();
                        if(userLocation.trim().isEmpty() || "null".equalsIgnoreCase(userLocation)) {
                            SystemLogger.error("Invalid location.");
                            System.out.println("");
                        } else {
                            break;
                        }
                    }

                    while (true) {
                        System.out.print("Zone (North/South/East/West): ");
                        userZone=sc.nextLine().trim();
                        if(Pattern.matches(Constants.ZONE_PATTERN, userZone)) {
                            break;
                        } else {
                            SystemLogger.error("Invalid zone. It must be North, South, East, or West.");
                            System.out.println("");
                        }
                    }

                    while (true) {
                        System.out.print("Password (min 6 chars): ");
                        userPassword = sc.nextLine().trim();
                        if (AuthenticationHelper.isValidPassword(userPassword)) {
                            break;
                        } else {
                            SystemLogger.error("Password must be at least 6 characters.");
                            System.out.println("");
                        }
                    }

                    try{
                        User newUser = userManager.registerUser(userName, userPhone, userEmail, userLocation, userZone, userPassword);
                        SystemLogger.success("User registered successfully. Your ID is: "+newUser.getId());
                    } catch (IllegalArgumentException e) {
                        SystemLogger.error("Registration failed: "+e.getMessage());
                        System.out.println("");
                    }

                    break;

                case 2:
                    System.out.println(Constants.CYAN+ "\n--- User Login ---" +Constants.RESET);
                    int userId;
                    String userPasswordLogin;

                    while (true) {
                        System.out.print("Enter your User ID: ");
                        String input = sc.nextLine();

                        try {
                            userId = Integer.parseInt(input);
                            break;
                        } catch (NumberFormatException e) {
                            SystemLogger.error("Please enter a valid number.");
                            System.out.println("");
                        }
                    }

                    System.out.print("Enter your password: ");
                    userPasswordLogin = sc.nextLine();

                    User loginUser = userManager.authenticateUserLogin(userId, userPasswordLogin);
                    System.out.println("\n" + "─".repeat(85));
                    if (loginUser != null) {
                        currentUser = loginUser;
                        SystemLogger.success("Login successful. Welcome "+loginUser.getName()+"!");
                    } else {
                        SystemLogger.error("Invalid credentials. Please check your ID and password.");
                    }
                    System.out.println("─".repeat(85));

                    break;

                case 3:
                    String respName, respPhone, respEmail, respZone, respPassword;
                    boolean available;

                    System.out.println(Constants.CYAN+ "\n--- Register Responder ---" +Constants.RESET);

                    while (true) {
                        System.out.print("Name: ");
                        respName = sc.nextLine().trim();
                        if(respName.isEmpty() ||  "null".equalsIgnoreCase(respName) || !respName.matches("[a-zA-Z ]+")) {
                            SystemLogger.error("Invalid name. Only letters and spaces allowed.");
                            System.out.println("");
                        } else {
                            break;
                        }
                    }

                    while (true) {
                        System.out.print("Phone (10-digit): +91 ");
                        respPhone = sc.nextLine();
                        if (Pattern.matches(Constants.PHONE_PATTERN, respPhone)) {
                            break;
                        } else {
                            SystemLogger.error("Invalid phone number.");
                            System.out.println("");
                        }
                    }

                    while (true) {
                        System.out.print("Email (must end with @gmail.com): ");
                        respEmail=sc.nextLine();
                        if(Pattern.matches(Constants.EMAIL_PATTERN, respEmail)) {
                            break;
                        } else {
                            SystemLogger.error("Invalid email format.");
                            System.out.println("");
                        }
                    }

                    while (true) {
                        System.out.print("Zone (North/South/East/West): ");
                        respZone=sc.nextLine();
                        if(Pattern.matches(Constants.ZONE_PATTERN, respZone)) {
                            break;
                        } else {
                            SystemLogger.error("Invalid zone. It must be North, South, East, or West.");
                            System.out.println("");
                        }
                    }

                    while (true) {
                        System.out.print("Available (true/false): ");
                        String availabilityInput = sc.nextLine().toLowerCase();
                        if ("true".equalsIgnoreCase(availabilityInput) || "false".equalsIgnoreCase(availabilityInput)) {
                            available = Boolean.parseBoolean(availabilityInput);
                            break;
                        } else {
                            SystemLogger.error("Invalid input. Please enter true or false.");
                            System.out.println("");
                        }
                    }

                    while (true) {
                        System.out.print("Password (min 6 chars): ");
                        respPassword = sc.nextLine().trim();
                        if (AuthenticationHelper.isValidPassword(respPassword)) {
                            break;
                        } else {
                            SystemLogger.error("Password must be at least 6 characters.");
                            System.out.println("");
                        }
                    }

                    try {
                        Responder responder = responderManager.registerResponder(respName, respPhone, respEmail, respZone, available, respPassword);
                        locationManager.addResponder(responder);
                        SystemLogger.success("Responder registered successfully. Your ID is: "+responder.getId());
                    } catch (IllegalArgumentException e) {
                        SystemLogger.error("Registration failed: "+e.getMessage());
                        System.out.println("");
                    }

                    break;

                case 4:
                    System.out.println(Constants.CYAN+ "\n--- Responder Login ---" +Constants.RESET);
                    int responderId;
                    String responderPasswordLogin;

                    while (true) {
                        System.out.print("Enter your Responder ID: ");
                        String input = sc.nextLine();

                        try {
                            responderId = Integer.parseInt(input);
                            if (responderId <= 0) {
                                SystemLogger.error("ID must be a positive number.");
                                System.out.println("");
                            } else {
                                break;
                            }
                        } catch (NumberFormatException e) {
                            SystemLogger.error("Please enter a valid number.");
                            System.out.println("");
                        }
                    }

                    System.out.print("Enter your password: ");
                    responderPasswordLogin = sc.nextLine();

                    Responder loginResponder = responderManager.authenticateResponderLogin(responderId, responderPasswordLogin);
                    System.out.println("\n" + "─".repeat(75));
                    if (loginResponder != null) {
                        currentResponder = loginResponder;
                        SystemLogger.success("Login successful. Welcome "+loginResponder.getName()+"!");
                    } else {
                        SystemLogger.error("Invalid credentials. Please check your ID and password.");
                    }
                    System.out.println("─".repeat(75));

                    break;

                case 5:
                    if (currentUser == null){
                        SystemLogger.warning("Please login first to raise an alert.");
                        System.out.println(""); 
                        break;
                    }

                    Alert alert = new Alert(currentUser);
                    dispatcher.addAlert(alert);
                    alertMap.put(currentUser.getId(), alert);

                    if(!responderStatusCheckerThread.isAlive()) {
                        responderStatusCheckerThread.start();
                    }
                    if(!alertLoopThread.isAlive()) {
                        alertLoopThread.start();
                    }

                    break;

                case 6:
                    dispatcher.processAllPendingAlerts();
                    break;

                case 7:
                    if (currentUser == null) {
                        SystemLogger.warning("Please login to resolve your alert.");
                        System.out.print("");
                        break;
                    }

                    Alert completeAlert = alertMap.get(currentUser.getId());
                    if (completeAlert != null && completeAlert.getStatus().equals(Constants.STATUS_ASSIGNED)) {
                        dispatcher.completeAlert(completeAlert);
                    } else {
                        SystemLogger.info("No active alert assigned to you.");
                        System.out.print("");
                    }

                    break;

                case 8:
                    dispatcher.showPendingAlerts();
                    break;
                    
                case 9:
                    if (currentUser == null) {
                        SystemLogger.warning("Please login first to update your details.");
                        System.out.println("");
                        break;
                    }
                    
                    System.out.println(Constants.CYAN + "\n--- Update User Details ---" + Constants.RESET);
                    System.out.println("Current User Information:");
                    System.out.println("─".repeat(50));
                    System.out.println("Name: " + currentUser.getName());
                    System.out.println("Phone: +91 " + currentUser.getPhone());
                    System.out.println("Email: " + currentUser.getEmail());
                    System.out.println("Location: " + currentUser.getLocation());
                    System.out.println("Zone: " + currentUser.getZone());
                    System.out.println("─".repeat(50));
                    System.out.println();
                    
                    boolean validUpdate = true;
                    while(validUpdate){
                        System.out.println("\nSelect the field you want to update:");
                        System.out.println("1. Name");
                        System.out.println("2. Phone");
                        System.out.println("3. Email");
                        System.out.println("4. Location");
                        System.out.println("5. Zone");
                        System.out.println("6. Password");
                        System.out.println("7. Update All Details (Except Password)");
                        System.out.println("0. Exit");
                        System.out.print("Enter your choice: ");
                        int updateUser = sc.nextInt();
                        sc.nextLine();
                        
                        switch (updateUser) {
                            case 1:
                                System.out.println(Constants.CYAN + "\n--- Update User Name ---" + Constants.RESET);
                                System.out.println("");
                                while(true){
                                    System.out.print("Current Name: " + currentUser.getName());
                                    System.out.print("\nNew Name: ");
                                    String newName = sc.nextLine().trim();

                                    if (newName.isEmpty() || "null".equalsIgnoreCase(newName) || !newName.matches("[a-zA-Z\\s]+$")) {
                                        SystemLogger.error("Update cancelled - Invalid name. Only letters and spaces are allowed.");
                                        System.out.println("");
                                    } else {
                                        String oldName = currentUser.getName();
                                        currentUser.setName(newName);

                                        if (userManager.updateUserInDatabase(currentUser.getId(), "name", oldName, newName)) {
                                            SystemLogger.success("Name updated successfully to: " + newName);
                                        } else {
                                            currentUser.setName(oldName);
                                            SystemLogger.error("Failed to update database. Changes reverted.");
                                        }
                                        break;
                                    }
                                }
                                break;
                                
                            case 2:
                                System.out.println(Constants.CYAN + "\n--- Update User Phone ---" + Constants.RESET);
                                System.out.println("");
                                while(true){
                                    System.out.print("Current Phone: +91 " + currentUser.getPhone());
                                    System.out.print("\nNew Phone (10-digit): +91 ");
                                    String newPhone = sc.nextLine().trim();

                                    if (newPhone.isEmpty() || !Pattern.matches(Constants.PHONE_PATTERN, newPhone)) {
                                        SystemLogger.error("Update cancelled - Invalid phone number.");
                                        System.out.println("");
                                    } else {
                                        String oldPhone = currentUser.getPhone();
                                        currentUser.setPhone(newPhone);
                                        if (userManager.updateUserInDatabase(currentUser.getId(), "phone", oldPhone, newPhone)) {
                                            SystemLogger.success("Phone updated successfully to: +91 " + newPhone);
                                        } else {
                                            currentUser.setPhone(oldPhone);
                                            SystemLogger.error("Failed to update database. Changes reverted.");
                                        }
                                        break;
                                    }
                                }
                                break;
                                
                            case 3:
                                System.out.println(Constants.CYAN + "\n--- Update User Email ---" + Constants.RESET);
                                System.out.println("");
                                while(true){
                                    System.out.print("Current Email: " + currentUser.getEmail());
                                    System.out.print("\nNew Email (must end with @gmail.com): ");
                                    String newEmail = sc.nextLine().trim();

                                    if (newEmail.isEmpty() || !Pattern.matches(Constants.EMAIL_PATTERN, newEmail)) {
                                        SystemLogger.error("Update cancelled - Invalid email format. Email must end with @gmail.com.");
                                        System.out.println("");
                                    } else {
                                        String oldEmail = currentUser.getEmail();
                                        currentUser.setEmail(newEmail);
                                        if (userManager.updateUserInDatabase(currentUser.getId(), "email", oldEmail, newEmail)) {
                                            SystemLogger.success("Email updated successfully to: " + newEmail);
                                        } else {
                                            currentUser.setEmail(oldEmail);
                                            SystemLogger.error("Failed to update database. Changes reverted.");
                                        }
                                        break;
                                    }
                                }
                                break;
                                
                            case 4:
                                System.out.println(Constants.CYAN + "\n--- Update User Location ---" + Constants.RESET);
                                System.out.println("");
                                while(true){
                                    System.out.print("Current Location: " + currentUser.getLocation());
                                    System.out.print("\nNew Location (Landmark): ");
                                    String newLocation = sc.nextLine().trim();
                                    if (newLocation.isEmpty() || "null".equalsIgnoreCase(newLocation)) {
                                        SystemLogger.error("Update cancelled - no input provided.");
                                        System.out.println("");
                                    } else {
                                        String oldLocation = currentUser.getLocation();
                                        currentUser.setLocation(newLocation);
                                        if (userManager.updateUserInDatabase(currentUser.getId(), "location", oldLocation, newLocation)) {
                                            SystemLogger.success("Location updated successfully to: " + newLocation);
                                        } else {
                                            currentUser.setLocation(oldLocation);
                                            SystemLogger.error("Failed to update database. Changes reverted.");
                                        }
                                        break;
                                    }
                                }
                                break;
                                
                            case 5:
                                System.out.println(Constants.CYAN + "\n--- Update User Zone ---" + Constants.RESET);
                                System.out.println("");
                                while(true){
                                    System.out.print("Current Zone: " + currentUser.getZone());
                                    System.out.print("\nNew Zone (North/South/East/West): ");
                                    String newZone = sc.nextLine().trim();

                                    if (newZone.isEmpty() || !Pattern.matches(Constants.ZONE_PATTERN, newZone)) {
                                        SystemLogger.error("Update cancelled - Invalid zone. It must be North, South, East, or West.");
                                        System.out.println("");
                                    } else {
                                        String oldZone = currentUser.getZone();
                                        double oldX = currentUser.getX();
                                        double oldY = currentUser.getY();
                                        
                                        currentUser.setZone(newZone);
                                        double[] coords = CoordinateGenerator.generateZoneBasedCoordinates(newZone);
                                        currentUser.setX(coords[1]);
                                        currentUser.setY(coords[0]);
                                        
                                        if (userManager.updateUserInDatabase(currentUser.getId(), "zone", oldZone, newZone)) {
                                            SystemLogger.success("Zone updated from " + oldZone + " to " + newZone);
                                            SystemLogger.info("Coordinates automatically updated for new zone.");
                                        } else {
                                            currentUser.setZone(oldZone);
                                            currentUser.setX(oldX);
                                            currentUser.setY(oldY);
                                            SystemLogger.error("Failed to update database. Changes reverted.");
                                        }
                                        break;
                                    }
                                }
                                break;
                                

                            case 6:
                                System.out.println(Constants.CYAN + "\n--- Update User Password ---" + Constants.RESET);
                                System.out.println("");
                                
                                while(true) {
                                    System.out.print("Enter current password: ");
                                    String oldPassword = sc.nextLine().trim();
                                    
                                    if (oldPassword.isEmpty()) {
                                        SystemLogger.error("Current password cannot be empty.");
                                        System.out.println("");
                                        continue;
                                    }
                                    
                                    System.out.print("Enter new password (min 6 chars): ");
                                    String newPassword = sc.nextLine().trim();
                                    
                                    if (!AuthenticationHelper.isValidPassword(newPassword)) {
                                        SystemLogger.error("New password must be at least 6 characters long.");
                                        System.out.println("");
                                        continue;
                                    }
                                    
                                    System.out.print("Confirm new password: ");
                                    String confirmPassword = sc.nextLine().trim();
                                    
                                    if (!newPassword.equals(confirmPassword)) {
                                        SystemLogger.error("Passwords do not match. Please try again.");
                                        System.out.println("");
                                        continue;
                                    }
                                    
                                    System.out.print("Are you sure you want to update your password? (y/n): ");
                                    String confirmation = sc.nextLine().trim().toLowerCase();
                                    
                                    if (!confirmation.equals("y") && !confirmation.equals("yes")) {
                                        SystemLogger.info("Password update cancelled.");
                                        break;
                                    }
                                    
                                    if (userManager.updateUserPassword(currentUser.getId(), oldPassword, newPassword)) {
                                        SystemLogger.success("Password updated successfully!");
                                    } else {
                                        SystemLogger.error("Failed to update password. Please check your current password.");
                                    }
                                    break;
                                }
                                break;

                            case 7:
                                System.out.println("");
                                System.out.println(Constants.INFO + "Updating all details:" + Constants.RESET);
                                System.out.println("Press Enter to keep current value, or enter new value to update.");
 
                                String oldName = currentUser.getName();
                                String oldPhone = currentUser.getPhone();
                                String oldEmail = currentUser.getEmail();
                                String oldLocation = currentUser.getLocation();
                                String oldZone = currentUser.getZone();
                                double oldX = currentUser.getX();
                                double oldY = currentUser.getY();
                                
                                boolean updateSuccess = true;
                                
                                System.out.print("Name [" + currentUser.getName() + "]: ");
                                String allName = sc.nextLine().trim();
                                if (!allName.isEmpty() && allName.matches("[a-zA-Z ]+")) {
                                    currentUser.setName(allName);
                                    if (!userManager.updateUserInDatabase(currentUser.getId(), "name", oldName, allName)) {
                                        updateSuccess = false;
                                        currentUser.setName(oldName); 
                                    }
                                }
                                
                                System.out.print("Phone [+91 " + currentUser.getPhone() + "]: +91 ");
                                String allPhone = sc.nextLine().trim();
                                if (!allPhone.isEmpty() && allPhone.matches(Constants.PHONE_PATTERN)) {
                                    String phoneOld = currentUser.getPhone();
                                    currentUser.setPhone(allPhone);
                                    if (!userManager.updateUserInDatabase(currentUser.getId(), "phone", phoneOld, allPhone)) {
                                        updateSuccess = false;
                                        currentUser.setPhone(phoneOld); 
                                    }
                                }
                                
                                System.out.print("Email [" + currentUser.getEmail() + "]: ");
                                String allEmail = sc.nextLine().trim();
                                if (!allEmail.isEmpty() && Pattern.matches(Constants.EMAIL_PATTERN, allEmail)) {
                                    String emailOld = currentUser.getEmail();
                                    currentUser.setEmail(allEmail);
                                    if (!userManager.updateUserInDatabase(currentUser.getId(), "email", emailOld, allEmail)) {
                                        updateSuccess = false;
                                        currentUser.setEmail(emailOld);
                                    }
                                }
                                
                                System.out.print("Location [" + currentUser.getLocation() + "]: ");
                                String allLocation = sc.nextLine().trim();
                                if (!allLocation.isEmpty()) {
                                    String locationOld = currentUser.getLocation();
                                    currentUser.setLocation(allLocation);
                                    if (!userManager.updateUserInDatabase(currentUser.getId(), "location", locationOld, allLocation)) {
                                        updateSuccess = false;
                                        currentUser.setLocation(locationOld); 
                                    }
                                }
                                
                                System.out.print("Zone [" + currentUser.getZone() + "]: ");
                                String allZone = sc.nextLine().trim();
                                if (!allZone.isEmpty() && Pattern.matches(Constants.ZONE_PATTERN, allZone)) {
                                    String zoneOld = currentUser.getZone();
                                    currentUser.setZone(allZone);
                                    double[] coords = CoordinateGenerator.generateZoneBasedCoordinates(allZone);
                                    currentUser.setX(coords[1]);
                                    currentUser.setY(coords[0]);
                                    
                                    if (!userManager.updateUserInDatabase(currentUser.getId(), "zone", zoneOld, allZone)) {
                                        updateSuccess = false;
                                        currentUser.setZone(zoneOld);
                                        currentUser.setX(oldX);
                                        currentUser.setY(oldY);
                                    }
                                }
                                
                                if (updateSuccess) {
                                    SystemLogger.success("All details updated successfully!");
                                } else {
                                    SystemLogger.error("Some updates failed. Please check the logs.");
                                }
                                
                                System.out.println("\nUpdated Information:");
                                System.out.println("─".repeat(50));
                                System.out.println("Name: " + currentUser.getName());
                                System.out.println("Phone: +91 " + currentUser.getPhone());
                                System.out.println("Email: " + currentUser.getEmail());
                                System.out.println("Location: " + currentUser.getLocation());
                                System.out.println("Zone: " + currentUser.getZone());
                                System.out.println("─".repeat(50));
                                System.out.println("");
                                break;
                                
                            case 0:
                                SystemLogger.info("Exiting menu.");
                                validUpdate = false;
                                break;
                                
                            default:
                                SystemLogger.error("Invalid option selected. Please try again.");
                                break;
                        }
                    }
                    break;

                case 10:
                    if (currentResponder == null) {
                        SystemLogger.warning("Please login first to update your details.");
                        System.out.println("");
                        break;
                    }

                    System.out.println(Constants.CYAN + "\n--- Update Responder Details ---" + Constants.RESET);
                    System.out.println("Current Responder Information:");
                    System.out.println("─".repeat(50));
                    System.out.println("Name: " + currentResponder.getName());
                    System.out.println("Phone: +91 " + currentResponder.getPhone());
                    System.out.println("Email: " + currentResponder.getEmail());
                    System.out.println("Zone: " + currentResponder.getZone());
                    System.out.println("Available: " + currentResponder.isAvailable());
                    System.out.println("─".repeat(50));
                    System.out.println();

                    boolean validResponderUpdate = true;

                    while(validResponderUpdate){
                        System.out.println("Select the field you want to update:");
                        System.out.println("1. Name");
                        System.out.println("2. Phone");
                        System.out.println("3. Email");
                        System.out.println("4. Zone");
                        System.out.println("5. Availability");
                        System.out.println("6. Password");
                        System.out.println("7. Update All Details");
                        System.out.println("0. Cancel");
                        System.out.print("Enter your choice: ");

                        int updateResponder = sc.nextInt();
                        sc.nextLine();

                        switch (updateResponder) {
                            case 1:
                                System.out.println(Constants.CYAN + "\n--- Update Responder Name ---" + Constants.RESET);
                                System.out.println("");

                                while(true){
                                    System.out.print("Current Name: " + currentResponder.getName());
                                    System.out.print("\nNew Name: ");
                                    String newName = sc.nextLine().trim();

                                    if (newName.isEmpty() ||  "null".equalsIgnoreCase(newName)  || !newName.matches("[a-zA-Z\\s]+$")) {
                                        SystemLogger.error("Update cancelled - Invalid name. Only letters and spaces are allowed.");
                                        System.out.println("");
                                    } else {
                                        String oldName = currentResponder.getName();
                                        currentResponder.setName(newName);

                                        if (responderManager.updateResponderInDatabase(currentResponder.getId(), "name", oldName, newName)) {
                                            SystemLogger.success("Name updated successfully to: " + newName);
                                        } else {
                                            currentResponder.setName(oldName);
                                            SystemLogger.error("Failed to update database. Changes reverted.");
                                        }
                                        break;
                                    }
                                }
                                break;

                            case 2:
                                System.out.println(Constants.CYAN + "\n--- Update Responder Phone ---" + Constants.RESET);

                                while(true){
                                    System.out.println("");
                                    System.out.print("Current Phone: +91 " + currentResponder.getPhone());
                                    System.out.print("\nNew Phone (10-digit): +91 ");
                                    String newPhone = sc.nextLine().trim();
                                    if (newPhone.isEmpty() || !Pattern.matches(Constants.PHONE_PATTERN, newPhone)) {
                                        SystemLogger.error("Update cancelled - Invalid phone number.");
                                        System.out.println("");
                                    } else {
                                        String oldPhone = currentResponder.getPhone();
                                        currentResponder.setPhone(newPhone);
                                        
                                        if (responderManager.updateResponderInDatabase(currentResponder.getId(), "phone", oldPhone, newPhone)) {
                                            SystemLogger.success("Phone updated successfully to: +91 " + newPhone);
                                        } else {
                                            currentResponder.setPhone(oldPhone);
                                            SystemLogger.error("Failed to update database. Changes reverted.");
                                        }
                                        break;
                                    }
                                }
                                break;

                            case 3:
                                System.out.println(Constants.CYAN + "\n--- Update Responder Email ---" + Constants.RESET);

                                while(true){
                                    System.out.println("");
                                    System.out.print("Current Email: " + currentResponder.getEmail());
                                    System.out.print("\nNew Email (must end with @gmail.com): ");
                                    String newEmail = sc.nextLine().trim();
                                    if (newEmail.isEmpty() || !Pattern.matches(Constants.EMAIL_PATTERN, newEmail)) {
                                        SystemLogger.error("Update cancelled - Invalid email format. Email must end with @gmail.com");
                                        System.out.println("");
                                    } else {
                                        String oldEmail = currentResponder.getEmail();
                                        currentResponder.setEmail(newEmail);

                                        if (responderManager.updateResponderInDatabase(currentResponder.getId(), "email", oldEmail, newEmail)) {
                                            SystemLogger.success("Email updated successfully to: " + newEmail);
                                        } else {
                                            currentResponder.setEmail(oldEmail);
                                            SystemLogger.error("Failed to update database. Changes reverted.");
                                        }
                                        break;
                                    }
                                }
                                break;

                            case 4:
                                System.out.println(Constants.CYAN + "\n--- Update Responder Zone ---" + Constants.RESET);

                                while(true){
                                    System.out.println("");
                                    System.out.print("Current Zone: " + currentResponder.getZone());
                                    System.out.print("\nNew Zone (North/South/East/West): ");
                                    String newZone = sc.nextLine().trim();
                                    if (newZone.isEmpty() || !Pattern.matches(Constants.ZONE_PATTERN, newZone)) {
                                        SystemLogger.error("Update cancelled - Invalid zone. It must be North, South, East, or West.");
                                        System.out.println("");
                                    } else {
                                        String oldZone = currentResponder.getZone();
                                        double oldX = currentResponder.getX();
                                        double oldY = currentResponder.getY();

                                        locationManager.removeResponder(currentResponder.getId());

                                        currentResponder.setZone(newZone);

                                        double[] coords = CoordinateGenerator.generateZoneBasedCoordinates(newZone);
                                        currentResponder.setX(coords[1]);
                                        currentResponder.setY(coords[0]);

                                        locationManager.addResponder(currentResponder);

                                        if (responderManager.updateResponderInDatabase(currentResponder.getId(), "zone", oldZone, newZone)) {
                                            SystemLogger.success("Zone updated from " + oldZone + " to " + newZone);
                                            SystemLogger.info("Coordinates automatically updated for new zone.");
                                        } else {
                                            locationManager.removeResponder(currentResponder.getId());
                                            currentResponder.setZone(oldZone);
                                            currentResponder.setX(oldX);
                                            currentResponder.setY(oldY);
                                            locationManager.addResponder(currentResponder);
                                            SystemLogger.error("Failed to update database. Changes reverted.");
                                        }
                                        break;
                                    }
                                }
                                break;

                            case 5:
                                System.out.println(Constants.CYAN + "\n--- Update Responder Availability ---" + Constants.RESET);

                                while(true){
                                    System.out.println("");
                                    System.out.print("Current Availability: " + currentResponder.isAvailable());
                                    System.out.print("\nNew Availability (true/false): ");
                                    String newAvailability = sc.nextLine().trim().toLowerCase();

                                    if (newAvailability.isEmpty() || (!newAvailability.equalsIgnoreCase("true") && !newAvailability.equalsIgnoreCase("false"))) {
                                        SystemLogger.error("Update cancelled - Invalid input. Please enter true or false.");
                                        System.out.println("");
                                    } else {
                                        boolean oldAvailability = currentResponder.isAvailable();
                                        boolean availability = Boolean.parseBoolean(newAvailability);
                                        currentResponder.setAvailable(availability);

                                        if (responderManager.updateResponderInDatabase(currentResponder.getId(), "availability", 
                                            String.valueOf(oldAvailability), String.valueOf(availability))) {
                                            SystemLogger.success("Availability updated from " + oldAvailability + " to " + availability);
                                        } else {
                                            currentResponder.setAvailable(oldAvailability);
                                            SystemLogger.error("Failed to update database. Changes reverted.");
                                        }
                                        break;
                                    }
                                }
                                break;

                            case 6:
                                System.out.println(Constants.CYAN + "\n--- Update Responder Password ---" + Constants.RESET);
                                System.out.println("");
                                
                                while(true) {
                                    System.out.print("Enter current password: ");
                                    String oldPassword = sc.nextLine().trim();
                                    
                                    if (oldPassword.isEmpty()) {
                                        SystemLogger.error("Current password cannot be empty.");
                                        System.out.println("");
                                        continue;
                                    }
                                    
                                    System.out.print("Enter new password (min 6 chars): ");
                                    String newPassword = sc.nextLine().trim();
                                    
                                    if (!AuthenticationHelper.isValidPassword(newPassword)) {
                                        SystemLogger.error("New password must be at least 6 characters long.");
                                        System.out.println("");
                                        continue;
                                    }
                                    
                                    System.out.print("Confirm new password: ");
                                    String confirmPassword = sc.nextLine().trim();
                                    
                                    if (!newPassword.equals(confirmPassword)) {
                                        SystemLogger.error("Passwords do not match. Please try again.");
                                        System.out.println("");
                                        continue;
                                    }
                                    
                                    System.out.print("Are you sure you want to update your password? (y/n): ");
                                    String confirmation = sc.nextLine().trim().toLowerCase();
                                    
                                    if (!confirmation.equals("y") && !confirmation.equals("yes")) {
                                        SystemLogger.info("Password update cancelled.");
                                        break;
                                    }
                                    
                                    if (responderManager.updateResponderPassword(currentResponder.getId(), oldPassword, newPassword)) {
                                        SystemLogger.success("Password updated successfully!");
                                    } else {
                                        SystemLogger.error("Failed to update password. Please check your current password.");
                                    }
                                    break;
                                }
                                break;

                            case 7:
                                System.out.println(Constants.INFO + "Updating all details:" + Constants.RESET);
                                System.out.println("Press Enter to keep current value, or enter new value to update.");

                                String oldName = currentResponder.getName();
                                String oldPhone = currentResponder.getPhone();
                                String oldEmail = currentResponder.getEmail();
                                String oldZone = currentResponder.getZone();
                                boolean oldAvailability = currentResponder.isAvailable();
                                double oldX = currentResponder.getX();
                                double oldY = currentResponder.getY();

                                boolean updateSuccess = true;

                                System.out.print("Name [" + currentResponder.getName() + "]: ");
                                String allName = sc.nextLine().trim();
                                if (!allName.isEmpty() && allName.matches("[a-zA-Z ]+")) {
                                    String nameOld = currentResponder.getName();
                                    currentResponder.setName(allName);
                                    if (!responderManager.updateResponderInDatabase(currentResponder.getId(), "name", nameOld, allName)) {
                                        updateSuccess = false;
                                        currentResponder.setName(oldName);
                                    }
                                }

                                System.out.print("Phone [+91 " + currentResponder.getPhone() + "]: +91 ");
                                String allPhone = sc.nextLine().trim();
                                if (!allPhone.isEmpty() && allPhone.matches(Constants.PHONE_PATTERN)) {
                                    String phoneOld = currentResponder.getPhone();
                                    currentResponder.setPhone(allPhone);
                                    if (!responderManager.updateResponderInDatabase(currentResponder.getId(), "phone", phoneOld, allPhone)) {
                                        updateSuccess = false;
                                        currentResponder.setPhone(phoneOld);
                                    }
                                }

                                System.out.print("Email [" + currentResponder.getEmail() + "]: ");
                                String allEmail = sc.nextLine().trim();
                                if (!allEmail.isEmpty() && Pattern.matches(Constants.EMAIL_PATTERN, allEmail)) {
                                    String emailOld= currentResponder.getEmail();
                                    currentResponder.setEmail(allEmail);
                                    if (!responderManager.updateResponderInDatabase(currentResponder.getId(), "email", emailOld, allEmail)) {
                                        updateSuccess = false;
                                        currentResponder.setEmail(emailOld);
                                    }
                                }

                                System.out.print("Zone [" + currentResponder.getZone() + "]: ");
                                String allZone = sc.nextLine().trim();
                                if (!allZone.isEmpty() && Pattern.matches(Constants.ZONE_PATTERN, allZone)) {

                                    String zoneOld = currentResponder.getZone();
                                    locationManager.removeResponder(currentResponder.getId());

                                    currentResponder.setZone(allZone);
                                    double[] coords = CoordinateGenerator.generateZoneBasedCoordinates(allZone);
                                    currentResponder.setX(coords[1]);
                                    currentResponder.setY(coords[0]);

                                    locationManager.addResponder(currentResponder);

                                    if (!responderManager.updateResponderInDatabase(currentResponder.getId(), "zone", zoneOld, allZone)) {
                                        updateSuccess = false;
                                        locationManager.removeResponder(currentResponder.getId());
                                        currentResponder.setZone(zoneOld);
                                        currentResponder.setX(oldX);
                                        currentResponder.setY(oldY);
                                        locationManager.addResponder(currentResponder);
                                    }
                                }

                                System.out.print("Availability [" + currentResponder.isAvailable() + "]: ");
                                String allAvailability = sc.nextLine().trim().toLowerCase();
                                if (!allAvailability.isEmpty() && (allAvailability.equals("true") || allAvailability.equals("false"))) {
                                    boolean availabilityOld = currentResponder.isAvailable();
                                    boolean newAvail = Boolean.parseBoolean(allAvailability);   
                                    currentResponder.setAvailable(newAvail);

                                    if (!responderManager.updateResponderInDatabase(currentResponder.getId(), "availability",String.valueOf(availabilityOld), String.valueOf(newAvail))) {
                                        updateSuccess = false;
                                        currentResponder.setAvailable(availabilityOld);
                                    }
                                }

                                if (updateSuccess) {
                                    SystemLogger.success("All details updated successfully!");
                                } else {
                                    SystemLogger.error("Some updates failed. Please check individual fields.");
                                }

                                SystemLogger.success("All details updated successfully!");
                                System.out.println("\nUpdated Information:");
                                System.out.println("─".repeat(50));
                                System.out.println("Name: " + currentResponder.getName());
                                System.out.println("Phone: +91 " + currentResponder.getPhone());
                                System.out.println("Email: " + currentResponder.getEmail());
                                System.out.println("Zone: " + currentResponder.getZone());
                                System.out.println("Available: " + currentResponder.isAvailable());
                                System.out.println("─".repeat(50));
                                break;

                            case 0:
                                SystemLogger.info("Exiting menu.");
                                validResponderUpdate = false;
                                break;

                            default:
                                SystemLogger.error("Invalid option selected. Please try again.");
                                break;
                        }
                    }
                    break;

                case 11:
                    userManager.displayAllUsers();
                    break;

                case 12:
                    responderManager.displayAllResponders();
                    break;

                case 13:
                    Constants.ENABLE_BACKGROUND_LOGGING = !Constants.ENABLE_BACKGROUND_LOGGING;
                    SystemLogger.info("Background logging "+(Constants.ENABLE_BACKGROUND_LOGGING ? "ENABLED" : "DISABLED"));
                    break;

                case 0:
                    running = false;
                    responderStatusCheckerThread.interrupt();
                    alertLoopThread.stopThread();
                    SystemLogger.info("Exiting the system.");
                    break;

                default:
                    SystemLogger.error("Invalid choice. Please try again.");
                    System.out.print("");
                    break;
            }
        }
    }
}