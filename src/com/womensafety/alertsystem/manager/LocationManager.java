package com.womensafety.alertsystem.manager;

import com.womensafety.alertsystem.model.Responder;
import com.womensafety.alertsystem.util.SystemLogger;
import java.util.*;

public class LocationManager {
    private HashMap<String, List<Responder>> zoneMap;
    private Random random;

    public LocationManager() {
        zoneMap = new HashMap<>();
        random=new Random();
    }

    public void addResponder(Responder responder) {
        if(responder==null || responder.getZone() == null || responder.getZone().trim().isEmpty()) {
            SystemLogger.error("Invalid responder or zone.");
            return;
        }
        String zone = responder.getZone();

        if (!zoneMap.containsKey(zone)) {
            zoneMap.put(zone, new ArrayList<>());
        }

        zoneMap.get(zone).add(responder);
        SystemLogger.success("Responder added to zone: "+zone);
    }

    public void removeResponder(int id) {
        for (String zone : zoneMap.keySet()) {
            List<Responder> responders = zoneMap.get(zone);
            responders.removeIf(r -> r.getId() == id);
        }
        SystemLogger.info("Responder with ID " + id + " removed.");
    }

    public List<Responder> getRespondersInZone(String zone) {
        if (zone == null)
            return new ArrayList<>();
        for (String key : zoneMap.keySet()) {
            if (key.equalsIgnoreCase(zone)) {
                return zoneMap.get(key);
            }
        }
        return new ArrayList<>();
    }

    public Responder findRandomAvailableResponderInZone(String zone) {
        List<Responder> responders = getRespondersInZone(zone);
        List<Responder> availableResponders = new ArrayList<>();

        for (Responder r : responders) {
            if (r.isAvailable()) {
                availableResponders.add(r);
            }
        }

        if (!availableResponders.isEmpty()) {
            int randomIndex = random.nextInt(availableResponders.size());
            return availableResponders.get(randomIndex);
        }

        return null;
    }

    public Responder findNearestAvailableResponder(double userX, double userY, String zone) {
        List<Responder> responders = getRespondersInZone(zone);
        Responder nearest = null;
        double minDistance = Double.MAX_VALUE;

        for (Responder r : responders){
            if (r.isAvailable()){
                double dist = Math.sqrt(Math.pow(userX - r.getX(), 2) + Math.pow(userY - r.getY(), 2));
                if(dist < minDistance){
                    minDistance = dist;
                    nearest = r;
                }
            }
        }
        return nearest;
    }

    public List<Responder> getAllResponders(){
        List<Responder> all = new ArrayList<>();
        for (List<Responder> responders : zoneMap.values()){
            all.addAll(responders);
        }
        return all;
    }

    public void printAllResponders(){
        for (String zone : zoneMap.keySet()){
            System.out.println("Zone: " + zone);
            for (Responder r : zoneMap.get(zone)){
                System.out.println("-> "+r.getName()+" (Available: "+r.isAvailable()+ ")");
            }
        }
    }
}