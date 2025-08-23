package com.womensafety.alertsystem.util;

import com.womensafety.alertsystem.model.Alert;
import java.io.FileWriter;
import java.io.IOException;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

public class EscalationLogger {
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
