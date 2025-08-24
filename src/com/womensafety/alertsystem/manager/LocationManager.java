package com.womensafety.alertsystem.manager;

import com.womensafety.alertsystem.model.Responder;
import com.womensafety.alertsystem.util.SystemLogger;
import java.util.*;

// LocationManager class handles responder location and zone management
// Manages responder distribution across zones and provides location-based services
public class LocationManager {
    private HashMap<String, List<Responder>> zoneMap; // Maps zone names to lists of responders
    private Random random; // Random generator for selecting random responders

    // Constructor initializes the zone map and random generator
    public LocationManager() {
        zoneMap = new HashMap<>(); // Initialize empty zone map
        random = new Random(); // Initialize random number generator
    }

    // Adds a responder to their designated zone
    public void addResponder(Responder responder) {
        if(responder == null || responder.getZone() == null || responder.getZone().trim().isEmpty()) {
            SystemLogger.error("Invalid responder or zone."); // Log error for invalid input
            return; // Exit if responder or zone is invalid
        }
        String zone = responder.getZone(); // Get the responder's zone

        if (!zoneMap.containsKey(zone)) {
            zoneMap.put(zone, new ArrayList<>()); // Create new list if zone doesn't exist
        }

        zoneMap.get(zone).add(responder); // Add responder to their zone's list
        SystemLogger.success("Responder added to zone: " + zone); // Log successful addition
    }

    // Removes a responder by ID from all zones
    public void removeResponder(int id) {
        for (String zone : zoneMap.keySet()) {
            List<Responder> responders = zoneMap.get(zone);
            responders.removeIf(r -> r.getId() == id); // Remove responder with matching ID
        }
        SystemLogger.info("Responder with ID " + id + " removed."); // Log removal
    }

    // Gets all responders in a specific zone (case-insensitive)
    // Returns: List of responders in the specified zone
    public List<Responder> getRespondersInZone(String zone) {
        if (zone == null)
            return new ArrayList<>(); // Return empty list for null zone
        for (String key : zoneMap.keySet()) {
            if (key.equalsIgnoreCase(zone)) {
                return zoneMap.get(key); // Return responders for matching zone
            }
        }
        return new ArrayList<>(); // Return empty list if zone not found
    }

    // Finds a random available responder in the specified zone
    // Returns: Random available responder or null if none found
    public Responder findRandomAvailableResponderInZone(String zone) {
        List<Responder> responders = getRespondersInZone(zone); // Get responders in zone
        List<Responder> availableResponders = new ArrayList<>(); // List for available responders

        for (Responder r : responders) {
            if (r.isAvailable()) {
                availableResponders.add(r); // Add available responders to list
            }
        }

        if (!availableResponders.isEmpty()) {
            int randomIndex = random.nextInt(availableResponders.size()); // Get random index
            return availableResponders.get(randomIndex); // Return random available responder
        }

        return null; // Return null if no available responders
    }

    // Finds the nearest available responder to given coordinates in a zone
    // Returns: Nearest available responder or null if none found
    public Responder findNearestAvailableResponder(double userX, double userY, String zone) {
        List<Responder> responders = getRespondersInZone(zone); // Get responders in zone
        Responder nearest = null; // Track nearest responder
        double minDistance = Double.MAX_VALUE; // Track minimum distance

        for (Responder r : responders){
            if (r.isAvailable()){ // Only consider available responders
                // Calculate Euclidean distance between user and responder
                double dist = Math.sqrt(Math.pow(userX - r.getX(), 2) + Math.pow(userY - r.getY(), 2));
                if(dist < minDistance){ // Update if this responder is closer
                    minDistance = dist;
                    nearest = r;
                }
            }
        }
        return nearest; // Return nearest available responder
    }

    // Gets all responders from all zones
    // Returns: List of all responders in the system
    public List<Responder> getAllResponders(){
        List<Responder> all = new ArrayList<>(); // Create combined list
        for (List<Responder> responders : zoneMap.values()){
            all.addAll(responders); // Add all responders from each zone
        }
        return all; // Return complete list
    }

    // Prints all responders organized by zone with availability status
    public void printAllResponders(){
        for (String zone : zoneMap.keySet()){
            System.out.println("Zone: " + zone); // Print zone header
            for (Responder r : zoneMap.get(zone)){
                // Print responder details with availability
                System.out.println("-> " + r.getName() + " (Available: " + r.isAvailable() + ")");
            }
        }
    }
}
