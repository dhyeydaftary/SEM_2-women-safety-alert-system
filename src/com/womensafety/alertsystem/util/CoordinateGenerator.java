package com.womensafety.alertsystem.util;

import java.util.Random;

public class CoordinateGenerator{
    public static final double INDIA_MIN_LAT =8.4;
    public static final double INDIA_MAX_LAT = 37.6;
    public static final double INDIA_MIN_LNG = 68.7;
    public static final double INDIA_MAX_LNG = 97.25;

    private static  Random random = new Random();

    public static double generateRandomLatitude(){
        return INDIA_MIN_LAT+(INDIA_MAX_LAT - INDIA_MIN_LAT)*random.nextDouble();
    }

    public static double generateRandomLongitude() {
        return INDIA_MIN_LNG + (INDIA_MAX_LNG - INDIA_MIN_LNG) * random.nextDouble();
    }

    public static double[] generateRandomCoordinates(){
        return new double[] {
                generateRandomLatitude(),
                generateRandomLongitude()
        };
    }

    public static double[] generateZoneBasedCoordinates(String zone){
        double baseLat,baseLng;
        double offset =2.0;

        switch (zone.toLowerCase()) {
            case "north":
                baseLat = 28.0;
                baseLng = 77.0;
                break;
            case "south":
                baseLat = 12.0;
                baseLng = 78.0;
                break;
            case "east":
                baseLat = 22.0;
                baseLng = 88.0;
                break;
            case "west":
                baseLat = 19.0;
                baseLng = 73.0;
                break;
            default:
                return generateRandomCoordinates();
        }

        double lat = baseLat + (random.nextDouble() - 0.5) * 2 * offset;
        double lng = baseLng + (random.nextDouble() - 0.5) * 2 * offset;

        lat = Math.max(INDIA_MIN_LAT, Math.min(INDIA_MAX_LAT, lat));
        lng = Math.max(INDIA_MIN_LNG, Math.min(INDIA_MAX_LNG, lng));

        return new double[] {lat, lng};
    }

    public static void main(String[] args) {
        System.out.println("Random coordinates in India:");


        for (int i = 0; i < 5; i++) {
            double[] coords = generateRandomCoordinates();
            System.out.printf("Coordinate %d: Lat=%.4f, Lng=%.4f%n",
                    i+1, coords[0], coords[1]);
        }

        System.out.println("\nZone-based coordinates:");
        String[] zones = {"North", "South", "East", "West"};

        for (String zone : zones) {
            double[] coords = generateZoneBasedCoordinates(zone);
            System.out.printf("%s Zone: Lat=%.4f, Lng=%.4f%n",
                    zone, coords[0], coords[1]);
        }
    }
}