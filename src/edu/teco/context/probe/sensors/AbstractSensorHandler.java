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
package edu.teco.context.probe.sensors;

import java.nio.BufferOverflowException;
import java.nio.FloatBuffer;

import android.util.Log;
import edu.teco.context.configuration.FrameworkContext;
import edu.teco.context.configuration.FrameworkKeys.IFeatureKeys;
import edu.teco.context.configuration.FrameworkKeys.IProbeKeys;
import edu.teco.context.features.FeatureCalculator;

public abstract class AbstractSensorHandler implements IProbeKeys, IFeatureKeys {
	
	/** Tag string for debug logs */
	private static final String TAG = AbstractSensorHandler.class.getSimpleName();
	
	private String[] mFeatureKeys;
	private String mSensorKey;
	private FloatBuffer[] mBuffer = new FloatBuffer[getValueSize()];
	private double mSampleWindow;
	
	public String[] getFeatureKeys() {
		return mFeatureKeys;
	}
	
	public void setFeatureKeys(String[] featureKeys) {
		this.mFeatureKeys = featureKeys;
	}
	
	public String getSensorKey() {
		return mSensorKey;
	}

	public void setSensorKey(String sensorKey) {
		mSensorKey = sensorKey;
	}
	
	public void createSensorBuffers(double sampleWindow) {
		mSampleWindow = sampleWindow;
		
		int capacity = (int) Math.ceil(getMaxSamplingRate() * sampleWindow);
		
		for (int i = 0; i < mBuffer.length; i++) {
			mBuffer[i] = FloatBuffer.allocate(capacity);
		}
	}
	
	public void clearSensorBuffers() {
		for (int i = 0; i < mBuffer.length; i++) {
			mBuffer[i].clear();
		}
	}
	
	public void flipSensorBuffers() {
		for (int i = 0; i < mBuffer.length; i++) {
			mBuffer[i].flip();
		}
	}
	
	public void putSensorValues(float[] values) {
		
		for (int i = 0; i < mBuffer.length; i++) {
			
			try {
				mBuffer[i].put(values[i]);
			} catch (BufferOverflowException e) {
				String errorMessage = "BufferOverFlow with valuesSize:"+ values.length + 
						" and BufferSize:"+ mBuffer.length + " BufferSizeLimit"+mBuffer[i].limit() +
						" BufferPosition:"+ mBuffer[i].position() +". Please increase the buffer size" +
						" in the specific SensorHandler (e.g. AccelerometerSensorHandler) by increasing the" +
						" value in the getMaxSamplingRate() method. These and following values will be omitted" +
						" and the SensorFrame will be incomplete.";
				if (FrameworkContext.ERROR) Log.e(TAG,errorMessage);
				e.printStackTrace();
				break;
			}
		}
	}
	
	public float[][] getValues() {
		
		int sampleLength = mBuffer[0].limit();
		float[][] valuesArray = new float[mBuffer.length][sampleLength];
		
		for (int i = 0; i < mBuffer.length; i++) {
			float[] values  = new float[sampleLength];
			mBuffer[i].get(values);
			valuesArray[i] = values;
		}
		return valuesArray;
	}
	
	public double[] calculateFeatures() {
		
		// number of features depends on the sensor dimensions and feature dimension
		int featureLength = mFeatureKeys.length * mBuffer.length;
		double[] features = new double[featureLength];
		float[][] allValues = getValues();
		
		boolean meanCalculated = false;
		
//		if (FrameworkContext.INFO) Log.i(TAG, "Sensor: " + getSensorKey());
		
		// TODO Threads for Feature Calculation?
		
		int i = 0;
		for (String featureKey : mFeatureKeys) {
			if (featureKey.equals(MEAN)) {	
				for (float[] values : allValues) {
					features[i] = FeatureCalculator.mean(values);
//					if (FrameworkContext.INFO) Log.i(TAG, "Calculated MEAN: " + features[i] + " with " + values.length + " values.");
					i++;
					meanCalculated = true;
				}
			} else if (featureKey.equals(MEDIAN)) {
				for (float[] values : allValues) {
					features[i] = FeatureCalculator.median(values);
//					if (FrameworkContext.INFO) Log.i(TAG, "Calculated MEDIAN: " + features[i] + " with " + values.length + " values.");
					i++;
				}
			} else if (featureKey.equals(VARIANCE)) {
				for (float[] values : allValues) {
					features[i] = FeatureCalculator.variance(values);
//					if (FrameworkContext.INFO) Log.i(TAG, "Calculated VARIANCE: " + features[i] + " with " + values.length + " values.");
					i++;
				}
			} else if (featureKey.equals(STANDARD_DEVIATION)) {
				for (float[] values : allValues) {
					if (meanCalculated) {
						int meanNumber = 0;
						features[i] = FeatureCalculator.standardDeviation(values, features[meanNumber]);
						meanNumber++;
					} else {
						features[i] = FeatureCalculator.standardDeviation(values);
					}
//					if (FrameworkContext.INFO) Log.i(TAG, "Calculated Standard Deviation: " + features[i] + " with " + values.length + " values.");
					i++;
				}
			} else if (featureKey.equals(DIFFERENCE_MAX_MIN)) {
				for (float[] values : allValues) {
					features[i] = FeatureCalculator.differenceMaxMin(values);
//					if (FrameworkContext.INFO) Log.i(TAG, "Calculated Difference between max and min value: " + features[i] + " with " + values.length + " values.");
					i++;
				}
			} else if (featureKey.equals(FREQUENCY_PEAK)) {
				for (float[] values : allValues) {
					features[i] = FeatureCalculator.dftFrequencyPeak(values, mSampleWindow);
//					if (FrameworkContext.INFO) Log.i(TAG, "Sensor Number: " + i + " MaxFrequency (Hz) = " + features[i]);
//					if (FrameworkContext.INFO) Log.i(TAG, "Values: " + Arrays.toString(values));
//					if (FrameworkContext.INFO) Log.i(TAG, "Calculated FFT Peaks: " + features[i] + " with " + values.length + " values.");
					i++;
				}
			} else if (featureKey.equals(ENTROPY)) {
				for (float[] values : allValues) {
					features[i] = FeatureCalculator.entropy(values);
//					if (FrameworkContext.INFO) Log.i(TAG, "Calculated Entropy: " + features[i] + " with " + values.length + " values.");
					i++;
				}
			} else if (featureKey.equals(FREQUENCY_DOMAIN_ENTROPY)) {
				for (float[] values : allValues) {
					features[i] = FeatureCalculator.frequencyDomainEntropyWithDFT(values, mSampleWindow);
//					if (FrameworkContext.INFO) Log.i(TAG, "Calculated frequency domain entropy: " + features[i] + " with " + values.length + " values.");
					i++;
				}
			}
		}
		
		return features;
	}
	
	public String sensorFeatureString() {
		StringBuilder string = new StringBuilder().append("------- SensorFeatureString -------\n").append(getSensorKey()).append(";");
		for (String valueName : getValueNames()) {
			string.append(valueName).append(",");
		}
		string.deleteCharAt(string.length()-1);
		string.append(";");
		for (String featureKey : getFeatureKeys()) {
			string.append(featureKey).append(",");
		}
		string.deleteCharAt(string.length()-1);
		
		if (FrameworkContext.INFO) Log.i(TAG, string.toString());
		
		return string.toString();
	}
	
	public int getValueSize() {
		return getValueNames().length;
	}
	
	public abstract int getSensorType();
	public abstract String[] getValueNames();
	public abstract double getMaxSamplingRate();

}
