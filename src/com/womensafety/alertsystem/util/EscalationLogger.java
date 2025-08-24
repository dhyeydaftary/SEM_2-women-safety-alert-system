package com.womensafety.alertsystem.util;

import com.womensafety.alertsystem.model.Alert;
import java.io.FileWriter;
import java.io.IOException;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

// Escalation logger class for logging alert escalation events to a file
public class EscalationLogger {
    String FILE_NAME="escalation_log.txt"; // Log file name for escalation events

    // Logs an alert escalation event to the log file
    // Parameters: alert - the alert object containing alert details
    //             reason - the reason for escalation
    public void logToFile(Alert alert, String reason){
        try (FileWriter fw=new FileWriter(FILE_NAME, true)){
            DateTimeFormatter formatter= DateTimeFormatter.ofPattern("dd-MM-yyyy HH:mm:ss");
            String timestamp=LocalDateTime.now().format(formatter);
            // Create log entry with timestamp, alert details, and escalation reason
            String logEntry =  "[" + timestamp + "] ALERT ID: " + alert.getAlertId()+" | USER: "+alert.getUser().getName()+
                    " | ZONE: "+alert.getUser().getZone()+" | REASON: "+reason+"\n";

            fw.write(logEntry); // Write log entry to file
        } catch (IOException e){
            System.out.println("Error writing to log file: " + e.getMessage());
        }
    }
}
