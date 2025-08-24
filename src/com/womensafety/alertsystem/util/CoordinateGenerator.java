package com.womensafety.alertsystem.util;

import java.util.Random;

// Coordinate generator class for generating random geographic coordinates within India
public class CoordinateGenerator{
    public static final double INDIA_MIN_LAT = 8.4;
    public static final double INDIA_MAX_LAT = 37.6;
    public static final double INDIA_MIN_LNG = 68.7;
    public static final double INDIA_MAX_LNG = 97.25;

    private static  Random random = new Random(); // Random number generator for coordinate generation

    // Generates a random latitude within India's boundaries
    public static double generateRandomLatitude(){
        return INDIA_MIN_LAT+(INDIA_MAX_LAT - INDIA_MIN_LAT)*random.nextDouble();
    }

    // Generates a random longitude within India's boundaries
    public static double generateRandomLongitude() {
        return INDIA_MIN_LNG + (INDIA_MAX_LNG - INDIA_MIN_LNG) * random.nextDouble();
    }

    // Generates random coordinates (latitude and longitude) within India
    // Returns: double array containing [latitude, longitude]
    public static double[] generateRandomCoordinates(){
        return new double[] {
                generateRandomLatitude(),
                generateRandomLongitude()
        };
    }

    // Generates coordinates based on a specified zone within India
    // Parameters: zone - the zone name (North, South, East, West)
    // Returns: double array containing [latitude, longitude] within the specified zone
    public static double[] generateZoneBasedCoordinates(String zone){
        double baseLat,baseLng;
        double offset =2.0; // Offset for random variation around the base coordinates

        // Set base coordinates based on the specified zone
        switch (zone.toLowerCase()) {
            case "north": // Northern zone
                baseLat = 28.0;
                baseLng = 77.0;
                break;
            case "south": // Southern zone
                baseLat = 12.0;
                baseLng = 78.0;
                break;
            case "east": // Eastern zone
                baseLat = 22.0;
                baseLng = 88.0;
                break;
            case "west": // Western zone
                baseLat = 19.0;
                baseLng = 73.0;
                break;
            default: // Invalid zone - fall back to random coordinates
                return generateRandomCoordinates();
        }

        // Generate random coordinates within the specified zone with variation
        double lat = baseLat + (random.nextDouble() - 0.5) * 2 * offset;
        double lng = baseLng + (random.nextDouble() - 0.5) * 2 * offset;

        // Ensure coordinates stay within India's boundaries
        lat = Math.max(INDIA_MIN_LAT, Math.min(INDIA_MAX_LAT, lat));
        lng = Math.max(INDIA_MIN_LNG, Math.min(INDIA_MAX_LNG, lng));

        return new double[] {lat, lng};
    }

    // Main method for testing coordinate generation functionality
    public static void main(String[] args) {
        System.out.println("Random coordinates in India:");

        // Generate and display 5 random coordinates
        for (int i = 0; i < 5; i++) {
            double[] coords = generateRandomCoordinates();
            System.out.printf("Coordinate %d: Lat=%.4f, Lng=%.4f%n",
                    i+1, coords[0], coords[1]);
        }

        System.out.println("\nZone-based coordinates:");
        String[] zones = {"North", "South", "East", "West"};

        // Generate and display coordinates for each zone
        for (String zone : zones) {
            double[] coords = generateZoneBasedCoordinates(zone);
            System.out.printf("%s Zone: Lat=%.4f, Lng=%.4f%n",
                    zone, coords[0], coords[1]);
        }
    }
}
