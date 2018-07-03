package com.audioar.wifipositioning.view;

import android.content.Context;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.os.Bundle;
import android.os.SystemClock;
import android.support.v7.app.AppCompatActivity;
import android.view.View;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.SeekBar;
import android.widget.TextView;
import android.widget.ZoomControls;

import com.audioar.wifipositioning.R;
import com.baidu.location.BDAbstractLocationListener;
import com.baidu.location.BDLocation;
import com.baidu.location.LocationClient;
import com.baidu.location.LocationClientOption;
import com.baidu.mapapi.map.BaiduMap;
import com.baidu.mapapi.map.BitmapDescriptorFactory;
import com.baidu.mapapi.map.MapStatus;
import com.baidu.mapapi.map.MapStatusUpdate;
import com.baidu.mapapi.map.MapStatusUpdateFactory;
import com.baidu.mapapi.map.MapView;
import com.baidu.mapapi.map.Marker;
import com.baidu.mapapi.map.MarkerOptions;
import com.baidu.mapapi.map.MyLocationData;
import com.baidu.mapapi.map.OverlayOptions;
import com.baidu.mapapi.map.PolylineOptions;
import com.baidu.mapapi.map.UiSettings;
import com.baidu.mapapi.model.LatLng;

import java.util.ArrayList;
import java.util.List;

import static android.hardware.Sensor.TYPE_ACCELEROMETER;
import static android.hardware.Sensor.TYPE_MAGNETIC_FIELD;
import static android.hardware.Sensor.TYPE_ROTATION_VECTOR;
import static android.hardware.Sensor.TYPE_STEP_COUNTER;
import static android.hardware.Sensor.TYPE_STEP_DETECTOR;


public class PDRActivity extends AppCompatActivity implements SensorEventListener {

    SensorManager mSensorManager;
    Sensor mStepDetector;
    Sensor mStepCounter;
    Sensor mAccelerometer;
    Sensor mMagneticField;
    Sensor mRotationVector;

    static final double X = 0.00000899321619;
    static final double Y = 0.00001049178636;

    private static float orientationStable = 0f;

    static final String COUNTED_STEPS = "counted steps";
    static final String DETECTED_STEPS = "detected steps";
    static final String INITIAL_COUNTED_STEPS = "initial detected steps";

    static double stepLengthConstant = 75;
    static double stepLengthHeight = 82;
    double stepLength;

    Boolean isSensorStepDetectorPresent = false;
    Boolean isSensorStepCounterPresent = false;
    Boolean isSensorAccelerometerPresent = false;
    Boolean isSensorMagneticFieldPresent = false;
    Boolean isSensorRotationVectorPresent = false;
    Boolean lastAccelerometerSet = false;
    Boolean lastMagnetometerSet = false;
    Boolean isOrientationSet = false;
    Boolean stepCountingActive = false;

    int numberOfStepsDetected = 0;
    int numberOfStepsCounted = 0;
    int orientationInDegrees = 0;
    int initialStepCounterValue = 0;

    double distance = 0;
    double distanceHeight = 0;
    double distanceFrequency = 0;
    double detectedStepsSensorValue = 0;
    double countedStepsSensorValue = 0;

    String orientation;

    float [] lastAccelerometer = new float[3];
    float [] lastMagnetometer = new float[3];

    float [] mRotationMatrix = new float[9];
    float [] mOrientationAngles = new float[3];

    double meanOrientationAngles = 0;
    double sumSinAngles = 0;
    double sumCosAngles = 0;
    long counter = 0;

    int azimuthInDegress = 0;

    float [] mRotationMatrixFromVector = new float[16];

    long timeCountingStarted = 0;
    long timeOfStep;
    double stepFrequency = 0;
    long totalTime = 0;

    double stepMeanFrequency = 0;
    double stepMeanTime = 0;
    double stepMeanAccDiff = 0;

    ArrayList<Long> stepTimeStamp = new ArrayList<>();

    double accelerationTotalMax = 0;
    double accelerationTotalMin = 0;
    double azimuthInRadians = 0;
    double sumAccData = 0;

    double accelerationTotal = 0;

    static final float ALPHA = 0.25f;

    private class BaiduMapManager extends BDAbstractLocationListener implements BaiduMap.OnMarkerDragListener {
        private MapView mMapView;
        private BaiduMap mBaiduMap;
        private LocationClient mLocationClient;
        private boolean isFirstLoc = true;
        private Marker mMarker;

        private ArrayList<Double> displacements = new ArrayList<>();
        private ArrayList<Double> directions = new ArrayList<>();


        void onCreate() {
            mMapView = findViewById(R.id.bmapView);
            mBaiduMap = mMapView.getMap();
            View child = mMapView.getChildAt(1);
            if (child != null && (child instanceof ImageView || child instanceof ZoomControls)){
                child.setVisibility(View.INVISIBLE);
            }
            mMapView.showScaleControl(false);
            mMapView.showZoomControls(false);

            // Zoom to the largest possible state as default :
            MapStatus mapStatus = new MapStatus.Builder().zoom(21).build();
            MapStatusUpdate mapStatusUpdate = MapStatusUpdateFactory.newMapStatus(mapStatus);
            mBaiduMap.setMapStatus(mapStatusUpdate);

            // Set location service listeners :
            mLocationClient = new LocationClient(PDRActivity.this);
            mLocationClient.registerLocationListener(this);
            LocationClientOption option = new LocationClientOption();
            option.setCoorType("bd09ll");
            option.setLocationMode(LocationClientOption.LocationMode.Hight_Accuracy);
            option.setOpenGps(true);
            option.setScanSpan(1000);
            mLocationClient.setLocOption(option);

            // Shutting down zooming and shifting operations :
            UiSettings uiSettings = mBaiduMap.getUiSettings();
            uiSettings.setZoomGesturesEnabled(false);
            uiSettings.setOverlookingGesturesEnabled(false);

            // Setting the GPS circle on the map, you need to implement a toggle button to shift to ... WIFI locationing.
            mBaiduMap.setMyLocationEnabled(true);

            // Setting the listener
            mBaiduMap.setOnMarkerDragListener(this);
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
            // Shift the map on first location;
            if (isFirstLoc) {
                mBaiduMap.setMyLocationData(data);
                isFirstLoc = false;
                LatLng latLng = new LatLng(bdLocation.getLatitude(), bdLocation.getLongitude());
                MapStatusUpdate mapStatusUpdate = MapStatusUpdateFactory.newLatLng(latLng);
                mBaiduMap.animateMapStatus(mapStatusUpdate);
                drawDraggablePinAtCurrentLocation();
                mLocationClient.stop();
                mBaiduMap.setMyLocationEnabled(false);
            }
        }

        void drawDraggablePinAtCurrentLocation() {
            mBaiduMap.clear();
            MyLocationData locationData = mBaiduMap.getLocationData();
            OverlayOptions options = new MarkerOptions()
                    .position(new LatLng(locationData.latitude, locationData.longitude))
                    .icon(BitmapDescriptorFactory.fromResource(R.drawable.icon_gcoding))
                    .zIndex(21)
                    .draggable(true);
            mMarker = (Marker) mBaiduMap.addOverlay(options);
        }

        void drawPath(LatLng beginningPosition) {
            if (directions.size() < 2) return;
            mBaiduMap.clear();
            // redraw the pin
            OverlayOptions options = new MarkerOptions()
                    .position(beginningPosition)
                    .icon(BitmapDescriptorFactory.fromResource(R.drawable.icon_gcoding))
                    .zIndex(21)
                    .draggable(true);
            mMarker = (Marker) mBaiduMap.addOverlay(options);
            // draw the recorded path.
            //构建折线点坐标
            List<LatLng> points = new ArrayList<LatLng>();
            points.add(beginningPosition);
            for (int i = 0; i < directions.size(); i++) {
                points.add(increment(points.get(points.size() - 1), displacements.get(i), directions.get(i)));
            }
            //绘制折线
            OverlayOptions ooPolyline = new PolylineOptions().width(2)
                    .color(0xAAFF0000).points(points);
            mBaiduMap.addOverlay(ooPolyline);
        }

        void addPathSegment(double displacement, float directionInDegree) {
            displacements.add(displacement);
            directions.add((double)directionInDegree);
            drawPath(mMarker.getPosition());
        }

        private LatLng increment(LatLng original, double displacement, double degree) {
            double dx = Math.cos(degree * Math.PI / 180) * displacement * X;
            double dy = Math.sin(degree * Math.PI / 180) * displacement * Y;
            return new LatLng(original.latitude + dx / 100D, original.longitude + dy / 100D);
        }

        @Override
        public void onMarkerDrag(Marker marker) {

        }

        @Override
        public void onMarkerDragEnd(Marker marker) {
            LatLng position = marker.getPosition();
            drawPath(position);
        }

        @Override
        public void onMarkerDragStart(Marker marker) {

        }

        private void setDirection(float degree) {
            MapStatus mapStatus = new MapStatus.Builder().rotate(degree).build();
            mBaiduMap.setMapStatus(MapStatusUpdateFactory.newMapStatus(mapStatus));
        }
    }

    private BaiduMapManager mBaiduMapManager;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_pdr);


        mSensorManager = (SensorManager) getSystemService(Context.SENSOR_SERVICE);
        if (mSensorManager.getDefaultSensor(TYPE_STEP_DETECTOR) != null) {
            mStepDetector = mSensorManager.getDefaultSensor(TYPE_STEP_DETECTOR);
            isSensorStepDetectorPresent = true;
        }
        if (mSensorManager.getDefaultSensor(TYPE_ACCELEROMETER) != null) {
            mAccelerometer = mSensorManager.getDefaultSensor(TYPE_ACCELEROMETER);
            isSensorAccelerometerPresent = true;
        }
        if (mSensorManager.getDefaultSensor(TYPE_MAGNETIC_FIELD) != null) {
            mMagneticField = mSensorManager.getDefaultSensor(TYPE_MAGNETIC_FIELD);
            isSensorMagneticFieldPresent = true;
        }
        if (mSensorManager.getDefaultSensor(TYPE_ROTATION_VECTOR) != null) {
            mRotationVector = mSensorManager.getDefaultSensor(TYPE_ROTATION_VECTOR);
            isSensorRotationVectorPresent = true;
        }
        if (mSensorManager.getDefaultSensor(TYPE_STEP_COUNTER) != null) {
            mStepCounter = mSensorManager.getDefaultSensor(TYPE_STEP_COUNTER);
            isSensorStepCounterPresent = true;
        }

        mBaiduMapManager = new BaiduMapManager();
        mBaiduMapManager.onCreate();
    }

    @Override
    public void onSensorChanged(SensorEvent event) {
        switch (event.sensor.getType()) {
            case TYPE_STEP_DETECTOR:
                if (stepCountingActive) {
                    numberOfStepsDetected++;
                    detectedStepsSensorValue++;

                    distance = distance + stepLengthConstant;
                    distanceHeight = distanceHeight + stepLengthHeight;

                    stepTimeStamp.add(event.timestamp);

                    if (numberOfStepsDetected == 1){
                        timeOfStep = event.timestamp/1000000L - timeCountingStarted;
                        totalTime = 0;
                        distanceFrequency = distanceFrequency + stepLengthHeight;
                    }

                    else {
                        timeOfStep = (event.timestamp - stepTimeStamp.get((stepTimeStamp.size() - 1) - 1)) /1000000L;

                        if (timeOfStep > 1000 ){
                            counter = 0;
                            meanOrientationAngles = 0;
                            sumCosAngles = 0;
                            sumSinAngles = 0;
                        }

                        totalTime = totalTime + timeOfStep;
                        stepFrequency = 1000D / timeOfStep;

                        stepLength = 44 * stepFrequency + 4.4;

                        distanceFrequency = distanceFrequency + stepLength;

                        stepMeanFrequency = (detectedStepsSensorValue - 1)* 1000D / totalTime;
                        stepMeanTime = totalTime / (detectedStepsSensorValue -  1);

                        sumAccData = sumAccData + Math.sqrt(accelerationTotalMax - accelerationTotalMin);
                        stepMeanAccDiff = sumAccData / (detectedStepsSensorValue - 1);
                    }


                    if (lastAccelerometerSet && lastMagnetometerSet) {
                        mSensorManager.getRotationMatrix(mRotationMatrix, null, lastAccelerometer, lastMagnetometer);
                        mSensorManager.getOrientation(mRotationMatrix, mOrientationAngles);

                        azimuthInRadians = mOrientationAngles[0];

                        azimuthInDegress = (int) (((azimuthInRadians * 180 / (float) Math.PI) + 360) % 360);

                        isOrientationSet = true;

                        orientation = "" + azimuthInDegress;

                    }

                    TextView detectedSteps = (TextView) findViewById(R.id.stepsDetectedTextView);
                    detectedSteps.setText("" + detectedStepsSensorValue);

                    TextView distanceView = (TextView) findViewById(R.id.distanceTextView);
                    distanceView.setText(Double.toString(distance/100D));

                    TextView distanceHeightView = (TextView) findViewById(R.id.distanceHeight);
                    distanceHeightView.setText(Double.toString(distanceHeight/100D));

                    TextView distanceFrequencyView = (TextView) findViewById(R.id.distanceFreq);
                    distanceFrequencyView.setText(String.format("%.2f",distanceFrequency/100D));

                    TextView TotalTimeView = (TextView) findViewById(R.id.totalTime);
                    TotalTimeView.setText(Long.toString(totalTime));

                    TextView meanFreqView = (TextView) findViewById(R.id.meanFreq);
                    meanFreqView.setText(String.format("%.5f",stepMeanFrequency));

                    TextView meanAccqView = (TextView) findViewById(R.id.meanAccdiff);
                    meanAccqView.setText(String.format("%.5f",stepMeanAccDiff));

                    onStepDetected(stepLength, orientationStable);


                    accelerationTotalMax = 0;
                    accelerationTotalMin = 0;
                    counter = 0;
                    meanOrientationAngles = 0;
                    sumCosAngles = 0;
                    sumSinAngles = 0;
                }


                break;

            case TYPE_STEP_COUNTER:

                if (stepCountingActive) {
                    if (initialStepCounterValue < 1) {
                        initialStepCounterValue = (int) event.values[0];
                    }

                    numberOfStepsCounted = (int) event.values[0] - initialStepCounterValue;

                    if (numberOfStepsCounted > numberOfStepsDetected) {

                        distance = distance + (numberOfStepsCounted - numberOfStepsDetected) * stepLengthConstant;
                        distanceHeight = distanceHeight + (numberOfStepsCounted - numberOfStepsDetected) * stepLengthHeight;

                        if (stepFrequency > 0){
                            distanceFrequency += (numberOfStepsCounted - numberOfStepsDetected) * (stepLength);
                        }else {
                            distanceFrequency += (numberOfStepsCounted - numberOfStepsDetected) * stepLengthHeight;
                        }

                        numberOfStepsDetected = numberOfStepsCounted;

                        TextView distanceView = (TextView) findViewById(R.id.distanceTextView);
                        distanceView.setText(Double.toString(distance/100D));

                        TextView distanceHeightView = (TextView) findViewById(R.id.distanceHeight);
                        distanceHeightView.setText(Double.toString(distanceHeight/100D));

                        TextView distanceFrequencyView = (TextView) findViewById(R.id.distanceFreq);
                        distanceFrequencyView.setText(String.format("%.2f",distanceFrequency/100D));

                    }

                    TextView countedSteps = (TextView) findViewById(R.id.countedStepsTextView);
                    countedSteps.setText(String.valueOf(numberOfStepsCounted));

                }
                else {
                    initialStepCounterValue = (int) event.values[0];
                }

                break;

            case TYPE_ACCELEROMETER:

                lastAccelerometerSet = true;

                System.arraycopy(event.values, 0, lastAccelerometer, 0, event.values.length);

                if (stepCountingActive && numberOfStepsDetected > 0){

                    long timeElapsedFromLastStep = event.timestamp - stepTimeStamp.get(stepTimeStamp.size() - 1);


                    if (event.timestamp/1000000L - stepTimeStamp.get(stepTimeStamp.size() - 1)/1000000L
                            < 1500){

                         accelerationTotal =
                                Math.sqrt(Math.pow(event.values[0], 2) +
                                        Math.pow(event.values[1], 2) +
                                        Math.pow(event.values[2], 2));

                        if (accelerationTotalMin == 0){
                            accelerationTotalMin = accelerationTotal;
                        }
                        else if(accelerationTotal < accelerationTotalMin) {
                            accelerationTotalMin = accelerationTotal;

                        }
                        if (accelerationTotalMax == 0){
                            accelerationTotalMax = accelerationTotal;
                        }
                        else if (accelerationTotal > accelerationTotalMax){
                            accelerationTotalMax = accelerationTotal;
                        }
                    }
                    else{
                        accelerationTotalMax = 0;
                        accelerationTotalMin = 0;
                    }


                }

                break;

            case TYPE_MAGNETIC_FIELD:

                lastMagnetometerSet = true;

                System.arraycopy(event.values, 0, lastMagnetometer, 0, event.values.length);

                lastMagnetometer = lowPass(event.values.clone(), lastMagnetometer);

                if (lastAccelerometerSet && lastMagnetometerSet)
                {
                    mSensorManager.getRotationMatrix(mRotationMatrix, null, lastAccelerometer, lastMagnetometer);
                    mSensorManager.getOrientation(mRotationMatrix, mOrientationAngles);

                    float azimuthInRadians = mOrientationAngles[0];

                    int azimuthInDegress = ((int)(azimuthInRadians * 180/(float) Math.PI) + 360) % 360;

                    TextView orientationView = (TextView) findViewById(R.id.orientationTextView);
                    orientationView.setText("" + azimuthInDegress);
                }

                break;

            case TYPE_ROTATION_VECTOR:

                SensorManager.getRotationMatrixFromVector(mRotationMatrixFromVector, event.values);
                SensorManager.getOrientation(mRotationMatrixFromVector, mOrientationAngles);

                orientationInDegrees = ((int)(mOrientationAngles[0] * 180/(float) Math.PI) + 360) % 360;

                if (detectedStepsSensorValue > 0){
                    sumCosAngles += Math.cos(mOrientationAngles[0]);
                    sumSinAngles += Math.sin(mOrientationAngles[0]);
                    meanOrientationAngles += mOrientationAngles[0];
                    counter ++;
                }

                TextView orientationView = (TextView) findViewById(R.id.rotationVectorTextView);
                orientationView.setText("" + orientationInDegrees);

                TextView rotVectAccuracy = (TextView) findViewById(R.id.rotationVectorAccView);
                rotVectAccuracy.setText("" + event.values[4]);
                orientationStable = ((orientationInDegrees - getAngleDisplacement()) + 360 ) % 360;
                mBaiduMapManager.setDirection(orientationStable);
                break;
        }
    }


    protected float[] lowPass( float[] input, float[] output ) {
        if ( output == null ) return input;

        for ( int i=0; i<input.length; i++ ) {
            output[i] = output[i] + ALPHA * (input[i] - output[i]);
        }
        return output;
    }

    @Override
    public void onAccuracyChanged(Sensor sensor, int i) {

    }

    @Override
    protected void onStop() {
        super.onStop();
        mSensorManager.unregisterListener(this);
    }

    @Override
    protected void onResume() {
        super.onResume();
        if (isSensorStepDetectorPresent) {
            mSensorManager.registerListener(this, mStepDetector,
                    SensorManager.SENSOR_DELAY_FASTEST);
        }
        if (isSensorAccelerometerPresent) {
            mSensorManager.registerListener(this, mAccelerometer,
                    SensorManager.SENSOR_DELAY_FASTEST);
        }
        if (isSensorMagneticFieldPresent) {
            mSensorManager.registerListener(this, mMagneticField,
                    SensorManager.SENSOR_DELAY_FASTEST);
        }
        if (isSensorRotationVectorPresent) {
            mSensorManager.registerListener(this, mRotationVector,
                    SensorManager.SENSOR_DELAY_FASTEST);
        }
        if (isSensorStepCounterPresent) {
            mSensorManager.registerListener(this, mStepCounter,
                    SensorManager.SENSOR_DELAY_FASTEST);
        }
        mBaiduMapManager.onResume();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        mBaiduMapManager.onDestroy();
    }

    @Override
    protected void onPause() {
        super.onPause();
        mBaiduMapManager.onPause();
    }

    @Override
    public void onSaveInstanceState(Bundle savedInstanceState) {

        super.onSaveInstanceState(savedInstanceState);
        savedInstanceState.putInt(DETECTED_STEPS, numberOfStepsDetected);
        savedInstanceState.putInt(COUNTED_STEPS, numberOfStepsCounted);
        savedInstanceState.putInt(INITIAL_COUNTED_STEPS, initialStepCounterValue);
    }


    @Override
    public void onRestoreInstanceState(Bundle savedInstanceState) {
        super.onRestoreInstanceState(savedInstanceState);
        numberOfStepsDetected = savedInstanceState.getInt(DETECTED_STEPS);
        numberOfStepsCounted = savedInstanceState.getInt(COUNTED_STEPS);
        initialStepCounterValue = savedInstanceState.getInt(INITIAL_COUNTED_STEPS);
    }

    public void startStop (View view){
        Button myButton = (Button) findViewById(R.id.startStopButton);
        if (stepCountingActive){
            stepCountingActive = false;
            timeCountingStarted = 0;
            myButton.setText("开始");
        }
        else {
            stepCountingActive = true;
            timeCountingStarted = SystemClock.elapsedRealtime();
            myButton.setText("停止");

        }
    }

    private void onStepDetected(double length, float directionInDegree) {
        mBaiduMapManager.addPathSegment(length, directionInDegree);
    }

    private SeekBar mSeekBar = null;
    private int getAngleDisplacement() {
        if (mSeekBar == null) {
            mSeekBar = findViewById(R.id.adjust);
        }
        return (mSeekBar.getProgress() - 20);
    }

    public void reset (View view){
        stepCountingActive = false;
        timeCountingStarted = 0;
        initialStepCounterValue = initialStepCounterValue + numberOfStepsCounted;

        numberOfStepsCounted = 0;
        numberOfStepsDetected = 0;
        detectedStepsSensorValue = 0;
        countedStepsSensorValue = 0;

        stepMeanFrequency = 0;
        stepMeanTime = 0;

        sumAccData = 0;

        distance = 0;
        distanceHeight = 0;
        distanceFrequency = 0;
        totalTime =0;

        TextView countedSteps = (TextView) findViewById(R.id.countedStepsTextView);
        countedSteps.setText(String.valueOf(numberOfStepsCounted));

        TextView detectedSteps = (TextView) findViewById(R.id.stepsDetectedTextView);
        detectedSteps.setText("" + numberOfStepsDetected);

        TextView distanceView = (TextView) findViewById(R.id.distanceTextView);
        distanceView.setText(Double.toString(distance));

        Button myButton = (Button) findViewById(R.id.startStopButton);
        myButton.setText("开始");
    }
}
