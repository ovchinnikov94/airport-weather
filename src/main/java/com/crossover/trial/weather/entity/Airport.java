package com.crossover.trial.weather.entity;

import org.apache.commons.lang3.builder.ReflectionToStringBuilder;
import org.apache.commons.lang3.builder.ToStringStyle;

/**
 * Basic airports information.
 *
 * @author code test administrator
 */
public class Airport {

    /**
     * the three letter IATA code
     */
    private final String iata;

    /**
     * latitude value in degrees
     */
    private final double latitude;

    /**
     * longitude value in degrees
     */
    private final double longitude;

    public Airport(String iata, double latitude, double longitude) {
        this.iata = iata;
        this.latitude = latitude;
        this.longitude = longitude;
    }

    public String getIata() {
        return iata;
    }

    public double getLatitude() {
        return latitude;
    }

    public double getLongitude() {
        return longitude;
    }


    public String toString() {
        return ReflectionToStringBuilder.toString(this, ToStringStyle.NO_CLASS_NAME_STYLE);
    }

    public boolean equals(Object other) {
        if (other instanceof Airport) {
            return ((Airport) other).getIata().equals(this.getIata());
        }

        return false;
    }
}
