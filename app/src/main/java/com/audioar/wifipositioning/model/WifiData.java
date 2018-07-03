package com.audioar.wifipositioning.model;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import android.net.wifi.ScanResult;
import android.os.Parcel;
import android.os.Parcelable;

public class WifiData implements Parcelable {
	private List<WIfiNetwork> mNetworks;

	public WifiData() {
		mNetworks = new ArrayList<>();
	}

	public WifiData(Parcel in) {
		in.readTypedList(mNetworks, WIfiNetwork.CREATOR);
	}

	public static final Creator<WifiData> CREATOR = new Creator<WifiData>() {
		public WifiData createFromParcel(Parcel in) {
			return new WifiData(in);
		}

		public WifiData[] newArray(int size) {
			return new WifiData[size];
		}
	};

	public void addNetworks(List<ScanResult> results) {
		mNetworks.clear();
		for (ScanResult result : results) {
			mNetworks.add(new WIfiNetwork(result));
		}
		Collections.sort(mNetworks);
	}

	@Override
	public int describeContents() {
		return 0;
	}

	@Override
	public void writeToParcel(Parcel dest, int flags) {
		dest.writeTypedList(mNetworks);
	}

	@Override
	public String toString() {
		if (mNetworks == null || mNetworks.size() == 0)
			return "Empty data";
		else
			return mNetworks.size() + " networks data";
	}

	public List<WIfiNetwork> getNetworks() {
		return mNetworks;
	}
}
