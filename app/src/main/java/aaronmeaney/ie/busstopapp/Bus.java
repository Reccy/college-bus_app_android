package aaronmeaney.ie.busstopapp;

public class Bus {
    private String name;
    private double latitude;
    private double longitude;
    private String registrationNumber;
    private String model;
    private String companyName;
    private BusRoute currentRoute;
    private BusStop currentStop;
    private int currentCapacity;
    private int maximumCapacity;
    private double timestamp;

    public Bus(String name, double latitude, double longitude, String registrationNumber,
               String model, String companyName, BusRoute currentRoute, BusStop currentStop,
               int currentCapacity, int maximumCapacity, double timestamp) {
        this.name = name;
        this.latitude = latitude;
        this.longitude = longitude;
        this.registrationNumber = registrationNumber;
        this.model = model;
        this.companyName = companyName;
        this.currentRoute = currentRoute;
        this.currentStop = currentStop;
        this.currentCapacity = currentCapacity;
        this.maximumCapacity = maximumCapacity;
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

    public String getRegistrationNumber() {
        return registrationNumber;
    }

    public String getModel() {
        return model;
    }

    public String getCompanyName() {
        return companyName;
    }

    public BusRoute getCurrentRoute() {
        return currentRoute;
    }

    public void setCurrentStop(BusStop currentStop) {
        this.currentStop = currentStop;
    }

    public BusStop getCurrentStop() {
        return currentStop;
    }

    public int getCurrentCapacity() {
        return currentCapacity;
    }

    public void setCurrentCapacity(int currentCapacity) {
        this.currentCapacity = currentCapacity;
    }

    public int getMaximumCapacity() {
        return maximumCapacity;
    }

    public void setTimestamp(double timestamp) {
        this.timestamp = timestamp;
    }

    public double getTimestamp() {
        return timestamp;
    }
}
