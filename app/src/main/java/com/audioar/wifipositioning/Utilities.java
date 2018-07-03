package com.audioar.wifipositioning;

import android.content.Context;
import android.content.SharedPreferences;
import android.location.LocationManager;
import android.preference.PreferenceManager;
import android.util.Log;

import com.audioar.wifipositioning.model.AccessPoint;
import com.audioar.wifipositioning.model.Project;
import com.audioar.wifipositioning.model.LocationWithDistance;
import com.audioar.wifipositioning.model.LocationWithNearbyPlaces;

import java.text.DecimalFormat;
import java.text.NumberFormat;
import java.util.ArrayList;
import java.util.Collections;

import io.realm.Realm;

import static android.content.Context.LOCATION_SERVICE;


public class Utilities {

    public static String getDefaultAlgo(Context context) {
        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(context);
        String prefAlgo = prefs.getString("prefAlgo", "2");
        return prefAlgo;
    }


    public static boolean isLocationEnabled(Context context) {
        LocationManager locManager = (LocationManager) context.getSystemService(LOCATION_SERVICE);
        if (locManager.isProviderEnabled(LocationManager.GPS_PROVIDER)) {
            //GPS enabled
            Log.d("Utilities", "isLocationEnabled:" + true);
            return true;
        } else {
            //GPS disabled
            Log.d("Utilities", "isLocationEnabled:" + false);
            return false;
        }
    }

    public static LocationWithDistance getTheNearestPoint(LocationWithNearbyPlaces loc) {
        ArrayList<LocationWithDistance> places = loc.getPlaces();
        if (places != null && places.size() > 0) {
            Collections.sort(places);
            return places.get(0);
        }
        return null;
    }

    public static String reduceDecimalPlaces(String location) {
        NumberFormat formatter = new DecimalFormat("#0.00");
        String[] split = location.split(" ");
        Double latValue = Double.valueOf(split[0]);
        Double lonValue = Double.valueOf(split[1]);
        String latFormat = formatter.format(latValue);
        String lonFormat = formatter.format(lonValue);
        return latFormat + ", " + lonFormat;
    }

    public static String getTheDistancefromOrigin(String location) {
        NumberFormat formatter = new DecimalFormat("#0.00");
        String[] split = location.split(" ");
        Double latValue = Double.valueOf(split[0]);
        Double lonValue = Double.valueOf(split[1]);
        double distance = Math.sqrt(latValue * latValue + lonValue * lonValue);
        String distanceValue = formatter.format(distance);
        return distanceValue;
    }

    public static void addAPtoProject(AccessPoint ap, String projectId) {
        Realm realm = Realm.getDefaultInstance();
        realm.beginTransaction();
        Project project = realm.where(Project.class).equalTo("id", projectId).findFirst();
        for (AccessPoint accessPoint : project.getAps()) {
            if (accessPoint.getMac_address().equals(ap.getMac_address())) {
                realm.commitTransaction();
                return;
            }
        }
        project.getAps().add(ap);
        realm.commitTransaction();
    }




}
