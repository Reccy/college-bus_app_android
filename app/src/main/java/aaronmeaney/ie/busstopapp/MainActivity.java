package aaronmeaney.ie.busstopapp;

import android.graphics.Bitmap;
import android.graphics.drawable.BitmapDrawable;
import android.support.annotation.NonNull;
import android.support.design.widget.BottomSheetBehavior;
import android.support.v4.app.FragmentActivity;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.LinearLayout;

import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.SupportMapFragment;
import com.google.android.gms.maps.model.BitmapDescriptorFactory;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.Marker;
import com.google.android.gms.maps.model.MarkerOptions;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.pubnub.api.models.consumer.pubsub.PNMessageResult;

import java.util.HashMap;

import butterknife.BindView;

public class MainActivity extends FragmentActivity implements OnMapReadyCallback {

    private GoogleMap mMap;

    @BindView(R.id.bus_bottom_sheet)
    public LinearLayout layoutBottomSheet;

    @BindView(R.id.bus_bottom_sheet_close_button)
    public Button btnCloseButtomSheet;

    private BottomSheetBehavior sheetBehavior;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        butterknife.ButterKnife.bind(this);

        // Obtain the SupportMapFragment and get notified when the map is ready to be used.
        SupportMapFragment mapFragment = (SupportMapFragment) getSupportFragmentManager()
                .findFragmentById(R.id.map);
        mapFragment.getMapAsync(this);

        // Setup bottom sheet
        sheetBehavior = BottomSheetBehavior.from(layoutBottomSheet);

        btnCloseButtomSheet.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                sheetBehavior.setHideable(true);
                sheetBehavior.setState(BottomSheetBehavior.STATE_HIDDEN);
            }
        });

        sheetBehavior.setBottomSheetCallback(new BottomSheetBehavior.BottomSheetCallback() {
            @Override
            public void onStateChanged(@NonNull View bottomSheet, int newState) {
                switch (newState) {
                    case BottomSheetBehavior.STATE_HIDDEN:
                        break;
                    case BottomSheetBehavior.STATE_EXPANDED:
                        break;
                    case BottomSheetBehavior.STATE_COLLAPSED:
                        break;
                    case BottomSheetBehavior.STATE_DRAGGING:
                        break;
                    case BottomSheetBehavior.STATE_SETTLING:
                        break;
                }
            }

            @Override
            public void onSlide(@NonNull View bottomSheet, float slideOffset) {

            }
        });
    }


    /*
     * Google Maps Attributes
     */

    private HashMap<String, Marker> busMarkers;
    private HashMap<String, Double> busTimestamps;
    private HashMap<JsonElement, Marker> busStopMarkers;
    private HashMap<JsonElement, Double> busStopTimestamps;

    /**
     * Manipulates the map once available.
     */
    @Override
    public void onMapReady(GoogleMap googleMap) {
        mMap = googleMap;

        // Setup Google Map listeners
        mMap.setOnMarkerClickListener(new GoogleMap.OnMarkerClickListener() {
            @Override
            public boolean onMarkerClick(Marker marker) {
                mMap.moveCamera(CameraUpdateFactory.newLatLng(new LatLng(marker.getPosition().latitude, marker.getPosition().longitude)));
                mMap.animateCamera(CameraUpdateFactory.zoomTo(15));

                sheetBehavior.setState(BottomSheetBehavior.STATE_COLLAPSED);
                sheetBehavior.setHideable(false);

                return true;
            }
        });

        // Setup BusStopAPI
        busMarkers = new HashMap<>();
        busTimestamps = new HashMap<>();
        busStopMarkers = new HashMap<>();
        busStopTimestamps = new HashMap<>();

        final BusStopAPI busStopAPI = BusStopAPI.getInstance();

        busStopAPI.addOnInitializedListener(new BusStopAPI.BusStopAPIInitializedListener() {
            @Override
            public void onBusStopAPIInitialized() {
                busStopAPI.getBusStops();
            }
        });

        /// DEBUG MARKER REMOVE WHEN PUB NUB GOES BACK ONLINE
        Bitmap bitmap = ((BitmapDrawable)getResources().getDrawable(R.drawable.bus_marker)).getBitmap();
        bitmap = Bitmap.createScaledBitmap(bitmap, 250, 250, false);

        Marker newMarker = mMap.addMarker(new MarkerOptions()
                .position(new LatLng(53.347753, -6.242407))
                .title("Debug Bus")
                .zIndex(2f)
                .icon(BitmapDescriptorFactory.fromBitmap(bitmap)));
        /// END OF DEBUG MARKER CODE

        busStopAPI.addOnMessageReceivedListener(new BusStopAPI.BusStopAPIMessageReceivedListener() {
            @Override
            public void onBusStopAPIMessageReceived(PNMessageResult messageResult) {
                handleReceivedMessage(messageResult.getMessage().getAsJsonObject());
            }
        });

        busStopAPI.addOnReceivedBusStops(new BusStopAPI.BusStopAPIReceivedBusStopsListener() {
            @Override
            public void onBusStopAPIReceivedBusStops(JsonArray busStops) {
                handleBusStopData(busStops);
            }
        });

        busStopAPI.initialize();
    }

    /**
     * Handles the bus stop data received from the API
     */
    private void handleBusStopData(JsonArray busStops) {

        for (final Marker busStopMarker : busStopMarkers.values()) {
            busStopMarker.remove();
        }
        busStopMarkers.clear();

        for (final JsonElement busStop : busStops) {
            runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    String id = busStop.getAsJsonObject().get("id").getAsString();
                    String internalId = busStop.getAsJsonObject().get("internal_id").getAsString();
                    double latitude = busStop.getAsJsonObject().get("latitude").getAsDouble();
                    double longitude = busStop.getAsJsonObject().get("longitude").getAsDouble();

                    Bitmap bitmap = ((BitmapDrawable)getResources().getDrawable(R.drawable.bus_stop_marker)).getBitmap();
                    bitmap = Bitmap.createScaledBitmap(bitmap, 200, 200, false);

                    Marker newMarker = mMap.addMarker(new MarkerOptions()
                            .position(new LatLng(latitude, longitude))
                            .icon(BitmapDescriptorFactory.fromBitmap(bitmap))
                            .title(id));

                    busStopMarkers.put(busStop, newMarker);
                }
            });
        }
    }

    /**
     * Handles the bus data received from the API
     */
    private void handleBusData(JsonObject message) {
        final String busName = message.get("bus_name").getAsString();
        final double latitude = message.get("latitude").getAsDouble();
        final double longitude = message.get("longitude").getAsDouble();
        final double sent_at = message.get("sent_at").getAsDouble();

        runOnUiThread(new Runnable() {
            @Override
            public void run() {

                LatLng position = new LatLng(latitude, longitude);

                // Prevent bus data from being updated out of order
                if (busTimestamps.containsKey(busName)) {
                    if (sent_at < busTimestamps.get(busName)) {
                        return;
                    }
                }
                busTimestamps.put(busName, sent_at);

                // Set marker
                if (busMarkers.containsKey(busName)) {
                    busMarkers.get(busName).setPosition(position);
                } else {
                    Bitmap bitmap = ((BitmapDrawable)getResources().getDrawable(R.drawable.bus_marker)).getBitmap();
                    bitmap = Bitmap.createScaledBitmap(bitmap, 250, 250, false);

                    Marker newMarker = mMap.addMarker(new MarkerOptions()
                            .position(position)
                            .title(busName)
                            .zIndex(2f)
                            .icon(BitmapDescriptorFactory.fromBitmap(bitmap)));

                    busMarkers.put(busName, newMarker);
                }
            }
        });
    }

    /**
     * Handles the end of a bus service message
     */
    private void handleBusEndService(JsonObject message) {
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
                handleBusData(message);
                break;
            case "bus_end_service":
                handleBusEndService(message);
                break;
        }
    }
}
