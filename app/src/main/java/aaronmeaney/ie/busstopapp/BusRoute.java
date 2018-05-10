package aaronmeaney.ie.busstopapp;

import com.google.android.gms.maps.model.LatLng;

import java.util.Arrays;
import java.util.List;

public class BusRoute {
    private String id;
    private String idInternal;
    private List<BusStop> busStops;
    private List<LatLng> waypoints;

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

    public List<LatLng> getWaypoints() {
        return waypoints;
    }

    public void setWaypoints(List<LatLng> waypoints) {
        this.waypoints = waypoints;
    }

    @Override
    public String toString() {
        return "BusRoute{" +
                "id='" + id + '\'' +
                ", idInternal='" + idInternal + '\'' +
                ", busStops=" + Arrays.toString(busStops.toArray()) +
                '}';
    }
}
