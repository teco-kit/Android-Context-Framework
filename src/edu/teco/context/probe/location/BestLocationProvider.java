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

import edu.teco.context.configuration.FrameworkContext;
import android.location.Location;
import android.util.Log;

public class BestLocationProvider {
	
	private Location mCurrentBestLocation = null;
	
	/** Singleton because only one writer should be open. */
	private static BestLocationProvider singletonInstance = null;
	
	private BestLocationProvider() {
		super();
	}

	/**
	 * Creates a singleton LocationHandler or returns the already created
	 * object. Call setLocationManager before usage.
	 * 
	 * @return The singleton object.
	 */
	public static BestLocationProvider getInstance() {
		if (singletonInstance == null) {
			singletonInstance = new BestLocationProvider();
		}
		return singletonInstance;
	}
	
	/** Tag string for debug logs */
	private static final String TAG = "BestLocationProvier";
	
	
	/**
	 * Put the new location, which is stored when it is better than the last location.
	 * 
	 * @param location new location by LocationManager
	 * @return true if new location is better, false if not.
	 */
	public boolean putLocation(Location location) {
		if (isBetterLocation(location, mCurrentBestLocation)) {
			mCurrentBestLocation = location;
			if (FrameworkContext.INFO) Log.i(TAG, "New location is better than old location");
			return true;
		} else {
			if (FrameworkContext.INFO) Log.i(TAG, "Old location is better than new location");
			return false;
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
		return mCurrentBestLocation;
	}

	
	/**
	 * Gets the current best coordinates in order Latitude, Longitude.
	 * 
	 * @return double array with 2 values: Latitude, Longitude or array with two 0 if no location available.
	 */
	public double[] getCurrentBestLocationCoordinates() {
		if (mCurrentBestLocation == null) {
			return new double[] { 0, 0 };
		} else {
			double[] coordinates = new double[2];
			coordinates[0] = mCurrentBestLocation.getLatitude();
			coordinates[1] = mCurrentBestLocation.getLongitude();
			return coordinates;
		}
	}
}
