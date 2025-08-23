package com.womensafety.alertsystem.main;

import com.womensafety.alertsystem.model.*;
import com.womensafety.alertsystem.model.Person;
import com.womensafety.alertsystem.manager.*;
import com.womensafety.alertsystem.service.*;
import com.womensafety.alertsystem.util.*;
import com.womensafety.alertsystem.security.*;
import java.sql.*;
import java.util.*;
import java.util.regex.Pattern;

class Main{
    private static Scanner sc=new Scanner(System.in);
    // Initialize core managers
    private static UserManager userManager=new UserManager();
    private static ResponderManager responderManager=new ResponderManager();
    private static AdminManager adminManager = new AdminManager();
    private static LocationManager locationManager=new LocationManager();
    private static Dispatcher dispatcher=new Dispatcher(locationManager, userManager, responderManager);
    // Start threads for background tasks
    private static AlertLoopThread alertLoopThread=new AlertLoopThread(dispatcher);
    private static Thread responderStatusCheckerThread =new Thread(new ResponderStatusChecker(locationManager));
    private static HashMap <Integer, Alert> alertMap = new HashMap<>();
    private static User currentUser = null;
    private static Responder currentResponder = null;

    /*
                       JAVA, DATA STRUCTURE & DBMS CONCEPTS USED                          
        
        JAVA CONCEPTS IMPLEMENTED:
        -> Object-Oriented Programming (OOP)
          | Classes and Objects (Person, User, Responder, Alert, etc.)
          | Inheritance (User extends Person, Responder extends Person)
          | Encapsulation (private fields with getter/setter methods)
          | Polymorphism (Method overriding - toString methods)
        -> Exception Handling (try-catch blocks throughout)
        -> Multithreading (AlertLoopThread, ResponderStatusChecker)
        -> Enumerations (LogLevel enum in SystemLogger)
        -> Static Methods and Variables (Constants, SystemLogger)
        -> Constructor Overloading (Multiple constructors)
        -> Regular Expressions (Pattern.matches for validation)
        -> File I/O Operations (FileWriter, PrintWriter for logging)
        -> Date/Time API (LocalDateTime, DateTimeFormatter)
        -> Anonymous Inner Classes
        -> Package Management (java.io.*, java.time.*, java.util.*, java.sql.*)
        -> Generic Programming
        
        DATA STRUCTURE CONCEPTS:
        -> HashMap<Integer, User> - User management by ID
        -> HashMap<Integer, Responder> - Responder management
        -> HashMap<String, List<Responder>> - Zone-based responder mapping
        -> Queue<Alert> (LinkedList) - Alert processing queue
        -> ArrayList<Responder> - Dynamic responder lists
        -> Arrays (double[]) - Coordinate storage
        -> Collection Framework - Generic collections
        -> Iterator Pattern - Collection traversal
        -> FIFO Queue Operations - First In, First Out alert processing
        -> Generic Types - Type-safe collections

        DATABASE MANAGEMENT CONCEPTS:
        -> JDBC (Java Database Connectivity)
        -> Database Connection Management
        -> SQL Operations:
          | SELECT queries (data retrieval)
          | INSERT statements (data insertion)
          | UPDATE statements (data modification)
          | JOIN operations (multi-table queries)
        -> PreparedStatement (SQL injection prevention)
        -> ResultSet (query result processing)
        -> Database Schema Design:
          | user_details table
          | responder_details table
          | alert_details table
          | dispatches table
          | alert_status_history table
          | user_update_logs table
          | responder_update_logs table
        -> Primary Keys (User_id, Responder_id, Alert_id)
        -> Foreign Key Relationships
        -> Database Constraints and Validation
        -> Transaction Management (implicit)
        -> Audit Trail (update logs)
        -> Data Integrity and Consistency
        
        DESIGN PATTERNS & PRINCIPLES:
        -> Singleton-like Pattern (Static managers)
        -> Factory Pattern concepts (Object creation)
        -> Observer Pattern (Alert notifications)");
        -> Strategy Pattern (Different responder finding strategies)
        -> Builder Pattern concepts (Complex object construction)
        
        ADVANCED FEATURES:
        -> Background Processing (Continuous alert monitoring)
        -> Coordinate-based Location Management
        -> Zone-based Resource Allocation
        -> Real-time Alert Processing
        -> Automated Responder Assignment
        -> Distance Calculation Algorithms
        -> Status Tracking and History
        -> Escalation Management
        -> Comprehensive Logging System
        
    */

    // Main entry point of the Women's Safety Alert System application
    public static void main(String[] args) throws Exception {
        System.out.println(Constants.CYAN + "\nStarting Women's Safety Alert System...." + Constants.RESET);
        AdminManager adminManager = new AdminManager();
        boolean running = true;
        
        // Main application loop - continues until user chooses to exit
        while (running) {
            if (!RBACManager.isLoggedIn()) {
                running = showLoginMenu(adminManager);
            } else {
                running = showRoleSelectionMenu(adminManager);
            }
        }
        
        // Cleanup - gracefully shutdown background threads
        if (responderStatusCheckerThread.isAlive()) {
            responderStatusCheckerThread.interrupt();
        }
        if (alertLoopThread.isAlive()) {
            alertLoopThread.stopThread();
        }
        SystemLogger.info("System shutting down.");
    }

    // Displays the login menu and handles user authentication flow
    private static boolean showLoginMenu(AdminManager adminManager) {   
        return showRoleSelectionMenu(adminManager);
    }

    // Displays the main role selection menu and handles user role choice
    private static boolean showRoleSelectionMenu(AdminManager adminManager) {
        System.out.println("\n" + "=".repeat(50));
        System.out.println(Constants.MAGENTA_ITALIC_BOLD + "            Saftey Alert System Menu            " + Constants.RESET);
        System.out.println("=".repeat(50));
        System.out.println("Please select your role:");
        System.out.println("1. User");
        System.out.println("2. Responder");
        System.out.println("3. Admin");
        System.out.println("0. Exit");
        System.out.println("=".repeat(45));
        
        int choice = getValidChoice(0, 3);
        
        switch (choice) {
            case 1: // User
                return showUserMenu();
            case 2: // Responder
                return showResponderMenu();
            case 3: // Admin
                return showAdminMenu(adminManager);
            case 0:
                return false;
        }
        return true;
    }


    private static boolean showUserMenu() {
        boolean runUser = true;
        while (runUser) {
            System.out.println("\n" + "=".repeat(50));
            System.out.println(Constants.BLUE + " USER MENU " + Constants.RESET);
            System.out.println("=".repeat(50));
            
            // Show different options based on whether user is logged in
            if (RBACManager.isLoggedIn() && RBACManager.hasRole(Role.USER)) {
                Person loggedInPerson = (Person) RBACManager.getCurrentUser();
                System.out.println("Logged in as: " + loggedInPerson.getName());
                System.out.println("3. Raise Alert");
                System.out.println("4. Update Profile");
                System.out.println("5. Logout");
            } else {
                System.out.println("1. Register User");
                System.out.println("2. Login User");
                System.out.println("0. Exit to Main Menu");
            }
            System.out.println("=".repeat(50));

            int maxChoice = RBACManager.isLoggedIn() && RBACManager.hasRole(Role.USER) ? 5 : 2;
            int choice = getValidChoice(0, maxChoice);

            switch (choice) {
                case 1:
                    if (!(RBACManager.isLoggedIn() && RBACManager.hasRole(Role.USER))) {
                        handleUserRegistration();
                    }
                    break;

                case 2:
                    if (!(RBACManager.isLoggedIn() && RBACManager.hasRole(Role.USER))) {
                        handleUserLogin();
                    }
                    break;

                case 3:
                    if (RBACManager.isLoggedIn() && RBACManager.hasRole(Role.USER)) {
                        try {
                            RBACManager.checkPermission(Permission.RAISE_ALERT);
                            handleRaiseAlert();
                        } catch (SecurityException e) {
                            SystemLogger.error("Access denied: " + e.getMessage());
                        }
                    }
                    break;

                case 4:
                    if (RBACManager.isLoggedIn() && RBACManager.hasRole(Role.USER)) {
                        try {
                            RBACManager.checkPermission(Permission.UPDATE_USER_DETAILS);
                            handleUpdateUserProfile();
                        } catch (SecurityException e) {
                            SystemLogger.error("Access denied: " + e.getMessage());
                        }
                    }
                    break;

                case 5:
                    if (RBACManager.isLoggedIn() && RBACManager.hasRole(Role.USER)) {
                        RBACManager.logout();
                        SystemLogger.success("User logged out successfully");
                    }
                    break;

                case 0:
                    runUser=false;
                    break;
            }
        }
        return true;
    }


    private static boolean showResponderMenu() {
        boolean runResponder=true;
        while(runResponder) {
            System.out.println("\n" + "=".repeat(50));
            System.out.println(Constants.BLUE + " RESPONDER MENU " + Constants.RESET);
            System.out.println("=".repeat(50));
            
            // Show different options based on whether responder is logged in
            if (RBACManager.isLoggedIn() && RBACManager.hasRole(Role.RESPONDER)) {
                System.out.println("Logged in as: " + RBACManager.getCurrentUser().getName());
                System.out.println("3. Process Next Alert");
                System.out.println("4. Complete Alert");
                System.out.println("5. Show Pending Alerts");
                System.out.println("6. Update Profile");
                System.out.println("7. Logout");
            } else {
                System.out.println("1. Register Responder");
                System.out.println("2. Login Responder");
                System.out.println("0. Exit to Main Menu");
            }
            System.out.println("=".repeat(50));

            int maxChoice = RBACManager.isLoggedIn() && RBACManager.hasRole(Role.RESPONDER) ? 7 : 2;
            int choice = getValidChoice(0, maxChoice);

            switch (choice) {
                case 1:
                    if (!(RBACManager.isLoggedIn() && RBACManager.hasRole(Role.RESPONDER))) {
                        handleResponderRegistration();
                    }
                    break;

                case 2:
                    if (!(RBACManager.isLoggedIn() && RBACManager.hasRole(Role.RESPONDER))) {
                        handleResponderLogin();
                    }
                    break;

                case 3:
                    if (RBACManager.isLoggedIn() && RBACManager.hasRole(Role.RESPONDER)) {
                        try {
                            RBACManager.checkPermission(Permission.PROCESS_NEXT_ALERT);
                            dispatcher.processNextAlert();
                        } catch (SecurityException e) {
                            SystemLogger.error("Access denied: " + e.getMessage());
                        }
                    }
                    break;

                case 4:
                    if (RBACManager.isLoggedIn() && RBACManager.hasRole(Role.RESPONDER)) {
                        try {
                            RBACManager.checkPermission(Permission.COMPLETE_ALERT);
                            handleCompleteAlert();
                        } catch (SecurityException e) {
                            SystemLogger.error("Access denied: " + e.getMessage());
                        }
                    }
                    break;

                case 5:
                    if (RBACManager.isLoggedIn() && RBACManager.hasRole(Role.RESPONDER)) {
                        try {
                            RBACManager.checkPermission(Permission.SHOW_PENDING_ALERTS);
                            dispatcher.showPendingAlerts();
                        } catch (SecurityException e) {
                            SystemLogger.error("Access denied: " + e.getMessage());
                        }
                    }
                    break;

                case 6:
                    if (RBACManager.isLoggedIn() && RBACManager.hasRole(Role.RESPONDER)) {
                        try {
                            RBACManager.checkPermission(Permission.UPDATE_RESPONDER_DETAILS);
                            handleUpdateResponderProfile();
                        } catch (SecurityException e) {
                            SystemLogger.error("Access denied: " + e.getMessage());
                        }
                    }
                    break;

                case 7:
                    if (RBACManager.isLoggedIn() && RBACManager.hasRole(Role.RESPONDER)) {
                        RBACManager.logout();
                        SystemLogger.success("Responder logged out successfully");
                    }
                    break;

                case 0:
                   runResponder=false;
            }
        }
        return true;
    }

    private static boolean showAdminMenu(AdminManager adminManager) {
        boolean runAdmin=true;
        while (runAdmin) {
            System.out.println("\n" + "=".repeat(50));
            System.out.println(Constants.BLUE + " ADMIN MENU " + Constants.RESET);
            System.out.println("=".repeat(50));
            
            // Show different options based on whether admin is logged in
            if (RBACManager.isLoggedIn() && RBACManager.hasRole(Role.ADMIN)) {
                System.out.println("Logged in as: " + RBACManager.getCurrentUser().getName());
                System.out.println("3. View All Users");
                System.out.println("4. View All Responders");
                System.out.println("5. View Pending Alerts");
                System.out.println("6. System Statistics");
                System.out.println("7. Logout");
            } else {
                System.out.println("1. Create Admin Account");
                System.out.println("2. Login Admin");
                System.out.println("0. Exit to Main Menu");
            }
            System.out.println("=".repeat(50));

            int maxChoice = RBACManager.isLoggedIn() && RBACManager.hasRole(Role.ADMIN) ? 7 : 2;
            int choice = getValidChoice(0, maxChoice);

            switch (choice) {
                case 1:
                    handleAdminCreation(adminManager);
                    break;

                case 2:
                    handleAdminLogin(adminManager);
                    break;

                case 3:
                    try {
                        RBACManager.checkPermission(Permission.DISPLAY_ALL_USERS);
                        userManager.displayAllUsers();
                    } catch (SecurityException e) {
                        SystemLogger.error("Access denied: " + e.getMessage());
                    }
                    break;

                case 4:
                    try {
                        RBACManager.checkPermission(Permission.DISPLAY_ALL_RESPONDERS);
                        responderManager.displayAllResponders();
                    } catch (SecurityException e) {
                        SystemLogger.error("Access denied: " + e.getMessage());
                    }
                    break;

                case 5:
                    try {
                        RBACManager.checkPermission(Permission.VIEW_PENDING_ALERTS);
                        dispatcher.showPendingAlerts();
                    } catch (SecurityException e) {
                        SystemLogger.error("Access denied: " + e.getMessage());
                    }
                    break;

                case 6:
                    try {
                        RBACManager.checkPermission(Permission.VIEW_SYSTEM_STATISTICS);
                        adminManager.displaySystemStatistics();
                    } catch (SecurityException e) {
                        SystemLogger.error("Access denied: " + e.getMessage());
                    }
                    break;

                case 7:
                    if (RBACManager.isLoggedIn() && RBACManager.hasRole(Role.ADMIN)) {
                        RBACManager.logout();
                        SystemLogger.success("Admin logged out successfully");
                    }
                    break;

                case 0:
                    runAdmin=false;
            }
        }
        return true;
    }


    private static int getValidChoice(int min, int max) {
        int choice = -1;
        boolean validInput = false;
        while (!validInput) {
            System.out.print("Enter your choice (" + min + "-" + max + "): ");
            try {
                if (sc.hasNextInt()) {
                    choice = sc.nextInt();
                    sc.nextLine();
                    if (choice >= min && choice <= max) {
                        validInput = true;
                    } else {
                        SystemLogger.error("Invalid choice! Please enter a number between " + min + " and " + max + ".");
                        System.out.println("");
                    }
                } else {
                    SystemLogger.error("Invalid input! Please enter a valid number.");
                    sc.nextLine(); // Clear invalid input
                    System.out.println("");
                }
            } catch (Exception e) {
                System.out.println("Error reading input. Please try again.");
                sc.nextLine();
            }
        }
        return choice;
    }


    private static void handleUserRegistration() {
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

        while (true) {
            System.out.print("Phone (10-digit): +91 ");
            userPhone = sc.nextLine().trim();

            // Check format first
            if (!userPhone.matches(Constants.PHONE_PATTERN)) {
                SystemLogger.error("Invalid phone number.");
                System.out.println("");
                continue;
            }

            // Check duplicate in DB
            if (userManager.isPhoneExists(userPhone)) {
                SystemLogger.error("This phone number is already registered. Please use another one.");
                System.out.println("");
                continue;
            }

            break;
        }

        while (true) {
            System.out.print("Email (must end with @gmail.com): ");
            userEmail=sc.nextLine().trim();
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
            userZone=sc.nextLine().trim().toLowerCase();
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
    }


    // Handle user login
    private static void handleUserLogin() {
        System.out.println(Constants.CYAN+ "\n--- User Login ---" +Constants.RESET);
        int userId;
        String userPasswordLogin;

        while (true) {
            System.out.print("Enter your User ID: ");
            String input = sc.nextLine().trim();

            try {
                userId = Integer.parseInt(input);
                break;
            } catch (NumberFormatException e) {
                SystemLogger.error("Please enter a valid number.");
                System.out.println("");
            }
        }

        System.out.print("Enter your password: ");
        userPasswordLogin = sc.nextLine().trim();

        User loginUser = userManager.authenticateUserLogin(userId, userPasswordLogin);
        System.out.println("\n" + "─".repeat(85));
        if (loginUser != null) {
            currentUser = loginUser;
            SystemLogger.success("Login successful. Welcome "+loginUser.getName()+"!");
        } else {
            SystemLogger.error("Invalid credentials. Please check your ID and password.");
        }
        System.out.println("─".repeat(85)); 
    }



    private static void handleResponderRegistration() {
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
            respPhone = sc.nextLine().trim();

            // Validate phone format first
            if (!Pattern.matches(Constants.PHONE_PATTERN, respPhone)) {
                SystemLogger.error("Invalid phone number.");
                System.out.println("");
                continue; // ask again
            }

            // Check for duplicate immediately
            if (responderManager.isPhoneExists(respPhone)) { // <-- Add this method
                SystemLogger.error("Phone number already exists. Please enter a new number.");
                System.out.println("");
                continue; // ask again
            }

            break; // valid & not duplicate
        }

        while (true) {
            System.out.print("Email (must end with @gmail.com): ");
            respEmail=sc.nextLine().trim();
            if(Pattern.matches(Constants.EMAIL_PATTERN, respEmail)) {
                break;
            } else {
                SystemLogger.error("Invalid email format.");
                System.out.println("");
            }
        }

        while (true) {
            System.out.print("Zone (North/South/East/West): ");
            respZone=sc.nextLine().trim().toLowerCase();
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
    }


    // Handle responder login
    private static void handleResponderLogin() {
        System.out.println(Constants.CYAN + "\n--- Responder Login ---" + Constants.RESET);
        int responderId;
        String responderPasswordLogin;

        while (true) {
            System.out.print("Enter your Responder ID: ");
            String input = sc.nextLine().trim();
            try {
                responderId = Integer.parseInt(input);
                break;
            } catch (NumberFormatException e) {
                SystemLogger.error("Please enter a valid number.");
                System.out.println("");
            }
        }

        System.out.print("Enter your password: ");
        responderPasswordLogin = sc.nextLine();

        Responder loginResponder = responderManager.authenticateResponderLogin(responderId, responderPasswordLogin);
        System.out.println("\n" + "─".repeat(85));
        if (loginResponder != null) {
            SystemLogger.success("Login successful. Welcome " + loginResponder.getName() + "!");
        } else {
            SystemLogger.error("Invalid credentials. Please check your ID and password.");
        }
        System.out.println("─".repeat(85));
    }


    // Handle admin creation
    private static void handleAdminCreation(AdminManager adminManager) {
        String adminName, adminPhone, adminEmail, adminPassword;
        System.out.println(Constants.CYAN + "\n--- Create Admin Account ---" + Constants.RESET);
        
        while (true) {
            System.out.print("Name: ");
            adminName = sc.nextLine().trim();
            if(adminName.isEmpty() || "null".equalsIgnoreCase(adminName) || !adminName.matches("[a-zA-Z ]+")) {
                SystemLogger.error("Invalid name. Only letters and spaces are allowed.");
                System.out.println("");
            } else {
                break;
            }
        }

        while (true) {
            System.out.print("Phone (10-digit): +91 ");
            adminPhone = sc.nextLine().trim();


            if (!adminPhone.matches(Constants.PHONE_PATTERN)) {
                SystemLogger.error("Invalid phone number. Must be 10 digits starting with 7, 8, or 9.");
                System.out.println("");
                continue;
            }


            if (adminManager.isPhoneExists(adminPhone)) {
                SystemLogger.error("This phone number is already registered for another Admin. Please use a different number.");
                System.out.println("");
                continue;
            }

            break;
        }

        while (true) {
            System.out.print("Email (must end with @gmail.com): ");
            adminEmail = sc.nextLine().trim();
            if(Pattern.matches(Constants.EMAIL_PATTERN, adminEmail)) {
                break;
            } else {
                SystemLogger.error("Invalid email format. Email must end with @gmail.com.");
                System.out.println("");
            }
        }
        
        while (true) {
            System.out.print("Password (min 6 chars): ");
            adminPassword = sc.nextLine().trim();
            if (AuthenticationHelper.isValidPassword(adminPassword)) {
                break;
            } else {
                SystemLogger.error("Password must be at least 6 characters long.");
                System.out.println("");
            }
        }
        
        System.out.println("\n" + Constants.INFO + "Admin Account Details:" + Constants.RESET);
        System.out.println("Name: " + adminName);
        System.out.println("Phone: +91 " + adminPhone);
        System.out.println("Email: " + adminEmail);
        System.out.print("\nConfirm admin account creation? (y/n): ");
        String confirmation = sc.nextLine().trim().toLowerCase();
        
        if (confirmation.equals("y") || confirmation.equals("yes")) {
            try {
                Admin newAdmin = adminManager.createAdmin(adminName, adminPhone, adminEmail, adminPassword);
                if (newAdmin != null) {
                    SystemLogger.success("Admin account created successfully!");
                    SystemLogger.success("Admin ID: " + newAdmin.getId());
                    SystemLogger.info("Please save this Admin ID for future logins.");
                } else {
                    SystemLogger.error("Failed to create admin account. Please try again.");
                }
            } catch (Exception e) {
                SystemLogger.error("Admin creation failed: " + e.getMessage());
            }
        } else {
            SystemLogger.info("Admin account creation cancelled.");
        }
    }


    // Handle admin login
    private static void handleAdminLogin(AdminManager adminManager) {
        System.out.println(Constants.CYAN + "\n--- Admin Login ---" + Constants.RESET);
        int adminId;
        String adminPasswordLogin;

        while (true) {
            System.out.print("Enter your Admin ID: ");
            String input = sc.nextLine();
            try {
                adminId = Integer.parseInt(input);
                break;
            } catch (NumberFormatException e) {
                SystemLogger.error("Please enter a valid number.");
                System.out.println("");
            }
        }

        System.out.print("Enter your password: ");
        adminPasswordLogin = sc.nextLine().trim();

        Admin loginAdmin = adminManager.authenticateAdminLogin(adminId, adminPasswordLogin);
        System.out.println("\n" + "─".repeat(85));
        
        if (loginAdmin != null) {
            SystemLogger.success("Login successful. Welcome " + loginAdmin.getName() + "!");
        } else {
            SystemLogger.error("Invalid credentials. Please check your ID and password.");
        }
        System.out.println("─".repeat(85));
    }


    // Check if a user has any active alerts (active, assigned, or waiting status)
    private static boolean hasActiveAlert(int userId) {
        try {
            String dburl = "jdbc:mysql://localhost:3306/WomenSafetyDB";
            String dbuser = "root";
            String dbpass = "";
            
            Connection con = DriverManager.getConnection(dburl, dbuser, dbpass);
            
            // Query to count active alerts for the user across multiple statuses
            String query = "SELECT COUNT(*) as count FROM alert_details WHERE User_id = ? AND Status IN (?, ?, ?)";
            PreparedStatement pst = con.prepareStatement(query);
            pst.setInt(1, userId);
            pst.setString(2, Constants.STATUS_ACTIVE);
            pst.setString(3, Constants.STATUS_ASSIGNED);
            pst.setString(4, Constants.STATUS_WAITING);
            
            ResultSet rs = pst.executeQuery();
            
            boolean hasAlert = false;
            if (rs.next()) {
                hasAlert = rs.getInt("count") > 0; // If count > 0, user has active alerts
            }
            
            rs.close();
            pst.close();
            con.close();
            
            return hasAlert;
            
        } catch (Exception e) {
            SystemLogger.error("Error checking for active alerts: " + e.getMessage());
            return false; // Return false on error to prevent false positives
        }
    }
    

    // Retrieve the most recent active alert for a specific user from database
    private static Alert getUserActiveAlert(int userId) {
        try {
            String dburl = "jdbc:mysql://localhost:3306/WomenSafetyDB";
            String dbuser = "root";
            String dbpass = "";
            
            Connection con = DriverManager.getConnection(dburl, dbuser, dbpass);
            
            // Complex query joining alert, user, and responder tables to get complete alert information
            String query = "SELECT a.*, u.Name as user_name, u.Phone_no as user_phone, u.Email as user_email, " +
                "u.Location as user_location, u.Zone as user_zone, u.Password as user_password, " +
                "u.X_coordinate as user_x, u.Y_coordinate as user_y, " +
                "r.Name as resp_name, r.Phone_no as resp_phone, r.Email as resp_email, " +
                "r.Zone as resp_zone, r.Availability as resp_availability, r.Password as resp_password, " +
                "r.X_coordinate as resp_x, r.Y_coordinate as resp_y " +
                "FROM alert_details a " + 
                "JOIN user_details u ON a.User_id = u.User_id " + // Join with user table
                "LEFT JOIN responder_details r ON a.Responder_id = r.Responder_id " + // Left join with responder table (may be null)
                "WHERE a.User_id = ? AND a.Status IN (?, ?, ?) " + // Filter by user ID and active statuses
                "ORDER by a.Alert_time DESC LIMIT 1"; // Get most recent alert
            
            PreparedStatement pst = con.prepareStatement(query);
            pst.setInt(1, userId);
            pst.setString(2, Constants.STATUS_ACTIVE);
            pst.setString(3, Constants.STATUS_ASSIGNED);
            pst.setString(4, Constants.STATUS_WAITING);
            
            ResultSet rs = pst.executeQuery();
            
            if (rs.next()) {
                // Reconstruct User object from database result
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
                // Only create responder object if responder_id is not 0 (alert has been assigned)
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

                // Create complete Alert object with all related data
                Alert alert = new Alert(
                    rs.getInt("Alert_id"),
                    user,
                    rs.getTimestamp("Alert_time").toLocalDateTime(),
                    rs.getString("Status"),
                    responder
                );
                
                rs.close();
                pst.close();
                con.close();
                
                return alert;
            }
            
            rs.close();
            pst.close();
            con.close();
            
        } catch (Exception e) {
            SystemLogger.error("Error getting user's active alert: " + e.getMessage());
        }
        
        return null; // Return null if no active alert found
    }


    // Update database with responder assignment details including distance calculation
    private static void updateAssignmentInDatabase(Alert alert, Responder responder, double distance) {
        try {
            String dburl = "jdbc:mysql://localhost:3306/WomenSafetyDB";
            String dbuser = "root";
            String dbpass = "";
            Connection con = DriverManager.getConnection(dburl, dbuser, dbpass);
            
            // Update alert status and assign responder in alert_details table
            String updateAlertSQL = "UPDATE alert_details SET Status = ?, Responder_id = ? WHERE Alert_id = ?";
            try (PreparedStatement pst = con.prepareStatement(updateAlertSQL)) {
                pst.setString(1, Constants.STATUS_ASSIGNED);
                pst.setInt(2, responder.getId());
                pst.setInt(3, alert.getAlertId());
                pst.executeUpdate();
            }
            
            // Create dispatch record with calculated distance in dispatches table
            String dispatchSQL = "INSERT INTO dispatches (Alert_id, Responder_id, Distance_km) VALUES (?, ?, ?)";
            try (PreparedStatement pst = con.prepareStatement(dispatchSQL)) {
                pst.setInt(1, alert.getAlertId());
                pst.setInt(2, responder.getId());
                pst.setDouble(3, distance * 111.32); // Convert degrees to kilometers (approx 111.32 km per degree)
                pst.executeUpdate();
            }
            
            // Update responder availability status to false (busy)
            String updateResponderSQL = "UPDATE responder_details SET Availability = ? WHERE Responder_id = ?";
            try (PreparedStatement pst = con.prepareStatement(updateResponderSQL)) {
                pst.setBoolean(1, false);
                pst.setInt(2, responder.getId());
                pst.executeUpdate();
            }
            
            // Log status change in alert_status_history for audit trail
            String historySQL = "INSERT INTO alert_status_history (Alert_id, Previous_status, Current_status, Responder_id) VALUES (?, ?, ?, ?)";
            try (PreparedStatement pst = con.prepareStatement(historySQL)) {
                pst.setInt(1, alert.getAlertId());
                pst.setString(2, Constants.STATUS_ACTIVE);
                pst.setString(3, Constants.STATUS_ASSIGNED);
                pst.setInt(4, responder.getId());
                pst.executeUpdate();
            }
            
            con.close();
            SystemLogger.info("Database updated successfully for automatic assignment.");
            
        } catch (Exception e) {
            SystemLogger.error("Database error during automatic assignment: " + e.getMessage());
        }
    }


    private static void handleRaiseAlert() {
        try {
            RBACManager.checkPermission(Permission.RAISE_ALERT);
            User currentUser = (User) RBACManager.getCurrentUser();
            
            // Check if user already has an active alert
            if (hasActiveAlert(currentUser.getId())) {
                SystemLogger.warning("You already have an active alert. Please wait for it to be resolved.");
                Alert existingAlert = getUserActiveAlert(currentUser.getId());
                if (existingAlert != null) {
                    SystemLogger.info("Your current alert ID: " + existingAlert.getAlertId() + " with status: " + existingAlert.getStatus());
                }
                return;
            }

            Alert alert = new Alert(currentUser);
            SystemLogger.info("Searching for available responder in " + currentUser.getZone() + " zone...");
            
            // Let the dispatcher handle saving to database - this will also add to queue
            dispatcher.addAlert(alert);
            
            // Only process the specific alert that was just created, not all pending alerts
            // This prevents duplicate processing of existing alerts in the queue
            dispatcher.processNextAlert();
            
            SystemLogger.info("Please stay safe and keep your phone accessible.");
            
            if (!responderStatusCheckerThread.isAlive()) {
                responderStatusCheckerThread.start();
            }
            if (!alertLoopThread.isAlive()) {
                alertLoopThread.start();
            }
            
        } catch (SecurityException e) {
            SystemLogger.error("Access denied: " + e.getMessage());
        } catch (Exception e) {
            SystemLogger.error("Unexpected error raising alert: " + e.getMessage());
        }
    }


    private static void handleCompleteAlert() {
        try {
            RBACManager.checkPermission(Permission.COMPLETE_ALERT);
            User currentUser = (User) RBACManager.getCurrentUser();
            Alert userAlert = getUserActiveAlert(currentUser.getId());
            if (userAlert != null) {
                dispatcher.completeAlert(userAlert);
                alertMap.remove(currentUser.getId());
            } else {
                SystemLogger.info("No active alert found for your account.");
            }
        } catch (Exception e) {
            SystemLogger.info("No Pending alert found for your account.");
        }
    }


    // Handle updating user profile
    private static void handleUpdateUserProfile() {
        Person currentPerson = RBACManager.getCurrentUser();
        if (currentPerson == null || !(currentPerson instanceof User)) {
            SystemLogger.error("No user is currently logged in.");
            return;
        }
        
        User currentUser = (User) currentPerson;
        
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
            int updateUser = getValidChoice(0, 7);
            
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
                    String allZone = sc.nextLine().trim().toLowerCase();
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
                    SystemLogger.info("Exiting update menu.");
                    validUpdate = false;
                    break;
                    
                default:
                    SystemLogger.error("Invalid option selected. Please try again.");
                    break;
            }
        }
    }

    private static void handleUpdateResponderProfile() {
        Person currentPerson = RBACManager.getCurrentUser();
        if (currentPerson == null || !(currentPerson instanceof Responder)) {
            SystemLogger.error("No responder is currently logged in.");
            return;
        }
        
        Responder currentResponder = (Responder) currentPerson;
        
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
        
        boolean validUpdate = true;
        while(validUpdate){
            System.out.println("\nSelect the field you want to update:");
            System.out.println("1. Name");
            System.out.println("2. Phone");
            System.out.println("3. Email");
            System.out.println("4. Zone");
            System.out.println("5. Availability");
            System.out.println("6. Password");
            System.out.println("7. Update All Details (Except Password)");
            System.out.println("0. Exit");
            
            int updateResponder = getValidChoice(0, 7);
            
            switch (updateResponder) {
                case 1:
                    System.out.println(Constants.CYAN + "\n--- Update Responder Name ---" + Constants.RESET);

                    while(true){
                        System.out.println("");
                        System.out.print("Current Name: " + currentResponder.getName());
                        System.out.print("\nNew Name: ");
                        String newName = sc.nextLine().trim();

                        if (newName.isEmpty() ||  "null".equalsIgnoreCase(newName)  || !newName.matches("[a-zA-Z\s]+$")) {
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
                        String newZone = sc.nextLine().trim().toLowerCase();
                        
                        if (newZone.isEmpty() || !Pattern.matches(Constants.ZONE_PATTERN, newZone)) {
                            SystemLogger.error("Update cancelled - Invalid zone. It must be North, South, East, or West.");
                            System.out.println("");
                        } else {
                            String oldZone = currentResponder.getZone();
                            double oldX = currentResponder.getX();
                            double oldY = currentResponder.getY();
                            
                            // Remove from old zone in location manager
                            locationManager.removeResponder(currentResponder.getId());
                            
                            // Update zone and coordinates
                            currentResponder.setZone(newZone);
                            double[] coords = CoordinateGenerator.generateZoneBasedCoordinates(newZone);
                            currentResponder.setX(coords[1]);
                            currentResponder.setY(coords[0]);
                            
                            // Add to new zone in location manager
                            locationManager.addResponder(currentResponder);
                            
                            if (responderManager.updateResponderInDatabase(currentResponder.getId(), "zone", oldZone, newZone)) {
                                SystemLogger.success("Zone updated from " + oldZone + " to " + newZone);
                                SystemLogger.info("Coordinates automatically updated for new zone.");
                            } else {
                                // Rollback changes
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
                                
                    while(true) {
                        System.out.println("");
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
                    String allZone = sc.nextLine().trim().toLowerCase();
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
                    SystemLogger.info("Exiting update menu.");
                    validUpdate = false;
                    break;
                default:
                    SystemLogger.error("Invalid option selected. Please try again.");
                    break;
            }
        }
    }

    private static boolean hasActiveAlertFromDB(int userId) {
        try {
            String dburl = "jdbc:mysql://localhost:3306/WomenSafetyDB";
            String dbuser = "root";
            String dbpass = "";

            Connection con = DriverManager.getConnection(dburl, dbuser, dbpass);

            String sql = "SELECT COUNT(*) FROM alert_details WHERE User_id = ? AND Status IN (?, ?)";
            PreparedStatement pst=con.prepareStatement(sql);

            pst.setInt(1, userId);
            pst.setString(2, Constants.STATUS_ASSIGNED);
            pst.setString(3, Constants.STATUS_WAITING);

            ResultSet rs = pst.executeQuery();

            if (rs.next()) {
                return rs.getInt(1) > 0;
            }
        } catch (SQLException e) {
            SystemLogger.error("DB error while checking active alerts: " + e.getMessage());
        }
        return false;
    }
}