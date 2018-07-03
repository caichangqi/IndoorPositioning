package com.audioar.wifipositioning.view;

import android.content.DialogInterface;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.v7.app.AlertDialog;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.DividerItemDecoration;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.view.View;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.ZoomControls;

import com.audioar.wifipositioning.R;
import com.audioar.wifipositioning.model.Project;
import com.audioar.wifipositioning.view.viewfrags.APSection;
import com.audioar.wifipositioning.view.viewfrags.RPSection;
import com.audioar.wifipositioning.model.AccessPoint;
import com.audioar.wifipositioning.model.ReferencePoint;
import com.audioar.wifipositioning.view.viewfrags.RecyclerItemClickListener;
import com.baidu.location.BDAbstractLocationListener;
import com.baidu.location.BDLocation;
import com.baidu.location.LocationClient;
import com.baidu.location.LocationClientOption;
import com.baidu.mapapi.SDKInitializer;
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

import java.util.Date;

import io.github.luizgrp.sectionedrecyclerviewadapter.SectionParameters;
import io.github.luizgrp.sectionedrecyclerviewadapter.SectionedRecyclerViewAdapter;
import io.realm.Realm;

public class ProjectDetailActivity extends AppCompatActivity implements View.OnClickListener, RecyclerItemClickListener.OnItemClickListener {
    static private String projectId = "AudioARProject";
    static private String desc = "Description : AudioARProject";

    private RecyclerView pointRV;
    private Button btnAddAp, btnAddRp, btnWIFI;
    private Project project;
    private SectionedRecyclerViewAdapter sectionAdapter = new SectionedRecyclerViewAdapter();
    private RPSection rpSec;
    private APSection apSec;
    private LinearLayoutManager layoutManager;
    private int PERM_REQ_CODE_RP_ACCESS_COARSE_LOCATION = 198;
    private int PERM_REQ_CODE_LM_ACCESS_COARSE_LOCATION = 197;


    private BaiduMapManager mapManager;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        SDKInitializer.initialize(getApplicationContext());
        setContentView(R.layout.activity_project_detail);
        mapManager = new BaiduMapManager();
        mapManager.onCreate();

        final Realm realm = Realm.getDefaultInstance();
        if (realm.where(Project.class).equalTo("id", projectId).count() == 0) {
            realm.executeTransactionAsync(new Realm.Transaction() {
                @Override
                public void execute(Realm bgRealm) {
                    project = bgRealm.createObject(Project.class, projectId);
                    project.setName(projectId);
                    project.setDesc(desc);
                    project.setCreatedAt(new Date());
                }
            }, new Realm.Transaction.OnSuccess() {
                @Override
                public void onSuccess() {
                    project = realm.where(Project.class).equalTo("id", projectId).findFirst();
                    initUI();
                    mapManager.onDrawOverlay(project);
                }
            }, new Realm.Transaction.OnError() {
                @Override
                public void onError(Throwable error) {
                    System.out.print(error.getMessage());
                }
            });
        } else {
            project = realm.where(Project.class).equalTo("id", projectId).findFirst();
            initUI();
            mapManager.onDrawOverlay(project);
        }
    }

    private void initUI() {
        pointRV = findViewById(R.id.rv_points);
        btnAddAp = findViewById(R.id.btn_add_ap);
        btnAddAp.setOnClickListener(this);

        btnAddRp = findViewById(R.id.btn_add_rp);
        btnAddRp.setOnClickListener(this);

        btnWIFI = findViewById(R.id.btn_wifi);
        btnWIFI.setOnClickListener(this);
        setCounts();

        SectionParameters sp = new SectionParameters.Builder(R.layout.item_point_details)
                .headerResourceId(R.layout.item_section_details)
                .build();

        apSec = new APSection(sp);
        rpSec = new RPSection(sp);
        apSec.setAccessPoints(project.getAps());
        rpSec.setReferencePoints(project.getRps());
        sectionAdapter.addSection(apSec);
        sectionAdapter.addSection(rpSec);
        layoutManager = new LinearLayoutManager(this);
        pointRV.addItemDecoration(new DividerItemDecoration(this, DividerItemDecoration.VERTICAL));
        pointRV.setLayoutManager(layoutManager);
        pointRV.setAdapter(sectionAdapter);
        pointRV.addOnItemTouchListener(new RecyclerItemClickListener(this,pointRV, this));
    }

    private class BaiduMapManager extends BDAbstractLocationListener {
        private MapView mMapView;
        private BaiduMap mBaiduMap;
        private LocationClient mLocationClient;
        private boolean isFirstLoc = true;

        void onCreate() {
            mMapView = findViewById(R.id.bmapView);
            mBaiduMap = mMapView.getMap();
            View child = mMapView.getChildAt(1);
            if (child != null && (child instanceof ImageView || child instanceof ZoomControls)){
                child.setVisibility(View.INVISIBLE);
            }
            mMapView.showScaleControl(false);
            mMapView.showZoomControls(false);

            MapStatus mapStatus = new MapStatus.Builder().zoom(21).build();
            MapStatusUpdate mapStatusUpdate = MapStatusUpdateFactory.newMapStatus(mapStatus);
            mBaiduMap.setMapStatus(mapStatusUpdate);

            mLocationClient = new LocationClient(ProjectDetailActivity.this);
            mLocationClient.registerLocationListener(this);
            LocationClientOption option = new LocationClientOption();
            option.setCoorType("bd09ll");
            option.setLocationMode(LocationClientOption.LocationMode.Hight_Accuracy);
            option.setOpenGps(true);
            option.setScanSpan(1000);
            mLocationClient.setLocOption(option);


            UiSettings uiSettings = mBaiduMap.getUiSettings();
            uiSettings.setZoomGesturesEnabled(false);
            uiSettings.setOverlookingGesturesEnabled(false);

            mBaiduMap.setMyLocationEnabled(true);
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
            if (isFirstLoc) {
                isFirstLoc = false;
                LatLng latLng = new LatLng(bdLocation.getLatitude(), bdLocation.getLongitude());
                MapStatusUpdate mapStatusUpdate = MapStatusUpdateFactory.newLatLng(latLng);
                mBaiduMap.animateMapStatus(mapStatusUpdate);
            }
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

    private void setCounts() {
        String name = project.getName();
        int apCount = project.getAps().size();
        int rpCount = project.getRps().size();

        if (getSupportActionBar() != null) {
            getSupportActionBar().setTitle(name);
        }
        if (apCount > 0) {
            ((TextView)findViewById(R.id.btn_add_ap)).setText("（"+String.valueOf(apCount) + "）接入点");
        }
        if (rpCount > 0) {
            ((TextView)findViewById(R.id.btn_add_rp)).setText("（"+String.valueOf(rpCount) + "）参考点");
        }
    }

    @Override
    protected void onResume() {
        super.onResume();
        mapManager.onResume();
        sectionAdapter.notifyDataSetChanged();
        setCounts();
        mapManager.onDrawOverlay(project);
    }

    @Override
    public void onClick(View view) {
        if (view.getId() == btnAddAp.getId()) {
            startAddAPActivity("");
        } else if (view.getId() == btnAddRp.getId()) {
            startAddRPActivity(null);
        } else if (view.getId() == btnWIFI.getId()) {
            startPositioningActivity();
        } else if (view.getId() == R.id.btn_pref) {
            startPrefActivity();
        } else if (view.getId() == R.id.btn_debug) {
            startDebugActivity();
        } else if (view.getId() == R.id.btn_remove_all) {
            Realm realm = Realm.getDefaultInstance();
            realm.beginTransaction();
            project.getAps().deleteAllFromRealm();
            project.getRps().deleteAllFromRealm();
            realm.commitTransaction();
            refreshList();
        } else if (view.getId() == R.id.btn_pdr) {
            Intent intent = new Intent(this, PDRActivity.class);
            startActivity(intent);
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, String[] permissions, int[] grantResults) {
        if (requestCode == PERM_REQ_CODE_RP_ACCESS_COARSE_LOCATION && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
            startAddRPActivity(null);
        } else if(requestCode == PERM_REQ_CODE_LM_ACCESS_COARSE_LOCATION && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
            startPositioningActivity();
        }
    }

    private void startDebugActivity() {
        Intent intent = new Intent(this, AlgorithmDebugActivity.class);
        intent.putExtra("projectId", projectId);
        startActivity(intent);
    }


    private void startPrefActivity() {
        Intent intent = new Intent(this, PrefActivity.class);
        startActivity(intent);
    }


    private void startAddAPActivity(String apId) {
        Intent intent = new Intent(this, SearchWifiAPActivity.class);
        intent.putExtra("projectId", projectId);
        startActivity(intent);
    }

    private void startAddRPActivity(String rpId) {
        Intent intent = new Intent(this, AddReferencePoint.class);
        intent.putExtra("projectId", projectId);
        intent.putExtra("rpId", rpId);
        startActivity(intent);
    }

    private void startPositioningActivity() {
        Intent intent = new Intent(this, PositioningActivity.class);
        intent.putExtra("projectId", projectId);
        startActivity(intent);
    }

    @Override
    public void onItemClick(View view, int position) {
        int apsCount = 0;
        if (project.getAps() != null) {
            apsCount = project.getAps().size();
        }
        if (position <= apsCount && position != 0) {
            AccessPoint accessPoint = project.getAps().get(position - 1);
            startAddAPActivity(accessPoint.getId());
        } else if (position > (apsCount+1)) {
            ReferencePoint referencePoint = project.getRps().get(position - apsCount - 1 - 1);
            startAddRPActivity(referencePoint.getId());
        }
    }

    @Override
    public void onLongClick(View view, int position) {
        int apsCount = 0;
        if (project.getAps() != null) {
            apsCount = project.getAps().size();
        }
        if (position <= apsCount && position != 0) {
            AccessPoint accessPoint = project.getAps().get(position - 1);
            showDeleteDialog(accessPoint, null);
        } else if (position > (apsCount+1)) {
            ReferencePoint referencePoint = project.getRps().get(position - apsCount - 1 - 1);
            showDeleteDialog(null, referencePoint);
        }
    }

    private void showDeleteDialog(final AccessPoint accessPoint,final ReferencePoint referencePoint) {
        AlertDialog.Builder builder = new AlertDialog.Builder(this, R.style.Theme_AppCompat_DayNight_Dialog);
        if (accessPoint != null) {
            builder.setTitle("删除该AP");
            builder.setMessage("删除 "+ accessPoint.getSsid());
        } else {
            builder.setTitle("删除该RP");
            builder.setMessage("删除 "+ referencePoint.getName());
        }

        builder.setPositiveButton("确定", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialogInterface, int i) {
                Realm realm = Realm.getDefaultInstance();
                if (accessPoint != null) {
                    realm.executeTransaction(new Realm.Transaction() {
                        @Override
                        public void execute(Realm realm) {
                            accessPoint.deleteFromRealm();
                            refreshList();
                        }
                    });
                } else {
                    realm.executeTransaction(new Realm.Transaction() {
                        @Override
                        public void execute(Realm realm) {
                            referencePoint.deleteFromRealm();
                            refreshList();
                        }
                    });
                }

            }
        });
        builder.setNegativeButton("取消", null);
        builder.show();
    }

    private void refreshList() {
        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                sectionAdapter.notifyDataSetChanged();
                mapManager.onDrawOverlay(project);
            }
        });
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        mapManager.onDestroy();
    }

    @Override
    protected void onPause() {
        super.onPause();
        mapManager.onPause();
    }

    // What should the UI do when it receives the locating result?

}