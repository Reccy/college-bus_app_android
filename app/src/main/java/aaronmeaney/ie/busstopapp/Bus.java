package aaronmeaney.ie.busstopapp;

public class Bus {
    private String name;
    private double latitude;
    private double longitude;
    private double timestamp;

    public Bus(String name, double latitude, double longitude, double timestamp) {
        this.name = name;
        this.latitude = latitude;
        this.longitude = longitude;
        this.timestamp = timestamp;
    }

    public String getName() {
        return name;
    }

    public double getLatitude() {
        return latitude;
    }

    public void setLatitude(double latitude) {
        this.latitude = latitude;
    }

    public double getLongitude() {
        return longitude;
    }

    public void setLongitude(double longitude) {
        this.longitude = longitude;
    }

    public double getTimestamp() {
        return timestamp;
    }
}
