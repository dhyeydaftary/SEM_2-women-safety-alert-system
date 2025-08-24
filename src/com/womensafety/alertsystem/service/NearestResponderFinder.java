package com.womensafety.alertsystem.service;

import com.womensafety.alertsystem.model.User;
import com.womensafety.alertsystem.model.Responder;
import java.util.*;

// Class for finding the nearest available responder to a user
public class NearestResponderFinder{
    // Finds the nearest available responder to a user
    public static Responder findNearestResponder(User user,List<Responder> responders){
        Responder nearestResponder = null;
        double minDistance = Double.MAX_VALUE;

        for(Responder r : responders){ // Iterate through all responders
            if(!r.isAvailable()) // Skip if responder is not available
                continue;
            double distance=calculateDistance(user.getX(), user.getY(), r.getX(), r.getY()); // Calculate distance to responder

            if(distance<minDistance){ // Check if this responder is closer
                minDistance=distance; // Update minimum distance
                nearestResponder=r; // Update nearest responder
            }
        }
        return nearestResponder; // Return the nearest available responder
    }

    // Calculates the Euclidean distance between two points
    public static double calculateDistance(double x1, double y1, double x2, double y2){
        return Math.sqrt(Math.pow(x2- x1,2) + Math.pow(y2-y1,2)); // Euclidean distance formula
    }
}
