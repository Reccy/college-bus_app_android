package aaronmeaney.ie.busstopapp;

import org.junit.Test;
import java.util.ArrayList;
import java.util.List;

import static junit.framework.Assert.assertEquals;

public class BusStopUnitTests {

    @Test
    public void objectModel_isValid() {
        // Test to ensure that the object model is valid

        List<BusStop> busStopList = new ArrayList<>();
        busStopList.add(new BusStop("Yulin", "c_Yulin", 22.633333, 110.15));
        busStopList.add(new BusStop("Maoming West", "c_Maoming West", 21.65, 110.916667));

        BusRoute r = new BusRoute("100", "y_100", busStopList);
        List<TimeSlot> timeslots = new ArrayList<>();
        timeslots.add(new TimeSlot(busStopList.get(0), "0"));
        timeslots.add(new TimeSlot(busStopList.get(1), "5"));

        Bus b = new Bus("Guangxi Provincial Bus", 0, 0,
                "01-00-0000", "Guangdong Speedster",
                "China Transport Ltd", r, r.getBusStops().get(0), new ArrayList<BusStop>(),
                timeslots, 20, 30, 0);

        assertEquals(b.getCurrentRoute(), r);
        assertEquals(b.getCurrentCapacity() <= b.getMaximumCapacity(), true);
        assertEquals(b.getCurrentStop(), r.getBusStops().get(0));
        assertEquals(b.getCurrentStop().getInternalId(), "c_Yulin");
    }
}
