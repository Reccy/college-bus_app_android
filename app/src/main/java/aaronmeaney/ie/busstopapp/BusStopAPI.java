package aaronmeaney.ie.busstopapp;

import com.google.gson.Gson;
import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.pubnub.api.PNConfiguration;
import com.pubnub.api.PubNub;
import com.pubnub.api.callbacks.PNCallback;
import com.pubnub.api.callbacks.SubscribeCallback;
import com.pubnub.api.enums.PNStatusCategory;
import com.pubnub.api.models.consumer.PNPublishResult;
import com.pubnub.api.models.consumer.PNStatus;
import com.pubnub.api.models.consumer.pubsub.PNMessageResult;
import com.pubnub.api.models.consumer.pubsub.PNPresenceEventResult;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.concurrent.TimeUnit;

import okhttp3.Call;
import okhttp3.Callback;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;

/**
 * Wrapper for the PubNub API with methods that make sense in the context of Bus Stop.
 */
class BusStopAPI {
    private static final BusStopAPI ourInstance = new BusStopAPI();

    static BusStopAPI getInstance() {
        return ourInstance;
    }

    public interface BusStopAPIInitializedListener {
        void onBusStopAPIInitialized();
    }
    private List<BusStopAPIInitializedListener> initListeners;

    public interface BusStopAPIMessageReceivedListener {
        void onBusStopAPIMessageReceived(PNMessageResult messageResult);
    }
    private List<BusStopAPIMessageReceivedListener> msgListeners;

    public interface BusStopAPIReceivedBusStopsListener {
        void onBusStopAPIReceivedBusStops(JsonArray busStops);
    }
    private List<BusStopAPIReceivedBusStopsListener> busStopsListeners;

    public interface BusStopAPIReceivedBusRoutesListener {
        void onBusStopAPIReceivedBusRoutes(JsonArray busRoutes);
    }
    private List<BusStopAPIReceivedBusRoutesListener> busRoutesListeners;

    public final String PUBNUB_CHANNEL = "bus_stop";
    public final String API_BASE_URL = "https://bus-stop-api.herokuapp.com/";
    private boolean initialized = false;
    private PubNub pubnub = null;
    private OkHttpClient okHttpClient = null;

    /**
     * Returns if the API is initialized and is ready to be used.
     */
    public boolean isInitialized() {
        return initialized;
    }

    public BusStopAPI() {
        initListeners = new ArrayList<>();
        msgListeners = new ArrayList<>();
        busStopsListeners = new ArrayList<>();
        busRoutesListeners = new ArrayList<>();
    }

    public void addOnInitializedListener(BusStopAPIInitializedListener listener) {
        initListeners.add(listener);
    }

    public void addOnMessageReceivedListener(BusStopAPIMessageReceivedListener listener) {
        msgListeners.add(listener);
    }

    public void addOnReceivedBusStops(BusStopAPIReceivedBusStopsListener listener) {
        busStopsListeners.add(listener);
    }

    public void addOnReceivedBusRoutes(BusStopAPIReceivedBusRoutesListener listener) {
        busRoutesListeners.add(listener);
    }

    /**
     * Initializes the PubNub API and OkHTTPClient.
     */
    public void initialize() {
        OkHttpClient.Builder b = new OkHttpClient.Builder();
        b.readTimeout(60, TimeUnit.SECONDS);
        b.writeTimeout(60, TimeUnit.SECONDS);
        okHttpClient = b.build();

        PNConfiguration pnConfiguration = new PNConfiguration();
        pnConfiguration.setSubscribeKey("sub-c-136ca9c4-4d58-11e8-9987-d26dac8959c0");
        pnConfiguration.setPublishKey("pub-c-c6e95d6d-cdba-4d5c-8c5d-29064d99fd0e");

        pubnub = new PubNub(pnConfiguration);

        pubnub.addListener(new SubscribeCallback() {
            @Override
            public void status(PubNub pubnub, PNStatus status) {
                if (status.getCategory() == PNStatusCategory.PNUnexpectedDisconnectCategory) {
                    System.out.println("[PubNub] Unexpected disconnection.");
                }
                else if (status.getCategory() == PNStatusCategory.PNConnectedCategory) {
                    if (status.getCategory() == PNStatusCategory.PNConnectedCategory){
                        publishMessage("android_connection_test", "This is just a connection test!", new PNCallback<PNPublishResult>() {
                            @Override
                            public void onResponse(PNPublishResult result, PNStatus status) {
                                if (!status.isError()) {
                                    System.out.println("[PubNub] Successfully initialized PubNub!");
                                    initialized = true;

                                    for (BusStopAPIInitializedListener listener : initListeners) {
                                        listener.onBusStopAPIInitialized();
                                    }
                                }
                            }
                        });
                    }
                }
                else if (status.getCategory() == PNStatusCategory.PNReconnectedCategory) {
                    System.out.println("[PubNub] Reconnected!");
                }
                else if (status.getCategory() == PNStatusCategory.PNDecryptionErrorCategory) {
                    System.out.println("[PubNub] Decryption Error!");
                }
            }

            @Override
            public void message(PubNub pubnub, PNMessageResult message) {
                if (message.getChannel() != null) {
                    System.out.println("[PubNub] Message Callback: " + message.getMessage());

                    for (BusStopAPIMessageReceivedListener listener : msgListeners) {
                        listener.onBusStopAPIMessageReceived(message);
                    }
                }
            }

            @Override
            public void presence(PubNub pubnub, PNPresenceEventResult presence) {}
        });

        pubnub.subscribe()
                .channels(Arrays.asList(PUBNUB_CHANNEL))
                .execute();
    }

    /**
     * Sends a hail message to a bus to make it prepare to stop.
     */
    public void sendHailToBus(Bus bus, BusStop stop) {
        HashMap<String, String> busToBusStopMap = new HashMap<>();
        busToBusStopMap.put(bus.getRegistrationNumber(), stop.getInternalId());
        publishMessage("hail_bus", busToBusStopMap);
    }

    /**
     * Sends a message to PubNub
     */
    public void publishMessage(String topic, Object payload) {
        HashMap<String, Object> payloadDict = new HashMap<>();
        payloadDict.put("message", payload);

        publishMessage(topic, payloadDict);
    }

    /**
     * Sends a message to PubNub and calls the callback on response.
     */
    public void publishMessage(String topic, Object payload, PNCallback<PNPublishResult> callback) {
        HashMap<String, Object> payloadDict = new HashMap<>();
        payloadDict.put("message", payload);

        publishMessage(topic, payloadDict, callback);
    }

    /**
     * Sends a message to PubNub
     */
    public void publishMessage(String topic, final HashMap<String, Object> payload) {
        payload.put("from", "android_client");
        payload.put("topic", topic);
        payload.put("sent_at", System.currentTimeMillis() / 1000L);

        pubnub.publish()
                .channel(PUBNUB_CHANNEL)
                .message(payload)
                .async(new PNCallback<PNPublishResult>() {
                    @Override
                    public void onResponse(PNPublishResult result, PNStatus status) {
                        if (!status.isError()) {
                            System.out.println("[PubNub] Successfully sent message to PubNub: " + payload.toString());
                        }
                        else
                        {
                            System.out.println(status.getErrorData());
                            System.out.println(status.getErrorData().getInformation());
                        }
                    }
                });
    }

    /**
     * Sends a message to PubNub and calls the callback on response.
     */
    public void publishMessage(String topic, final HashMap<String, Object> payload, PNCallback<PNPublishResult> callback)
    {
        payload.put("from", "android_client");
        payload.put("topic", topic);
        payload.put("sent_at", System.currentTimeMillis() / 1000L);

        pubnub.publish()
                .channel(PUBNUB_CHANNEL)
                .message(payload)
                .async(callback);
    }

    /**
     * Returns the Bus Stops configured on the Bus Stop API.
     */
    public void getBusStops() {
        String url = API_BASE_URL + "bus_stops";

        final Request request = new Request.Builder()
                .url(url)
                .build();

        okHttpClient.newCall(request).enqueue(new Callback() {
            @Override
            public void onFailure(Call call, IOException e) {
                System.out.println("[Bus Stop API] Get Bus Stops error: " + e);
            }

            @Override
            public void onResponse(Call call, Response response) throws IOException {
                JsonArray busStopsJson = new JsonParser().parse(response.body().string()).getAsJsonArray();

                System.out.println("[Bus Stop API] Bus Stops: " + busStopsJson.toString());

                for (BusStopAPIReceivedBusStopsListener listener : busStopsListeners) {
                    listener.onBusStopAPIReceivedBusStops(busStopsJson);
                }
            }
        });
    }

    public void getBusRoutes() {
        String url = API_BASE_URL + "bus_routes";

        final Request request = new Request.Builder()
                .url(url)
                .build();

        okHttpClient.newCall(request).enqueue(new Callback() {
            @Override
            public void onFailure(Call call, IOException e) {
                System.out.println("[Bus Stop API] Get Bus Routes error: " + e);
            }

            @Override
            public void onResponse(Call call, Response response) throws IOException {
                JsonArray busRoutesJson = new JsonParser().parse(response.body().string()).getAsJsonArray();

                System.out.println("[Bus Stop API] Bus Routes: " + busRoutesJson.toString());

                for (BusStopAPIReceivedBusRoutesListener listener : busRoutesListeners) {
                    listener.onBusStopAPIReceivedBusRoutes(busRoutesJson);
                }
            }
        });
    }
}
