package aaronmeaney.ie.busstopapp;

import com.pubnub.api.PNConfiguration;
import com.pubnub.api.PubNub;
import com.pubnub.api.callbacks.PNCallback;
import com.pubnub.api.callbacks.SubscribeCallback;
import com.pubnub.api.enums.PNStatusCategory;
import com.pubnub.api.models.consumer.PNPublishResult;
import com.pubnub.api.models.consumer.PNStatus;
import com.pubnub.api.models.consumer.pubsub.PNMessageResult;
import com.pubnub.api.models.consumer.pubsub.PNPresenceEventResult;

import java.util.Arrays;
import java.util.HashMap;

/**
 * Wrapper for the PubNub API with methods that make sense in the context of Bus Stop.
 */
class BusStopAPI {
    private static final BusStopAPI ourInstance = new BusStopAPI();

    static BusStopAPI getInstance() {
        return ourInstance;
    }

    public final String PUBNUB_CHANNEL = "bus_stop";
    private boolean initialized = false;
    private PubNub pubnub = null;

    /**
     * Returns if the API is initialized and is ready to be used.
     */
    public boolean isInitialized() {
        return initialized;
    }

    /**
     * Initializes the PubNub API.
     */
    public void initialize() {
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
                }
            }

            @Override
            public void presence(PubNub pubnub, PNPresenceEventResult presence) {

            }
        });

        pubnub.subscribe()
                .channels(Arrays.asList(PUBNUB_CHANNEL))
                .execute();
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
}
