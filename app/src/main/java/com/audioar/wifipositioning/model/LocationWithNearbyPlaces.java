package com.audioar.wifipositioning.model;

import java.util.ArrayList;

public class LocationWithNearbyPlaces {

    private String location;
    private ArrayList<LocationWithDistance> places;

    public LocationWithNearbyPlaces(String location, ArrayList<LocationWithDistance> places) {
        this.location = location;
        this.places = places;
    }

    public String getLocation() {
        return location;
    }

    public void setLocation(String location) {
        this.location = location;
    }

    public ArrayList<LocationWithDistance> getPlaces() {
        return places;
    }

    public void setPlaces(ArrayList<LocationWithDistance> places) {
        this.places = places;
    }
}
