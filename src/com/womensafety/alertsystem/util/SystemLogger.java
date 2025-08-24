package com.womensafety.alertsystem.util;

import java.io.*;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

// System logger class for logging messages to console and file
public class SystemLogger {
    private static final String LOG_FILE = "system_log.txt"; // Log file name for system logs
    private static final DateTimeFormatter TIMESTAMP_FORMAT = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

    // Enum for log levels
    public enum LogLevel {
        INFO, WARNING, ERROR, SUCCESS
    }

    // Logs a message with a specific log level
    // Parameters: level - the log level (INFO, WARNING, ERROR, SUCCESS)
    //             message - the message to log
    public static void log(LogLevel level, String message) {
        String timestamp = LocalDateTime.now().format(TIMESTAMP_FORMAT);
        String logEntry = String.format("[%s] %s: %s", timestamp, level, message);

        printToConsole(level, logEntry); // Print log entry to console
        writeToFile(logEntry); // Write log entry to file
    }

    // Prints a log message to the console with color based on log level
    // Parameters: level - the log level
    //             message - the message to print
    private static void printToConsole(LogLevel level, String message) {
        String color = switch (level) {     // ANSI color codes for different log levels
            case INFO -> "\u001B[37m"+"[INFO] ";
            case SUCCESS -> "\u001B[32m"+"[SUCCESS] ";
            case WARNING -> "\u001B[33m"+"[WARNING] ";
            case ERROR -> "\u001B[31m"+"[ERROR] ";
        };
        System.out.println(color + message + "\u001b[0m");
    }

    // Writes a log entry to the log file
    // Parameters: logEntry - the log entry to write
    private static void writeToFile(String logEntry) {
        try (FileWriter fw = new FileWriter(LOG_FILE, true);
             PrintWriter pw = new PrintWriter(fw)) {
            pw.println(logEntry);
        } catch (IOException e) {
            System.err.println("Failed to write to log file: " + e.getMessage());
        }
    }

    // Convenience method for logging info messages
    public static void info(String message) {
        log(LogLevel.INFO, message); // Log info message
    }
    // Convenience method for logging warning messages
    public static void warning(String message) {
        log(LogLevel.WARNING, message); // Log warning message
    }
    // Convenience method for logging error messages
    public static void error(String message) {
        log(LogLevel.ERROR, message); // Log error message
    }
    // Convenience method for logging success messages
    public static void success(String message) {
        log(LogLevel.SUCCESS, message); // Log success message
    }
}