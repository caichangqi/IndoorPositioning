package com.audioar.wifipositioning;


public class SharedConstants {

    //Reference points
    public static final int FETCH_INTERVAL = 3000;//0.3 secs
    public static final int READINGS_BATCH = 10;//10 values in every 3 secs

    public static final Float NaN = -110.0f;//RSSI value for no reception

    public static final String INTENT_FILTER = "ANDROID_WIFI_SCANNER";
    public static final String WIFI_DATA = "WIFI_DATA";
}
