package com.crossover.trial.weather.service;

import com.crossover.trial.weather.entity.Airport;
import com.crossover.trial.weather.entity.AtmosphericInformation;
import com.crossover.trial.weather.entity.DataPoint;
import com.crossover.trial.weather.entity.DataPointType;
import com.crossover.trial.weather.exception.WeatherException;
import com.google.gson.Gson;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

/**
 * Airport weather service, which provides operations with airports and weather.
 * <p>
 * Created by dmitry on 16.09.16.
 */
public class AirportWeatherService {
    private static volatile AirportWeatherService instance;

    /**
     * earth radius in KM
     */
    public static final double R = 6372.8;

    /**
     * shared gson json to object factory
     */
    public static final Gson gson = new Gson();

    /**
     * all known airports
     */
    public static List<Airport> airports = new CopyOnWriteArrayList<>();

    /**
     * atmospheric information for each airports, idx corresponds with airports
     */
    public static List<AtmosphericInformation> atmosphericInformation = new CopyOnWriteArrayList<>();

    /**
     * Internal performance counter to better understand most requested information, this map can be improved but
     * for now provides the basis for future performance optimizations. Due to the stateless deployment architecture
     * we don't want to write this to disk, but will pull it off using a REST request and aggregate with other
     * performance metrics {@link #ping()}
     */
    public static Map<Airport, Integer> requestFrequency = new ConcurrentHashMap<>();

    public static Map<Double, Integer> radiusFreq = new ConcurrentHashMap<Double, Integer>();

    public static AirportWeatherService getInstance() {
        if (instance == null) {
            synchronized (AirportWeatherService.class) {
                instance = new AirportWeatherService();
            }
        }
        return instance;
    }

    static {
        init();
    }


    /**
     * Calculates statiscics of AWS.
     *
     * @return Map of stats
     */
    public Map<String, Object> ping() {
        Map<String, Object> returnValue = new HashMap<String, Object>();

        int dataSize = 0;
        for (AtmosphericInformation ai : atmosphericInformation) {
            // we only count recent readings
            if (ai.getCloudCover() != null
                    || ai.getHumidity() != null
                    || ai.getPressure() != null
                    || ai.getPrecipitation() != null
                    || ai.getTemperature() != null
                    || ai.getWind() != null) {
                // updated in the last day
                if (ai.getLastUpdateTime() > System.currentTimeMillis() - 86400000) {
                    dataSize++;
                }
            }
        }
        returnValue.put("datasize", dataSize);

        Map<String, Double> freq = new HashMap<>();
        // fraction of queries
        for (Airport data : airports) {
            double frac = (double) requestFrequency.getOrDefault(data, 0) / requestFrequency.size();
            frac = Double.isNaN(frac) ? 0 : frac;
            freq.put(data.getIata(), frac);
        }
        returnValue.put("iata_freq", freq);

        int m = radiusFreq.keySet().stream()
                .max(Double::compare)
                .orElse(1000.0).intValue() + 1;

        int[] hist = new int[m];
        for (Map.Entry<Double, Integer> e : radiusFreq.entrySet()) {
            int i = e.getKey().intValue();
            hist[i] += e.getValue();
        }
        returnValue.put("radius_freq", hist);
        return returnValue;
    }

    /**
     * Finds all atmospheric information near the Airport(iataCode) within a radius(radiusString).
     *
     * @param iataCode
     * @param radiusString
     * @return List of AtmosphericInformation
     */
    public List<AtmosphericInformation> getWeather(String iataCode, String radiusString) {
        double radius = radiusString == null || radiusString.trim().isEmpty() ? 0 : Double.valueOf(radiusString);
        updateRequestFrequency(iataCode, radius);

        List<AtmosphericInformation> returnValue = new ArrayList<>();
        if (radius == 0) {
            int idx = getAirportDataIdx(iataCode);
            returnValue.add(atmosphericInformation.get(idx));
        } else {
            Airport ad = findAirportData(iataCode);
            for (int i = 0; i < airports.size(); i++) {
                if (calculateDistance(ad, airports.get(i)) <= radius) {
                    AtmosphericInformation ai = atmosphericInformation.get(i);
                    if (ai.getCloudCover() != null || ai.getHumidity() != null || ai.getPrecipitation() != null
                            || ai.getPressure() != null || ai.getTemperature() != null || ai.getWind() != null) {
                        returnValue.add(ai);
                    }
                }
            }
        }
        return returnValue;
    }

    /**
     * Records information about how often requests are made
     *
     * @param iata   an iata code
     * @param radius query radius
     */
    public void updateRequestFrequency(String iata, Double radius) {
        Airport airport = findAirportData(iata);
        requestFrequency.put(airport, requestFrequency.getOrDefault(airport, 0) + 1);
        radiusFreq.put(radius, radiusFreq.getOrDefault(radius, 0) + 1);
    }

    /**
     * Given an iataCode find the airports data
     *
     * @param iataCode as a string
     * @return airports data or null if not found
     */
    public static Airport findAirportData(String iataCode) {
        return airports.stream()
                .filter(ap -> ap.getIata().equals(iataCode))
                .findFirst().orElse(null);
    }

    /**
     * Given an iataCode find the airports data
     *
     * @param iataCode as a string
     * @return airports data or null if not found
     */
    public static int getAirportDataIdx(String iataCode) {
        Airport ad = findAirportData(iataCode);
        return airports.indexOf(ad);
    }

    /**
     * Haversine distance between two airports.
     *
     * @param ad1 airports 1
     * @param ad2 airports 2
     * @return the distance in KM
     */
    public double calculateDistance(Airport ad1, Airport ad2) {
        double deltaLat = Math.toRadians(ad2.getLatitude() - ad1.getLatitude());
        double deltaLon = Math.toRadians(ad2.getLongitude() - ad1.getLongitude());
        double a = Math.pow(Math.sin(deltaLat / 2), 2) + Math.pow(Math.sin(deltaLon / 2), 2)
                * Math.cos(ad1.getLatitude()) * Math.cos(ad2.getLatitude());
        double c = 2 * Math.asin(Math.sqrt(a));
        return R * c;
    }

    /**
     * Update the airports weather data with the collected data.
     *
     * @param iataCode  the 3 letter IATA code
     * @param pointType the point type {@link DataPointType}
     * @param dp        a datapoint object holding pointType data
     * @throws WeatherException if the update can not be completed
     */
    public void addDataPoint(String iataCode, String pointType, DataPoint dp) throws WeatherException {
        int airportDataIdx = getAirportDataIdx(iataCode);
        AtmosphericInformation ai = atmosphericInformation.get(airportDataIdx);
        updateAtmosphericInformation(ai, pointType, dp);
    }

    /**
     * update atmospheric information with the given data point for the given point type
     *
     * @param ai        the atmospheric information object to update
     * @param pointType the data point type as a string
     * @param dp        the actual data point
     */
    public void updateAtmosphericInformation(AtmosphericInformation ai, String pointType, DataPoint dp) throws WeatherException {
        final DataPointType dataPointType = DataPointType.valueOf(pointType.toUpperCase());
        if (dataPointType != null) {
            if (dataPointType.isValid(dp)) {
                ai.updateInfo(dataPointType, dp);
                return;
            }
        }
        throw new WeatherException("couldn't update atmospheric data");
    }

    /**
     * Add a new known airports to our list.
     *
     * @param iataCode  3 letter code
     * @param latitude  in degrees
     * @param longitude in degrees
     * @return the added airports
     */
    public static Airport saveAirport(String iataCode, double latitude, double longitude) {
        Airport ad = new Airport(iataCode, latitude, longitude);
        AtmosphericInformation ai = new AtmosphericInformation();
        Lock lock = new ReentrantLock();
        lock.lock();
        try {
            airports.add(ad);
            atmosphericInformation.add(ai);
        } finally {
            lock.unlock();
        }
        return ad;
    }

    /**
     * A dummy init method that loads hard coded data
     */
    public static void init() {
        airports.clear();
        atmosphericInformation.clear();
        requestFrequency.clear();


        saveAirport("BOS", 42.364347, -71.005181);
        saveAirport("EWR", 40.6925, -74.168667);
        saveAirport("JFK", 40.639751, -73.778925);
        saveAirport("LGA", 40.777245, -73.872608);
        saveAirport("MMU", 40.79935, -74.4148747);
    }

}
