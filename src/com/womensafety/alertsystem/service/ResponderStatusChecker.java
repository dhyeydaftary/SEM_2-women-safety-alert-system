package com.womensafety.alertsystem.service;

import com.womensafety.alertsystem.manager.LocationManager;
import com.womensafety.alertsystem.model.Responder;
import com.womensafety.alertsystem.util.Constants;
import java.util.*;

// Thread class for periodically checking and reporting responder statuses
public class ResponderStatusChecker implements Runnable{
    private LocationManager locationManager;

    // Constructor for ResponderStatusChecker
    public ResponderStatusChecker(LocationManager locationManager){
        this.locationManager = locationManager;
    }

    // Main thread execution method
    public void run(){
        while(!Thread.currentThread().isInterrupted()){ // Continue running until interrupted
            try{
                checkResponderStatuses(); // Check responder statuses
                Thread.sleep(Constants.RESPONDER_STATUS_CHECKER_INTERVAL); // Sleep for configured interval
            }catch (InterruptedException e) {
                Thread.currentThread().interrupt(); // Restore interrupt status
                System.out.println(Constants.CYAN+ "Responder status checker shutting down gracefully." + Constants.RESET);
                break;
            }
        }
    }
    
    // Checks and reports the status of all responders
    private void checkResponderStatuses(){
        if (!Constants.ENABLE_BACKGROUND_LOGGING) { // Check if background logging is enabled
            return; // Exit if background logging is disabled
        }
        System.out.println("\n[Background] Checking responder statuses...");
        List<Responder>responders = locationManager.getAllResponders(); // Get all responders

        for(Responder r : responders){
            if(r.isAvailable()){ // Check if responder is available
                System.out.println("[Background] "+r.getName()+" is AVAILABLE");
            } else {
                System.out.println("[Background] "+r.getName()+" is BUSY");
            }
        }
    }
}
