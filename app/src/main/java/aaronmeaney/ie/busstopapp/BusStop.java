package aaronmeaney.ie.busstopapp;

public class BusStop {
    private String id;
    private String internalId;
    private double latitude;
    private double longitude;

    public BusStop(String id, String internalId, double latitude, double longitude) {
        this.id = id;
        this.internalId = internalId;
        this.latitude = latitude;
        this.longitude = longitude;
    }

    public String getId() {
        return id;
    }

    public String getInternalId() {
        return internalId;
    }

    public double getLatitude() {
        return latitude;
    }

    public double getLongitude() {
        return longitude;
    }
}
