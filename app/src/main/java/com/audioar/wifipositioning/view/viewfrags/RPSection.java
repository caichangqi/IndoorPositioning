package com.audioar.wifipositioning.view.viewfrags;

import android.support.v7.widget.RecyclerView;
import android.view.View;

import com.audioar.wifipositioning.model.ReferencePoint;

import java.util.ArrayList;
import java.util.List;

import io.github.luizgrp.sectionedrecyclerviewadapter.SectionParameters;
import io.github.luizgrp.sectionedrecyclerviewadapter.StatelessSection;

public class RPSection extends StatelessSection {
    private List<ReferencePoint> referencePoints = new ArrayList<>();


    public RPSection(SectionParameters sectionParameters) {
        super(sectionParameters);
    }

    @Override
    public int getContentItemsTotal() {
        return referencePoints.size();
    }

    @Override
    public RecyclerView.ViewHolder getItemViewHolder(View view) {
        return new PointViewHolder(view);
    }

    @Override
    public void onBindItemViewHolder(RecyclerView.ViewHolder holder, int position) {
        PointViewHolder itemHolder = (PointViewHolder) holder;
        ReferencePoint referencePoint = referencePoints.get(position);
        itemHolder.tvIdentifier.setText(referencePoint.getName());
        String x = String.valueOf(referencePoint.getX());
        String y = String.valueOf(referencePoint.getY());
        if(x.length() > 10) x = x.substring(0, 9);
        if(y.length() > 10) x = x.substring(0, 9);
        itemHolder.tvPointX.setText(x);
        itemHolder.tvPointY.setText(y);
    }

    @Override
    public void onBindHeaderViewHolder(RecyclerView.ViewHolder holder) {
        super.onBindHeaderViewHolder(holder);
        SectionHeader headerViewHolder = (SectionHeader) holder;
        headerViewHolder.title.setText("参考点列表");
    }

    @Override
    public RecyclerView.ViewHolder getHeaderViewHolder(View view) {
        return new SectionHeader(view);
    }

    public List<ReferencePoint> getReferencePoints() {
        return referencePoints;
    }

    public void setReferencePoints(List<ReferencePoint> referencePoints) {
        this.referencePoints = referencePoints;
    }
}
