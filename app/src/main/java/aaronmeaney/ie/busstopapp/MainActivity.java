package aaronmeaney.ie.busstopapp;

import android.graphics.Bitmap;
import android.graphics.drawable.BitmapDrawable;
import android.support.annotation.NonNull;
import android.support.design.widget.BottomSheetBehavior;
import android.support.v4.app.FragmentActivity;
import android.os.Bundle;
import android.support.v7.widget.DefaultItemAnimator;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.view.MotionEvent;
import android.view.View;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.TextView;

import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.SupportMapFragment;
import com.google.android.gms.maps.model.BitmapDescriptorFactory;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.Marker;
import com.google.android.gms.maps.model.MarkerOptions;
import com.google.common.collect.BiMap;
import com.google.common.collect.HashBiMap;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.pubnub.api.models.consumer.pubsub.PNMessageResult;

import java.util.ArrayList;
import java.util.HashMap;

import butterknife.BindView;

public class MainActivity extends FragmentActivity implements OnMapReadyCallback {

    private GoogleMap mMap;

    @BindView(R.id.bottom_sheet)
    public LinearLayout bottomSheetLayout;

    @BindView(R.id.bottom_sheet_close_btn)
    public Button bottomSheetCloseBtn;

    @BindView(R.id.shadow)
    public View bottomSheetShadow;

    @BindView(R.id.bottom_sheet_list)
    public RecyclerView layoutBottomSheetList;

    @BindView(R.id.bottom_title)
    public TextView bottomTitle;

    @BindView(R.id.bottom_subtitle)
    public TextView bottomSubtitle;

    private BottomSheetBehavior bottomSheet;

    private BusStopAdapter busStopAdapter;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        butterknife.ButterKnife.bind(this);

        // Obtain the SupportMapFragment and get notified when the map is ready to be used.
        SupportMapFragment mapFragment = (SupportMapFragment) getSupportFragmentManager()
                .findFragmentById(R.id.map);
        mapFragment.getMapAsync(this);

        setupBottomSheet();
    }

    //region Setup Bottom Sheet
    private void setupBottomSheet()
    {
        // Setup bottom sheet
        bottomSheet = BottomSheetBehavior.from(bottomSheetLayout);

        bottomTitle.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (bottomSheet.getState() == BottomSheetBehavior.STATE_COLLAPSED) {
                    bottomSheet.setState(BottomSheetBehavior.STATE_EXPANDED);
                }

                if (bottomSheet.getState() == BottomSheetBehavior.STATE_EXPANDED) {
                    bottomSheet.setState(BottomSheetBehavior.STATE_COLLAPSED);
                }
            }
        });

        bottomSheetCloseBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                bottomSheet.setHideable(true);
                bottomSheet.setState(BottomSheetBehavior.STATE_HIDDEN);
                bottomSheetShadow.setVisibility(View.GONE);
            }
        });

        bottomSheet.setBottomSheetCallback(new BottomSheetBehavior.BottomSheetCallback() {
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
            public void onSlide(@NonNull View bottomSheet, float slideOffset) {}
        });

        bottomSheet.setState(BottomSheetBehavior.STATE_HIDDEN);
        bottomSheetShadow.setVisibility(View.GONE);

        // Fix to prevent ListView and BottomSheet from interfering with touch events
        // Source: https://stackoverflow.com/a/46128956
        layoutBottomSheetList.setOnTouchListener(new View.OnTouchListener() {
            @Override
            public boolean onTouch(View v, MotionEvent event) {
                int action = event.getAction();
                switch (action) {
                    case MotionEvent.ACTION_DOWN:
                        // Disallow NestedScrollView to intercept touch events.
                        v.getParent().requestDisallowInterceptTouchEvent(true);
                        break;

                    case MotionEvent.ACTION_UP:
                        // Allow NestedScrollView to intercept touch events.
                        v.getParent().requestDisallowInterceptTouchEvent(false);
                        break;
                }

                // Handle ListView touch events.
                v.onTouchEvent(event);
                return true;
            }
        });
    }
    //endregion

    //region Google Maps
    /*
     * Google Maps Attributes
     */
    private BiMap<BusStop, Marker> busStopMarkers;
    private BiMap<Bus, Marker> busMarkers;

    /**
     * Manipulates the map once available.
     */
    @Override
    public void onMapReady(GoogleMap googleMap) {
        mMap = googleMap;

        // Set the map position over Dublin
        mMap.moveCamera(CameraUpdateFactory.newLatLngZoom(new LatLng(53.410562, -6.227770), 10));

        // Setup Google Map listeners
        mMap.setOnMarkerClickListener(new GoogleMap.OnMarkerClickListener() {
            @Override
            public boolean onMarkerClick(Marker marker) {

                if (busMarkers.containsValue(marker)) {
                    handleBusMarkerSelected(busMarkers.inverse().get(marker), marker);

                }

                if (busStopMarkers.containsValue(marker)) {
                    handleBusStopMarkerSelected(busStopMarkers.inverse().get(marker), marker);
                }

                mMap.moveCamera(CameraUpdateFactory.newLatLng(new LatLng(marker.getPosition().latitude, marker.getPosition().longitude)));
                mMap.animateCamera(CameraUpdateFactory.zoomTo(15));

                final float scale = getApplicationContext().getResources().getDisplayMetrics().density;
                int pixels = (int) (80 * scale + 0.5f);
                bottomSheet.setPeekHeight(pixels);
                bottomSheet.setState(BottomSheetBehavior.STATE_COLLAPSED);
                bottomSheet.setHideable(false);
                bottomSheetShadow.setVisibility(View.VISIBLE);

                return true;
            }
        });

        // Setup BusStopAPI
        busMarkers = HashBiMap.create();
        busStopMarkers = HashBiMap.create();

        final BusStopAPI busStopAPI = BusStopAPI.getInstance();

        busStopAPI.addOnInitializedListener(new BusStopAPI.BusStopAPIInitializedListener() {
            @Override
            public void onBusStopAPIInitialized() {
                busStopAPI.getBusStops();
            }
        });

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

    private void handleBusMarkerSelected(Bus bus, Marker marker) {
        bottomTitle.setText(bus.getName());
        bottomSubtitle.setVisibility(View.VISIBLE);
    }

    private void handleBusStopMarkerSelected(BusStop busStop, Marker marker) {
        bottomTitle.setText(busStop.getId());
        bottomSubtitle.setVisibility(View.INVISIBLE);
    }

    /**
     * Handles the bus stop data received from the API
     */
    private void handleBusStopData(JsonArray busStops) {

        for (final Marker busStopMarker : busStopMarkers.values()) {
            busStopMarker.remove();
        }
        busStopMarkers.clear();

        for (final JsonElement busStopJson : busStops) {
            runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    String id = busStopJson.getAsJsonObject().get("id").getAsString();
                    String internalId = busStopJson.getAsJsonObject().get("internal_id").getAsString();
                    double latitude = busStopJson.getAsJsonObject().get("latitude").getAsDouble();
                    double longitude = busStopJson.getAsJsonObject().get("longitude").getAsDouble();

                    BusStop busStop = new BusStop(id, internalId, latitude, longitude);

                    Bitmap bitmap = ((BitmapDrawable)getResources().getDrawable(R.drawable.bus_stop_marker)).getBitmap();
                    bitmap = Bitmap.createScaledBitmap(bitmap, 150, 150, false);

                    Marker newMarker = mMap.addMarker(new MarkerOptions()
                            .position(new LatLng(latitude, longitude))
                            .icon(BitmapDescriptorFactory.fromBitmap(bitmap))
                            .title(id));

                    busStopMarkers.put(busStop, newMarker);
                }
            });
        }
    }
    //endregion

    //region API Handlers
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

                Bus bus = new Bus(busName, latitude, longitude, sent_at);
                LatLng position = new LatLng(bus.getLatitude(), bus.getLongitude());

                // Set the current bus
                for (Bus b : busMarkers.inverse().values()) {
                    if (busName.equals(b.getName())) {
                        bus = b;
                        if (sent_at < b.getTimestamp()) {
                            return;
                        } else {
                            break;
                        }
                    }
                }

                // Set marker
                if (busMarkers.containsKey(bus)) {
                    System.out.println("Updating bus " + bus.getName() + " position.");
                    busMarkers.get(bus).setPosition(position);
                } else {
                    System.out.println("Creating new bus marker for " + bus.getName());
                    Bitmap bitmap = ((BitmapDrawable)getResources().getDrawable(R.drawable.bus_marker)).getBitmap();
                    bitmap = Bitmap.createScaledBitmap(bitmap, 250, 250, false);

                    Marker newMarker = mMap.addMarker(new MarkerOptions()
                            .position(position)
                            .title(busName)
                            .zIndex(2f)
                            .icon(BitmapDescriptorFactory.fromBitmap(bitmap)));

                    busMarkers.forcePut(bus, newMarker);
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

        Bus busTemp = null;

        for (Bus b : busMarkers.inverse().values()) {
            if (b.getName().equals(busName)) {
                busTemp = b;
            } else {
                return;
            }
        }

        final Bus bus = busTemp;

        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                if (busMarkers.containsKey(bus)) {
                    System.out.println("Removing marker...");

                    busMarkers.get(bus).remove();
                    busMarkers.remove(bus);
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
    //endregion
}
