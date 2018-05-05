package aaronmeaney.ie.busstopapp;

import java.util.List;

public class BusRoute {
    private String id;
    private String idInternal;
    private List<BusStop> busStops;

    public BusRoute(String id, String idInternal, List<BusStop> busStops) {
        this.id = id;
        this.idInternal = idInternal;
        this.busStops = busStops;
    }

    public String getId() {
        return id;
    }

    public String getIdInternal() {
        return idInternal;
    }

    public List<BusStop> getBusStops() {
        return busStops;
    }
}
