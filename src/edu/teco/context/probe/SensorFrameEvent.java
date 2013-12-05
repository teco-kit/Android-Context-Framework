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

import java.util.EventObject;

public class SensorFrameEvent extends EventObject {
	
	/**
	 * 
	 */
	private static final long serialVersionUID = 1L;
	
	private double[] mFeatureValues;
	// TODO: implement order object? sensorKey; (sensorDimension / sensorValueNames) is already given; featureNames
	
	public SensorFrameEvent(Object source, double[] featureValues) {
		super(source);
		mFeatureValues = featureValues;
	}
	
	public double[] getFeatureValues() {
		return mFeatureValues;
	}
	
	public String getFeatureValuesToString() {
		StringBuilder valueString = new StringBuilder();
		for (double value : mFeatureValues) {
			valueString.append(value).append(";");
		}
		
		return valueString.toString();
	}

}
