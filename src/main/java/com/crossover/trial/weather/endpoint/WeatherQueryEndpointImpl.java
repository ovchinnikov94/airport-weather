package com.crossover.trial.weather.endpoint;

import com.crossover.trial.weather.entity.Airport;
import com.crossover.trial.weather.entity.AtmosphericInformation;
import com.crossover.trial.weather.service.AirportWeatherService;
import com.google.gson.Gson;

import javax.ws.rs.Path;
import javax.ws.rs.core.Response;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.logging.Logger;

/**
 * The Weather App REST endpoint allows clients to query, update and check health stats. Currently, all data is
 * held in memory. The end point deploys to a single container
 *
 * @author code test administrator
 */
@Path("/query")
public class WeatherQueryEndpointImpl implements WeatherQueryEndpoint {

    public final static Logger LOGGER = Logger.getLogger("WeatherQuery");

    /**
     * shared gson json to object factory
     */
    public static final Gson gson = new Gson();

    protected static final AirportWeatherService service = AirportWeatherService.getInstance();

    /**
     * Retrieve service health including total size of valid data points and request frequency information.
     *
     * @return health stats for the service as a string
     */
    @Override
    public String ping() {
        return gson.toJson(service.ping());
    }

    /**
     * Given a query in json format {'iata': CODE, 'radius': km} extracts the requested airports information and
     * return a list of matching atmosphere information.
     *
     * @param iata         the iataCode
     * @param radiusString the radius in km
     * @return a list of atmospheric information
     */
    @Override
    public Response weather(String iata, String radiusString) {
        return Response.status(Response.Status.OK).entity(service.getWeather(iata, radiusString)).build();
    }

}
