/*
* AnyPlace: A free and open Indoor Navigation Service with superb accuracy!
*
* Anyplace is a first-of-a-kind indoor information service offering GPS-less
* localization, navigation and search inside buildings using ordinary smartphones.
*
* Authors: C. Laoudias, G.Larkou, G. Constantinou, M. Constantinides, S. Nicolaou
* 
* Supervisor: Demetrios Zeinalipour-Yazti
*
* URL: http://anyplace.cs.ucy.ac.cy
* Contact: anyplace@cs.ucy.ac.cy
*
* Copyright (c) 2015, Data Management Systems Lab (DMSL), University of Cyprus.
* All rights reserved.
*
* Permission is hereby granted, free of charge, to any person obtaining a copy of
* this software and associated documentation files (the "Software"), to deal in the
* Software without restriction, including without limitation the rights to use, copy,
* modify, merge, publish, distribute, sublicense, and/or sell copies of the Software,
* and to permit persons to whom the Software is furnished to do so, subject to the
* following conditions:
*
* The above copyright notice and this permission notice shall be included in all
* copies or substantial portions of the Software.
*
* THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS
* OR IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
* FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
* AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
* LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING
* FROM, OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER
* DEALINGS IN THE SOFTWARE.
*
*/

package com.audioar.wifipositioning;

import android.util.Log;

import com.audioar.wifipositioning.model.AccessPoint;
import com.audioar.wifipositioning.model.Project;
import com.audioar.wifipositioning.model.LocationWithDistance;
import com.audioar.wifipositioning.model.LocationWithNearbyPlaces;
import com.audioar.wifipositioning.model.ReferencePoint;
import com.audioar.wifipositioning.model.WIfiNetwork;


import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;

import io.realm.RealmList;

public class CoreAlgorithm {

	public static LocationWithNearbyPlaces processingAlgorithms(List<WIfiNetwork> latestScanList, Project proj, int algorithm_choice) {

		int i, j;
		RealmList<AccessPoint> aps = proj.getAps();
		ArrayList<Float> observedRSSValues = new ArrayList<>();
		WIfiNetwork temp_LR;
		int notFoundCounter = 0;

		for (i = 0; i < aps.size(); ++i) {
			for (j = 0; j < latestScanList.size(); ++j) {
				temp_LR = latestScanList.get(j);
				if (aps.get(i).getMac_address().compareTo(temp_LR.getBssid()) == 0) {
					observedRSSValues.add(Float.valueOf(temp_LR.getLevel()).floatValue());
					break;
				}
			}
			if (j == latestScanList.size()) {
				observedRSSValues.add(SharedConstants.NaN);
				++notFoundCounter;
			}
		}

		if (notFoundCounter == aps.size())
			return null;

		switch (algorithm_choice) {
			case 1:
				return KNN_Algorithm(proj, observedRSSValues, 1, false);
			case 2:
				return KNN_Algorithm(proj, observedRSSValues, 4, true);
			case 3:
				return BAYES_Algorithm(proj, observedRSSValues, 10f, false);
			case 4:
				return BAYES_Algorithm(proj, observedRSSValues, 10f, true);
			case 5:
				return COS_Algorithm(proj, observedRSSValues);
			}
		return null;

	}

	public static final String TAG = "Algorithm";

	private static LocationWithNearbyPlaces COS_Algorithm(Project proj, ArrayList<Float> observedRSSValues) {
		Log.d(TAG, "COS_Algorithm");

		RealmList<AccessPoint> rssValues;
		double curResult;
		ArrayList<LocationWithDistance> locationWithDistanceResultsList = new ArrayList<LocationWithDistance>();
		String myLocation;

		for (ReferencePoint referencePoint : proj.getRps()) {
			rssValues = referencePoint.getReadings();
			curResult = calculateCosDistance(rssValues, observedRSSValues);

			locationWithDistanceResultsList.add(0, new LocationWithDistance(curResult, referencePoint.getLocId(), referencePoint.getName()));
		}

		Collections.sort(locationWithDistanceResultsList, new Comparator<LocationWithDistance>() {
			public int compare(LocationWithDistance gd1, LocationWithDistance gd2) {
				return (gd1.getDistance() > gd2.getDistance() ? 1 : (gd1.getDistance() == gd2.getDistance() ? 0 : -1));
			}
		});

		myLocation = calculateAverageKDistanceLocations(locationWithDistanceResultsList, 1);

		LocationWithNearbyPlaces places = new LocationWithNearbyPlaces(myLocation, locationWithDistanceResultsList);
		return places;
	}

	private static LocationWithNearbyPlaces KNN_Algorithm(Project proj, ArrayList<Float> observedRSSValues, int K, boolean isWeighted) {
		Log.d(TAG, "KNN_Algorithm K = " + Integer.toString(K));

		RealmList<AccessPoint> rssValues;
		float curResult;
		ArrayList<LocationWithDistance> locationWithDistanceResultsList = new ArrayList<LocationWithDistance>();
		String myLocation;

		for (ReferencePoint referencePoint : proj.getRps()) {
			rssValues = referencePoint.getReadings();
			curResult = calculateEuclideanDistance(rssValues, observedRSSValues);

			if (curResult == Float.NEGATIVE_INFINITY)
				return null;

			locationWithDistanceResultsList.add(0, new LocationWithDistance(curResult, referencePoint.getLocId(), referencePoint.getName()));
		}

		Collections.sort(locationWithDistanceResultsList, new Comparator<LocationWithDistance>() {
			public int compare(LocationWithDistance gd1, LocationWithDistance gd2) {
				return (gd1.getDistance() > gd2.getDistance() ? 1 : (gd1.getDistance() == gd2.getDistance() ? 0 : -1));
			}
		});

		if (!isWeighted) {
			myLocation = calculateAverageKDistanceLocations(locationWithDistanceResultsList, K);
		} else {
			myLocation = calculateWeightedAverageKDistanceLocations(locationWithDistanceResultsList, K);
		}

		LocationWithNearbyPlaces places = new LocationWithNearbyPlaces(myLocation, locationWithDistanceResultsList);

		return places;
	}




	private static LocationWithNearbyPlaces BAYES_Algorithm(Project proj, ArrayList<Float> observedRssValues, float sGreek, boolean isWeighted) {
		RealmList<AccessPoint> rssValues;
		double curResult = 0.0d;
		String myLocation = null;
		double highestProbability = Double.NEGATIVE_INFINITY;
		ArrayList<LocationWithDistance> locationWithDistanceResultsList = new ArrayList<LocationWithDistance>();

		for (ReferencePoint referencePoint : proj.getRps()) {
			rssValues = referencePoint.getReadings();
			curResult = calculateProbability(rssValues, observedRssValues, sGreek);

			if (curResult == Double.NEGATIVE_INFINITY)
				return null;
			else if (curResult > highestProbability) {
				highestProbability = curResult;
				myLocation = referencePoint.getLocId();
			}

			if (isWeighted)
				locationWithDistanceResultsList.add(0, new LocationWithDistance(curResult, referencePoint.getLocId(), referencePoint.getName()));
		}

		if (isWeighted)
			myLocation = calculateWeightedAverageProbabilityLocations(locationWithDistanceResultsList);
		LocationWithNearbyPlaces places = new LocationWithNearbyPlaces(myLocation, locationWithDistanceResultsList);
		return places;
	}

	private static float calculateEuclideanDistance(RealmList<AccessPoint> l1, ArrayList<Float> l2) {

		float finalResult = 0;
		float v1;
		float v2;
		float temp = 0;

		for (int i = 0; i < l1.size(); ++i) {

			try {
				l1.get(i).getMeanRss();
				v1 = (float) l1.get(i).getMeanRss();
				v2 = l2.get(i);
			} catch (Exception e) {
				return Float.NEGATIVE_INFINITY;
			}

			// do the procedure
			if (v2 != SharedConstants.NaN) {
				temp = v1 - v2;
				temp *= temp;
			}

			// do the procedure
			finalResult += temp;
		}
		return finalResult;
	}

	private static double calculateCosDistance(RealmList<AccessPoint> l1, ArrayList<Float> l2) {
		double A = 0, B = 0, AB = 0;
		for (int i = 0; i < l2.size(); i++) {
			double a = l1.get(i).getMeanRss();
			double b = l2.get(i);
			A += a * a;
			B += b * b;
			AB += a * b;
		}
		return Math.acos(AB / Math.sqrt(A) / Math.sqrt(B));
	}

	private static double calculateProbability(RealmList<AccessPoint> l1, ArrayList<Float> l2, float sGreek) {
		double finalResult = 1;
		float v1;
		float v2;
		double temp;

		for (int i = 0; i < l1.size(); ++i) {

			try {
				v1 = (float) l1.get(i).getMeanRss();
				v2 = l2.get(i);
			} catch (Exception e) {
				return Double.NEGATIVE_INFINITY;
			}

			temp = v1 - v2;

			temp *= temp;

			temp = -temp;

			temp /= (double) (sGreek * sGreek);
			temp = 100.0 * Math.exp(temp);

			//Do not allow ze ro instead stop on small possibility
			if (finalResult * temp != 0)
				finalResult = finalResult * temp;
		}
		return finalResult;
	}

	private static String calculateAverageKDistanceLocations(ArrayList<LocationWithDistance> locationWithDistance_Results_List, int K) {
		float sumX = 0.0f;
		float sumY = 0.0f;

		String[] LocationArray;
		float x, y;

		int K_Min = K < locationWithDistance_Results_List.size() ? K : locationWithDistance_Results_List.size();

		// Calculate the sum of X and Y
		for (int i = 0; i < K_Min; ++i) {
			LocationArray = locationWithDistance_Results_List.get(i).getLocation().split(" ");

			try {
				x = Float.valueOf(LocationArray[0].trim()).floatValue();
				y = Float.valueOf(LocationArray[1].trim()).floatValue();
			} catch (Exception e) {
				return null;
			}

			sumX += x;
			sumY += y;
		}

		// Calculate the average
		sumX /= K_Min;
		sumY /= K_Min;

		return sumX + " " + sumY;
	}

	private static String calculateWeightedAverageKDistanceLocations(ArrayList<LocationWithDistance> locationWithDistance_Results_List, int K) {
		double LocationWeight = 0.0f;
		double sumWeights = 0.0f;
		double WeightedSumX = 0.0f;
		double WeightedSumY = 0.0f;

		String[] LocationArray = new String[2];
		float x, y;

		int K_Min = K < locationWithDistance_Results_List.size() ? K : locationWithDistance_Results_List.size();

		// Calculate the weighted sum of X and Y
		for (int i = 0; i < K_Min; ++i) {
			if (locationWithDistance_Results_List.get(i).getDistance() != 0.0) {
				LocationWeight = 100000 / Math.pow(locationWithDistance_Results_List.get(i).getDistance(), 2);
			} else {
				LocationWeight = 100;
			}
			LocationArray = locationWithDistance_Results_List.get(i).getLocation().split(" ");

			try {
				x = Float.valueOf(LocationArray[0].trim()).floatValue();
				y = Float.valueOf(LocationArray[1].trim()).floatValue();
			} catch (Exception e) {
				return null;
			}

			sumWeights += LocationWeight;
			WeightedSumX += LocationWeight * x;
			WeightedSumY += LocationWeight * y;
		}

		WeightedSumX /= sumWeights;
		WeightedSumY /= sumWeights;

		return WeightedSumX + " " + WeightedSumY;
	}

	private static String calculateWeightedAverageProbabilityLocations(ArrayList<LocationWithDistance> locationWithDistance_Results_List) {
		double sumProbabilities = 0.0f;
		double WeightedSumX = 0.0f;
		double WeightedSumY = 0.0f;
		double NP;
		float x, y;
		String[] LocationArray;

		for (int i = 0; i < locationWithDistance_Results_List.size(); ++i)
			sumProbabilities += locationWithDistance_Results_List.get(i).getDistance();

		for (int i = 0; i < locationWithDistance_Results_List.size(); ++i) {
			LocationArray = locationWithDistance_Results_List.get(i).getLocation().split(" ");

			try {
				x = Float.valueOf(LocationArray[0].trim()).floatValue();
				y = Float.valueOf(LocationArray[1].trim()).floatValue();
			} catch (Exception e) {
				return null;
			}

			NP = locationWithDistance_Results_List.get(i).getDistance() / sumProbabilities;

			WeightedSumX += (x * NP);
			WeightedSumY += (y * NP);
		}

		return WeightedSumX + " " + WeightedSumY;
	}

}
