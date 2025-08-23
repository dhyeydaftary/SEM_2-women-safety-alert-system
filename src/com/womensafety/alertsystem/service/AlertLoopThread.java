package com.womensafety.alertsystem.service;

import com.womensafety.alertsystem.util.Constants;

public class AlertLoopThread extends Thread{
    private Dispatcher dispatcher;
    private boolean running = true;

    public AlertLoopThread(Dispatcher dispatcher){
        this.dispatcher = dispatcher;
    }

    public void run(){
        while (running) {
            dispatcher.checkUnassignedAlerts();
            try {
                Thread.sleep(Constants.PENDING_ALERT_CHECKER_INTERVAL);
            }catch (InterruptedException e){
                System.out.println("Thread interrupted.");
            }
        }
    }

    public void stopThread(){
        running = false;
    }
}