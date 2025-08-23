package com.womensafety.alertsystem.util;

import java.io.*;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

public class SystemLogger {
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