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

import android.util.Log;
import edu.teco.context.configuration.FrameworkContext;
import edu.teco.context.configuration.FrameworkKeys.IProbeKeys;

/**
 * 
 * @author Sven Frauen
 *
 */
public class StaticSensorHandlerFactory implements IProbeKeys{
	
	/** Tag string for debug logs */
	private static final String TAG = "StaticSensorHandlerFactory";
	
	/**
	 * This methods must be changed to introduce new SensorHandlers.
	 * 
	 * @param sensorKey
	 * @return SensorHandler class specified by key.
	 */
	private static AbstractSensorHandler getSensorHandler(String sensorKey) {
		
		AbstractSensorHandler abstractSensorHandler = null;
		
		if (sensorKey.equals(ACCELEROMETER)) {
			abstractSensorHandler = new AccelerometerSensorHandler();
		} /*else if (sensorKey.equals(LINEAR_ACCELERATION)) {
			abstractSensorHandler = new LinearAccelerationSensorHandler();
		} 
		else if (sensorKey.equals(GRAVITY)) {
			abstractSensorHandler = new GravitySensorHandler();
			} 
			else if (sensorKey.equals(ROTATION_VECTOR)) {
			abstractSensorHandler = new RotationVectorSensorHandler();
		
		} */else if (sensorKey.equals(MAGNETIC_FIELD)) {
			abstractSensorHandler = new MagneticFieldSensorHandler();
		} else if (sensorKey.equals(LIGHT)) {
			abstractSensorHandler = new LightSensorHandler();
		} else if (sensorKey.equals(PROXIMITY)) {
			abstractSensorHandler = new ProximitySensorHandler();
		} else if (sensorKey.equals(ORIENTATION)) {
			abstractSensorHandler = new OrientationSensorHandler();
		} else if (sensorKey.equals(GYROSCOPE)) {
			abstractSensorHandler = new GyroscopeSensorHandler();
		} else {
			if (FrameworkContext.WARN) Log.w(TAG, "SensorKey is not reconized. Wrong SensorKey or not yet implemented.");
		}
		
		return abstractSensorHandler;
	}

	public static AbstractSensorHandler getSensorHandler(String sensorKey, String[] featureKeys, double sampleWindow) {
		
		AbstractSensorHandler abstractSensorHandler = null;
		
		if (featureKeys.length > 0) {
			
			abstractSensorHandler = getSensorHandler(sensorKey);
			
			if (abstractSensorHandler != null) {
				abstractSensorHandler.setFeatureKeys(featureKeys);
				abstractSensorHandler.setSensorKey(sensorKey);
				abstractSensorHandler.createSensorBuffers(sampleWindow);
			}
		} else {
			if (FrameworkContext.WARN) Log.w(TAG, "No featureKeys for sensor spezified.");
		}
		
		return abstractSensorHandler;
	}
	
	public static String[] getSensorValueKeys(String sensorKey) {
		
		AbstractSensorHandler abstractSensorHandler = getSensorHandler(sensorKey);
		
		if (abstractSensorHandler != null) {
			return abstractSensorHandler.getValueNames();
		} else {
			return new String[] {};
		}
	}
}
