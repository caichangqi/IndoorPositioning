package com.audioar.wifipositioning.view.viewfrags;

import android.support.v7.widget.RecyclerView;
import android.view.View;
import android.widget.TextView;

import com.audioar.wifipositioning.R;

public class SectionHeader extends RecyclerView.ViewHolder {
    final TextView title;


    public SectionHeader(View headerView) {
        super(headerView);
        title = (TextView) headerView.findViewById(R.id.tv_section_name);
    }
}
