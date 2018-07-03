package com.audioar.wifipositioning.view;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.net.wifi.ScanResult;
import android.net.wifi.WifiManager;
import android.os.Bundle;
import android.os.Handler;
import android.support.annotation.Nullable;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.DividerItemDecoration;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.text.TextUtils;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;
import android.widget.ZoomControls;

import com.audioar.wifipositioning.R;
import com.audioar.wifipositioning.model.Project;
import com.audioar.wifipositioning.view.viewfrags.ReferenceReadingsAdapter;
import com.audioar.wifipositioning.model.AccessPoint;
import com.audioar.wifipositioning.model.ReferencePoint;
import com.audioar.wifipositioning.SharedConstants;
import com.audioar.wifipositioning.Utilities;
import com.baidu.location.BDAbstractLocationListener;
import com.baidu.location.BDLocation;
import com.baidu.location.LocationClient;
import com.baidu.location.LocationClientOption;
import com.baidu.mapapi.map.BaiduMap;
import com.baidu.mapapi.map.BitmapDescriptorFactory;
import com.baidu.mapapi.map.MapStatus;
import com.baidu.mapapi.map.MapStatusUpdate;
import com.baidu.mapapi.map.MapStatusUpdateFactory;
import com.baidu.mapapi.map.MapView;
import com.baidu.mapapi.map.Marker;
import com.baidu.mapapi.map.MarkerOptions;
import com.baidu.mapapi.map.MyLocationConfiguration;
import com.baidu.mapapi.map.MyLocationData;
import com.baidu.mapapi.map.OverlayOptions;
import com.baidu.mapapi.map.UiSettings;
import com.baidu.mapapi.model.LatLng;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import io.realm.Realm;
import io.realm.RealmList;

public class AddReferencePoint extends AppCompatActivity implements View.OnClickListener {

    private String TAG = "AddReferencePoint";
    private String projectId;

    private RecyclerView rvPoints;
    private LinearLayoutManager layoutManager;
    private EditText etRpName;
    private TextView etRpX, etRpY;
    private Button bnRpSave;

    private ReferenceReadingsAdapter readingsAdapter = new ReferenceReadingsAdapter();
    private List<AccessPoint> apsWithReading = new ArrayList<>();
    private java.util.Map<String, List<Integer>> readings = new HashMap<>();
    private java.util.Map<String, AccessPoint> aps = new HashMap<>();

    private AvailableAPsReceiver receiverWifi;

    private boolean wifiWasEnabled;
    private WifiManager mainWifi;
    private final Handler handler = new Handler();
    private boolean isCaliberating = false;
    private int readingsCount = 0;
    private boolean isEdit = false;
    private String rpId;
    private ReferencePoint referencePointFromDB;

    private Project project;

    // Mapping :
    private BaiduMapManager mapManager;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_add_reference_point);

        projectId = getIntent().getStringExtra("projectId");
        if (projectId == null) {
            Toast.makeText(this, "没有找到参考点", Toast.LENGTH_LONG).show();
            this.finish();
        }

        if (getIntent().getStringExtra("rpId") != null) {
            isEdit = true;
            rpId = getIntent().getStringExtra("rpId");
        }
        initUI();
        Realm realm = Realm.getDefaultInstance();
        if (isEdit) {
            referencePointFromDB = realm.where(ReferencePoint.class).equalTo("id", rpId).findFirst();
            if (referencePointFromDB == null) {
                Toast.makeText(this, "没有找到参考点", Toast.LENGTH_LONG).show();
                this.finish();
            }
            RealmList<AccessPoint> readings = referencePointFromDB.getReadings();
            for (AccessPoint ap:readings) {
                readingsAdapter.addAP(ap);
            }
            readingsAdapter.notifyDataSetChanged();
            etRpName.setText(referencePointFromDB.getName());
            etRpX.setText(String.valueOf(referencePointFromDB.getX()));
            etRpY.setText(String.valueOf(referencePointFromDB.getY()));
            project = realm.where(Project.class).equalTo("id", projectId).findFirst();

        } else {
            mainWifi = (WifiManager) getApplicationContext().getSystemService(Context.WIFI_SERVICE);
            receiverWifi = new AvailableAPsReceiver();
            wifiWasEnabled = mainWifi.isWifiEnabled();
            project = realm.where(Project.class).equalTo("id", projectId).findFirst();
            RealmList<AccessPoint> points = project.getAps();
            for (AccessPoint accessPoint : points) {
                aps.put(accessPoint.getMac_address(), accessPoint);
            }
            if (aps.isEmpty()) {
                Toast.makeText(this, "没有找到接入点", Toast.LENGTH_SHORT).show();
            }
            if (!Utilities.isLocationEnabled(this)) {
                Toast.makeText(this,"请将定位功能打开", Toast.LENGTH_SHORT).show();
            }
        }

        mapManager = new BaiduMapManager();
        mapManager.onCreate();
    }

    private class BaiduMapManager extends BDAbstractLocationListener implements BaiduMap.OnMarkerDragListener {
        private MapView mMapView;
        private BaiduMap mBaiduMap;
        private MyLocationConfiguration.LocationMode mLocationMode;
        private LocationClient mLocationClient;
        private boolean isFirstLoc = true;
        private Marker mMarker;

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

            // Set location service listeners :
            mLocationClient = new LocationClient(AddReferencePoint.this);
            mLocationClient.registerLocationListener(this);
            LocationClientOption option = new LocationClientOption();
            option.setCoorType("bd09ll");
            option.setLocationMode(LocationClientOption.LocationMode.Hight_Accuracy);
            option.setOpenGps(true);
            option.setScanSpan(1000);
            mLocationClient.setLocOption(option);


            // Shutting down zooming and shifting operations :
            UiSettings uiSettings = mBaiduMap.getUiSettings();
            uiSettings.setZoomGesturesEnabled(false);
            uiSettings.setOverlookingGesturesEnabled(false);

            // Setting the GPS circle on the map, you need to implement a toggle button to shift to ... WIFI locationing.
            mBaiduMap.setMyLocationEnabled(true);

            // Setting the listener
            mBaiduMap.setOnMarkerDragListener(this);
        }

        void onResume() {
            mMapView.onResume();
            mLocationClient.start();
        }

        void onDestroy() {
            mMapView.onDestroy();
            mLocationClient.stop();
        }

        void onPause() {
            mMapView.onPause();
            mLocationClient.stop();
        }

        @Override
        public void onReceiveLocation(BDLocation bdLocation) {
            MyLocationData data = new MyLocationData.Builder()
                    .accuracy(bdLocation.getRadius())
                    .direction(bdLocation.getDirection())
                    .latitude(bdLocation.getLatitude())
                    .longitude(bdLocation.getLongitude())
                    .build();
            mBaiduMap.setMyLocationData(data);
            // Shift the map on first location;
            if (isFirstLoc) {
                isFirstLoc = false;
                LatLng latLng = new LatLng(bdLocation.getLatitude(), bdLocation.getLongitude());
                MapStatusUpdate mapStatusUpdate = MapStatusUpdateFactory.newLatLng(latLng);
                mBaiduMap.animateMapStatus(mapStatusUpdate);
                // Bad implementation here.
                onDrawOverlay(project);
            }
        }

        void onDrawOverlay(Project project) {
            mBaiduMap.clear();

            // Draws all the reference points in the project :
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

            // Draws a draggable Handle for the new reference point :
            MyLocationData locationData = mBaiduMap.getLocationData();
            OverlayOptions options = new MarkerOptions()
                    .position(new LatLng(locationData.latitude, locationData.longitude))
                    .icon(BitmapDescriptorFactory.fromResource(R.drawable.icon_gcoding))
                    .zIndex(21)
                    .draggable(true);
            mMarker = (Marker) mBaiduMap.addOverlay(options);

        }

        @Override
        public void onMarkerDrag(Marker marker) {
            LatLng position = marker.getPosition();
            etRpX.setText(Double.toString(position.latitude));
            etRpY.setText(Double.toString(position.longitude));
        }

        @Override
        public void onMarkerDragEnd(Marker marker) {

        }

        @Override
        public void onMarkerDragStart(Marker marker) {

        }
    }


    @Override
    protected void onResume() {
        if (!isEdit) {
            registerReceiver(receiverWifi, new IntentFilter(WifiManager.SCAN_RESULTS_AVAILABLE_ACTION));
            if (!isCaliberating) {
                isCaliberating = true;
                refresh();
            }
        }
        super.onResume();
        mapManager.onResume();
    }

    @Override
    protected void onPause() {
        if (!isEdit) {
            unregisterReceiver(receiverWifi);
            isCaliberating = false;
        }
        super.onPause();
        mapManager.onPause();
    }

    public void refresh() {
        handler.postDelayed(new Runnable() {
            @Override
            public void run() {
                mainWifi.startScan();
                if (readingsCount < SharedConstants.READINGS_BATCH) {
                    refresh();
                } else {
                    caliberationCompleted();
                }
            }
        }, SharedConstants.FETCH_INTERVAL);
    }

    private void caliberationCompleted() {
        isCaliberating = false;
        Map<String, List<Integer>> values = readings;
        for (Map.Entry<String, List<Integer>> entry : values.entrySet()) {
            List<Integer> readingsOfAMac = entry.getValue();
            Double mean = calculateMeanValue(readingsOfAMac);
            AccessPoint accessPoint = aps.get(entry.getKey());
            AccessPoint updatedPoint = new AccessPoint(accessPoint);
            updatedPoint.setMeanRss(mean);
            apsWithReading.add(updatedPoint);
        }
        readingsAdapter.setReadings(apsWithReading);
        readingsAdapter.notifyDataSetChanged();
        bnRpSave.setEnabled(true);
        bnRpSave.setText("保存");
    }

    private Double calculateMeanValue(List<Integer> readings) {
        if (readings.isEmpty()) {
            return 0.0d;
        }
        Integer sum = 0;
        for (Integer integer : readings) {
            sum = sum + integer;
        }
        double mean = Double.valueOf(sum) / Double.valueOf(readings.size());
        return mean;
    }

    private void initUI() {
        layoutManager = new LinearLayoutManager(this);
        rvPoints = findViewById(R.id.rv_points);
        rvPoints.setLayoutManager(layoutManager);
        rvPoints.addItemDecoration(new DividerItemDecoration(this, DividerItemDecoration.VERTICAL));
        rvPoints.setAdapter(readingsAdapter);

        bnRpSave = findViewById(R.id.bn_rp_save);
        bnRpSave.setOnClickListener(this);

        if (!isEdit) {
            bnRpSave.setEnabled(false);
            bnRpSave.setText("收集数据中...");
        } else {
            bnRpSave.setEnabled(true);
            bnRpSave.setText("保存参考点");
        }

        etRpName = findViewById(R.id.et_rp_name);
        etRpX = findViewById(R.id.et_rp_x);
        etRpY = findViewById(R.id.et_rp_y);
    }

    @Override
    public void onClick(View view) {
        if (view.getId() == bnRpSave.getId() && !isEdit) {
            Realm realm = Realm.getDefaultInstance();
            realm.beginTransaction();
            ReferencePoint referencePoint = new ReferencePoint();
            referencePoint = setValues(referencePoint);
            referencePoint.setCreatedAt(Calendar.getInstance().getTime());
            referencePoint.setDescription("");
            if (referencePoint.getReadings() == null) {
                RealmList<AccessPoint> readings = new RealmList<>();
                readings.addAll(apsWithReading);
                referencePoint.setReadings(readings);
            } else {
                referencePoint.getReadings().addAll(apsWithReading);
            }

            referencePoint.setId(UUID.randomUUID().toString());

            Project project = realm.where(Project.class).equalTo("id", projectId).findFirst();
            if (project.getRps() == null) {
                RealmList<ReferencePoint> points = new RealmList<>();
                points.add(referencePoint);
                project.setRps(points);
            } else {
                project.getRps().add(referencePoint);
            }

            realm.commitTransaction();
            Toast.makeText(this,"参考点已添加", Toast.LENGTH_SHORT).show();
            this.finish();
        } else if (view.getId() == bnRpSave.getId() && isEdit) {
            Realm realm = Realm.getDefaultInstance();
            realm.beginTransaction();
            referencePointFromDB = setValues(referencePointFromDB);
            realm.commitTransaction();
            Toast.makeText(this,"参考点已更正", Toast.LENGTH_SHORT).show();
            this.finish();
        }
    }

    private ReferencePoint setValues(ReferencePoint referencePoint) {
        String x = etRpX.getText().toString();
        String y = etRpY.getText().toString();
        if (TextUtils.isEmpty(x)) {
            referencePoint.setX(0.0d);
        } else {
            referencePoint.setX(Double.valueOf(x));
        }

        if (TextUtils.isEmpty(y)) {
            referencePoint.setY(0.0d);
        } else {
            referencePoint.setY(Double.valueOf(y));
        }
        referencePoint.setLocId(referencePoint.getX() + " " + referencePoint.getY());
        referencePoint.setName(etRpName.getText().toString());
        return referencePoint;
    }

    class AvailableAPsReceiver extends BroadcastReceiver {
        @Override
        public void onReceive(Context context, Intent intent) {
            List<ScanResult> scanResults = mainWifi.getScanResults();
            ++readingsCount;
            for (java.util.Map.Entry<String, AccessPoint> entry : aps.entrySet()) {
                String apMac = entry.getKey();
                for (ScanResult scanResult : scanResults) {
                    if (entry.getKey().equals(scanResult.BSSID)) {
                        checkAndAddApRSS(apMac, scanResult.level);
                        apMac = null;
                        break;
                    }
                }
                if (apMac != null) {
                    checkAndAddApRSS(apMac, SharedConstants.NaN.intValue());
                }
            }
        }
    }

    private void checkAndAddApRSS(String apMac, Integer level) {
        if (readings.containsKey(apMac)) {
            List<Integer> integers = readings.get(apMac);
            integers.add(level);
        } else {
            List<Integer> integers = new ArrayList<>();
            integers.add(level);
            readings.put(apMac, integers);
        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (!wifiWasEnabled && !isEdit) {
            mainWifi.setWifiEnabled(false);
        }
        mapManager.onDestroy();
    }
}
