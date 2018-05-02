package aaronmeaney.ie.busstopapp;

import android.support.v4.app.FragmentActivity;
import android.os.Bundle;

import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.SupportMapFragment;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.Marker;
import com.google.android.gms.maps.model.MarkerOptions;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.pubnub.api.models.consumer.pubsub.PNMessageResult;

import org.json.JSONObject;

import java.util.HashMap;

public class MainActivity extends FragmentActivity implements OnMapReadyCallback {

    private GoogleMap mMap;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        // Obtain the SupportMapFragment and get notified when the map is ready to be used.
        SupportMapFragment mapFragment = (SupportMapFragment) getSupportFragmentManager()
                .findFragmentById(R.id.map);
        mapFragment.getMapAsync(this);
    }


    /*
     * Google Maps Attributes
     */

    private HashMap<String, Marker> busMarkers;

    /**
     * Manipulates the map once available.
     */
    @Override
    public void onMapReady(GoogleMap googleMap) {
        mMap = googleMap;

        busMarkers = new HashMap<>();

        BusStopAPI busStopAPI = BusStopAPI.getInstance();

        busStopAPI.addOnMessageReceivedListener(new BusStopAPI.BusStopAPIMessageReceivedListener() {
            @Override
            public void onBusStopAPIMessageReceived(PNMessageResult messageResult) {
                handleReceivedMessage(messageResult.getMessage().getAsJsonObject());
            }
        });

        busStopAPI.initialize();
    }

    /**
     * Handles the bus data received from the API
     */
    private void HandleBusData(JsonObject message) {
        final String busName = message.get("bus_name").getAsString();
        final double latitude = message.get("latitude").getAsDouble();
        final double longitude = message.get("longitude").getAsDouble();

        runOnUiThread(new Runnable() {
            @Override
            public void run() {

                LatLng position = new LatLng(latitude, longitude);

                if (busMarkers.containsKey(busName)) {
                    busMarkers.get(busName).setPosition(position);
                } else {
                    Marker newMarker = mMap.addMarker(new MarkerOptions()
                            .position(position)
                            .title(busName));

                    busMarkers.put(busName, newMarker);
                }
            }
        });
    }

    /**
     * Handles the end of a bus service message
     */
    private void HandleBusEndService(JsonObject message) {
        final String busName = message.get("bus_name").getAsString();

        System.out.println("Bus " + busName + " ended its service.");

        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                if (busMarkers.containsKey(busName)) {
                    System.out.println("Removing marker...");
                    
                    busMarkers.get(busName).remove();
                    busMarkers.remove(busName);
                }
            }
        });
    }

    private void handleReceivedMessage(JsonObject message) {
        switch (message.get("topic").getAsString()) {
            case "bus_data":
                HandleBusData(message);
                break;
            case "bus_end_service":
                HandleBusEndService(message);
                break;
        }
    }
}
