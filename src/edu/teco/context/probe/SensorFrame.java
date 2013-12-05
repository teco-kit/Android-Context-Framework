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
package edu.teco.context.probe;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import android.util.Log;
import edu.teco.context.configuration.FrameworkContext;
import edu.teco.context.configuration.FrameworkKeys.ILocationKeys;
import edu.teco.context.probe.location.BestLocationProvider;
import edu.teco.context.probe.location.LocationBuffer;
import edu.teco.context.probe.sensors.AbstractSensorHandler;
import edu.teco.context.probe.sensors.StaticSensorHandlerFactory;

//import android.util.Log;

public class SensorFrame {
	
	/** Tag string for debug logs */
	private static final String TAG = "SensorFrame";
	
	private int mValueCounter = 0;

	private double mSampleWindow = 2.0;
	private double mFrameTimer = 0.0;
	private boolean mActive = false;
	
	private LocationBuffer mLocationBuffer = null;
	private BestLocationProvider mBestLocationProvider = null;

	private Map<String, AbstractSensorHandler> mSensorFrameMap = new LinkedHashMap<String, AbstractSensorHandler>();

	private List<ISensorFrameListener> mListeners = new ArrayList<ISensorFrameListener>();

	public SensorFrame(double sampleWindow) {
		mSampleWindow = sampleWindow;
	}

	public void addSensor(String sensorKey, String[] featureKeys) {
		mSensorFrameMap.put(sensorKey,
				StaticSensorHandlerFactory.getSensorHandler(sensorKey, featureKeys, mSampleWindow));
	}
	
	public void addLocationSensor(String locationStrategy) {
		if (locationStrategy.equals(ILocationKeys.STRATEGY_MEAN_LOCATIONS)) {
			mLocationBuffer = new LocationBuffer(mSampleWindow);
		} else if (locationStrategy.equals(ILocationKeys.STRATEGY_BEST_LOCATION)) {
			mBestLocationProvider = BestLocationProvider.getInstance();
		}
	}

	public void clearAllSensors() {
		mSensorFrameMap.clear();
	}

	public boolean isActive() {
		return mActive;
	}

	public void setActive(boolean active) {
		this.mActive = active;
	}

	public void putSensorValues(String sensorKey, float[] values, double timeDifference) {
		if (isActive()) {
			mValueCounter++;
			mFrameTimer += timeDifference;
			AbstractSensorHandler abstractSensorHandler = mSensorFrameMap.get(sensorKey);
			abstractSensorHandler.putSensorValues(values);
			if (mFrameTimer > mSampleWindow) {
				frameIsFinished();
			}
		}
	}
	
	public void putLocationCoordinates(double latitude, double longitude) {
		if (isActive()) {
			if (mLocationBuffer != null) {
				mLocationBuffer.putCoordinates(latitude, longitude);
			} else {
				if (FrameworkContext.WARN) Log.w(TAG, "No LocationBuffer added. You must call addLocation() first before putting location values.");
			}
		}
	}

	private void frameIsFinished() {
		setActive(false);
		
		if (FrameworkContext.INFO) Log.i(TAG, "Number of received values " + mValueCounter);
		mValueCounter = 0;
		
		double[] allFeatures = new double[0];
		
		for (AbstractSensorHandler abstractSensorHandler : mSensorFrameMap.values()) {
			abstractSensorHandler.flipSensorBuffers();
			double[] features = abstractSensorHandler.calculateFeatures();
			allFeatures = concat(allFeatures, features);
		}
		
		// location coordinates are added at the end.
		if (mLocationBuffer != null) {
			double coordinates[] = mLocationBuffer.getMeanCoordinates();
			allFeatures = concat(allFeatures, coordinates);
		} else if (mBestLocationProvider != null) {
			double coordinates[] = mBestLocationProvider.getCurrentBestLocationCoordinates();
			allFeatures = concat(allFeatures, coordinates);
		}

		notifyFeaturesCalculated(new SensorFrameEvent(this, allFeatures));
		resetSensorFrame();
	}

	private double[] concat(double[] A, double[] B) {
		double[] C = new double[A.length + B.length];
		System.arraycopy(A, 0, C, 0, A.length);
		System.arraycopy(B, 0, C, A.length, B.length);

		return C;
	}

	private void resetSensorFrame() {
		for (AbstractSensorHandler abstractSensorHandler : mSensorFrameMap.values()) {
			abstractSensorHandler.clearSensorBuffers();
		}
		
		if (mLocationBuffer != null) {
			mLocationBuffer.clearBuffer();
		}
		
		mFrameTimer = 0.0;
	}
	
	public void reset() {
		setActive(false);
		resetSensorFrame();
	}

	// Listener methods
	
	public void addSensorFrameListener(ISensorFrameListener listener) {
		if (!mListeners.contains(listener)) {
			mListeners.add(listener);
		}
	}

	public void removeSensorFrameListener(ISensorFrameListener listener) {
		mListeners.remove(listener);
	}

	private void notifyFeaturesCalculated(SensorFrameEvent event) {
		for (ISensorFrameListener listener : mListeners) {
			listener.onFeaturesCalculated(event);
		}
	}

}
