/*******************************************************************************
 * Copyright 2013 Karlsruhe Institute of Technology. This Work has been 
 * partially supported by the EIT ICT Labs funded research project 
 * Towards a Mobile Cloud (activity CLD 12206).
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
package edu.teco.context;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Date;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

import android.content.Context;
import android.content.Intent;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.location.LocationManager;
import android.os.Environment;
import android.util.Log;
import edu.teco.context.configuration.FrameworkConfiguration;
import edu.teco.context.configuration.FrameworkContext;
import edu.teco.context.configuration.FrameworkKeys;
import edu.teco.context.configuration.FrameworkKeys.ILocationKeys;
import edu.teco.context.configuration.FrameworkKeys.IMetaDataTags;
import edu.teco.context.configuration.FrameworkKeys.IProbeKeys;
import edu.teco.context.configuration.FrameworkState;
import edu.teco.context.log.DataLogger;

import edu.teco.context.probe.ISensorFrameListener;
import edu.teco.context.probe.ProbeDataListener;
import edu.teco.context.probe.SensorFrame;
import edu.teco.context.probe.SensorFrameEvent;
import edu.teco.context.probe.audio.AudioReceiver;
import edu.teco.context.probe.location.CustomLocationManager;
import edu.teco.context.probe.location.LocationReceiver;

import edu.teco.context.probe.sensors.StaticSensorHandlerFactory;
import edu.teco.context.recognition.IWekaListener;
import edu.teco.context.recognition.WekaEvent;
import edu.teco.context.recognition.WekaManager;

public class FrameworkManager implements IContextFramework,
		SensorEventListener, ISensorFrameListener, ProbeDataListener,
		IWekaListener, IProbeKeys, ILocationKeys, IMetaDataTags {

	/** Tag string for debug logs. */
	private static final String TAG = "FrameworkManager";

	private Context mContext = null;

	/**
	 * Framework Context (Singleton) with application global variables like
	 * FrameworkState
	 */
	private FrameworkContext mFrameworkContext = null;

	/** Manager for sensors */
	private SensorManager mSensorManager = null;

	/** Manager and listener for location */
	private LocationReceiver mLocationReceiver = null;

	private CustomLocationManager mLocationManager = null;

	/** Manager and listener for audio volume */
	private AudioReceiver mAudioReceiver = null;

	private Map<String, Sensor> mSensorMap = null;

	/** Manager for machine learning with WEKA. */
	private WekaManager mWekaManager = null;

	/** Logger for sensor data. */
	private DataLogger mDataLogger = null;

	/** Configuration object to fully configure the framework. */
	private FrameworkConfiguration mConfiguration = null;

	private ContextListener mContextListener = null;

	/** sample window in seconds */
	private double mSampleWindow;
	/** overlap as ratio */
	private double mOverlap;
	/** time delta between sensor data */
	private double mPreviousSeconds;

	/** time of current sensor frame */
	private double mTimer;
	/** when should next frame start how many frame buffers are needed */
	private double mOverlapStartTime;
	/** how many SensorFrame Instances are needed */
	private int mSensorFrameListSize;

	/** current class name for logging, training and evaluation */
	private String mCurrentContextLabel = null;

	/** list of current frame buffers */
	private ArrayList<SensorFrame> mSensorFrameList = null;

	private String mCurrentParameter = null;

	// ******************************************************************** //
	// Constructor
	//
	// ******************************************************************** //

	public FrameworkManager(Context context) {
		initializeFrameWorkManager(context);
	}

	/**
	 * Hack because on HTC Desire Android 2.2 the sensors are turned off when
	 * the screen is turned off and WAKE_LOCK does not help.
	 * http://stackoverflow
	 * .com/questions/2143102/accelerometer-stops-delivering-
	 * samples-when-the-screen-is-off-on-droid-nexus-one
	 */
	/*
	 * public BroadcastReceiver mReceiver = new BroadcastReceiver() {
	 * 
	 * @Override public void onReceive(Context context, Intent intent) { //
	 * Check action just to be on the safe side. if
	 * (intent.getAction().equals(Intent.ACTION_SCREEN_OFF)) { if
	 * (FrameworkContext.VERBOSE) Log.v(TAG,"trying re-registration"); //
	 * Unregisters the listener and registers it again.
	 * mSensorManager.unregisterListener(FrameworkManager.this); for (Sensor
	 * sensor : mSensorMap.values()) { if (FrameworkContext.VERBOSE) Log.v(TAG,
	 * "registering sensor " + sensor.getName());
	 * mSensorManager.registerListener(FrameworkManager.this, sensor,
	 * SensorManager.SENSOR_DELAY_FASTEST); } } } };
	 */

	private void initializeFrameWorkManager(Context context) {
		// first create an reference to the Framework Context
		mFrameworkContext = FrameworkContext.getInstance();

		// get an instance of framework configuration
		mConfiguration = FrameworkConfiguration.getInstance();

		mDataLogger = DataLogger.getInstance();

		mWekaManager = WekaManager.getInstance();

		mLocationManager = CustomLocationManager.getInstance();

		mContext = context;

		if (mFrameworkContext.changeFrameworkState(FrameworkState.INITIALIZED)) {

			mSensorManager = (SensorManager) context
					.getSystemService(Context.SENSOR_SERVICE);

			mSensorFrameList = new ArrayList<SensorFrame>();

			mSensorMap = new LinkedHashMap<String, Sensor>();

			// standard values
			mSampleWindow = mConfiguration.getSampleWindow();
			mOverlap = mConfiguration.getOverlap();
			setTimerData();

			// TODO this must be set by configuration
			mWekaManager.setClassifier(null, null);
			mWekaManager.addWekaListener(this);

			if (mContextListener != null) {
				mContextListener.onStateChanged(getCurrentState());
			}

			// Register our receiver for the ACTION_SCREEN_OFF action. This will
			// make our receiver
			// code be called whenever the phone enters standby mode.
			/*
			 * IntentFilter filter = new IntentFilter(Intent.ACTION_SCREEN_OFF);
			 * mContext.registerReceiver(mReceiver, filter);
			 */
		} else {
			if (FrameworkContext.ERROR)
				Log.e(TAG, "Could not initialize ContextFramework.");
		}
	}

	// ******************************************************************** //
	// ContextFramework interface methods
	// ******************************************************************** //

	public boolean configure(FrameworkConfiguration configuration) {
		if (mFrameworkContext.changeFrameworkState(FrameworkState.CONFIGURED)) {

			mConfiguration = configuration;

			mSampleWindow = mConfiguration.getSampleWindow();
			mOverlap = mConfiguration.getOverlap();
			setTimerData();

			registerLocation(mConfiguration.getLocationConfiguration());
			registerAudioVolume(mConfiguration.getAudioVolume());
			registerSensors(mConfiguration.getSensorKeys());

			createSensorFrames();

			mWekaManager.configure(mConfiguration);

			if (FrameworkContext.INFO)
				Log.i(TAG, "ContextFramework configured.");

			if (mContextListener != null) {
				mContextListener.onStateChanged(getCurrentState());
			}
			return true;
		} else {
			if (FrameworkContext.ERROR)
				Log.e(TAG, "Could not configure ContextFramework.");
			return false;
		}
	}

	public boolean trainWithARFF(String fileName) {
		if (mFrameworkContext.changeFrameworkState(FrameworkState.TRAINED)) {

			mFrameworkContext.setIsConfigurationAlreadySaved(true);
			String arffDirectory = FrameworkConfiguration.getInstance()
					.getArffDirectory();
			File arffDir = new File(Environment.getExternalStorageDirectory()
					+ arffDirectory);
			File file = new File(arffDir, fileName);

			mWekaManager.configureWithArffFile(file);

			if (mContextListener != null) {
				mContextListener.onStateChanged(getCurrentState());
			}
			return true;
		} else {
			if (FrameworkContext.ERROR)
				Log.e(TAG, "Could not train ContextFramework.");
			return false;
		}
	}

	public boolean configureAndTrainWithARFF(String fileName) {
		try {
			String arffDirectory = FrameworkConfiguration.getInstance()
					.getArffDirectory();
			File arffDir = new File(Environment.getExternalStorageDirectory()
					+ arffDirectory);
			File file = new File(arffDir, fileName);

			FileReader input = new FileReader(file);
			BufferedReader bufRead = new BufferedReader(input);
			String myLine = null;

			while ((myLine = bufRead.readLine()) != null) {

				if (myLine.contains(CONFIG_BEGIN)) {
					if (FrameworkContext.INFO)
						Log.i(TAG, "Started configuration reading from file "
								+ fileName);
				} else if (myLine.contains(CONFIG_END)) {
					if (FrameworkContext.INFO)
						Log.i(TAG, "Ended configuration reading from file "
								+ fileName);
					break;
				} else if (myLine.contains(SAMPLE_WINDOW_BEGIN)) {

					// sample window
					int startPosition = myLine.indexOf(SAMPLE_WINDOW_BEGIN)
							+ SAMPLE_WINDOW_BEGIN.length();
					int endPosition = myLine.indexOf(SAMPLE_WINDOW_END,
							startPosition);
					String sampleWindow = myLine.substring(startPosition,
							endPosition);
					mConfiguration.setSampleWindow(Double
							.parseDouble(sampleWindow));

				} else if (myLine.contains(OVERLAP_BEGIN)) {

					// overlap
					int startPosition = myLine.indexOf(OVERLAP_BEGIN)
							+ OVERLAP_BEGIN.length();
					int endPosition = myLine
							.indexOf(OVERLAP_END, startPosition);
					String overlap = myLine.substring(startPosition,
							endPosition);
					mConfiguration.setOverlap(Double.parseDouble(overlap));

				} else if (myLine.contains(PROBE_BEGIN)) {

					// probe keys
					int startPosition = myLine.indexOf(PROBE_BEGIN)
							+ PROBE_BEGIN.length();
					int endPosition = myLine.indexOf(PROBE_END, startPosition);
					String probeKey = myLine.substring(startPosition,
							endPosition);

					// feature keys
					String[] splits = myLine.split(FEATURE_BEGIN);
					List<String> featureList = new ArrayList<String>();
					for (String split : splits) {
						if (split.contains(FEATURE_END)) {
							int endPos = split.indexOf(FEATURE_END);
							featureList.add(split.substring(0, endPos));
						}
					}
					String[] features = new String[featureList.size()];
					featureList.toArray(features);

					mConfiguration.addSensorFeaturesCombination(probeKey,
							features);
				} else if (myLine.contains(CONTEXT_LABEL_BEGIN)) {

					// context labels
					String[] splits = myLine.split(CONTEXT_LABEL_BEGIN);
					List<String> contextLabelsList = new ArrayList<String>();
					for (String split : splits) {
						if (split.contains(CONTEXT_LABEL_END)) {
							int endPos = split.indexOf(CONTEXT_LABEL_END);
							contextLabelsList.add(split.substring(0, endPos));
						}
					}
					mConfiguration.setContextLabels(contextLabelsList);
				}
			}

			if (FrameworkContext.INFO)
				Log.i(TAG, mConfiguration.toString());
			mFrameworkContext.setIsConfigurationAlreadySaved(true);
			boolean isConfigured = configure(mConfiguration);

			if (isConfigured) {
				if (trainWithARFF(fileName)) {
					return true;
				}
			}

		} catch (FileNotFoundException e) {
			e.printStackTrace();
		} catch (IOException e) {
			e.printStackTrace();
		}

		return false;

	}

	public boolean startLogging() {
		if (mFrameworkContext.changeFrameworkState(FrameworkState.LOGGING)) {
			if (mContextListener != null) {
				mContextListener.onStateChanged(getCurrentState());
			}
			return true;
		} else {
			if (FrameworkContext.ERROR)
				Log.e(TAG, "Could not start logging.");
			return false;
		}
	}

	public boolean startLogging(String contextLabel) {
		if (mFrameworkContext.changeFrameworkState(FrameworkState.LOGGING)) {
			mCurrentContextLabel = contextLabel;
			if (mContextListener != null) {
				mContextListener.onStateChanged(getCurrentState());
			}
			return true;
		} else {
			if (FrameworkContext.ERROR)
				Log.e(TAG, "Could not start logging.");
			return false;
		}
	}

	public boolean stopLogging() {
		if (mFrameworkContext.resumeToPreviousState()) {
			resetFrameworkManagerData();
			mDataLogger.flushWriter();
			if (mContextListener != null) {
				mContextListener.onStateChanged(getCurrentState());
			}
			return true;
		} else {
			if (FrameworkContext.ERROR)
				Log.e(TAG, "Could not stop logging.");
			return false;
		}
	}

	public void setLogAll(boolean isLogAll) {
		mFrameworkContext.setLogAllData(isLogAll);
	}

	public boolean getLogAll() {
		return mFrameworkContext.isLogAllData();
	}

	public boolean setCurrentContextLabel(String contextLabel) {
		if (contextLabel != null
				&& mConfiguration.getContextLabels().contains(contextLabel)) {
			mCurrentContextLabel = contextLabel;
			if (FrameworkContext.INFO)
				Log.i(TAG, "Changed to new context label: " + contextLabel);
			return true;
		}
		if (FrameworkContext.ERROR)
			Log.e(TAG, "Could not change to new context label: " + contextLabel);
		return false;
	}

	public boolean startTrainingRecording() {
		// context label must be set before
		if (mCurrentContextLabel == null)
			return false;

		if (mFrameworkContext.changeFrameworkState(FrameworkState.TRAINING)) {
			if (mContextListener != null) {
				mContextListener.onStateChanged(getCurrentState());
			}
			return true;
		} else {
			if (FrameworkContext.ERROR)
				Log.e(TAG, "Could not start training.");
			return false;
		}
	}

	public boolean stopTrainingRecording() {
		if (mFrameworkContext.resumeToPreviousState()) {
			resetFrameworkManagerData();
			mDataLogger.flushWriter();
			if (mContextListener != null) {
				mContextListener.onStateChanged(getCurrentState());
			}
			return true;
		} else {
			if (FrameworkContext.ERROR)
				Log.e(TAG, "Could not stop training.");
			return false;
		}
	}

	public boolean trainWithRecordedData() {
		// first check if training data exists before changing state
		if (!mWekaManager.isTrainingDataAvailable()) {
			return false;
		}

		if (mFrameworkContext.changeFrameworkState(FrameworkState.TRAINED)) {
			mWekaManager.trainClassifier(true);
			if (mContextListener != null) {
				mContextListener.onStateChanged(getCurrentState());
			}
			return true;
		} else {
			if (FrameworkContext.ERROR)
				Log.e(TAG, "Could not train ContextFramework.");
			return false;
		}
	}

	public boolean startEvaluationRecording() {
		// context label must be set before
		if (mCurrentContextLabel == null)
			return false;

		if (mFrameworkContext.changeFrameworkState(FrameworkState.EVALUATING)) {
			if (mContextListener != null) {
				mContextListener.onStateChanged(getCurrentState());
			}
			return true;
		} else {
			if (FrameworkContext.ERROR)
				Log.e(TAG, "Could not start testing.");
			return false;
		}
	}

	public boolean stopEvaluationRecording() {
		if (mFrameworkContext.resumeToPreviousState()) {
			resetFrameworkManagerData();
			mDataLogger.flushWriter();
			if (mContextListener != null) {
				mContextListener.onStateChanged(getCurrentState());
			}
			return true;
		} else {
			if (FrameworkContext.ERROR)
				Log.e(TAG, "Could not stop testing.");
			return false;
		}
	}

	public boolean testWithRecordedData() {
		// first check if testing data exists before changing state
		if (!mWekaManager.isTestingDataAvailable()) {
			return false;
		}

		// must be in state trained (training data must be existing)
		if (mFrameworkContext.getFrameworkState() == FrameworkState.TRAINED) {
			mWekaManager.testClassification();
			if (mContextListener != null) {
				mContextListener.onStateChanged(getCurrentState());
			}
			return true;
		} else {
			return false;
		}
	}

	public void addContextListener(ContextListener listener) {
		mContextListener = listener;
	}

	public void removeContextListener(ContextListener listener) {
		mContextListener = null;
	}

	public boolean startLiveClassification() {
		if (mFrameworkContext.changeFrameworkState(FrameworkState.CLASSIFYING)) {
			if (mContextListener != null) {
				mContextListener.onStateChanged(getCurrentState());
			}
			return true;
		} else {
			if (FrameworkContext.ERROR)
				Log.e(TAG, "Could not start classification.");
			return false;
		}
	}

	public boolean stopLiveClassification() {
		if (mFrameworkContext.resumeToPreviousState()) {
			resetFrameworkManagerData();
			mDataLogger.flushWriter();
			if (mContextListener != null) {
				mContextListener.onStateChanged(getCurrentState());
			}
			return true;
		} else {
			if (FrameworkContext.ERROR)
				Log.e(TAG, "Could not stop classification.");
			return false;
		}
	}

	public boolean resetRecordedData() {
		// reset all data (in WekaManager) and return to state configured
		if (mFrameworkContext.resetToConfiguredFrameworkState()) {
			resetFrameworkManagerData();
			mWekaManager.removeAllData();
			if (FrameworkContext.INFO)
				Log.i(TAG, "Removed all data and returned to CONFIGURED state.");
			if (mContextListener != null) {
				mContextListener.onStateChanged(getCurrentState());
			}
			return true;
		} else {
			return false;
		}
	}

	public boolean resetFramework(Context context) {
		if (getCurrentState() == FrameworkState.INITIAL) {
			return false;
		}
		if (FrameworkState.isFeatureCalculationState(getCurrentState())) {
			mDataLogger.flushWriter();
			resetFrameworkManagerData();
		}
		if (mFrameworkContext.resetToConfiguredFrameworkState()) {
			resetFrameworkManagerData();
			mWekaManager.removeAllData();
		}

		unregisterAllProbes();
		mWekaManager.removeWekaListener(this);
		mWekaManager.reset();
		mDataLogger.createNewLogFile();

		mConfiguration.reset();

		mFrameworkContext.resetToInitialState();

		initializeFrameWorkManager(context);

		if (mContextListener != null) {
			mContextListener.onStateChanged(getCurrentState());
		}

		return true;
	}

	public boolean destroyFramework() {
		if (FrameworkState.isFeatureCalculationState(getCurrentState())) {
			resetFrameworkManagerData();
			mDataLogger.flushWriter();
		}
		if (mFrameworkContext.resetToConfiguredFrameworkState()) {
			resetFrameworkManagerData();
			mWekaManager.removeAllData();
		}

		unregisterAllProbes();
		mWekaManager.removeWekaListener(this);
		mWekaManager.reset();

		mConfiguration.reset();

		mFrameworkContext.resetToInitializedState();

		return true;
	}

	public void resume() {
		if (mFrameworkContext.getFrameworkState() == FrameworkState.PAUSED) {
			try {
				registerSensors(mConfiguration.getSensorKeys());
				resetFrameworkManagerData();
				mFrameworkContext.resumeToPreviousState();
				if (mContextListener != null) {
					mContextListener.onStateChanged(getCurrentState());
				}
			} catch (Exception e) {
				if (FrameworkContext.ERROR)
					Log.e(TAG, "Exception: " + e.getCause());
				e.printStackTrace();
			}
		}
	}

	public void pause() {
		// always possible to pause
		if (FrameworkState.isFeatureCalculationState(mFrameworkContext
				.getFrameworkState())) {
			resetFrameworkManagerData();
		} else if (mFrameworkContext.getFrameworkState() == FrameworkState.LOGGING) {
			stopLogging();
		}

		unregisterAllProbes();
		mFrameworkContext.changeFrameworkState(FrameworkState.PAUSED);
		if (mContextListener != null) {
			mContextListener.onStateChanged(getCurrentState());
		}
	}

	public FrameworkState getCurrentState() {
		return mFrameworkContext.getFrameworkState();
	}

	public void togglePredictionTrainingForTraining(
			boolean isPredictionTrainingOn) {
		mFrameworkContext
				.setPredictionTrainingForTraining(isPredictionTrainingOn);
	}

	public void togglePredictionTrainingForEvaluation(
			boolean isPredictionTrainingOn) {
		mFrameworkContext
				.setPredictionTrainingForEvaluation(isPredictionTrainingOn);
	}

	public void togglePredictionTrainingForLiveClassification(
			boolean isPredictionTrainingOn) {
		mFrameworkContext
				.setPredictionTrainingForLiveClassification(isPredictionTrainingOn);
	}

	public Map<String, Double> getParameterPrediction() {
		return null;
	}

	public boolean savePredictor() {
		return false;
	}

	public boolean loadPredictor(String fileName) {
		return false;
	}

	public void toggleGpsLogging(boolean isEnabled) {

		if (!(mLocationManager.isEnabled() == isEnabled)) {

			if (isEnabled) {
				mLocationManager.setLocationManager((LocationManager) mContext
						.getSystemService(Context.LOCATION_SERVICE), 0,
						"PROVIDER_GPS_NETWORK", false);
				if (FrameworkContext.INFO)
					Log.i(TAG, "Enabling Location Manager.");
			} else {
				mLocationManager.removeUpdates();
				if (FrameworkContext.INFO)
					Log.i(TAG, "Disabling Location Manager.");
			}

		}
	}

	// ******************************************************************** //
	// FrameworkManager handling methods
	// ******************************************************************** //

	private void setTimerData() {
		mPreviousSeconds = 0.0;
		mTimer = 0.0;
		mOverlapStartTime = mSampleWindow - (mSampleWindow * mOverlap);
		mSensorFrameListSize = (int) Math.ceil(1.0 / mOverlap);
	}

	private void registerSensors(String[] sensorKeys) {
		for (String sensorKey : sensorKeys) {
			// unregister and delete sensor if already existing
			if (mSensorMap.containsKey(sensorKey)) {
				mSensorManager.unregisterListener(this,
						mSensorMap.get(sensorKey));
				mSensorMap.remove(sensorKey);
			}

			// add a new sensor to map and register it with the SensorManager
			if (sensorKey.equals(ACCELEROMETER)) {
				Sensor accelerometer = mSensorManager
						.getDefaultSensor(Sensor.TYPE_ACCELEROMETER);
				mSensorMap.put(sensorKey, accelerometer);
				mSensorManager.registerListener(this, accelerometer,
						SensorManager.SENSOR_DELAY_FASTEST);

			}
			/*
			 * else if (sensorKey.equals(LINEAR_ACCELERATION)) { Sensor
			 * linearAcceleration =
			 * mSensorManager.getDefaultSensor(Sensor.TYPE_LINEAR_ACCELERATION);
			 * mSensorMap.put(sensorKey, linearAcceleration);
			 * mSensorManager.registerListener(this, linearAcceleration,
			 * SensorManager.SENSOR_DELAY_FASTEST);
			 * 
			 * } else if (sensorKey.equals(GRAVITY)) { Sensor gravitiy =
			 * mSensorManager.getDefaultSensor(Sensor.TYPE_GRAVITY);
			 * mSensorMap.put(sensorKey, gravitiy);
			 * mSensorManager.registerListener(this, gravitiy,
			 * SensorManager.SENSOR_DELAY_FASTEST);
			 * 
			 * }
			 */
			else if (sensorKey.equals(MAGNETIC_FIELD)) {
				Sensor magneticField = mSensorManager
						.getDefaultSensor(Sensor.TYPE_MAGNETIC_FIELD);
				mSensorMap.put(sensorKey, magneticField);
				mSensorManager.registerListener(this, magneticField,
						SensorManager.SENSOR_DELAY_FASTEST);

			} else if (sensorKey.equals(LIGHT)) {
				Sensor light = mSensorManager
						.getDefaultSensor(Sensor.TYPE_LIGHT);
				mSensorMap.put(sensorKey, light);
				mSensorManager.registerListener(this, light,
						SensorManager.SENSOR_DELAY_FASTEST);

			} else if (sensorKey.equals(PROXIMITY)) {
				Sensor proximity = mSensorManager
						.getDefaultSensor(Sensor.TYPE_PROXIMITY);
				mSensorMap.put(sensorKey, proximity);
				mSensorManager.registerListener(this, proximity,
						SensorManager.SENSOR_DELAY_FASTEST);

			} else if (sensorKey.equals(ORIENTATION)) {
				Sensor orientation = mSensorManager
						.getDefaultSensor(Sensor.TYPE_ORIENTATION);
				mSensorMap.put(sensorKey, orientation);
				mSensorManager.registerListener(this, orientation,
						SensorManager.SENSOR_DELAY_FASTEST);

			}
			/*
			 * else if (sensorKey.equals(ROTATION_VECTOR)) { Sensor
			 * rotationVector =
			 * mSensorManager.getDefaultSensor(Sensor.TYPE_ROTATION_VECTOR);
			 * mSensorMap.put(sensorKey, rotationVector);
			 * mSensorManager.registerListener(this, rotationVector,
			 * SensorManager.SENSOR_DELAY_FASTEST);
			 * 
			 * }
			 */
			else if (sensorKey.equals(GYROSCOPE)) {
				Sensor gyroscope = mSensorManager
						.getDefaultSensor(Sensor.TYPE_GYROSCOPE);
				mSensorMap.put(sensorKey, gyroscope);
				mSensorManager.registerListener(this, gyroscope,
						SensorManager.SENSOR_DELAY_FASTEST);
			}

			logSensorDescription(sensorKey);
		}
	}

	private void registerLocation(String[] locationConfiguration) {
		if (locationConfiguration != null) {
			if (locationConfiguration != null) {
				mLocationReceiver = new LocationReceiver(
						(LocationManager) mContext
								.getSystemService(Context.LOCATION_SERVICE),
						this);
			}

			String locationProvider = locationConfiguration[0];
			String locationStrategy = locationConfiguration[1];

			mLocationReceiver.registerLocation(locationProvider,
					locationStrategy);
		}
	}

	private void registerAudioVolume(boolean isVolumeUsed) {
		if (isVolumeUsed) {
			if (mAudioReceiver == null) {
				mAudioReceiver = new AudioReceiver(this);
			}
			mAudioReceiver.registerAudio();
		}
	}

	private void unregisterAllProbes() {
		if (mSensorManager != null) {
			mSensorManager.unregisterListener(this);
		}

		if (mLocationReceiver != null) {
			mLocationReceiver.unregisterLocation();
		}

		if (mAudioReceiver != null) {
			mAudioReceiver.unregisterAudio();
		}

		if (mSensorMap != null) {
			mSensorMap.clear();
		}
	}

	private void createSensorFrames() {
		for (int i = 0; i < mSensorFrameListSize; i++) {
			SensorFrame frame = new SensorFrame(mSampleWindow);
			for (Entry<String, String[]> sensorFeatures : mConfiguration
					.getSensorFeaturesSet()) {
				frame.addSensor(sensorFeatures.getKey(),
						sensorFeatures.getValue());
			}
			frame.addSensorFrameListener(this);

			if (mLocationReceiver != null) {
				frame.addLocationSensor(mLocationReceiver.getLocationStrategy());
			}

			mSensorFrameList.add(frame);
		}

		// set the first sensor frame to active to be ready for input
		if (mSensorFrameList.size() > 0) {
			mSensorFrameList.get(0).setActive(true);
		}
	}

	/**
	 * Only used to get back from states LOGGING, TRAINING, TESTING, CLASSIFYING
	 * to a "safe" state with no sensor recording. Therefore all timer data must
	 * be reset and also all frames.
	 */
	private void resetFrameworkManagerData() {
		// reset all the frame and timer data
		// mCurrentContextLabel = null;
		mPreviousSeconds = 0.0;
		mTimer = 0.0;
		for (SensorFrame sensorFrame : mSensorFrameList) {
			sensorFrame.reset();
		}

		// set the first sensor framework to active to be ready for input
		if (mSensorFrameList.size() > 0) {
			mSensorFrameList.get(0).setActive(true);
		}
	}

	public void listAllDeviceSensors() {
		List<Sensor> allSensors = mSensorManager.getSensorList(Sensor.TYPE_ALL);
		for (Sensor sensor : allSensors) {
			if (FrameworkContext.INFO)
				Log.i(TAG,
						"Type: " + sensor.getType() + "\nName: "
								+ sensor.getName() + "\nVendor: "
								+ sensor.getVendor() + "\nVersion: "
								+ sensor.getVersion() + "\nMaximum Range: "
								+ sensor.getMaximumRange() + "\nPower (mA): "
								+ sensor.getPower() + "\nResolution: "
								+ sensor.getResolution());
		}
	}

	// ******************************************************************** //
	// LocationManager interface methods
	// ******************************************************************** //

	public void onAccuracyChanged(Sensor sensor, int accuracy) {
		// if (FrameworkContext.INFO) Log.i(TAG, "Accury of sensor " +
		// sensor.getName() +
		// "changed with accuracy = " + accuracy);
	}

	public void onSensorChanged(SensorEvent event) {

		String sensorKey = "";

		// define sensorKey
		switch (event.sensor.getType()) {
		case Sensor.TYPE_ACCELEROMETER:
			sensorKey = ACCELEROMETER;
			break;
		/*
		 * case Sensor.TYPE_LINEAR_ACCELERATION: sensorKey =
		 * LINEAR_ACCELERATION; break; case Sensor.TYPE_GRAVITY: sensorKey =
		 * GRAVITY; break;
		 */
		case Sensor.TYPE_MAGNETIC_FIELD:
			sensorKey = MAGNETIC_FIELD;
			break;
		case Sensor.TYPE_LIGHT:
			sensorKey = LIGHT;
			break;
		case Sensor.TYPE_PROXIMITY:
			sensorKey = PROXIMITY;
			break;
		/*
		 * case Sensor.TYPE_ROTATION_VECTOR: sensorKey = ROTATION_VECTOR; break;
		 */
		case Sensor.TYPE_ORIENTATION:
			sensorKey = ORIENTATION;
			break;
		case Sensor.TYPE_GYROSCOPE:
			sensorKey = GYROSCOPE;
			break;
		default:
			if (FrameworkContext.WARN)
				Log.w(TAG, "Could not recognize sensorKey of sensor: "
						+ event.sensor.getName()
						+ ". Add the sensor to the supported sensorKeys.");
			break;
		}

		// if (FrameworkContext.INFO) Log.i(TAG, sensorKey + " values " +
		// Arrays.toString(event.values));
		// TODO: cut array for light and proximity

		// only log data if state is LOGGING (no feature calculation)
		if (mFrameworkContext.getFrameworkState() == FrameworkState.LOGGING) {
			String contextLabel = "";
			if (mCurrentContextLabel != null)
				contextLabel = mCurrentContextLabel;
			mDataLogger.logData(createSensorMessage(sensorKey, contextLabel,
					event.timestamp, event.values));
		} else if (FrameworkState.isFeatureCalculationState(mFrameworkContext
				.getFrameworkState())) {

			if (mFrameworkContext.isLogAllData()) {
				String contextLabel = "";
				if (mCurrentContextLabel != null)
					contextLabel = mCurrentContextLabel;
				mDataLogger.logData(createSensorMessage(sensorKey,
						contextLabel, event.timestamp, event.values));
			}

			// only calculate features when in data collecting state
			// event.timestamp is the time the event happened in nanoseconds
			double currentSeconds = (event.timestamp) / 1000000000.0d;

			if (mPreviousSeconds == 0) {
				mPreviousSeconds = currentSeconds;
			}

			double differenceSeconds = currentSeconds - mPreviousSeconds;
			mPreviousSeconds = currentSeconds;

			mTimer += differenceSeconds;

			// Use another SensorFrame because of overlap
			if (mTimer >= mOverlapStartTime) {
				// if (FrameworkContext.INFO) Log.i(TAG, "Timer: " + mTimer +
				// " Overlap Start Time: " +
				// mOverlapStartTime);
				mTimer = 0.0;
				for (SensorFrame sensorFrame : mSensorFrameList) {
					if (!sensorFrame.isActive()) {
						sensorFrame.setActive(true);
						break;
					}
				}

			}

			// Store measurements in SensorFrame
			for (SensorFrame sensorFrame : mSensorFrameList) {
				sensorFrame.putSensorValues(sensorKey, event.values,
						differenceSeconds);
			}

			// TODO make Thread save?
			/*
			 * synchronized (this) {
			 * 
			 * // HashMap mapping switch (event.sensor.getType()) {
			 * 
			 * case Sensor.TYPE_ACCELEROMETER: break;
			 * 
			 * case Sensor.TYPE_MAGNETIC_FIELD: break; } }
			 */
		}
	}

	private void logSensorDescription(String sensorKey) {

		StringBuilder sb = new StringBuilder(sensorKey)
				.append(" (ProbeKey Name);").append("System Time (ms);")
				.append("Context Label Name;").append("event timestamp (ns)")
				.append(";Latitude;Longitude");
		String[] sensorValues = StaticSensorHandlerFactory
				.getSensorValueKeys(sensorKey);
		for (int i = 0; i < sensorValues.length; i++) {
			sb.append(";").append(sensorValues[i]);
		}
		mDataLogger.logComment(sb.toString());
	}

	private String createSensorMessage(String sensorKey, String contextLabel,
			long timeStamp, float[] values) {
		StringBuilder sb = new StringBuilder(sensorKey).append(";")
				.append(System.currentTimeMillis()).append(";")
				.append(contextLabel).append(";").append(timeStamp);

		double[] coordinates = null;

		if (mLocationManager.isEnabled()) {
			coordinates = mLocationManager.getCurrentBestLocationCoordinates();
		}

		if (coordinates != null && coordinates.length >= 2) {
			sb.append(";").append(coordinates[0]).append(";")
					.append(coordinates[1]);
		} else {
			sb.append(";").append(0).append(";").append(0);
		}

		for (int i = 0; i < values.length; i++) {
			sb.append(";").append(values[i]);
		}

		// also log parameter
		if (mCurrentParameter != null) {
			String parameter = mCurrentParameter;
			if (parameter.equals("HSDPA"))
				parameter = "UMTS";
			sb.append(";").append(parameter);
		}

		return sb.toString();
	}

	// ******************************************************************** //
	// SensorEventListener interface methods
	// ******************************************************************** //

	public void onFeaturesCalculated(SensorFrameEvent event) {
		if (FrameworkContext.INFO)
			Log.i(TAG, "SensorFrameEvent received.");

		if (mFrameworkContext.getFrameworkState() == FrameworkState.TRAINING) {
			mWekaManager.addTrainingData(event.getFeatureValues(),
					mCurrentContextLabel);

			if (FrameworkContext.INFO)
				Log.i(TAG,
						"Training Data with values:"
								+ event.getFeatureValuesToString());
		}

		if (mFrameworkContext.getFrameworkState() == FrameworkState.EVALUATING) {
			mWekaManager.addEvaluationData(event.getFeatureValues(),
					mCurrentContextLabel);

			if (FrameworkContext.INFO)
				Log.i(TAG,
						"Evaluation Data with values:"
								+ event.getFeatureValuesToString());
		}

		if (mFrameworkContext.getFrameworkState() == FrameworkState.CLASSIFYING) {
			if (FrameworkContext.INFO)
				Log.i(TAG,
						"Classify current data with values:"
								+ event.getFeatureValuesToString());
			mWekaManager.classifyInstance(event.getFeatureValues());
		}
	}

	// ******************************************************************** //
	// DateReceiver interface methods
	// ******************************************************************** //

	public void onLocationUpdate(double latitude, double longitude) {
		if (FrameworkContext.INFO)
			Log.i("Location", "Latitude: " + latitude + " Longitude: "
					+ longitude);
		if (FrameworkState.isFeatureCalculationState(mFrameworkContext
				.getFrameworkState())) {
			for (SensorFrame sensorFrame : mSensorFrameList) {
				sensorFrame.putLocationCoordinates(latitude, longitude);
			}
		}
	}

	public void onAudioVolumeUpdate(double volume) {
		// TODO do something with the volume

	}

	// ******************************************************************** //
	// WekaListener interface methods
	// ******************************************************************** //

	public void onClassChanged(WekaEvent event) {
		// if (mContextListener != null) {
		// if (!event.getClassName().equals(event.getPreviousClassName())) {
		// }
		// }
	}

	public void onClassCalculated(WekaEvent event) {

		Intent classification = new Intent(
				FrameworkKeys.IBroadcastActions.BROADCAST_ACTION_LIVE_CLASSIFICATION);
		classification.putExtra("contextLabel", event.getClassName());
		classification.putExtra("probability", event.getProbability());
		mContext.sendBroadcast(classification);

		if (mContextListener != null) {
			mContextListener.onContextLabelCalculated(event.getClassName());
		}
	}

	public void onTestCalculated(String result) {
		if (mContextListener != null) {
			mContextListener.onTestCalculated(result);
		}
	}

	// ******************************************************************** //
	// ContextListener interface
	// ******************************************************************** //

	public interface ContextListener {
		public void onContextLabelCalculated(String contextLabel);

		public void onTestCalculated(String result);

		public void onStateChanged(FrameworkState state);
	}
}
