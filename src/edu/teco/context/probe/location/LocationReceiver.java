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
import edu.teco.context.configuration.FrameworkKeys.IProbeKeys;
import edu.teco.context.configuration.FrameworkState;
import edu.teco.context.log.DataLogger;
import edu.teco.context.probe.ProbeDataListener;

public class LocationReceiver implements ILocationKeys {

	private static final String TAG = "LocationReceiver";

	FrameworkContext mFrameworkContext = FrameworkContext.getInstance();

	ProbeDataListener mDataReceiver = null;

	LocationManager mLocationManager = null;
	LocationListener mLocationListener = null;
	String mLocationStrategy = null;
	BestLocationProvider mBestLocationProvider = null;

	public LocationReceiver(LocationManager locationManager, ProbeDataListener probeDataListener) {
		mLocationManager = locationManager;
		mDataReceiver = probeDataListener;
	}

	public void registerLocation(String locationProvider, String locationStrategy) {

		mLocationStrategy = locationStrategy;

		if (mLocationStrategy.equals(STRATEGY_BEST_LOCATION)) {
			mBestLocationProvider = BestLocationProvider.getInstance();
		}

		if (mLocationManager != null && mLocationListener != null) {
			mLocationManager.removeUpdates(mLocationListener);
			mLocationListener = null;
		}

		// Define a listener that responds to location updates
		mLocationListener = new LocationListener() {

			public void onLocationChanged(Location location) {
				if (FrameworkContext.INFO) Log.i(TAG, "Location changed: " + location.toString());

				double latitude = location.getLatitude();
				double longitude = location.getLongitude();
				// long time = location.getTime();

				if (FrameworkContext.INFO) Log.i(TAG, "Location lat: " + latitude + " long: " + longitude);

				if (FrameworkState.isFeatureCalculationState(mFrameworkContext.getFrameworkState())) {

					if (mLocationStrategy.equals(STRATEGY_MEAN_LOCATIONS)) {
						mDataReceiver.onLocationUpdate(latitude, longitude);
					} else if (mLocationStrategy.equals(STRATEGY_BEST_LOCATION)) {
						mBestLocationProvider.putLocation(location);
					}
				}

				if (mFrameworkContext.isLogging()) {
					DataLogger.getInstance().logData(createLocationMessage(IProbeKeys.LOCATION, latitude, longitude));
				}
			}

			public void onProviderDisabled(String provider) {
				if (FrameworkContext.INFO) Log.i(TAG, "Provider " + provider + " disabled.");
			}

			public void onProviderEnabled(String provider) {
				if (FrameworkContext.INFO) Log.i(TAG, "Provider " + provider + " enabled.");
			}

			public void onStatusChanged(String provider, int status, Bundle extras) {
				if (FrameworkContext.INFO) Log.i(TAG,
						"Status changed with " + provider + " new status = " + status + " and bundle: "
								+ extras.toString());
			}
		};

		if (locationProvider.equals(PROVIDER_GPS)) {
			mLocationManager.requestLocationUpdates(LocationManager.GPS_PROVIDER, 0, 0, mLocationListener);
		} else if (locationProvider.equals(PROVIDER_NETWORK)) {
			mLocationManager.requestLocationUpdates(LocationManager.NETWORK_PROVIDER, 0, 0, mLocationListener);
		} else if (locationProvider.equals(PROVIDER_GPS_NETWORK)) {
			mLocationManager.requestLocationUpdates(LocationManager.GPS_PROVIDER, 0, 0, mLocationListener);
			mLocationManager.requestLocationUpdates(LocationManager.NETWORK_PROVIDER, 0, 0, mLocationListener);
		}
		
		logDescription(locationProvider);
	}

	public void unregisterLocation() {
		if (mLocationManager != null && mLocationListener != null) {
			mLocationManager.removeUpdates(mLocationListener);
			mLocationListener = null;
		}
	}

	public String getLocationStrategy() {
		return mLocationStrategy;
	}

	private void logDescription(String locationProvider) {

		String logMessage = IProbeKeys.LOCATION + " (ProbeKey name) with location strategy: " + mLocationStrategy
				+ " and location provider: " + locationProvider + ";System Time (ms);" + LATITUDE + ";" + LONGITUDE;
		DataLogger.getInstance().logComment(logMessage);
	}

	private String createLocationMessage(String locationKey, double latitude, double longitude) {
		StringBuilder sb = new StringBuilder(locationKey).append(";").append(System.currentTimeMillis()).append(";")
				.append(latitude).append(";").append(longitude);
		return sb.toString();
	}

}
