package com.audioar.wifipositioning.model;

import android.support.annotation.NonNull;

public class LocationWithDistance implements Comparable<LocationWithDistance> {
    private double distance;
    private String location;
    private String name;

    public LocationWithDistance(double distance, String location, String name) {
        this.distance = distance;
        this.location = location;
        this.name = name;
    }

    public double getDistance() {
        return distance;
    }

    public String getLocation() {
        return location;
    }

    public String getName() {
        return name;
    }

    @Override
    public int compareTo(@NonNull LocationWithDistance obj) {
        if (distance == obj.getDistance())
            return 0;
        else if (distance > obj.getDistance())
            return 1;
        else
            return -1;
    }
}