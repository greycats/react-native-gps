package com.syarul.rnlocation;

import android.content.Context;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.util.Log;

import com.facebook.react.bridge.Arguments;
import com.facebook.react.bridge.ReactApplicationContext;
import com.facebook.react.bridge.ReactContext;
import com.facebook.react.bridge.ReactContextBaseJavaModule;
import com.facebook.react.bridge.ReactMethod;
import com.facebook.react.bridge.WritableMap;
import com.facebook.react.modules.core.DeviceEventManagerModule;

public class RNLocationModule extends ReactContextBaseJavaModule {

    // React Class Name as called from JS
    public static final String REACT_CLASS = "RNLocation";
    // Unique Name for Log TAG
    public static final String TAG = RNLocationModule.class.getSimpleName();
    // Save last Location Provided
    private Location mLastLocation;
    private LocationListener mLocationListener;
    private LocationManager locationManager;

    //The React Native Context
    ReactApplicationContext mReactContext;

    boolean isGPSEnabled = false;
    boolean isNetworkEnabled = false;
    boolean canGetLocation = false;


    // Constructor Method as called in Package
    public RNLocationModule(ReactApplicationContext reactContext) {
        super(reactContext);
        // Save Context for later use
        mReactContext = reactContext;

        locationManager = (LocationManager) mReactContext.getSystemService(Context.LOCATION_SERVICE);
        mLastLocation = locationManager.getLastKnownLocation(LocationManager.NETWORK_PROVIDER);

        isGPSEnabled = locationManager
                .isProviderEnabled(LocationManager.GPS_PROVIDER);

        // getting network status
        isNetworkEnabled = locationManager
                .isProviderEnabled(LocationManager.NETWORK_PROVIDER);

    }


    @Override
    public String getName() {
        return REACT_CLASS;
    }

    /*
     * Location permission request (Not implemented yet)
     */
    @ReactMethod
    public void requestWhenInUseAuthorization() {
        Log.i(TAG, "Requesting authorization");
    }

    /*
     * Location Callback as called by JS
     */
    @ReactMethod
    public void startUpdatingLocation() {
        mLocationListener = new LocationListener() {
            @Override
            public void onStatusChanged(String str, int in, Bundle bd) {
            }

            @Override
            public void onProviderEnabled(String str) {
            }

            @Override
            public void onProviderDisabled(String str) {
            }

            @Override
            public void onLocationChanged(Location loc) {
                if (loc != null) {
                    mLastLocation = loc;
                    try {
                        double longitude;
                        double latitude;
                        double speed;
                        double altitude;
                        double accuracy;
                        double course;

                        // Receive Longitude / Latitude from (updated) Last Location
                        longitude = mLastLocation.getLongitude();
                        latitude = mLastLocation.getLatitude();
                        speed = mLastLocation.getSpeed();
                        altitude = mLastLocation.getAltitude();
                        accuracy = mLastLocation.getAccuracy();
                        course = mLastLocation.getBearing();

                        Log.i(TAG, "Got new location. Lng: " + longitude + " Lat: " + latitude);

                        // Create Map with Parameters to send to JS
                        WritableMap params = Arguments.createMap();
                        params.putDouble("longitude", longitude);
                        params.putDouble("latitude", latitude);
                        params.putDouble("speed", speed);
                        params.putDouble("altitude", altitude);
                        params.putDouble("accuracy", accuracy);
                        params.putDouble("course", course);

                        // Send Event to JS to update Location
                        sendEvent(mReactContext, "locationUpdated", params);
                    } catch (Exception e) {
                        e.printStackTrace();
                        Log.i(TAG, "Location services disconnected.");
                    }
                } else {
                    Log.i(TAG, "location is null");
                }
            }
        };


        if (!isGPSEnabled && !isNetworkEnabled) {
            // no network provider is enabled
            // and if location is not cached, we can not get the location info
            if (mLastLocation == null) {
                canGetLocation = false;
            }
        } else {
            this.canGetLocation = true;
            // First get location from Network Provider
            if (isNetworkEnabled) {
                locationManager.requestLocationUpdates(LocationManager.NETWORK_PROVIDER, 500, 1, mLocationListener);

                if (locationManager != null) {
                    mLastLocation = locationManager
                            .getLastKnownLocation(LocationManager.NETWORK_PROVIDER);
                }
            }
            // if GPS Enabled get lat/long using GPS Services
            if (isGPSEnabled) {
                if (mLastLocation == null) {
                    locationManager.requestLocationUpdates(LocationManager.GPS_PROVIDER, 500, 1, mLocationListener);

                    if (locationManager != null) {
                        mLastLocation = locationManager
                                .getLastKnownLocation(LocationManager.GPS_PROVIDER);
                    }
                }
            }
        }
    }

    @ReactMethod
    public void stopUpdatingLocation() {
        try {
            locationManager.removeUpdates(mLocationListener);
            Log.i(TAG, "Location service disabled.");
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    /*
     * Internal function for communicating with JS
     */
    private void sendEvent(ReactContext reactContext, String eventName, @Nullable WritableMap params) {
        if (reactContext.hasActiveCatalystInstance()) {
            reactContext
                    .getJSModule(DeviceEventManagerModule.RCTDeviceEventEmitter.class)
                    .emit(eventName, params);
        } else {
            Log.i(TAG, "Waiting for CatalystInstance...");
        }
    }
}
