# Women's Safety Alert System

A comprehensive Java-based backend system designed to enhance women's safety through real-time alert dispatching, role-based access control, and efficient emergency response coordination.

##  Personal Note

This was developed as my Java project in ***Semester-2***.

## Features

### User Module
- **User Registration & Authentication** - Secure account creation with comprehensive validation
- **Alert Raising** - Instant emergency alert generation with precise location tracking
- **Profile Management** - Complete user information management system
- **Zone-based Coordination** - Geographic zone allocation (North/South/East/West)

### Responder Module  
- **Responder Registration** - Emergency personnel onboarding and management
- **Availability Management** - Real-time status updates and coordination
- **Alert Processing** - Efficient emergency response handling system
- **Distance Calculation** - Optimal responder assignment based on geographic coordinates

### Admin Module
- **System Monitoring** - Comprehensive dashboard with real-time statistics
- **User Management** - Complete oversight of user accounts and activities
- **Responder Management** - Emergency personnel coordination and management
- **Alert Tracking** - End-to-end alert lifecycle monitoring and reporting

### Security & RBAC
- **Role-Based Access Control** - Three distinct security roles (User, Responder, Admin)
- **Permission Management** - Granular access control system with detailed permissions
- **Input Validation** - Comprehensive data integrity and validation checks
- **Authentication System** - Secure login/logout mechanisms with session management

### Alert Management
- **Real-time Dispatching** - Instant alert assignment to available responders
- **Queue System** - FIFO (First-In-First-Out) processing of emergency alerts
- **Status Tracking** - Complete lifecycle management (Active → Assigned → Waiting → Resolved)
- **Escalation System** - Automatic reassignment mechanism when responders are unavailable

### System Features
- **Multithreading** - Background processing for continuous system monitoring
- **Database Persistence** - MySQL integration with comprehensive data models
- **Logging System** - Detailed activity tracking and comprehensive audit trails
- **Coordinate Management** - Geospatial calculations for optimal responder assignment

## Tech Stack

- **Programming Language**: Java 8+
- **Database**: MySQL with JDBC connectivity
- **Concurrency**: Multithreading for background processing and real-time operations
- **File Handling**: Comprehensive logging and file management system
- **Version Control**: Git & GitHub for collaborative development
- **Design Patterns**: Object-Oriented Programming, Singleton, Factory, Observer patterns

## Prerequisites

Before running the application, ensure you have the following installed:

- Java Development Kit (JDK) 8 or higher
- MySQL Server 5.7 or higher installed and running
- MySQL Connector/J driver for database connectivity
- Git for version control and collaboration

## Installation & Setup

### 1. Clone the Repository
```bash
git clone https://github.com/your-username/women-safety-alert-system.git
cd women-safety-alert-system
```

### 2. Database Setup
```sql
-- Create the database
CREATE DATABASE WomenSafetyDB;

-- Use the database
USE WomenSafetyDB;

-- The application will automatically create all required tables including:
-- user_details, responder_details, admin_details, alert_details,
-- alert_status_history, dispatches, user_update_logs, responder_update_logs
```

### 3. Configure Database Connection
Update the database connection details in the relevant service classes:
```java
// Database configuration found in service classes
String dburl = "jdbc:mysql://localhost:3306/WomenSafetyDB";
String dbuser = "root";
String dbpass = "your_password";
```

### 4. Compile and Run the Application
```bash
# Compile the entire project
javac -d out src/com/womensafety/alertsystem/**/*.java

# Run the main application
java -cp out com.womensafety.alertsystem.main.Main
```

## Project Structure

```
Women-Safety-Alert-System/
├── db/                                             # Database files
│   └── schema.sql                                  # Database schema with all table definitions                      
├── src/                                            # Java source code
│   └── com/
│       └── womensafety/
│           └── alertsystem/
│               ├── main/
│               │   └── Main.java                   # Application entry point and controller
│               ├── model/                          # Data models and entities
│               │   ├── User.java                   # User entity with validation
│               │   ├── Responder.java              # Responder entity with availability
│               │   ├── Admin.java                  # Admin entity with privileges
│               │   ├── Alert.java                  # Alert entity with status tracking
│               │   ├── Role.java                   # Role enumeration (USER, RESPONDER, ADMIN)
│               │   └── Permission.java             # Permission enumeration system
│               ├── manager/                        # Business logic managers
│               │   ├── UserManager.java            # User operations and management
│               │   ├── ResponderManager.java       # Responder operations and coordination
│               │   ├── AdminManager.java           # Admin operations and system management
│               │   └── LocationManager.java        # Location coordination and zone management
│               ├── service/                        # Core services and processing
│               │   ├── Dispatcher.java             # Alert dispatching and queue management
│               │   ├── AuthenticationHelper.java   # Authentication utilities
│               │   ├── AlertLoopThread.java        # Background alert processing thread
│               │   ├── ResponderStatusChecker.java # Responder availability monitoring
│               │   └── NearestResponderFinder.java # Distance calculation service
│               ├── security/                       # Security components
│               │   ├── RBACManager.java            # Role-based access control system
│               │   └── RolePermissionManager.java  # Permission management and validation
│               ├── database/                       # Database connectivity
│               │   └── DatabaseConnection.java     # Database connection setup and management
│               └── util/                           # Utility classes and helpers
│                   ├── Constants.java              # Application constants and configurations
│                   ├── SystemLogger.java           # System logging and activity tracking
│                   ├── EscalationLogger.java       # Alert escalation logging and monitoring
│                   └── CoordinateGenerator.java    # Coordinate generation and management
├── out/                                            # Compiled Java classes
├── docs/                                           # ER diagram, Flowcharts etc.                                         
└── README.md                                       # Project documentation
```

## System Workflow

### Alert Lifecycle
1. **Alert Creation** - User raises emergency alert with location data and details
2. **Queue Processing** - Alert enters FIFO queue for systematic processing
3. **Responder Assignment** - System identifies and assigns nearest available responder
4. **Status Update** - Alert status transitions to "Assigned" state
5. **Response Execution** - Responder processes, addresses, and resolves the alert
6. **Completion** - Alert marked as "Resolved" and responder status updated to available

### Escalation Process
1. **Initial Assignment Attempt** - System attempts zone-specific responder assignment
2. **Waiting Status** - Alert enters waiting state if no responders are available
3. **Background Monitoring** - Continuous system checking for responder availability
4. **Automatic Reassignment** - System automatically reassigns alerts when responders become available
5. **Escalation Logging** - Comprehensive tracking and logging of all escalation events

## Security & Validation

### Role-Based Access Control
- **User Role** - Can raise alerts and manage personal profile information
- **Responder Role** - Can process alerts and update availability status
- **Admin Role** - Full system access with comprehensive monitoring capabilities

### Input Validation
- **Email Validation** - Strict format validation requiring @gmail.com domain
- **Phone Validation** - 10-digit Indian phone number validation (starting with 7,8,9)
- **Password Policy** - Minimum 6 character requirement for security
- **Zone Validation** - Restricted to North/South/East/West zone options only
- **Name Validation** - Letters and spaces only, excluding special characters

### Database Security
- **Prepared Statements** - Comprehensive SQL injection prevention
- **Transaction Management** - Data consistency and integrity
- **Audit Logging** - Detailed change tracking and activity monitoring
- **Data Integrity** - Foreign key relationships and constraint enforcement

### Logging System
- **System Logger** - General application logging and activity tracking
- **Escalation Logger** - Specialized alert escalation monitoring and reporting
- **Database Audit** - Comprehensive change logging in database tables
- **Status History** - Complete alert status transition history tracking

## Author

***Dhyey Daftary***  
LinkedIn: [https://www.linkedin.com/in/dhyey-daftary/](https://www.linkedin.com/in/dhyey-daftary/)

## License

This project is developed for **educational purposes** as part of academic coursework. The system demonstrates advanced Java programming concepts, database management principles, and comprehensive software engineering methodologies.

---

### Key Learning Objectives Demonstrated:
- Object-Oriented Programming principles and best practices
- Database design, implementation, and JDBC integration  
- Multithreading and concurrency management in real-time systems
- Role-Based Access Control implementation and security management
- Input validation, data integrity, and security best practices
- Software design patterns and architectural principles
- Comprehensive logging, error handling, and system monitoring
- Real-time system design, coordination, and emergency response management

For any questions, suggestions, or contributions, please contact the author through LinkedIn.
