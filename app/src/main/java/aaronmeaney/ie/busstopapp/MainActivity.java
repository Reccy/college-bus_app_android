package aaronmeaney.ie.busstopapp;

import android.graphics.Bitmap;
import android.graphics.Color;
import android.graphics.drawable.BitmapDrawable;
import android.support.annotation.NonNull;
import android.support.design.widget.BottomSheetBehavior;
import android.support.v4.app.FragmentActivity;
import android.os.Bundle;
import android.support.v7.widget.DefaultItemAnimator;
import android.support.v7.widget.DividerItemDecoration;
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
import com.google.android.gms.maps.model.Polyline;
import com.google.android.gms.maps.model.PolylineOptions;
import com.google.common.collect.BiMap;
import com.google.common.collect.HashBiMap;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.pubnub.api.models.consumer.pubsub.PNMessageResult;

import java.util.ArrayList;
import java.util.List;

import butterknife.BindView;

public class MainActivity extends FragmentActivity implements OnMapReadyCallback, GoogleMap.OnMarkerClickListener, AdapterHailCallback, AdapterViewBusStopCallback {

    private GoogleMap mMap;
    private BusStopAPI busStopAPI;

    @BindView(R.id.bottom_sheet)
    public LinearLayout bottomSheetLayout;

    @BindView(R.id.bottom_sheet_location_btn)
    public Button bottomSheetLocationBtn;

    @BindView(R.id.bottom_sheet_more_info)
    public Button bottomSheetMoreInfoBtn;

    @BindView(R.id.shadow)
    public View bottomSheetShadow;

    @BindView(R.id.bottom_sheet_list)
    public RecyclerView layoutBottomSheetList;

    @BindView(R.id.bottom_sheet_info)
    public LinearLayout layoutBottomSheetInfo;

    @BindView(R.id.bottom_title)
    public TextView bottomTitle;

    @BindView(R.id.bottom_subtitle)
    public TextView bottomSubtitle;

    @BindView(R.id.bus_registration_number)
    public TextView busRegistrationNumberText;

    @BindView(R.id.bus_model)
    public TextView busModelText;

    @BindView(R.id.bus_company)
    public TextView busCompanyText;

    @BindView(R.id.bus_latitude)
    public TextView busLatitude;

    @BindView(R.id.bus_longitude)
    public TextView busLongitude;

    private BottomSheetBehavior bottomSheet;

    private TimeSlotAdapter timeSlotAdapter;

    private Polyline busRouteLine;

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

        // Disables drag on bottom sheet
        bottomSheet.setBottomSheetCallback(new BottomSheetBehavior.BottomSheetCallback() {
            @Override
            public void onStateChanged(@NonNull View bottomSheetView, int newState) {
                if (newState == BottomSheetBehavior.STATE_DRAGGING) {
                    bottomSheet.setState(BottomSheetBehavior.STATE_COLLAPSED);
                }
            }

            @Override
            public void onSlide(@NonNull View bottomSheet, float slideOffset) {}
        });

        // Hides and shows the bottom sheet on click
        bottomTitle.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (selectedBus == null)
                    return;

                if (bottomSheet.getState() == BottomSheetBehavior.STATE_COLLAPSED) {
                    bottomSheet.setState(BottomSheetBehavior.STATE_EXPANDED);
                }

                if (bottomSheet.getState() == BottomSheetBehavior.STATE_EXPANDED) {
                    bottomSheet.setState(BottomSheetBehavior.STATE_COLLAPSED);
                }
            }
        });

        // Centers the camera on the highlighted marker on click
        bottomSheetLocationBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (selectedBus != null) {
                    Marker marker = busMarkers.get(selectedBus);
                    mMap.moveCamera(CameraUpdateFactory.newLatLng(new LatLng(marker.getPosition().latitude, marker.getPosition().longitude)));
                    mMap.animateCamera(CameraUpdateFactory.zoomTo(Math.max(15, mMap.getCameraPosition().zoom)));
                } else if (selectedBusStop != null) {
                    Marker marker = busStopMarkers.get(selectedBusStop);
                    mMap.moveCamera(CameraUpdateFactory.newLatLng(new LatLng(marker.getPosition().latitude, marker.getPosition().longitude)));
                    mMap.animateCamera(CameraUpdateFactory.zoomTo(Math.max(15, mMap.getCameraPosition().zoom)));
                }
            }
        });

        // Changes the info panel on click
        bottomSheetMoreInfoBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (selectedBus != null)
                {
                    if (layoutBottomSheetInfo.getVisibility() == View.GONE) {
                        layoutBottomSheetInfo.setVisibility(View.VISIBLE);
                        layoutBottomSheetList.setVisibility(View.GONE);
                    } else {
                        layoutBottomSheetInfo.setVisibility(View.GONE);
                        layoutBottomSheetList.setVisibility(View.VISIBLE);
                    }
                }
            }
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
    private ArrayList<BusRoute> busRoutes;
    private Bus selectedBus;
    private BusStop selectedBusStop;

    /**
     * Manipulates the map once available.
     */
    @Override
    public void onMapReady(GoogleMap googleMap) {
        mMap = googleMap;

        // Set the map position over Dublin
        mMap.moveCamera(CameraUpdateFactory.newLatLngZoom(new LatLng(53.410562, -6.227770), 10));

        // Setup Google Map listeners
        mMap.setOnMarkerClickListener(this);

        // Setup route polyline
        PolylineOptions polylineOptions = new PolylineOptions();
        polylineOptions.color(Color.RED);
        polylineOptions.width(10);
        busRouteLine = mMap.addPolyline(polylineOptions);

        // Setup recycler view
        layoutBottomSheetList.addItemDecoration(new DividerItemDecoration(getApplicationContext(), LinearLayoutManager.VERTICAL));
        RecyclerView.LayoutManager layoutManager = new LinearLayoutManager(getApplicationContext());
        layoutBottomSheetList.setLayoutManager(layoutManager);
        layoutBottomSheetList.setItemAnimator(new DefaultItemAnimator());

        // Setup BusStopAPI
        busMarkers = HashBiMap.create();
        busStopMarkers = HashBiMap.create();
        busRoutes = new ArrayList<>();
        selectedBus = null;

        busStopAPI = BusStopAPI.getInstance();

        busStopAPI.addOnInitializedListener(new BusStopAPI.BusStopAPIInitializedListener() {
            @Override
            public void onBusStopAPIInitialized() {
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
                        busStopAPI.getBusRoutes();
                    }
                });

                busStopAPI.addOnReceivedBusRoutes(new BusStopAPI.BusStopAPIReceivedBusRoutesListener() {
                    @Override
                    public void onBusStopAPIReceivedBusRoutes(JsonArray busRoutes) {
                        handleBusRouteData(busRoutes);
                    }
                });

                busStopAPI.addOnReceivedRouteWaypoints(new BusStopAPI.BusStopAPIReceivedRouteWaypointsListener() {
                    @Override
                    public void onBusStopAPIRecievedRouteWaypoints(BusRoute route, List<LatLng> routeWaypoints) {
                        handleRouteWaypointsData(route, routeWaypoints);
                    }
                });

                busStopAPI.getBusStops();
            }
        });

        busStopAPI.initialize();
    }

    @Override
    public boolean onMarkerClick(Marker marker) {
        if (busMarkers.containsValue(marker)) {
            handleBusMarkerSelected(busMarkers.inverse().get(marker), marker);
        }

        if (busStopMarkers.containsValue(marker)) {
            handleBusStopMarkerSelected(busStopMarkers.inverse().get(marker), marker);
        }

        mMap.moveCamera(CameraUpdateFactory.newLatLng(new LatLng(marker.getPosition().latitude, marker.getPosition().longitude)));
        mMap.animateCamera(CameraUpdateFactory.zoomTo(Math.max(15, mMap.getCameraPosition().zoom)));

        final float scale = getApplicationContext().getResources().getDisplayMetrics().density;
        int pixels = (int) (80 * scale + 0.5f);
        bottomSheet.setPeekHeight(pixels);
        bottomSheet.setHideable(false);

        if (busMarkers.containsValue(marker)) {
            if (bottomSheet.getState() != BottomSheetBehavior.STATE_EXPANDED)
                bottomSheet.setState(BottomSheetBehavior.STATE_COLLAPSED);
        }
        else {
            bottomSheet.setState(BottomSheetBehavior.STATE_COLLAPSED);
        }

        bottomSheetShadow.setVisibility(View.VISIBLE);
        return true;
    }

    private void handleBusMarkerSelected(Bus bus, Marker marker) {
        if (bus.equals(selectedBus))
            return;

        updateSelectedBus(bus);
        updateSelectedBusStop(null);

        bottomTitle.setText(bus.getCompanyName() + " - " + bus.getCurrentRoute().getId());
        bottomSubtitle.setText(selectedBus.getCurrentCapacity() + " / " + selectedBus.getMaximumCapacity() + " seats");
        bottomSubtitle.setVisibility(View.VISIBLE);
        bottomSheetMoreInfoBtn.setVisibility(View.VISIBLE);

        updateBusInfoPanel(bus);
    }

    private void handleBusStopMarkerSelected(BusStop busStop, Marker marker) {
        updateSelectedBus(null);
        updateSelectedBusStop(busStop);

        bottomTitle.setText(busStop.getId());
        bottomSubtitle.setVisibility(View.INVISIBLE);
        bottomSheetMoreInfoBtn.setVisibility(View.GONE);
    }

    @Override
    public void onHailClickedCallback(Bus bus, BusStop busStop) {
        System.out.println("Hailing " + bus.getRegistrationNumber() + " for " + busStop.getInternalId());
        busStopAPI.sendHailToBus(bus, busStop);
    }

    @Override
    public void onViewBusStopClicked(BusStop busStop) {
        System.out.println("Selected Bus Stop: " + busStop.getInternalId());
        this.onMarkerClick(busStopMarkers.get(busStop));
    }

    /**
     * Updates the recycler view list on the bottom sheet.
     */
    private void updateBusInfoPanel(Bus bus) {
        if (bus != selectedBus)
            return;

        // Draw Polyline
        if (bus.getCurrentRoute().getWaypoints() != null)
            busRouteLine.setPoints(bus.getCurrentRoute().getWaypoints());

        // Update List
        int currentStopIndex = bus.getCurrentRoute().getBusStops().indexOf(bus.getCurrentStop());
        ArrayList<TimeSlot> displayedList = new ArrayList<>(bus.getTimeslots().subList(currentStopIndex, bus.getTimeslots().size()));

        if (timeSlotAdapter == null || timeSlotAdapter.getItemCount() == 0) {
            timeSlotAdapter = new TimeSlotAdapter(bus, displayedList, this, this);
            layoutBottomSheetList.setAdapter(timeSlotAdapter);
        } else {
            timeSlotAdapter.update(displayedList);
            System.out.println(bus.getCurrentRoute());
            System.out.println(timeSlotAdapter.toString());
        }

        // Update Info Panel
        busRegistrationNumberText.setText(bus.getRegistrationNumber());
        busModelText.setText(bus.getModel());
        busCompanyText.setText(bus.getCompanyName());
        busLatitude.setText(Double.toString(bus.getLatitude()));
        busLongitude.setText(Double.toString(bus.getLongitude()));
    }

    /**
     * Updates the selected bus and clears any data related to it.
     */
    private void updateSelectedBus(Bus bus) {
        selectedBus = bus;
        timeSlotAdapter = null;
        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                busRouteLine.setPoints(new ArrayList<LatLng>());
            }
        });
    }

    /**
     * Updates the selected bus stop and clears any data related to it;
     */
    private void updateSelectedBusStop(BusStop stop) {
        selectedBusStop = stop;
        timeSlotAdapter = null;
        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                busRouteLine.setPoints(new ArrayList<LatLng>());
            }
        });
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

    /**
     * Handles the bus route data received from the API
     */
    private void handleBusRouteData(final JsonArray busRoutesJson)
    {
        // runOnUiThread to prevent ConcurrencyException while accessing busStopMarkers
        runOnUiThread(new Runnable()
        {
            @Override
            public void run()
            {
                for (JsonElement busRouteJson : busRoutesJson)
                {
                    String id = busRouteJson.getAsJsonObject().get("id").getAsString();
                    String internalId = busRouteJson.getAsJsonObject().get("internal_id").getAsString();

                    ArrayList<BusStop> tempBusStops = new ArrayList<>();

                    for (JsonElement jsonIdInternal : busRouteJson.getAsJsonObject().get("bus_stops").getAsJsonArray()){
                        for (BusStop busStop : busStopMarkers.inverse().values()) {
                            if (busStop.getInternalId().equals(jsonIdInternal.getAsString())) {
                                System.out.println("Adding stop " + busStop.getInternalId() + " to route " + internalId);
                                tempBusStops.add(busStop);
                                break;
                            }
                        }
                    }

                    BusRoute busRoute = new BusRoute(id, internalId, tempBusStops);
                    busRoutes.add(busRoute);
                    busStopAPI.getBusRouteWaypoints(busRoute);
                }
            }
        });
    }

    private void handleRouteWaypointsData(final BusRoute route, final List<LatLng> waypoints) {
        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                route.setWaypoints(waypoints);

                if (selectedBus != null && selectedBus.getCurrentRoute().equals(route)) {
                    busRouteLine.setPoints(waypoints);
                }
            }
        });
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
        final String registrationNumber = message.get("registration_number").getAsString();
        final String model = message.get("model").getAsString();
        final String company = message.get("company").getAsString();
        final String routeIdInternal = message.get("route_id_internal").getAsString();
        final String currentStopIdInternal = message.get("current_stop_id_internal").getAsString();
        final JsonArray hailedBusStopsJson = message.get("hailed_stops").getAsJsonArray();
        final JsonObject timeslotsJson = message.get("timeslots").getAsJsonObject();
        final int currentCapacity = message.get("current_capacity").getAsInt();
        final int maximumCapacity = message.get("maximum_capacity").getAsInt();
        final double sentAt = message.get("sent_at").getAsDouble();

        runOnUiThread(new Runnable() {
            @Override
            public void run() {

                BusRoute busRoute = null;
                BusStop busStop = null;

                for (BusRoute route : busRoutes) {
                    if (route.getIdInternal().equals(routeIdInternal)) {
                        busRoute = route;
                        break;
                    }
                }

                for (BusStop stop : busStopMarkers.inverse().values()) {
                    if (stop.getInternalId().equals(currentStopIdInternal)) {
                        busStop = stop;
                        break;
                    }
                }

                List<TimeSlot> timeslots = new ArrayList<>();
                for (Object k : timeslotsJson.keySet()) {
                    String key = (String)k;
                    String val = timeslotsJson.get(key).getAsString();

                    BusStop stop = null;
                    for (BusStop stopCheck : busStopMarkers.inverse().values()) {
                        if (stopCheck.getInternalId().equals(key)) {
                            stop = stopCheck;
                            break;
                        }
                    }

                    TimeSlot newTimeslot = new TimeSlot(stop, val);
                    timeslots.add(newTimeslot);
                }

                List<BusStop> hailedBusStops = new ArrayList<>();
                for (JsonElement s : hailedBusStopsJson) {
                   for (BusStop stopCheck : busStopMarkers.inverse().values()) {
                       if (stopCheck.getInternalId().equals(s.getAsString())) {
                           hailedBusStops.add(stopCheck);
                           break;
                       }
                   }
                }

                Bus bus = new Bus(busName, latitude, longitude, registrationNumber, model, company,
                        busRoute, busStop, hailedBusStops, timeslots, currentCapacity,
                        maximumCapacity, sentAt);
                LatLng position = new LatLng(bus.getLatitude(), bus.getLongitude());

                // Set the current bus
                for (Bus b : busMarkers.inverse().values()) {
                    if (busName.equals(b.getName())) {
                        bus = b;
                        bus.setLatitude(latitude);
                        bus.setLongitude(longitude);
                        bus.setCurrentStop(busStop);
                        bus.setHailedStops(hailedBusStops);
                        bus.setCurrentCapacity(currentCapacity);
                        bus.setTimestamp(sentAt);
                        if (sentAt < b.getTimestamp()) {
                            return;
                        } else {
                            break;
                        }
                    }
                }

                // Update list
                if (selectedBus != null) {
                    bottomSubtitle.setText(selectedBus.getCurrentCapacity() + " / " + selectedBus.getMaximumCapacity() + " seats");
                    updateBusInfoPanel(selectedBus);
                }

                if (timeSlotAdapter != null) {
                    timeSlotAdapter.notifyDataSetChanged();
                }

                // Set marker
                Marker marker = null;
                if (busMarkers.containsKey(bus)) {
                    System.out.println("Updating bus " + bus.getName() + " position.");
                    marker = busMarkers.get(bus);
                    marker.setPosition(position);
                } else {
                    System.out.println("Creating new bus marker for " + bus.getName());
                    Bitmap bitmap = ((BitmapDrawable)getResources().getDrawable(R.drawable.bus_marker)).getBitmap();
                    bitmap = Bitmap.createScaledBitmap(bitmap, 250, 250, false);

                    Marker newMarker = mMap.addMarker(new MarkerOptions()
                            .position(position)
                            .title(busName)
                            .zIndex(2f)
                            .icon(BitmapDescriptorFactory.fromBitmap(bitmap)));

                    marker = busMarkers.forcePut(bus, newMarker);
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

        if (selectedBus != null && bus.equals(selectedBus)) {
            updateSelectedBus(null);

            runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    bottomSheet.setHideable(true);
                    bottomSheet.setState(BottomSheetBehavior.STATE_HIDDEN);
                    bottomSheetShadow.setVisibility(View.GONE);
                    busRouteLine.setPoints(new ArrayList<LatLng>());
                }
            });
        }

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
