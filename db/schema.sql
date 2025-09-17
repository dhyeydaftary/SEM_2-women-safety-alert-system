-- Women Safety Alert System Database Schema
-- This schema defines the database structure for the Women Safety Alert System
-- Database: WomenSafetyDB
-- Author: BLACKBOXAI
-- Date: 2024

SET SQL_MODE = "NO_AUTO_VALUE_ON_ZERO";
START TRANSACTION;
SET time_zone = "+00:00";

-- =====================
-- User Details Table
-- =====================
CREATE TABLE user_details (
    User_id INT PRIMARY KEY,
    Name VARCHAR(50) NOT NULL,
    Phone_no VARCHAR(14) NOT NULL UNIQUE,
    Email VARCHAR(100) NOT NULL UNIQUE,
    Location VARCHAR(100) NOT NULL,
    Zone VARCHAR(20) NOT NULL CHECK (Zone IN ('North', 'South', 'East', 'West')),
    Password VARCHAR(255) NOT NULL,
    X_coordinate DECIMAL(9,6),
    Y_coordinate DECIMAL(9,6)
);

-- =====================
-- Responder Details Table
-- =====================
CREATE TABLE responder_details (
    Responder_id INT PRIMARY KEY,
    Name VARCHAR(50) NOT NULL,
    Phone_no VARCHAR(14) NOT NULL UNIQUE,
    Email VARCHAR(100) NOT NULL UNIQUE,
    Zone VARCHAR(20) NOT NULL CHECK (Zone IN ('North', 'South', 'East', 'West')),
    Availability BOOLEAN DEFAULT TRUE,
    Password VARCHAR(255) NOT NULL,
    X_coordinate DECIMAL(9,6),
    Y_coordinate DECIMAL(9,6)
);

-- =====================
-- Admin Details Table
-- =====================
CREATE TABLE admin_details (
    Admin_id INT PRIMARY KEY,
    Name VARCHAR(50) NOT NULL,
    Phone_no VARCHAR(14) NOT NULL UNIQUE,
    Email VARCHAR(100) NOT NULL UNIQUE,
    Password VARCHAR(255) NOT NULL,
    Role VARCHAR(20) DEFAULT 'ADMIN'
);

-- =====================
-- Alert Details Table
-- =====================
CREATE TABLE alert_details (
    Alert_id INT PRIMARY KEY,
    User_id INT NOT NULL,
    Responder_id INT,
    Status VARCHAR(20) DEFAULT 'Active' CHECK (Status IN ('Active', 'Assigned', 'Waiting', 'Resolved')),
    Alert_time TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    X_coordinate DECIMAL(9,6),
    Y_coordinate DECIMAL(9,6),
    FOREIGN KEY (User_id) REFERENCES user_details(User_id) ON DELETE CASCADE,
    FOREIGN KEY (Responder_id) REFERENCES responder_details(Responder_id) ON DELETE SET NULL
);

-- =====================
-- Alert Status History Table
-- =====================
CREATE TABLE alert_status_history (
    Alert_id INT NOT NULL,
    Previous_status VARCHAR(20),
    Current_status VARCHAR(20) NOT NULL,
    Responder_id INT,
    Changed_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    FOREIGN KEY (Alert_id) REFERENCES alert_details(Alert_id) ON DELETE CASCADE,
    FOREIGN KEY (Responder_id) REFERENCES responder_details(Responder_id) ON DELETE SET NULL,
    PRIMARY KEY (Alert_id, Changed_at)
);

-- =====================
-- Dispatches Table
-- =====================
CREATE TABLE dispatches (
    Alert_id INT NOT NULL,
    Responder_id INT NOT NULL,
    Distance_km DECIMAL(5,2) NOT NULL,
    Completion_time TIMESTAMP NULL,
    FOREIGN KEY (Alert_id) REFERENCES alert_details(Alert_id) ON DELETE CASCADE,
    FOREIGN KEY (Responder_id) REFERENCES responder_details(Responder_id) ON DELETE CASCADE,
    PRIMARY KEY (Alert_id, Responder_id)
);

-- =====================
-- User Update Logs Table
-- =====================
CREATE TABLE user_update_logs (
    Log_id INT AUTO_INCREMENT PRIMARY KEY,
    User_id INT NOT NULL,
    Field_changed VARCHAR(50) NOT NULL,
    Old_value VARCHAR(100),
    New_value VARCHAR(100),
    Updated_by INT NOT NULL,
    Update_timestamp TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    FOREIGN KEY (User_id) REFERENCES user_details(User_id) ON UPDATE CASCADE
);

-- =====================
-- Responder Update Logs Table
-- =====================
CREATE TABLE responder_update_logs (
    Log_id INT AUTO_INCREMENT PRIMARY KEY,
    Responder_id INT NOT NULL,
    Field_changed VARCHAR(50) NOT NULL,
    Old_value VARCHAR(100),
    New_value VARCHAR(100),
    Updated_by INT NOT NULL,
    Update_timestamp TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    FOREIGN KEY (Responder_id) REFERENCES responder_details(Responder_id) ON UPDATE CASCADE
);

COMMIT;
