package com.audioar.wifipositioning.view;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.v4.content.LocalBroadcastManager;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.DividerItemDecoration;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.util.Log;
import android.widget.TextView;
import android.widget.Toast;

import com.audioar.wifipositioning.CoreAlgorithm;
import com.audioar.wifipositioning.R;
import com.audioar.wifipositioning.Utilities;
import com.audioar.wifipositioning.model.Project;
import com.audioar.wifipositioning.view.viewfrags.NRAdapter;
import com.audioar.wifipositioning.WifiService;
import com.audioar.wifipositioning.model.LocationWithDistance;
import com.audioar.wifipositioning.model.LocationWithNearbyPlaces;
import com.audioar.wifipositioning.model.WifiData;
import com.audioar.wifipositioning.SharedConstants;

import io.realm.Realm;

public class AlgorithmDebugActivity extends AppCompatActivity {

    private WifiData mWifiData;
    private String projectId, defaultAlgo;
    private Project project;
    private MainActivityReceiver mReceiver = new MainActivityReceiver();
    private Intent wifiServiceIntent;
    private RecyclerView rvPoints;
    private LinearLayoutManager layoutManager;
    private NRAdapter readingsAdapter = new NRAdapter();

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        mWifiData = null;

        LocalBroadcastManager.getInstance(this).registerReceiver(mReceiver, new IntentFilter(SharedConstants.INTENT_FILTER));

        wifiServiceIntent = new Intent(this, WifiService.class);
        startService(wifiServiceIntent);

        mWifiData = (WifiData) getLastNonConfigurationInstance();

        setContentView(R.layout.activity_debug);
        initUI();

        defaultAlgo = Utilities.getDefaultAlgo(this);
        projectId = getIntent().getStringExtra("projectId");
        if (projectId == null) {
            Toast.makeText(getApplicationContext(), "No such project", Toast.LENGTH_LONG).show();
            this.finish();
        }
        Realm realm = Realm.getDefaultInstance();
        project = realm.where(Project.class).equalTo("id", projectId).findFirst();
    }

    private void initUI() {
        layoutManager = new LinearLayoutManager(this);
        rvPoints = findViewById(R.id.rv_nearby_points);
        rvPoints.setLayoutManager(layoutManager);
        rvPoints.addItemDecoration(new DividerItemDecoration(this, DividerItemDecoration.VERTICAL));
        rvPoints.setAdapter(readingsAdapter);
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
                if (loc != null) {
                    readingsAdapter.setReadings(loc.getPlaces());
                    readingsAdapter.notifyDataSetChanged();
                }
            }
        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        LocalBroadcastManager.getInstance(this).unregisterReceiver(mReceiver);
        stopService(wifiServiceIntent);
    }
}
