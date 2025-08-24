package com.womensafety.alertsystem.service;

import com.womensafety.alertsystem.util.Constants;

// Thread class for continuously checking and managing unassigned alerts
public class AlertLoopThread extends Thread{
    private Dispatcher dispatcher;
    private boolean running = true;

    // Constructor for AlertLoopThread
    public AlertLoopThread(Dispatcher dispatcher){
        this.dispatcher = dispatcher;
    }

    // Main thread execution method
    public void run(){
        while (running) { // Continue running while flag is true
            dispatcher.checkUnassignedAlerts(); // Check for unassigned alerts
            try {
                Thread.sleep(Constants.PENDING_ALERT_CHECKER_INTERVAL); // Sleep for configured interval
            }catch (InterruptedException e){
                System.out.println("Thread interrupted."); // Handle thread interruption
            }
        }
    }

    // Method to stop the thread execution
    public void stopThread(){
        running = false;
    }
}
