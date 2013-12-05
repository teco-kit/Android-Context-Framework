/*******************************************************************************
 * Copyright 2013 Karlsruhe Institute of Technology. This Work has been partially supported by the EIT ICT Labs funded research project Towards a Mobile Cloud (activity CLD 12206).
 * 
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * 
 *   http://www.apache.org/licenses/LICENSE-2.0
 * 
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 ******************************************************************************/
package edu.teco.context.probe.location;

import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.os.Bundle;
import android.util.Log;
import edu.teco.context.configuration.FrameworkContext;
import edu.teco.context.configuration.FrameworkKeys.ILocationKeys;

public class CustomLocationManager implements ILocationKeys {

	/** Singleton because only one writer should be open. */
	private static CustomLocationManager singletonInstance = null;
	
	private CustomLocationManager() {
		super();
	}

	/**
	 * Creates a singleton LocationHandler or returns the already created
	 * object. Call setLocationManager before usage.
	 * 
	 * @return The singleton object.
	 */
	public static CustomLocationManager getInstance() {
		if (singletonInstance == null) {
			singletonInstance = new CustomLocationManager();
		}
		return singletonInstance;
	}
	
	/** Tag string for debug logs */
	private static final String TAG = "Location";

	// for GPS data
	private LocationManager mLocationManager = null;
	private LocationListener mLocationListener = null;
	private Location mCurrentLocation = null;
	
	private boolean isUseBestLocation = true;
	
	private long mMinUpdateTime;

	/**
	 * Configure LocationHandler before usage.
	 * 
	 * @param locationManager 
	 * @param minUpdateTime minimal time between location updates.
	 * @param provider must be either "PROVIDER_NETWORK", "PROVIDER_GPS" or "PROVIDER_GPS_NETWORK"
	 */
	public void setLocationManager(LocationManager locationManager, long minUpdateTime, String provider, boolean bestLocation) {
		
		isUseBestLocation = bestLocation;
		
		removeUpdates();
		
		mMinUpdateTime = minUpdateTime;
		mLocationManager = locationManager;

		// Define a listener that responds to location updates
		mLocationListener = new LocationListener() {

			public void onLocationChanged(Location location) {
				if (isUseBestLocation) {
					if (FrameworkContext.INFO) Log.i(TAG, "Location changed: " + location.toString());
					if (isBetterLocation(location, mCurrentLocation)) {
						mCurrentLocation = location;
						if (FrameworkContext.INFO) Log.i(TAG, "New location is better than old location");
					} else {
						if (FrameworkContext.INFO) Log.i(TAG, "Old location is better than new location");
					}
				} else {
					mCurrentLocation = location;
				}
			}

			public void onProviderDisabled(String provider) {
				if (FrameworkContext.INFO) Log.i(TAG, "Provider " + provider + " disabled.");
			}

			public void onProviderEnabled(String provider) {
				if (FrameworkContext.INFO) Log.i(TAG, "Provider " + provider + " enabled.");
			}

			public void onStatusChanged(String provider, int status, Bundle extras) {
				if (FrameworkContext.INFO) Log.i(TAG, "Status changed with " + provider + " new status = " + status
						+ " and bundle: " + extras.toString());
			}
		};

		if (FrameworkContext.INFO) Log.i(TAG, "Location update minTime = " + mMinUpdateTime + " milliseconds.");
		
		if (provider.equals(PROVIDER_NETWORK)) {
			mLocationManager.requestLocationUpdates(LocationManager.NETWORK_PROVIDER, mMinUpdateTime, 0,
					mLocationListener);
		} else if (provider.equals(PROVIDER_GPS)) {
			mLocationManager.requestLocationUpdates(LocationManager.GPS_PROVIDER, mMinUpdateTime, 0,
					mLocationListener);
		} else if (provider.equals(PROVIDER_GPS_NETWORK)) {
			mLocationManager.requestLocationUpdates(LocationManager.NETWORK_PROVIDER, mMinUpdateTime, 0,
					mLocationListener);
			mLocationManager.requestLocationUpdates(LocationManager.GPS_PROVIDER, mMinUpdateTime, 0,
					mLocationListener);
		} else {
			if (FrameworkContext.WARN) Log.w(TAG, "No supported provider specified.");
		}
	}

	private static final int TWO_MINUTES = 1000 * 60 * 2;

	/**
	 * Determines whether one Location reading is better than the current
	 * Location fix see for detailed information:
	 * http://developer.android.com/guide/topics/location/strategies.html
	 * 
	 * @param location
	 *            The new Location that you want to evaluate
	 * @param currentBestLocation
	 *            The current Location fix, to which you want to compare the new
	 *            one
	 */
	private boolean isBetterLocation(Location location, Location currentBestLocation) {
		if (currentBestLocation == null) {
			// A new location is always better than no location
			return true;
		}

		// Check whether the new location fix is newer or older
		long timeDelta = location.getTime() - currentBestLocation.getTime();
		boolean isSignificantlyNewer = timeDelta > TWO_MINUTES;
		boolean isSignificantlyOlder = timeDelta < -TWO_MINUTES;
		boolean isNewer = timeDelta > 0;

		// If it's been more than two minutes since the current location, use
		// the new location
		// because the user has likely moved
		if (isSignificantlyNewer) {
			return true;
			// If the new location is more than two minutes older, it must be
			// worse
		} else if (isSignificantlyOlder) {
			return false;
		}

		// Check whether the new location fix is more or less accurate
		int accuracyDelta = (int) (location.getAccuracy() - currentBestLocation.getAccuracy());
		boolean isLessAccurate = accuracyDelta > 0;
		boolean isMoreAccurate = accuracyDelta < 0;
		boolean isSignificantlyLessAccurate = accuracyDelta > 200;

		// Check if the old and new location are from the same provider
		boolean isFromSameProvider = isSameProvider(location.getProvider(),
				currentBestLocation.getProvider());

		// Determine location quality using a combination of timeliness and
		// accuracy
		if (isMoreAccurate) {
			return true;
		} else if (isNewer && !isLessAccurate) {
			return true;
		} else if (isNewer && !isSignificantlyLessAccurate && isFromSameProvider) {
			return true;
		}
		return false;
	}

	/** Checks whether two providers are the same */
	private boolean isSameProvider(String provider1, String provider2) {
		if (provider1 == null) {
			return provider2 == null;
		}
		return provider1.equals(provider2);
	}
	
	/**
	 * Get the current best location object provided by GPS or aGPS.
	 * 
	 * @return Current best location estimate.
	 */
	public Location getCurrenBestLocation() {
		return mCurrentLocation;
	}

	
	/**
	 * Gets the current best coordinates in order Latitude, Longitude.
	 * 
	 * @return double array with 2 values: Latitude, Longitude or empty array when no location is available.
	 */
	public double[] getCurrentBestLocationCoordinates() {
		if (mCurrentLocation == null) {
			return new double[0];
		} else {
			double[] coordinates = new double[2];
			coordinates[0] = mCurrentLocation.getLatitude();
			coordinates[1] = mCurrentLocation.getLongitude();
			return coordinates;
		}
	}
	
	public void removeUpdates() {
		if (mLocationManager != null && mLocationListener != null) {
			mLocationManager.removeUpdates(mLocationListener);
			mLocationListener = null;
		}
	}
	
	public boolean isEnabled() {
		if (mLocationListener != null) {
			return true;
		}
		return false;
	}
}
