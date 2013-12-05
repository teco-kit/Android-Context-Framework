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
package edu.teco.context.configuration;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;

import android.util.Log;
import edu.teco.context.configuration.FrameworkKeys.IFeatureKeys;
import edu.teco.context.configuration.FrameworkKeys.ILocationKeys;
import edu.teco.context.configuration.FrameworkKeys.IProbeKeys;

public class FrameworkConfiguration implements IProbeKeys, IFeatureKeys, ILocationKeys {
	
	
	private final static FrameworkConfiguration INSTANCE = new FrameworkConfiguration();

	/** Tag string for debug logs */
	private static final String TAG = "FrameworkConfiguration";
	
	private String mFrameworkDirectory = "/ContextFramework";
	private String mArffDirectory = "/ARFF";
	private String mLogDirectory = "/Log";
	private String mHistoryDirectory = "/History";
	private String mPredictionDirectory = "/Prediction";

	private String mConfigurationName;
	private double mSampleWindow;
	private double mOverlap;
	private Map<String, String[]> mSensorFeaturMap;
	private List<String> mContextLabels;

	private boolean mIsLocationSensor;
	private String mLocationProvider;
	private String mLocationStrategy;

	private boolean mIsAudioVolumeProbe;

	// Private constructor suppresses generation of
	// a (public) default constructor
	private FrameworkConfiguration() {
		initialize();
	}
	
	private void initialize() {
		mConfigurationName = "ContextFramework";
		setSampleWindow(2.0);
		setOverlap(0.5);
		
		mSensorFeaturMap = new LinkedHashMap<String, String[]>();
		mContextLabels = new ArrayList<String>();
		
		addContextLabel("Sitting");
		addContextLabel("Standing");
		addContextLabel("Walking");
		addContextLabel("Climbing Stairs");
		addContextLabel("Jogging");
		addContextLabel("Other");

		mIsLocationSensor = false;
		mLocationProvider = null;
		mLocationStrategy = null;

		mIsAudioVolumeProbe = false;
	}

	// thread safe
	public static synchronized FrameworkConfiguration getInstance() {
		return INSTANCE;
	}
	
	// TODO sanity check and delete methods

	public String getConfigurationName() {
		return mConfigurationName;
	}

	public void setConfigurationName(String configurationName) {
		mConfigurationName = configurationName;
	}

	public double getSampleWindow() {
		return mSampleWindow;
	}

	public void setSampleWindow(double sampleWindow) {
		mSampleWindow = sampleWindow;
	}

	public double getOverlap() {
		return mOverlap;
	}

	public void setOverlap(double overlap) {
		mOverlap = overlap;
	}

	public void addSensorFeaturesCombination(String sensor, String[] features) {
		mSensorFeaturMap.put(sensor, features);
		if (FrameworkContext.INFO) Log.i(TAG, "Added probe " + sensor + " with features " + Arrays.toString(features));
	}

	public Set<Entry<String, String[]>> getSensorFeaturesSet() {
		return mSensorFeaturMap.entrySet();
	}

	public String[] getSensorKeys() {

		Set<String> sensorSet = mSensorFeaturMap.keySet();
		String[] sensorKeys = new String[sensorSet.size()];
		sensorSet.toArray(sensorKeys);
		return sensorKeys;
	}
	
	public boolean containsSensorKey(String sensorKey) {
		if (mSensorFeaturMap.keySet().contains(sensorKey)) {
			return true;
		} else {
			return false;
		}
	}

	public void addContextLabel(String contextLabel) {
		boolean hasValue = false;

		for (String value : mContextLabels) {
			if (value.equals(contextLabel)) {
				hasValue = true;
				break;
			}
		}

		if (!hasValue) {
			mContextLabels.add(contextLabel);
		}
	}

	public String[] getContextLabelArray() {
		return mContextLabels.toArray(new String[mContextLabels.size()]);
	}

	public List<String> getContextLabels() {
		return mContextLabels;
	}
	
	public void setContextLabels(List<String> contextLabels) {
		
		// remove any duplicate labels, null values and empty strings
		LinkedHashMap<String, String> list = new LinkedHashMap<String, String>();
		
		for (String contextLabel : contextLabels) {
			if (contextLabel != null && !contextLabel.equals("")) {
				list.put(contextLabel, null);
			}
		}
		
		if (list.size() > 0) {
			mContextLabels.clear();
			mContextLabels.addAll(list.keySet());
			if (FrameworkContext.INFO) Log.i(TAG, "Added context labels: " + list.keySet().toString());
		} else {
			throw new RuntimeException("New list is empty or had only invalid list items.");
		}
	}

	public void addLocationSensor(String provider, String strategy) {
		if ((provider.equals(PROVIDER_GPS) || provider.equals(PROVIDER_NETWORK) || provider
				.equals(PROVIDER_GPS_NETWORK))
				&& (strategy.equals(STRATEGY_BEST_LOCATION) || strategy.equals(STRATEGY_MEAN_LOCATIONS))) {
			mLocationProvider = provider;
			mLocationStrategy = strategy;
			mIsLocationSensor = true;
		} else {
			if (FrameworkContext.WARN) Log.w(TAG, "Wrong location sensor configuration.");
		}
	}

	/**
	 * 
	 * 
	 * @return Array or provider and strategy or null if no location sensor
	 *         used.
	 */
	public String[] getLocationConfiguration() {
		if (mIsLocationSensor) {
			return new String[] { mLocationProvider, mLocationStrategy };
		} else {
			return null;
		}
	}
	
	public void addAudioVolume() {
		mIsAudioVolumeProbe = true;
	}
	
	public boolean getAudioVolume() {
		return mIsAudioVolumeProbe;
	}
	
	public List<String> getSupportedProbeNames() {
		List<String> probeNames = new ArrayList<String>();
		probeNames.add(FrameworkKeys.IProbeKeys.ACCELEROMETER);
		probeNames.add(FrameworkKeys.IProbeKeys.LINEAR_ACCELERATION);
		probeNames.add(FrameworkKeys.IProbeKeys.GRAVITY);
		probeNames.add(FrameworkKeys.IProbeKeys.LIGHT);
		probeNames.add(FrameworkKeys.IProbeKeys.MAGNETIC_FIELD);
		probeNames.add(FrameworkKeys.IProbeKeys.ORIENTATION);
		probeNames.add(FrameworkKeys.IProbeKeys.PROXIMITY);
		probeNames.add(FrameworkKeys.IProbeKeys.ROTATION_VECTOR);
		probeNames.add(FrameworkKeys.IProbeKeys.GYROSCOPE);
		return probeNames;
	}
	
	// directory names
	public String getArffDirectory() {
		return mFrameworkDirectory + mArffDirectory;
	}
	
	public String getLogDirectory() {
		return mFrameworkDirectory + mLogDirectory;
	}
	
	public String getHistoryDirectory() {
		return mFrameworkDirectory + mHistoryDirectory;
	}
	
	public String getPredictionDirectory() {
		return mFrameworkDirectory + mPredictionDirectory;
	}

	public void reset() {
		initialize();
	}

	@Override
	public String toString() {
		return "FrameworkConfiguration [Name=" + mConfigurationName + ", SampleWindow=" + mSampleWindow
				+ ", Overlap=" + mOverlap + ", SensorFeaturMap=" + mSensorFeaturMap + ", ContextLabels="
				+ mContextLabels + "]";
	}

}
