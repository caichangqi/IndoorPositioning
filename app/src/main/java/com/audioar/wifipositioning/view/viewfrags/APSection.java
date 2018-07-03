package com.audioar.wifipositioning.view.viewfrags;

import android.support.v7.widget.RecyclerView;
import android.view.View;

import com.audioar.wifipositioning.model.AccessPoint;

import java.util.ArrayList;
import java.util.List;

import io.github.luizgrp.sectionedrecyclerviewadapter.SectionParameters;
import io.github.luizgrp.sectionedrecyclerviewadapter.StatelessSection;

public class APSection extends StatelessSection {
    private List<AccessPoint> accessPoints = new ArrayList<>();

    public APSection(SectionParameters sectionParameters) {
        super(sectionParameters);
    }

    @Override
    public int getContentItemsTotal() {
        return accessPoints.size();
    }

    @Override
    public RecyclerView.ViewHolder getItemViewHolder(View view) {
        return new PointViewHolder(view);
    }

    @Override
    public void onBindItemViewHolder(RecyclerView.ViewHolder holder, int position) {
        PointViewHolder itemHolder = (PointViewHolder) holder;
        AccessPoint accessPoint = accessPoints.get(position);
        itemHolder.tvIdentifier.setText(accessPoint.getSsid());
        itemHolder.tvIdentifier2.setText(accessPoint.getMac_address());
        itemHolder.tvPointX.setText("-");
        itemHolder.tvPointY.setText("-");
    }

    public List<AccessPoint> getAccessPoints() {
        return accessPoints;
    }

    public void setAccessPoints(List<AccessPoint> accessPoints) {
        this.accessPoints = accessPoints;
    }

    @Override
    public void onBindHeaderViewHolder(RecyclerView.ViewHolder holder) {
        super.onBindHeaderViewHolder(holder);
        SectionHeader headerViewHolder = (SectionHeader) holder;
        headerViewHolder.title.setText("接入点列表");
    }

    @Override
    public RecyclerView.ViewHolder getHeaderViewHolder(View view) {
        return new SectionHeader(view);
    }
}
