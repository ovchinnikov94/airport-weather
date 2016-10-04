package com.crossover.trial.weather.entity;

import com.crossover.trial.weather.entity.DataPoint;

/**
 * The various types of data points we can collect.
 *
 * @author code test administrator
 */
public enum DataPointType {
    WIND {
        @Override
        public boolean isValid(DataPoint dataPoint) {
            return dataPoint.getMean() >= 0;
        }
    },
    TEMPERATURE {
        @Override
        public boolean isValid(DataPoint dataPoint) {
            return dataPoint.getMean() >= -50 && dataPoint.getMean() < 100;
        }
    },
    HUMIDITY {
        @Override
        public boolean isValid(DataPoint dataPoint) {
            return dataPoint.getMean() >= 0 && dataPoint.getMean() < 100;
        }
    },
    PRESSURE {
        @Override
        public boolean isValid(DataPoint dataPoint) {
            return dataPoint.getMean() >= 650 && dataPoint.getMean() < 800;
        }
    },
    CLOUDCOVER {
        @Override
        public boolean isValid(DataPoint dataPoint) {
            return dataPoint.getMean() >= 0 && dataPoint.getMean() < 100;
        }
    },
    PRECIPITATION {
        @Override
        public boolean isValid(DataPoint dataPoint) {
            return dataPoint.getMean() >= 0 && dataPoint.getMean() < 100;
        }
    };

    public abstract boolean isValid(DataPoint dataPoint);
}
