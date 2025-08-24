// Main class for the Women's Safety Alert System application
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

class Main {
    private static Scanner sc = new Scanner(System.in); // Scanner for user input
    // Initialize core managers for user, responder, admin, and location management
    private static UserManager userManager = new UserManager();
    private static ResponderManager responderManager = new ResponderManager();
    private static AdminManager adminManager = new AdminManager();
    private static LocationManager locationManager = new LocationManager();
    private static Dispatcher dispatcher = new Dispatcher(locationManager, userManager, responderManager);
    // Start threads for background tasks related to alerts and responder status
    private static AlertLoopThread alertLoopThread = new AlertLoopThread(dispatcher);
    private static Thread responderStatusCheckerThread = new Thread(new ResponderStatusChecker(locationManager));
    private static HashMap<Integer, Alert> alertMap = new HashMap<>(); // Map to track alerts by user ID
    private static User currentUser = null; // Currently logged-in user
    private static Responder currentResponder = null; // Currently logged-in responder

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
        System.out.println(Constants.MAGENTA_ITALIC_BOLD + "            Safety Alert System Menu            " + Constants.RESET);
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

    // Displays the user menu and handles user interactions based on login status
    private static boolean showUserMenu() {
        boolean runUser = true; // Control variable for the user menu loop
        while (runUser) {
            System.out.println("\n" + "=".repeat(50));
            System.out.println(Constants.BLUE + " USER MENU " + Constants.RESET);
            System.out.println("=".repeat(50));
            
            // Show different options based on whether user is logged in
            if (RBACManager.isLoggedIn() && RBACManager.hasRole(Role.USER)) {
                // User is logged in - show post-login options
                Person loggedInPerson = (Person) RBACManager.getCurrentUser();
                System.out.println("Logged in as: " + loggedInPerson.getName());
                System.out.println("3. Raise Alert");
                System.out.println("4. Update Profile");
                System.out.println("5. Logout");
            } else {
                // User is not logged in - show pre-login options
                System.out.println("1. Register User");
                System.out.println("2. Login User");
                System.out.println("0. Exit to Main Menu");
            }
            System.out.println("=".repeat(50));

            // Determine maximum valid choice based on login status
            int maxChoice = RBACManager.isLoggedIn() && RBACManager.hasRole(Role.USER) ? 5 : 2;
            int choice = getValidChoice(0, maxChoice); // Get validated user input

            // Handle menu selection based on user's choice
            switch (choice) {
                case 1: // User registration
                    if (!(RBACManager.isLoggedIn() && RBACManager.hasRole(Role.USER))) {
                        handleUserRegistration(); // Call user registration handler
                    }
                    break;

                case 2: // User login
                    if (!(RBACManager.isLoggedIn() && RBACManager.hasRole(Role.USER))) {
                        handleUserLogin(); // Call user login handler
                    }
                    break;

                case 3: // Raise alert
                    if (RBACManager.isLoggedIn() && RBACManager.hasRole(Role.USER)) {
                        try {
                            RBACManager.checkPermission(Permission.RAISE_ALERT); // Check permission to raise alert
                            handleRaiseAlert(); // Call alert creation handler
                        } catch (SecurityException e) {
                            SystemLogger.error("Access denied: " + e.getMessage()); // Log permission denial
                        }
                    }
                    break;

                case 4: // Update profile
                    if (RBACManager.isLoggedIn() && RBACManager.hasRole(Role.USER)) {
                        try {
                            RBACManager.checkPermission(Permission.UPDATE_USER_DETAILS); // Check permission to update profile
                            handleUpdateUserProfile(); // Call profile update handler
                        } catch (SecurityException e) {
                            SystemLogger.error("Access denied: " + e.getMessage()); // Log permission denial
                        }
                    }
                    break;

                case 5: // Logout
                    if (RBACManager.isLoggedIn() && RBACManager.hasRole(Role.USER)) {
                        RBACManager.logout(); // Terminate current user session
                        SystemLogger.success("User logged out successfully"); // Log successful logout
                    }
                    break;

                case 0: // Exit to main menu
                    runUser = false; // Break out of user menu loop
                    break;
            }
        }
        return true; // Return control to calling method
    }

    // Displays the responder menu and handles responder interactions based on login status
    private static boolean showResponderMenu() {
        boolean runResponder = true; // Control variable for the responder menu loop
        while (runResponder) {
            System.out.println("\n" + "=".repeat(50));
            System.out.println(Constants.BLUE + " RESPONDER MENU " + Constants.RESET);
            System.out.println("=".repeat(50));
            
            // Show different options based on whether responder is logged in
            if (RBACManager.isLoggedIn() && RBACManager.hasRole(Role.RESPONDER)) {
                // Responder is logged in - show post-login options
                System.out.println("Logged in as: " + RBACManager.getCurrentUser().getName());
                System.out.println("3. Process Next Alert");
                System.out.println("4. Complete Alert");
                System.out.println("5. Show Pending Alerts");
                System.out.println("6. Update Profile");
                System.out.println("7. Logout");
            } else {
                // Responder is not logged in - show pre-login options
                System.out.println("1. Register Responder");
                System.out.println("2. Login Responder");
                System.out.println("0. Exit to Main Menu");
            }
            System.out.println("=".repeat(50));

            // Determine maximum valid choice based on login status
            int maxChoice = RBACManager.isLoggedIn() && RBACManager.hasRole(Role.RESPONDER) ? 7 : 2;
            int choice = getValidChoice(0, maxChoice); // Get validated responder input

            // Handle menu selection based on responder's choice
            switch (choice) {
                case 1: // Responder registration
                    if (!(RBACManager.isLoggedIn() && RBACManager.hasRole(Role.RESPONDER))) {
                        handleResponderRegistration(); // Call responder registration handler
                    }
                    break;

                case 2: // Responder login
                    if (!(RBACManager.isLoggedIn() && RBACManager.hasRole(Role.RESPONDER))) {
                        handleResponderLogin(); // Call responder login handler
                    }
                    break;

                case 3: // Process next alert
                    if (RBACManager.isLoggedIn() && RBACManager.hasRole(Role.RESPONDER)) {
                        try {
                            RBACManager.checkPermission(Permission.PROCESS_NEXT_ALERT); // Check permission to process alerts
                            dispatcher.processNextAlert(); // Call alert processing handler
                        } catch (SecurityException e) {
                            SystemLogger.error("Access denied: " + e.getMessage()); // Log permission denial
                        }
                    }
                    break;

                case 4: // Complete alert
                    if (RBACManager.isLoggedIn() && RBACManager.hasRole(Role.RESPONDER)) {
                        try {
                            RBACManager.checkPermission(Permission.COMPLETE_ALERT); // Check permission to complete alerts
                            handleCompleteAlert(); // Call alert completion handler
                        } catch (SecurityException e) {
                            SystemLogger.error("Access denied: " + e.getMessage()); // Log permission denial
                        }
                    }
                    break;

                case 5: // Show pending alerts
                    if (RBACManager.isLoggedIn() && RBACManager.hasRole(Role.RESPONDER)) {
                        try {
                            RBACManager.checkPermission(Permission.SHOW_PENDING_ALERTS); // Check permission to view alerts
                            dispatcher.showPendingAlerts(); // Call pending alerts display handler
                        } catch (SecurityException e) {
                            SystemLogger.error("Access denied: " + e.getMessage()); // Log permission denial
                        }
                    }
                    break;

                case 6: // Update profile
                    if (RBACManager.isLoggedIn() && RBACManager.hasRole(Role.RESPONDER)) {
                        try {
                            RBACManager.checkPermission(Permission.UPDATE_RESPONDER_DETAILS); // Check permission to update profile
                            handleUpdateResponderProfile(); // Call profile update handler
                        } catch (SecurityException e) {
                            SystemLogger.error("Access denied: " + e.getMessage()); // Log permission denial
                        }
                    }
                    break;

                case 7: // Logout
                    if (RBACManager.isLoggedIn() && RBACManager.hasRole(Role.RESPONDER)) {
                        RBACManager.logout(); // Terminate current responder session
                        SystemLogger.success("Responder logged out successfully"); // Log successful logout
                    }
                    break;

                case 0: // Exit to main menu
                    runResponder = false; // Break out of responder menu loop
                    break;
            }
        }
        return true; // Return control to calling method
    }

    // Displays the admin menu and handles admin interactions based on login status
    private static boolean showAdminMenu(AdminManager adminManager) {
        boolean runAdmin = true; // Control variable for the admin menu loop
        while (runAdmin) {
            System.out.println("\n" + "=".repeat(50)); // Menu header formatting
            System.out.println(Constants.BLUE + " ADMIN MENU " + Constants.RESET);
            System.out.println("=".repeat(50));
            
            // Show different options based on whether admin is logged in
            if (RBACManager.isLoggedIn() && RBACManager.hasRole(Role.ADMIN)) {
                // Admin is logged in - show post-login options
                System.out.println("Logged in as: " + RBACManager.getCurrentUser().getName());
                System.out.println("3. View All Users");
                System.out.println("4. View All Responders");
                System.out.println("5. View Pending Alerts");
                System.out.println("6. System Statistics");
                System.out.println("7. Logout");
            } else {
                // Admin is not logged in - show pre-login options
                System.out.println("1. Create Admin Account");
                System.out.println("2. Login Admin");
                System.out.println("0. Exit to Main Menu");
            }
            System.out.println("=".repeat(50));

            // Determine maximum valid choice based on login status
            int maxChoice = RBACManager.isLoggedIn() && RBACManager.hasRole(Role.ADMIN) ? 7 : 2;
            int choice = getValidChoice(0, maxChoice); // Get validated admin input

            // Handle menu selection based on admin's choice
            switch (choice) {
                case 1: // Admin account creation
                    handleAdminCreation(adminManager); // Call admin registration handler
                    break;

                case 2: // Admin login
                    handleAdminLogin(adminManager); // Call admin authentication handler
                    break;

                case 3: // View all users
                    try {
                        RBACManager.checkPermission(Permission.DISPLAY_ALL_USERS); // Check permission to view users
                        userManager.displayAllUsers(); // Call user display handler
                    } catch (SecurityException e) {
                        SystemLogger.error("Access denied: " + e.getMessage()); // Log permission denial
                    }
                    break;

                case 4: // View all responders
                    try {
                        RBACManager.checkPermission(Permission.DISPLAY_ALL_RESPONDERS); // Check permission to view responders
                        responderManager.displayAllResponders(); // Call responder display handler
                    } catch (SecurityException e) {
                        SystemLogger.error("Access denied: " + e.getMessage()); // Log permission denial
                    }
                    break;

                case 5: // View pending alerts
                    try {
                        RBACManager.checkPermission(Permission.VIEW_PENDING_ALERTS); // Check permission to view alerts
                        dispatcher.showPendingAlerts(); // Call pending alerts display handler
                    } catch (SecurityException e) {
                        SystemLogger.error("Access denied: " + e.getMessage()); // Log permission denial
                    }
                    break;

                case 6: // View system statistics
                    try {
                        RBACManager.checkPermission(Permission.VIEW_SYSTEM_STATISTICS); // Check permission to view statistics
                        adminManager.displaySystemStatistics(); // Call system statistics handler
                    } catch (SecurityException e) {
                        SystemLogger.error("Access denied: " + e.getMessage()); // Log permission denial
                    }
                    break;

                case 7: // Admin logout
                    if (RBACManager.isLoggedIn() && RBACManager.hasRole(Role.ADMIN)) {
                        RBACManager.logout(); // Terminate current admin session
                        SystemLogger.success("Admin logged out successfully"); // Log successful logout
                    }
                    break;

                case 0: // Exit to main menu
                    runAdmin = false; // Break out of admin menu loop
                    break;
            }
        }
        return true; // Return control to calling method
    }

    // Validates and retrieves user input within a specified range
    private static int getValidChoice(int min, int max) {
        int choice = -1;
        boolean validInput = false;
        
        while (!validInput) {
            System.out.print("Enter your choice (" + min + "-" + max + "): ");
            try {
                // Check if next input is an integer
                if (sc.hasNextInt()) {
                    choice = sc.nextInt();
                    sc.nextLine();
                    
                    // Validate if choice is within the specified range
                    if (choice >= min && choice <= max) {
                        validInput = true; // Mark input as valid
                    } else {
                        // Log error for out-of-range input
                        SystemLogger.error("Invalid choice! Please enter a number between " + min + " and " + max + ".");
                        System.out.println("");
                    }
                } else {
                    // Log error for non-integer input
                    SystemLogger.error("Invalid input! Please enter a valid number.");
                    sc.nextLine();
                    System.out.println("");
                }
            } catch (Exception e) {
                System.out.println("Error reading input. Please try again.");
                sc.nextLine();
            }
        }
        return choice; // Return the validated choice
    }

    // Handles the complete user registration process with comprehensive input validation
    private static void handleUserRegistration() {
        String userName, userPhone, userEmail, userLocation, userZone, userPassword;
        System.out.println(Constants.CYAN+ "\n--- Register User ---" +Constants.RESET);

        // Validate and collect user name with alphabetic characters and spaces only
        while (true) {
            System.out.print("Name: ");
            userName = sc.nextLine().trim();

            // Check for empty input, "null" values, and non-alphabetic characters
            if(userName.isEmpty() || "null".equalsIgnoreCase(userName) || !userName.matches("[a-zA-Z ]+")) {
                SystemLogger.error("Invalid name.");
                System.out.println("");
            } else {
                break; // Exit loop when valid name is provided
            }
        }

        // Validate and collect phone number with format and duplicate checks
        while (true) {
            System.out.print("Phone (10-digit): +91 ");
            userPhone = sc.nextLine().trim();

            // Check format first - validate against phone pattern regex
            if (!userPhone.matches(Constants.PHONE_PATTERN)) {
                SystemLogger.error("Invalid phone number.");
                System.out.println("");
                continue; // Continue loop for re-entry
            }

            // Check duplicate in DB - ensure phone number is not already registered
            if (userManager.isPhoneExists(userPhone)) {
                SystemLogger.error("This phone number is already registered. Please use another one.");
                System.out.println("");
                continue; // Continue loop for re-entry
            }

            break; // Exit loop when valid and unique phone is provided
        }

        // Validate and collect email address with specific domain requirement
        while (true) {
            System.out.print("Email (must end with @gmail.com): ");
            userEmail=sc.nextLine().trim();

            // Validate email format against predefined pattern
            if(Pattern.matches(Constants.EMAIL_PATTERN, userEmail)) {
                break; // Exit loop when valid email is provided
            } else {
                SystemLogger.error("Invalid email format.");
                System.out.println("");
            }
        }

        // Validate and collect user location/landmark information
        while (true) {
            System.out.print("Location (Landmark): ");
            userLocation = sc.nextLine();

            // Check for empty input or "null" values
            if(userLocation.trim().isEmpty() || "null".equalsIgnoreCase(userLocation)) {
                SystemLogger.error("Invalid location.");
                System.out.println("");
            } else {
                break; // Exit loop when valid location is provided
            }
        }

        // Validate and collect zone information with specific allowed values
        while (true) {
            System.out.print("Zone (North/South/East/West): ");
            userZone=sc.nextLine().trim().toLowerCase();

            // Validate zone against predefined pattern (north/south/east/west)
            if(Pattern.matches(Constants.ZONE_PATTERN, userZone)) {
                break; // Exit loop when valid zone is provided
            } else {
                SystemLogger.error("Invalid zone. It must be North, South, East, or West.");
                System.out.println("");
            }
        }

        // Validate and collect password with minimum length requirement
        while (true) {
            System.out.print("Password (min 6 chars): ");
            userPassword = sc.nextLine().trim();
            
            // Validate password meets minimum security requirements
            if (AuthenticationHelper.isValidPassword(userPassword)) {
                break; // Exit loop when valid password is provided
            } else {
                SystemLogger.error("Password must be at least 6 characters.");
                System.out.println("");
            }
        }

        // Attempt to register user with all validated data
        try{
            // Call user manager to create new user record in database
            User newUser = userManager.registerUser(userName, userPhone, userEmail, userLocation, userZone, userPassword);
            // Display success message with generated user ID
            SystemLogger.success("User registered successfully. Your ID is: "+newUser.getId());
        } catch (IllegalArgumentException e) {
            // Handle registration failures and display error message
            SystemLogger.error("Registration failed: "+e.getMessage());
            System.out.println("");
        }
    }

    // Handle user login - authenticates existing users and establishes session
    private static void handleUserLogin() {
        System.out.println(Constants.CYAN+ "\n--- User Login ---" +Constants.RESET);
        int userId;
        String userPasswordLogin;

        // Validate and collect user ID input with numeric validation
        while (true) {
            System.out.print("Enter your User ID: ");
            String input = sc.nextLine().trim();

            try {
                userId = Integer.parseInt(input);
                break;
            } catch (NumberFormatException e) {
                // Handle non-numeric input with error message
                SystemLogger.error("Please enter a valid number.");
                System.out.println("");
            }
        }

        // Collect password input without echo (security consideration)
        System.out.print("Enter your password: ");
        userPasswordLogin = sc.nextLine().trim();

        // Authenticate user credentials against database using UserManager
        User loginUser = userManager.authenticateUserLogin(userId, userPasswordLogin);
        
        System.out.println("\n" + "─".repeat(85));
        
        // Handle authentication result
        if (loginUser != null) {
            currentUser = loginUser; // Set current user session
            // Display personalized welcome message with user's name
            SystemLogger.success("Login successful. Welcome "+loginUser.getName()+"!");
        } else {
            // Display authentication failure message
            SystemLogger.error("Invalid credentials. Please check your ID and password.");
        }
        System.out.println("─".repeat(85)); 
    }

    // Handles the complete responder registration process with comprehensive input validation
    private static void handleResponderRegistration() {
        String respName, respPhone, respEmail, respZone, respPassword;
        boolean available;

        System.out.println(Constants.CYAN+ "\n--- Register Responder ---" +Constants.RESET);

        // Validate and collect responder name with alphabetic characters and spaces only
        while (true) {
            System.out.print("Name: ");
            respName = sc.nextLine().trim();
            // Check for empty input, "null" values, and non-alphabetic characters
            if(respName.isEmpty() ||  "null".equalsIgnoreCase(respName) || !respName.matches("[a-zA-Z ]+")) {
                SystemLogger.error("Invalid name. Only letters and spaces allowed.");
                System.out.println("");
            } else {
                break; // Exit loop when valid name is provided
            }
        }

        // Validate and collect phone number with format validation and duplicate checking
        while (true) {
            System.out.print("Phone (10-digit): +91 ");
            respPhone = sc.nextLine().trim();

            // Validate phone format first - ensure it matches the required pattern
            if (!Pattern.matches(Constants.PHONE_PATTERN, respPhone)) {
                SystemLogger.error("Invalid phone number.");
                System.out.println("");
                continue; // Continue loop for re-entry
            }

            // Check for duplicate immediately - ensure phone number is not already registered
            if (responderManager.isPhoneExists(respPhone)) {
                SystemLogger.error("Phone number already exists. Please enter a new number.");
                System.out.println("");
                continue; // Continue loop for re-entry
            }

            break; // Exit loop when valid and unique phone is provided
        }

        // Validate and collect email address with specific domain requirement
        while (true) {
            System.out.print("Email (must end with @gmail.com): ");
            respEmail=sc.nextLine().trim();
            // Validate email format against predefined pattern
            if(Pattern.matches(Constants.EMAIL_PATTERN, respEmail)) {
                break; // Exit loop when valid email is provided
            } else {
                SystemLogger.error("Invalid email format.");
                System.out.println("");
            }
        }

        // Validate and collect zone information with specific allowed values
        while (true) {
            System.out.print("Zone (North/South/East/West): ");
            respZone=sc.nextLine().trim().toLowerCase();
            // Validate zone against predefined pattern (north/south/east/west)
            if(Pattern.matches(Constants.ZONE_PATTERN, respZone)) {
                break; // Exit loop when valid zone is provided
            } else {
                SystemLogger.error("Invalid zone. It must be North, South, East, or West.");
                System.out.println("");
            }
        }

        // Validate and collect availability status with boolean input validation
        while (true) {
            System.out.print("Available (true/false): ");
            String availabilityInput = sc.nextLine().toLowerCase();
            // Validate input is either "true" or "false" (case-insensitive)
            if ("true".equalsIgnoreCase(availabilityInput) || "false".equalsIgnoreCase(availabilityInput)) {
                available = Boolean.parseBoolean(availabilityInput);
                break; // Exit loop when valid availability is provided
            } else {
                SystemLogger.error("Invalid input. Please enter true or false.");
                System.out.println("");
            }
        }

        // Validate and collect password with minimum length requirement
        while (true) {
            System.out.print("Password (min 6 chars): ");
            respPassword = sc.nextLine().trim();
            // Validate password meets minimum security requirements
            if (AuthenticationHelper.isValidPassword(respPassword)) {
                break; // Exit loop when valid password is provided
            } else {
                SystemLogger.error("Password must be at least 6 characters.");
                System.out.println("");
            }
        }

        // Attempt to register responder with all validated data
        try {
            // Call responder manager to create new responder record in database
            Responder responder = responderManager.registerResponder(respName, respPhone, respEmail, respZone, available, respPassword);
            // Add responder to location manager for zone-based tracking
            locationManager.addResponder(responder);
            // Display success message with generated responder ID
            SystemLogger.success("Responder registered successfully. Your ID is: "+responder.getId());
        } catch (IllegalArgumentException e) {
            // Handle registration failures and display error message
            SystemLogger.error("Registration failed: "+e.getMessage());
            System.out.println("");
        }
    }

    // Handle responder login
    private static void handleResponderLogin() {
        System.out.println(Constants.CYAN + "\n--- Responder Login ---" + Constants.RESET);
        int responderId;
        String responderPasswordLogin;

        // Validate and collect responder ID input with numeric validation
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

        // Collect password input without echo (security consideration)
        System.out.print("Enter your password: ");
        responderPasswordLogin = sc.nextLine();

        // Authenticate responder credentials against database using ResponderManager
        Responder loginResponder = responderManager.authenticateResponderLogin(responderId, responderPasswordLogin);
        System.out.println("\n" + "─".repeat(85));
        
        // Handle authentication result
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
        
        // Validate and collect admin name with alphabetic characters and spaces only
        while (true) {
            System.out.print("Name: ");
            adminName = sc.nextLine().trim();
            if(adminName.isEmpty() || "null".equalsIgnoreCase(adminName) || !adminName.matches("[a-zA-Z ]+")) {
                SystemLogger.error("Invalid name. Only letters and spaces are allowed.");
                System.out.println("");
            } else {
                break; // Exit loop when valid name is provided
            }
        }

        // Validate and collect phone number with format validation and duplicate checking
        while (true) {
            System.out.print("Phone (10-digit): +91 ");
            adminPhone = sc.nextLine().trim();

            // Validate phone format first - ensure it matches the required pattern
            if (!adminPhone.matches(Constants.PHONE_PATTERN)) {
                SystemLogger.error("Invalid phone number. Must be 10 digits starting with 7, 8, or 9.");
                System.out.println("");
                continue; // Continue loop for re-entry
            }

            // Check for duplicate immediately - ensure phone number is not already registered
            if (adminManager.isPhoneExists(adminPhone)) {
                SystemLogger.error("This phone number is already registered for another Admin. Please use a different number.");
                System.out.println("");
                continue; // Continue loop for re-entry
            }

            break; // Exit loop when valid and unique phone is provided
        }

        // Validate and collect email address with specific domain requirement
        while (true) {
            System.out.print("Email (must end with @gmail.com): ");
            adminEmail = sc.nextLine().trim();
            if(Pattern.matches(Constants.EMAIL_PATTERN, adminEmail)) {
                break; // Exit loop when valid email is provided
            } else {
                SystemLogger.error("Invalid email format. Email must end with @gmail.com.");
                System.out.println("");
            }
        }
        
        // Validate and collect password with minimum length requirement
        while (true) {
            System.out.print("Password (min 6 chars): ");
            adminPassword = sc.nextLine().trim();
            if (AuthenticationHelper.isValidPassword(adminPassword)) {
                break; // Exit loop when valid password is provided
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

        // Validate and collect admin ID input with numeric validation
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

        // Collect password input
        System.out.print("Enter your password: ");
        adminPasswordLogin = sc.nextLine().trim();

        // Authenticate admin credentials against database using AdminManager
        Admin loginAdmin = adminManager.authenticateAdminLogin(adminId, adminPasswordLogin);
        System.out.println("\n" + "─".repeat(85));
        
        // Handle authentication result
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

    // Handle raising an alert by the user
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

    // Handle completing an alert
    private static void handleCompleteAlert() {
        try {
            RBACManager.checkPermission(Permission.COMPLETE_ALERT); // Verify user has permission to complete alerts
            User currentUser = (User) RBACManager.getCurrentUser(); // Get the currently logged-in user
            Alert userAlert = getUserActiveAlert(currentUser.getId()); // Retrieve user's active alert from database
            if (userAlert != null) {
                dispatcher.completeAlert(userAlert); // Notify dispatcher to mark alert as completed
                alertMap.remove(currentUser.getId()); // Remove alert from local tracking map
            } else {
                SystemLogger.info("No active alert found for your account."); // Log info if no active alert exists
            }
        } catch (Exception e) {
            SystemLogger.info("No Pending alert found for your account."); // Handle exception when no pending alerts
        }
    }

    // Handle updating user profile - allows users to modify their personal information
    private static void handleUpdateUserProfile() {
        Person currentPerson = RBACManager.getCurrentUser(); // Get currently logged-in user from RBAC system
        if (currentPerson == null || !(currentPerson instanceof User)) { // Validate user is logged in and is a User type
            SystemLogger.error("No user is currently logged in."); // Log error if no user is authenticated
            return; // Exit method if validation fails
        }
        
        User currentUser = (User) currentPerson; // Cast to User type for user-specific operations
        
        System.out.println(Constants.CYAN + "\n--- Update User Details ---" + Constants.RESET); // Display update header
        System.out.println("Current User Information:"); // Show current user info section
        System.out.println("─".repeat(50)); // Visual separator
        System.out.println("Name: " + currentUser.getName()); // Display current name
        System.out.println("Phone: +91 " + currentUser.getPhone()); // Display current phone
        System.out.println("Email: " + currentUser.getEmail()); // Display current email
        System.out.println("Location: " + currentUser.getLocation()); // Display current location
        System.out.println("Zone: " + currentUser.getZone()); // Display current zone
        System.out.println("─".repeat(50)); // Visual separator
        System.out.println(); // Empty line for spacing
        
        boolean validUpdate = true; // Control variable for update menu loop
        while(validUpdate){ // Main update menu loop
            System.out.println("\nSelect the field you want to update:"); // Display update options
            System.out.println("1. Name"); // Option to update name
            System.out.println("2. Phone"); // Option to update phone
            System.out.println("3. Email"); // Option to update email
            System.out.println("4. Location"); // Option to update location
            System.out.println("5. Zone"); // Option to update zone
            System.out.println("6. Password"); // Option to update password
            System.out.println("7. Update All Details (Except Password)"); // Option for bulk update
            System.out.println("0. Exit"); // Option to exit update menu
            System.out.print("Enter your choice: "); // Prompt for user input
            int updateUser = getValidChoice(0, 7); // Get validated user choice within range 0-7
            
            switch (updateUser) { // Process user's menu selection
                case 1: // Name update section - handles user name modification
                    System.out.println(Constants.CYAN + "\n--- Update User Name ---" + Constants.RESET); // Name update header
                    System.out.println(""); // Empty line for spacing
                    while(true){ // Name validation loop
                        System.out.print("Current Name: " + currentUser.getName()); // Show current name
                        System.out.print("\nNew Name: ");
                        String newName = sc.nextLine().trim();

                        if (newName.isEmpty() || "null".equalsIgnoreCase(newName) || !newName.matches("[a-zA-Z\\s]+$")) { // Validate name format
                            SystemLogger.error("Update cancelled - Invalid name. Only letters and spaces are allowed."); // Log validation error
                            System.out.println("");
                        } else {
                            String oldName = currentUser.getName(); // Store old name for potential rollback
                            currentUser.setName(newName); // Update local user object

                            if (userManager.updateUserInDatabase(currentUser.getId(), "name", oldName, newName)) { // Attempt database update
                                SystemLogger.success("Name updated successfully to: " + newName); // Log success
                            } else {
                                currentUser.setName(oldName); // Rollback local changes on database failure
                                SystemLogger.error("Failed to update database. Changes reverted."); // Log database error
                            }
                            break; // Exit name validation loop
                        }
                    }
                    break;
                    
                // Phone update section - handles user phone number modification
                case 2:
                    System.out.println(Constants.CYAN + "\n--- Update User Phone ---" + Constants.RESET); // Phone update header
                    System.out.println("");
                    while(true){ // Phone validation loop
                        System.out.print("Current Phone: +91 " + currentUser.getPhone()); // Show current phone
                        System.out.print("\nNew Phone (10-digit): +91 ");
                        String newPhone = sc.nextLine().trim();

                        if (newPhone.isEmpty() || !Pattern.matches(Constants.PHONE_PATTERN, newPhone)) { // Validate phone format
                            SystemLogger.error("Update cancelled - Invalid phone number."); // Log validation error
                            System.out.println("");
                        } else {
                            String oldPhone = currentUser.getPhone(); // Store old phone for potential rollback
                            currentUser.setPhone(newPhone); // Update local user object
                                        
                            if (userManager.updateUserInDatabase(currentUser.getId(), "phone", oldPhone, newPhone)) { // Attempt database update
                                SystemLogger.success("Phone updated successfully to: +91 " + newPhone); // Log success
                            } else {
                                currentUser.setPhone(oldPhone); // Rollback local changes on database failure
                                SystemLogger.error("Failed to update database. Changes reverted."); // Log database error
                            }
                            break; // Exit phone validation loop
                        }
                    }
                    break;
                    
                case 3: // Email update section - handles user email modification
                    System.out.println(Constants.CYAN + "\n--- Update User Email ---" + Constants.RESET); // Email update header
                    System.out.println("");
                    while(true){ // Email validation loop
                        System.out.print("Current Email: " + currentUser.getEmail()); // Show current email
                        System.out.print("\nNew Email (must end with @gmail.com): ");
                        String newEmail = sc.nextLine().trim();

                        if (newEmail.isEmpty() || !Pattern.matches(Constants.EMAIL_PATTERN, newEmail)) { // Validate email format
                            SystemLogger.error("Update cancelled - Invalid email format. Email must end with @gmail.com."); // Log validation error
                            System.out.println("");
                            String oldEmail = currentUser.getEmail(); // Store old email for potential rollback
                            currentUser.setEmail(newEmail); // Update local user object
                            if (userManager.updateUserInDatabase(currentUser.getId(), "email", oldEmail, newEmail)) { // Attempt database update
                                SystemLogger.success("Email updated successfully to: " + newEmail); // Log success
                            } else {
                                currentUser.setEmail(oldEmail); // Rollback local changes on database failure
                                SystemLogger.error("Failed to update database. Changes reverted."); // Log database error
                            }
                            break; // Exit email validation loop
                        }
                    }
                    break;
                    
                case 4:  // Location update section - handles user location modification
                    System.out.println(Constants.CYAN + "\n--- Update User Location ---" + Constants.RESET); // Location update header
                    System.out.println("");
                    while(true){ // Location validation loop
                        System.out.print("Current Location: " + currentUser.getLocation()); // Show current location
                        System.out.print("\nNew Location (Landmark): ");
                        String newLocation = sc.nextLine().trim();
                        if (newLocation.isEmpty() || "null".equalsIgnoreCase(newLocation)) { // Validate location input
                            SystemLogger.error("Update cancelled - no input provided."); // Log validation error
                            System.out.println("");
                        } else {
                            String oldLocation = currentUser.getLocation(); // Store old location for potential rollback
                            currentUser.setLocation(newLocation); // Update local user object
                            if (userManager.updateUserInDatabase(currentUser.getId(), "location", oldLocation, newLocation)) { // Attempt database update
                                SystemLogger.success("Location updated successfully to: " + newLocation); // Log success
                            } else {
                                currentUser.setLocation(oldLocation); // Rollback local changes on database failure
                                SystemLogger.error("Failed to update database. Changes reverted."); // Log database error
                            }
                            break; // Exit location validation loop
                        }
                    }
                    break;
                    
                case 5: // Zone update section - handles user zone modification with coordinate regeneration
                    System.out.println(Constants.CYAN + "\n--- Update User Zone ---" + Constants.RESET); // Zone update header
                    System.out.println(""); 
                    while(true){ // Zone validation loop
                        System.out.print("Current Zone: " + currentUser.getZone()); // Show current zone
                        System.out.print("\nNew Zone (North/South/East/West): ");
                        String newZone = sc.nextLine().trim().toLowerCase();

                        if (newZone.isEmpty() || !Pattern.matches(Constants.ZONE_PATTERN, newZone)) { // Validate zone format
                            SystemLogger.error("Update cancelled - Invalid zone. It must be North, South, East, or West."); // Log validation error
                            System.out.println("");
                        } else {
                            String oldZone = currentUser.getZone();
                            double oldX = currentUser.getX();
                            double oldY = currentUser.getY();
                            
                            currentUser.setZone(newZone); // Update local user object with new zone
                            double[] coords = CoordinateGenerator.generateZoneBasedCoordinates(newZone); // Generate new coordinates for the zone
                            currentUser.setX(coords[1]);
                            currentUser.setY(coords[0]);
                            
                            if (userManager.updateUserInDatabase(currentUser.getId(), "zone", oldZone, newZone)) { // Attempt database update
                                SystemLogger.success("Zone updated from " + oldZone + " to " + newZone); // Log success
                                SystemLogger.info("Coordinates automatically updated for new zone."); // Inform about coordinate update
                            } else {
                                currentUser.setZone(oldZone);
                                currentUser.setX(oldX);
                                currentUser.setY(oldY);
                                SystemLogger.error("Failed to update database. Changes reverted."); // Log database error
                            }
                            break; // Exit zone validation loop
                        }
                    }
                    break;

                case 6: // Password update section - handles secure password change with multiple validations
                    System.out.println(Constants.CYAN + "\n--- Update User Password ---" + Constants.RESET);
                    System.out.println("");
                    
                    while(true) { // Password validation loop
                        System.out.print("Enter current password: ");
                        String oldPassword = sc.nextLine().trim();
                        
                        if (oldPassword.isEmpty()) { // Validate current password is not empty
                            SystemLogger.error("Current password cannot be empty."); // Log validation error
                            System.out.println("");
                            continue;
                        }
                        
                        System.out.print("Enter new password (min 6 chars): "); 
                        String newPassword = sc.nextLine().trim();
                        
                        if (!AuthenticationHelper.isValidPassword(newPassword)) { // Validate new password meets requirements
                            SystemLogger.error("New password must be at least 6 characters long."); // Log validation error
                            System.out.println("");
                            continue;
                        }
                        
                        System.out.print("Confirm new password: ");
                        String confirmPassword = sc.nextLine().trim();
                        
                        if (!newPassword.equals(confirmPassword)) { // Validate passwords match
                            SystemLogger.error("Passwords do not match. Please try again."); // Log validation error
                            System.out.println("");
                            continue;
                        }
                        
                        System.out.print("Are you sure you want to update your password? (y/n): "); // Final confirmation
                        String confirmation = sc.nextLine().trim().toLowerCase();
                        
                        if (!confirmation.equals("y") && !confirmation.equals("yes")) { // Validate confirmation
                            SystemLogger.info("Password update cancelled."); // Log cancellation
                            break;
                        }
                        
                        if (userManager.updateUserPassword(currentUser.getId(), oldPassword, newPassword)) { // Attempt password update
                            SystemLogger.success("Password updated successfully!"); // Log success
                        } else {
                            SystemLogger.error("Failed to update password. Please check your current password."); // Log failure
                        }
                        break;
                    }
                    break;

                case 7: // Bulk update all user details in a single operation (except password)
                    System.out.println("");
                    System.out.println(Constants.INFO + "Updating all details:" + Constants.RESET);
                    System.out.println("Press Enter to keep current value, or enter new value to update."); 

                     // Store original for potential rollback
                    String oldName = currentUser.getName();
                    double oldX = currentUser.getX();
                    double oldY = currentUser.getY();
                    
                    boolean updateSuccess = true; // Track overall success of all database operations
                    
                    // Name update section - validates and updates user name if provided
                    System.out.print("Name [" + currentUser.getName() + "]: ");
                    String allName = sc.nextLine().trim();
                    if (!allName.isEmpty() && allName.matches("[a-zA-Z ]+")) { 
                        currentUser.setName(allName);
                        if (!userManager.updateUserInDatabase(currentUser.getId(), "name", oldName, allName)) { 
                            updateSuccess = false; 
                            currentUser.setName(oldName);
                        }
                    }
                    
                    // Phone update section - validates and updates phone number if provided
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
                    
                    // Email update section - validates and updates email if provided
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
                    
                    // Location update section - validates and updates location if provided
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
                    
                    // Zone update section - most complex due to coordinate management
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
                    
                    // Display appropriate success/error message based on overall update status
                    if (updateSuccess) {
                        SystemLogger.success("All details updated successfully!"); // Success message for all updates
                    } else {
                        SystemLogger.error("Some updates failed. Please check the logs."); // Error message for partial failures
                    }
                    
                    // Display updated user information for confirmation
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

    // Handles the responder profile update process with comprehensive field validation and database persistence
    private static void handleUpdateResponderProfile() {
        // Verify that a responder is currently logged in before proceeding with updates
        Person currentPerson = RBACManager.getCurrentUser();
        if (currentPerson == null || !(currentPerson instanceof Responder)) {
            SystemLogger.error("No responder is currently logged in.");
            return;
        }
        
        // Cast the current person to Responder type for profile operations
        Responder currentResponder = (Responder) currentPerson;
        
        // Display current responder information for reference before making changes
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
        
        // Main update loop - continues until user chooses to exit
        boolean validUpdate = true;
        while(validUpdate){
            // Display menu options for different responder fields that can be updated
            System.out.println("\nSelect the field you want to update:");
            System.out.println("1. Name");
            System.out.println("2. Phone");
            System.out.println("3. Email");
            System.out.println("4. Zone");
            System.out.println("5. Availability");
            System.out.println("6. Password");
            System.out.println("7. Update All Details (Except Password)");
            System.out.println("0. Exit");
            
            // Get validated user choice within the allowed range (0-7)
            int updateResponder = getValidChoice(0, 7);
            
            // Process the selected update option
            switch (updateResponder) {
                case 1: // Name update section
                    System.out.println(Constants.CYAN + "\n--- Update Responder Name ---" + Constants.RESET);

                    // Name validation loop - ensures only valid alphabetic names are accepted
                    while(true){
                        System.out.println("");
                        System.out.print("Current Name: " + currentResponder.getName());
                        System.out.print("\nNew Name: ");
                        String newName = sc.nextLine().trim();

                        // Validate name: non-empty, not "null", and contains only letters/spaces
                        if (newName.isEmpty() ||  "null".equalsIgnoreCase(newName)  || !newName.matches("[a-zA-Z\\s]+$")) {
                            SystemLogger.error("Update cancelled - Invalid name. Only letters and spaces are allowed.");
                            System.out.println("");
                        } else {
                            // Store old value for potential rollback if database update fails
                            String oldName = currentResponder.getName();
                            currentResponder.setName(newName);

                            // Attempt database update and handle success/failure
                            if (responderManager.updateResponderInDatabase(currentResponder.getId(), "name", oldName, newName)) {
                                SystemLogger.success("Name updated successfully to: " + newName);
                            } else {
                                // Rollback local changes if database update fails
                                currentResponder.setName(oldName);
                                SystemLogger.error("Failed to update database. Changes reverted.");
                            }
                            break; // Exit name validation loop
                        }
                    }
                    break;

                case 2: // Phone number update section
                    System.out.println(Constants.CYAN + "\n--- Update Responder Phone ---" + Constants.RESET);

                    // Phone validation loop - ensures valid 10-digit Indian phone format
                    while(true){
                        System.out.println("");
                        System.out.print("Current Phone: +91 " + currentResponder.getPhone());
                        System.out.print("\nNew Phone (10-digit): +91 ");
                        String newPhone = sc.nextLine().trim();
                        
                        // Validate phone format using predefined pattern
                        if (newPhone.isEmpty() || !Pattern.matches(Constants.PHONE_PATTERN, newPhone)) {
                            SystemLogger.error("Update cancelled - Invalid phone number.");
                            System.out.println("");
                        } else {
                            // Store old value for potential rollback
                            String oldPhone = currentResponder.getPhone();
                            currentResponder.setPhone(newPhone);
                                        
                            // Update database and handle result
                            if (responderManager.updateResponderInDatabase(currentResponder.getId(), "phone", oldPhone, newPhone)) {
                                SystemLogger.success("Phone updated successfully to: +91 " + newPhone);
                            } else {
                                // Rollback on database failure
                                currentResponder.setPhone(oldPhone);
                                SystemLogger.error("Failed to update database. Changes reverted.");
                            }
                            break; // Exit phone validation loop
                        }
                    }
                    break;

                case 3: // Email update section
                    System.out.println(Constants.CYAN + "\n--- Update Responder Email ---" + Constants.RESET);

                    // Email validation loop - ensures valid Gmail format
                    while(true){
                        System.out.println("");
                        System.out.print("Current Email: " + currentResponder.getEmail());
                        System.out.print("\nNew Email (must end with @gmail.com): ");
                        String newEmail = sc.nextLine().trim();
                        
                        // Validate email format using predefined pattern
                        if (newEmail.isEmpty() || !Pattern.matches(Constants.EMAIL_PATTERN, newEmail)) {
                            SystemLogger.error("Update cancelled - Invalid email format. Email must end with @gmail.com");
                            System.out.println("");
                        } else {
                            // Store old value for potential rollback
                            String oldEmail = currentResponder.getEmail();
                            currentResponder.setEmail(newEmail);

                            // Update database and handle result
                            if (responderManager.updateResponderInDatabase(currentResponder.getId(), "email", oldEmail, newEmail)) {
                                SystemLogger.success("Email updated successfully to: " + newEmail);
                            } else {
                                // Rollback on database failure
                                currentResponder.setEmail(oldEmail);
                                SystemLogger.error("Failed to update database. Changes reverted.");
                            }
                            break; // Exit email validation loop
                        }
                    }
                    break;

                case 4: // Zone update section - most complex due to coordinate management
                    System.out.println(Constants.CYAN + "\n--- Update Responder Zone ---" + Constants.RESET);
    
                    // Zone validation loop - ensures valid zone selection
                    while(true){
                        System.out.println("");
                        System.out.print("Current Zone: " + currentResponder.getZone());
                        System.out.print("\nNew Zone (North/South/East/West): ");
                        String newZone = sc.nextLine().trim().toLowerCase();
                        
                        // Validate zone format using predefined pattern
                        if (newZone.isEmpty() || !Pattern.matches(Constants.ZONE_PATTERN, newZone)) {
                            SystemLogger.error("Update cancelled - Invalid zone. It must be North, South, East, or West.");
                            System.out.println("");
                        } else {
                            // Store old values for comprehensive rollback if needed
                            String oldZone = currentResponder.getZone();
                            double oldX = currentResponder.getX();
                            double oldY = currentResponder.getY();
                            
                            // Remove responder from current zone in location manager before update
                            locationManager.removeResponder(currentResponder.getId());
                            
                            // Update zone and generate new coordinates based on the selected zone
                            currentResponder.setZone(newZone);
                            double[] coords = CoordinateGenerator.generateZoneBasedCoordinates(newZone);
                            currentResponder.setX(coords[1]);
                            currentResponder.setY(coords[0]);
                            
                            // Add responder to new zone in location manager
                            locationManager.addResponder(currentResponder);
                            
                            // Update database with zone change
                            if (responderManager.updateResponderInDatabase(currentResponder.getId(), "zone", oldZone, newZone)) {
                                SystemLogger.success("Zone updated from " + oldZone + " to " + newZone);
                                SystemLogger.info("Coordinates automatically updated for new zone.");
                            } else {
                                // Comprehensive rollback: remove from new zone, restore old values, add back to old zone
                                locationManager.removeResponder(currentResponder.getId());
                                currentResponder.setZone(oldZone);
                                currentResponder.setX(oldX);
                                currentResponder.setY(oldY);
                                locationManager.addResponder(currentResponder);
                                SystemLogger.error("Failed to update database. Changes reverted.");
                            }
                            break; // Exit zone validation loop
                        }
                    }
                    break;

                case 5: // Availability status update section
                    System.out.println(Constants.CYAN + "\n--- Update Responder Availability ---" + Constants.RESET);

                    // Availability validation loop - ensures valid boolean input
                    while(true){
                        System.out.println("");
                        System.out.print("Current Availability: " + currentResponder.isAvailable());
                        System.out.print("\nNew Availability (true/false): ");
                        String newAvailability = sc.nextLine().trim().toLowerCase();

                        // Validate boolean input format
                        if (newAvailability.isEmpty() || (!newAvailability.equalsIgnoreCase("true") && !newAvailability.equalsIgnoreCase("false"))) {
                            SystemLogger.error("Update cancelled - Invalid input. Please enter true or false.");
                            System.out.println("");
                        } else {
                            // Store old value for potential rollback
                            boolean oldAvailability = currentResponder.isAvailable();
                            boolean availability = Boolean.parseBoolean(newAvailability);
                            currentResponder.setAvailable(availability);

                            // Update database with string representation of boolean
                            if (responderManager.updateResponderInDatabase(currentResponder.getId(), "availability", 
                                String.valueOf(oldAvailability), String.valueOf(availability))) {
                                SystemLogger.success("Availability updated from " + oldAvailability + " to " + availability);
                            } else {
                                // Rollback on database failure
                                currentResponder.setAvailable(oldAvailability);
                                SystemLogger.error("Failed to update database. Changes reverted.");
                            }
                            break; // Exit availability validation loop
                        }
                    }
                    break;

                case 6: // Password update section - includes security confirmation
                    System.out.println(Constants.CYAN + "\n--- Update Responder Password ---" + Constants.RESET);
                                
                    // Password update loop with multiple validation steps
                    while(true) {
                        System.out.println("");
                        System.out.print("Enter current password: ");
                        String oldPassword = sc.nextLine().trim();
                                    
                        // Validate current password is not empty
                        if (oldPassword.isEmpty()) {
                            SystemLogger.error("Current password cannot be empty.");
                            System.out.println("");
                            continue;
                        }
                                    
                        System.out.print("Enter new password (min 6 chars): ");
                        String newPassword = sc.nextLine().trim();
                                    
                        // Validate new password meets minimum security requirements
                        if (!AuthenticationHelper.isValidPassword(newPassword)) {
                            SystemLogger.error("New password must be at least 6 characters long.");
                            System.out.println("");
                            continue;
                        }
                                    
                        System.out.print("Confirm new password: ");
                        String confirmPassword = sc.nextLine().trim();
                                    
                        // Ensure new password and confirmation match
                        if (!newPassword.equals(confirmPassword)) {
                            SystemLogger.error("Passwords do not match. Please try again.");
                            System.out.println("");
                            continue;
                        }
                                    
                        // Final confirmation before making irreversible password change
                        System.out.print("Are you sure you want to update your password? (y/n): ");
                        String confirmation = sc.nextLine().trim().toLowerCase();
                                    
                        // Allow user to cancel password update
                        if (!confirmation.equals("y") && !confirmation.equals("yes")) {
                            SystemLogger.info("Password update cancelled.");
                            break;
                        }
                                    
                        // Attempt password update with specialized password method
                        if (responderManager.updateResponderPassword(currentResponder.getId(), oldPassword, newPassword)) {
                            SystemLogger.success("Password updated successfully!");
                        } else {
                            SystemLogger.error("Failed to update password. Please check your current password.");
                        }
                        break; // Exit password update loop
                    }
                    break;

                case 7: // Bulk update all responder details except password in a single operation
                    System.out.println(Constants.INFO + "Updating all details:" + Constants.RESET); // Inform user about bulk update mode
                    System.out.println("Press Enter to keep current value, or enter new value to update."); // Instructions for user input

                    // Store original values for potential rollback if database updates fail
                    String oldName = currentResponder.getName();
                    double oldX = currentResponder.getX();
                    double oldY = currentResponder.getY();

                    boolean updateSuccess = true; // Track overall success of all database operations

                    // Name update section - validates and updates responder name if provided
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

                    // Phone update section - validates and updates phone number if provided
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

                    // Email update section - validates and updates email if provided
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

                    // Zone update section - most complex due to coordinate and location manager coordination
                    System.out.print("Zone [" + currentResponder.getZone() + "]: ");
                    String allZone = sc.nextLine().trim().toLowerCase(); 
                    if (!allZone.isEmpty() && Pattern.matches(Constants.ZONE_PATTERN, allZone)) { 
                        String zoneOld = currentResponder.getZone();
                        locationManager.removeResponder(currentResponder.getId());

                        currentResponder.setZone(allZone);
                        double[] coords = CoordinateGenerator.generateZoneBasedCoordinates(allZone);
                        currentResponder.setX(coords[1]);
                        currentResponder.setY(coords[0]);

                        locationManager.addResponder(currentResponder); // Add responder to new zone in location manager

                        if (!responderManager.updateResponderInDatabase(currentResponder.getId(), "zone", zoneOld, allZone)) {
                            updateSuccess = false;
                            locationManager.removeResponder(currentResponder.getId()); // Remove from new zone in location manager
                            currentResponder.setZone(zoneOld);
                            currentResponder.setX(oldX);
                            currentResponder.setY(oldY);
                            locationManager.addResponder(currentResponder);
                        }
                    }

                    // Availability update section - validates and updates availability status if provided
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

                    // Display appropriate success/error message based on overall update status
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

    // Checks if a user has any active alerts in the database (assigned or waiting status)
    private static boolean hasActiveAlertFromDB(int userId) {
        try {
            String dburl = "jdbc:mysql://localhost:3306/WomenSafetyDB";
            String dbuser = "root";
            String dbpass = "";

            Connection con = DriverManager.getConnection(dburl, dbuser, dbpass);

            // SQL query to count active alerts for the user with specific statuses
            String sql = "SELECT COUNT(*) FROM alert_details WHERE User_id = ? AND Status IN (?, ?)";
            PreparedStatement pst=con.prepareStatement(sql);

            pst.setInt(1, userId);
            pst.setString(2, Constants.STATUS_ASSIGNED);
            pst.setString(3, Constants.STATUS_WAITING);

            ResultSet rs = pst.executeQuery();

            // Check if there are results and return true if count > 0
            if (rs.next()) {
                return rs.getInt(1) > 0;  // Return true if at least one active alert exists
            }
        } catch (SQLException e) {
            // Log database errors for debugging and monitoring
            SystemLogger.error("DB error while checking active alerts: " + e.getMessage());
        }
        // Return false if no active alerts found or if an error occurred
        return false;
    }
}