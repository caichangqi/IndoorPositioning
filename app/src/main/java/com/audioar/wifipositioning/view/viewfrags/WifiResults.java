package com.audioar.wifipositioning.view.viewfrags;

import android.net.wifi.ScanResult;
import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.ViewGroup;
import android.widget.LinearLayout;
import android.widget.TextView;

import com.audioar.wifipositioning.R;

import java.util.ArrayList;
import java.util.List;

public class WifiResults extends RecyclerView.Adapter<WifiResults.ViewHolder> {
    private List<ScanResult> results = new ArrayList<>();

    @Override
    public WifiResults.ViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
        LinearLayout linearLayout = (LinearLayout) LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_wifi_result, parent, false);
        WifiResults.ViewHolder vh = new WifiResults.ViewHolder(linearLayout);
        return vh;
    }

    @Override
    public void onBindViewHolder(ViewHolder holder, int position) {
        holder.bssid.setText("MAC: "+results.get(position).BSSID);
        holder.ssid.setText("SSID: "+results.get(position).SSID);
        holder.capabilities.setText("Type: "+results.get(position).capabilities);
        holder.frequency.setText("Frequency: "+String.valueOf(results.get(position).frequency));
        holder.level.setText(String.valueOf("RSSI:"+results.get(position).level));
    }

    @Override
    public int getItemCount() {
        return results.size();
    }

    public static class ViewHolder extends RecyclerView.ViewHolder {
        public TextView bssid, ssid, capabilities, level, frequency;

        public ViewHolder(LinearLayout v) {
            super(v);
            bssid = v.findViewById(R.id.wifi_bssid);
            ssid = v.findViewById(R.id.wifi_ssid);
            capabilities = v.findViewById(R.id.wifi_capabilities);
            frequency = v.findViewById(R.id.wifi_frequency);
            level = v.findViewById(R.id.wifi_level);
        }
    }

    public List<ScanResult> getResults() {
        return results;
    }

    public void setResults(List<ScanResult> results) {
        this.results = results;
    }
}
