package aaronmeaney.ie.busstopapp;

public class TimeSlot {
    private BusStop busStop;
    private String time;

    public TimeSlot(BusStop busStop, String time) {
        this.busStop = busStop;
        this.time = time;
    }

    public BusStop getBusStop() {
        return busStop;
    }

    public String getTime() {
        return time;
    }
}
