package aaronmeaney.ie.busstopapp;

/**
 * Callback called when the user clicks a button in the adapter.
 * Source: https://stackoverflow.com/a/44238888
 */

public interface AdapterHailCallback {
    void onHailClickedCallback(Bus bus, BusStop busStop);
}
