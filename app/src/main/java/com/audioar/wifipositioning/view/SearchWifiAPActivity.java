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
import android.support.v7.widget.DefaultItemAnimator;
import android.support.v7.widget.DividerItemDecoration;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.util.Log;
import android.view.View;
import android.widget.Button;

import com.audioar.wifipositioning.R;
import com.audioar.wifipositioning.Utilities;
import com.audioar.wifipositioning.view.viewfrags.WifiResults;
import com.audioar.wifipositioning.model.AccessPoint;
import com.audioar.wifipositioning.view.viewfrags.RecyclerItemClickListener;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

public class SearchWifiAPActivity extends AppCompatActivity implements View.OnClickListener,
        RecyclerItemClickListener.OnItemClickListener  {

    private String TAG = "SearchWifiAPActivity";
    private RecyclerView rvWifis;
    private RecyclerView.LayoutManager layoutManager;
    private Button banRefresh;

    private WifiManager mainWifi;
    private WifiListReceiver receiverWifi;
    private final Handler handler = new Handler();
    private List<ScanResult> results = new ArrayList<>();
    private WifiResults wifiResultsAdapter = new WifiResults();

    private String projectId;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_seach_wifis);
        initUI();

        projectId = getIntent().getStringExtra("projectId");

        mainWifi = (WifiManager) getApplicationContext().getSystemService(Context.WIFI_SERVICE);
        receiverWifi = new WifiListReceiver();

        if (!mainWifi.isWifiEnabled()) {
            mainWifi.setWifiEnabled(true);
        }
        layoutManager = new LinearLayoutManager(this);
        rvWifis.setLayoutManager(layoutManager);
        rvWifis.addItemDecoration(new DividerItemDecoration(this, DividerItemDecoration.VERTICAL));

        rvWifis.setItemAnimator(new DefaultItemAnimator());
        rvWifis.setAdapter(wifiResultsAdapter);
        rvWifis.addOnItemTouchListener(new RecyclerItemClickListener(this,rvWifis, this));
    }

    public void refresh() {
        handler.postDelayed(new Runnable() {
            @Override
            public void run() {
                mainWifi.startScan();
            }
        }, 1000);
    }

    @Override
    protected void onResume() {
        registerReceiver(receiverWifi, new IntentFilter(
                WifiManager.SCAN_RESULTS_AVAILABLE_ACTION));
        refresh();
        super.onResume();
    }

    @Override
    protected void onPause() {
        unregisterReceiver(receiverWifi);
        super.onPause();
    }

    private void initUI() {
        rvWifis = findViewById(R.id.rv_wifis);
        banRefresh = findViewById(R.id.btn_wifi_refresh);
        banRefresh.setOnClickListener(this);
    }

    @Override
    public void onItemClick(View view, int position) {


    }

    @Override
    public void onLongClick(View view, int position) {

    }

    @Override
    public void onClick(View view) {
        if (view.getId() == banRefresh.getId()) {
            refresh();
        }
    }

    class WifiListReceiver extends BroadcastReceiver {
        @Override
        public void onReceive(Context context, Intent intent) {
            results = mainWifi.getScanResults();
            for (ScanResult result : results) {
                AccessPoint accessPoint = new AccessPoint();
                accessPoint.setId(UUID.randomUUID().toString());
                accessPoint.setMac_address(result.BSSID);
                accessPoint.setSsid(result.SSID);
                accessPoint.setBssid(result.BSSID);
                accessPoint.setDescription(result.capabilities);
                Log.v(TAG, "  BSSID       =" + result.BSSID);
                Log.v(TAG, "  SSID        =" + result.SSID);
                Log.v(TAG, "  Capabilities=" + result.capabilities);
                Log.v(TAG, "  Frequency   =" + result.frequency);
                Log.v(TAG, "  Level       =" + result.level);
                Log.v(TAG, "---------------");
                if (result.level > -85)
                    Utilities.addAPtoProject(accessPoint, projectId);
            }
            wifiResultsAdapter.setResults(results);
            wifiResultsAdapter.notifyDataSetChanged();
        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
    }
}
