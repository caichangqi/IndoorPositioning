package com.audioar.wifipositioning;

import android.app.Application;

import io.realm.Realm;


public class WifiPositioningApplication extends Application {

    @Override
    public void onCreate() {
        super.onCreate();
        Realm.init(this);
    }
}
