package com.audioar.wifipositioning.view;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.v4.content.LocalBroadcastManager;
import android.support.v7.app.AppCompatActivity;
import android.view.View;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.ZoomControls;

import com.audioar.wifipositioning.CoreAlgorithm;
import com.audioar.wifipositioning.R;
import com.audioar.wifipositioning.Utilities;
import com.audioar.wifipositioning.WifiService;
import com.audioar.wifipositioning.model.Project;
import com.audioar.wifipositioning.model.LocationWithNearbyPlaces;
import com.audioar.wifipositioning.model.ReferencePoint;
import com.audioar.wifipositioning.model.WifiData;
import com.audioar.wifipositioning.SharedConstants;
import com.baidu.mapapi.map.BaiduMap;
import com.baidu.mapapi.map.BitmapDescriptorFactory;
import com.baidu.mapapi.map.MapStatus;
import com.baidu.mapapi.map.MapStatusUpdate;
import com.baidu.mapapi.map.MapStatusUpdateFactory;
import com.baidu.mapapi.map.MapView;
import com.baidu.mapapi.map.MarkerOptions;
import com.baidu.mapapi.map.MyLocationData;
import com.baidu.mapapi.map.UiSettings;
import com.baidu.mapapi.model.LatLng;

import io.realm.Realm;

public class PositioningActivity extends AppCompatActivity {

    private WifiData mWifiData;
    private String projectId, defaultAlgo;
    private Project project;
    private MainActivityReceiver mReceiver = new MainActivityReceiver();
    private Intent wifiServiceIntent;
    private TextView tvLocation;

    private BaiduMapManager mapManager;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        mWifiData = null;

        LocalBroadcastManager.getInstance(this).registerReceiver(mReceiver, new IntentFilter(SharedConstants.INTENT_FILTER));

        wifiServiceIntent = new Intent(this, WifiService.class);
        startService(wifiServiceIntent);

        mWifiData = (WifiData) getLastNonConfigurationInstance();

        setContentView(R.layout.activity_positioning);
        defaultAlgo = Utilities.getDefaultAlgo(this);
        projectId = getIntent().getStringExtra("projectId");
        Realm realm = Realm.getDefaultInstance();
        project = realm.where(Project.class).equalTo("id", projectId).findFirst();
        mapManager = new BaiduMapManager();
        mapManager.onCreate();
        mapManager.onDrawOverlay(project);
    }

    @Override
    public Object onRetainCustomNonConfigurationInstance() {
        return mWifiData;
    }

    public class MainActivityReceiver extends BroadcastReceiver {
        @Override
        public void onReceive(Context context, Intent intent) {
            mWifiData = intent.getParcelableExtra(SharedConstants.WIFI_DATA);

            if (mWifiData != null) {
                LocationWithNearbyPlaces loc = CoreAlgorithm.processingAlgorithms(mWifiData.getNetworks(), project, Integer.parseInt(defaultAlgo));
                if (loc == null) {
                    // If the position is null, do error handling.
                } else {
                    String location = loc.getLocation();
                    String[] split = location.split(" ");
                    Double latValue = Double.valueOf(split[0]);
                    Double lonValue = Double.valueOf(split[1]);
                    onWIFIPosition(latValue, lonValue);
                    tvLocation = findViewById(R.id.tv_location);
                    tvLocation.setText("Location: " + location);
                }
            }
        }
    }

    private class BaiduMapManager {
        private MapView mMapView;
        private BaiduMap mBaiduMap;

        void onCreate() {
            mMapView = findViewById(R.id.bmapView);
            mBaiduMap = mMapView.getMap();
            View child = mMapView.getChildAt(1);
            if (child != null && (child instanceof ImageView || child instanceof ZoomControls)){
                child.setVisibility(View.INVISIBLE);
            }
            mMapView.showScaleControl(false);
            mMapView.showZoomControls(false);

            // Zoom to the largest possible state as default :
            MapStatus mapStatus = new MapStatus.Builder().zoom(21).build();
            MapStatusUpdate mapStatusUpdate = MapStatusUpdateFactory.newMapStatus(mapStatus);
            mBaiduMap.setMapStatus(mapStatusUpdate);

            // Shutting down zooming and shifting operations :
            UiSettings uiSettings = mBaiduMap.getUiSettings();
            uiSettings.setZoomGesturesEnabled(false);
            uiSettings.setOverlookingGesturesEnabled(false);

            // Setting the GPS circle on the map, you need to implement a toggle button to shift to ... WIFI locationing.
            mBaiduMap.setMyLocationEnabled(true);
        }

        void onResume() {
            mMapView.onResume();
        }

        void onDestroy() {
            mMapView.onDestroy();
        }

        void onPause() {
            mMapView.onPause();
        }

        public void onReceiveLocation(double lat, double lng) {
            MyLocationData data = new MyLocationData.Builder()
                    .accuracy(5)
                    .latitude(lat)
                    .longitude(lng)
                    .build();
            mBaiduMap.setMyLocationData(data);

            LatLng latLng = new LatLng(lat, lng);
            MapStatusUpdate mapStatusUpdate = MapStatusUpdateFactory.newLatLng(latLng);
            mBaiduMap.animateMapStatus(mapStatusUpdate);
        }

        void onDrawOverlay(Project project) {
            mBaiduMap.clear();
            for (ReferencePoint referencePoint : project.getRps()) {
                LatLng point = new LatLng(referencePoint.getX(), referencePoint.getY());
                mBaiduMap.addOverlay(
                    new MarkerOptions()
                        .flat(true)
                        .position(point)
                        .icon(
                                BitmapDescriptorFactory.fromResource(R.drawable.pin)
                        )
                        .anchor(0.5f, 0.5f)
                );
            }
        }
    }

    private void onWIFIPosition(double lat, double lng) {
        mapManager.onReceiveLocation(lat, lng);
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        LocalBroadcastManager.getInstance(this).unregisterReceiver(mReceiver);
        stopService(wifiServiceIntent);
        mapManager.onDestroy();
    }

    @Override
    protected void onPause() {
        super.onPause();
        mapManager.onPause();
    }

    @Override
    protected void onResume() {
        super.onResume();
        mapManager.onResume();
    }
}
