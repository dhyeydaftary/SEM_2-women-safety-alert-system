package com.womensafety.alertsystem.service;

import com.womensafety.alertsystem.model.User;
import com.womensafety.alertsystem.model.Responder;
import java.util.List;

public class NearestResponderFinder{
    public static Responder findNearestResponder(User user,List<Responder> responders){
        Responder nearestResponder = null;
        double minDistance = Double.MAX_VALUE;

        for(Responder r : responders){
            if(!r.isAvailable())
                continue;
            double distance=calculateDistance(user.getX(), user.getY(), r.getX(), r.getY());

            if(distance<minDistance){
                minDistance=distance;
                nearestResponder=r;
            }
        }
        return nearestResponder;
    }

    public static double calculateDistance(double x1, double y1, double x2, double y2){
        return Math.sqrt(Math.pow(x2- x1,2) + Math.pow(y2-y1,2));
    }
}