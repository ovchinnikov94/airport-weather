package com.crossover.trial.weather.endpoint;

import com.crossover.trial.weather.exception.WeatherException;
import com.crossover.trial.weather.entity.Airport;
import com.crossover.trial.weather.entity.AtmosphericInformation;
import com.crossover.trial.weather.entity.DataPoint;
import com.crossover.trial.weather.entity.DataPointType;
import com.crossover.trial.weather.service.AirportWeatherService;
import com.google.gson.Gson;

import javax.ws.rs.Path;
import javax.ws.rs.core.Response;
import java.util.Set;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;
import java.util.logging.Logger;
import java.util.stream.Collectors;

/**
 * A REST implementation of the WeatherCollector API. Accessible only to airports weather collection
 * sites via secure VPN.
 *
 * @author code test administrator
 */

@Path("/collect")
public class WeatherCollectorEndpointImpl implements WeatherCollectorEndpoint {
    public final static Logger LOGGER = Logger.getLogger(WeatherCollectorEndpointImpl.class.getName());

    private static final AirportWeatherService service = AirportWeatherService.getInstance();

    private static final Gson gson = new Gson();

    @Override
    public Response ping() {
        return Response.status(Response.Status.OK).entity("ready").build();
    }

    @Override
    public Response updateWeather(String iataCode,
                                  String pointType,
                                  String datapointJson) {
        try {
            service.addDataPoint(iataCode, pointType, gson.fromJson(datapointJson, DataPoint.class));
        } catch (WeatherException e) {
            e.printStackTrace();
        }
        return Response.status(Response.Status.OK).build();
    }


    @Override
    public Response getAirports() {
        Set<String> returnValue = service.airports.stream().map(Airport::getIata).collect(Collectors.toSet());
        return Response.status(Response.Status.OK).entity(returnValue).build();
    }


    @Override
    public Response getAirport(String iataCode) {
        Airport ad = service.findAirportData(iataCode);
        return Response.status(Response.Status.OK).entity(ad).build();
    }

    @Override
    public Response addAirport(String iataCode, String latString, String longString) {
        service.saveAirport(iataCode, Double.valueOf(latString), Double.valueOf(longString));
        return Response.status(Response.Status.OK).build();
    }

    @Override
    public Response deleteAirport(String iataCode) {
        int idx = service.getAirportDataIdx(iataCode);
        service.airports.remove(service.airports.get(idx));
        service.atmosphericInformation.remove(service.atmosphericInformation.get(idx));
        return Response.status(Response.Status.OK).build();
    }

    @Override
    public Response exit() {
        System.exit(0);
        return Response.noContent().build();
    }
}
