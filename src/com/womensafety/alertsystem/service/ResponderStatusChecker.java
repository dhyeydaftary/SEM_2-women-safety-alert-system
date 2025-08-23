package com.womensafety.alertsystem.service;

import com.womensafety.alertsystem.manager.LocationManager;
import com.womensafety.alertsystem.model.Responder;
import com.womensafety.alertsystem.util.Constants;
import java.util.List;

public class ResponderStatusChecker implements Runnable{
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
