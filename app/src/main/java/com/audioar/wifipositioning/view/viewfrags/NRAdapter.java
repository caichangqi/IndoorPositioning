package com.audioar.wifipositioning.view.viewfrags;

import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.ViewGroup;
import android.widget.LinearLayout;
import android.widget.TextView;

import com.audioar.wifipositioning.R;
import com.audioar.wifipositioning.model.LocationWithDistance;

import java.util.ArrayList;
import java.util.List;

public class NRAdapter extends RecyclerView.Adapter<NRAdapter.ViewHolder> {
    private ArrayList<LocationWithDistance> readings = new ArrayList<>();

    @Override
    public NRAdapter.ViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
        LinearLayout linearLayout = (LinearLayout) LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_reference_reading, parent, false);
        // set the view's size, margins, paddings and layout parameters
        NRAdapter.ViewHolder vh = new NRAdapter.ViewHolder(linearLayout);
        return vh;
    }

    @Override
    public void onBindViewHolder(ViewHolder holder, int position) {
        holder.name.setText(readings.get(position).getName());
        holder.loc.setText(readings.get(position).getLocation());
        holder.distance.setText(String.valueOf(readings.get(position).getDistance()));
    }

    @Override
    public int getItemCount() {
        return readings.size();
    }

    public static class ViewHolder extends RecyclerView.ViewHolder {
        // each data item is just a string in this case
        public TextView name, loc, distance;

        public ViewHolder(LinearLayout v) {
            super(v);
            name = v.findViewById(R.id.wifi_ssid);
            loc = v.findViewById(R.id.wifi_bssid);
            distance = v.findViewById(R.id.wifi_level);
        }
    }

    public List<LocationWithDistance> getReadings() {
        return readings;
    }

    public void addAP(LocationWithDistance locationWithDistance) {
        readings.add(locationWithDistance);
    }

    public void setReadings(ArrayList<LocationWithDistance> readings) {
        this.readings = readings;
    }
}
