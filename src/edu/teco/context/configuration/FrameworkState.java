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

public enum FrameworkState {
	INITIAL, INITIALIZED, CONFIGURED, LOGGING, TRAINING, TRAINED, EVALUATING, CLASSIFYING, PAUSED;
	
	/** Tag string for debug logs. */
	@SuppressWarnings("unused")
	private static final String TAG = "FrameworkState";

	@Override
	public String toString() {
		// only capitalize the first letter
		String s = super.toString();
		return s.substring(0, 1) + s.substring(1).toLowerCase();
	}
	
	public static boolean isChangeAllowed(FrameworkState currentState, FrameworkState newState) {
		
		boolean isChangeAllowed = false;
		
		// only look at allowed changes
		switch (currentState) {
		
		case INITIAL:
			if (newState == INITIALIZED) isChangeAllowed = true;
			break;
		
		case INITIALIZED:
			if (newState == CONFIGURED) isChangeAllowed = true;
			else if (newState == PAUSED) isChangeAllowed = true;
			break;
		
		case CONFIGURED:
			if (newState == LOGGING) isChangeAllowed = true;
			else if (newState == TRAINING) isChangeAllowed = true;
			else if (newState == TRAINED) isChangeAllowed = true;
			else if (newState == PAUSED) isChangeAllowed = true;
			break;

		case LOGGING:
			if (newState == CONFIGURED) isChangeAllowed = true;
			else if (newState == TRAINED) isChangeAllowed = true;
			else if (newState == PAUSED) isChangeAllowed = true;
			break;
			
		case TRAINING:
			if (newState == CONFIGURED) isChangeAllowed = true;
			else if (newState == PAUSED) isChangeAllowed = true;
			break;
			
		case TRAINED:
			if (newState == LOGGING) isChangeAllowed = true;
			else if (newState == TRAINED) isChangeAllowed = true; // for testWithRecordedData();
			else if (newState == EVALUATING) isChangeAllowed = true;
			else if (newState == CLASSIFYING) isChangeAllowed = true;
			else if (newState == PAUSED) isChangeAllowed = true;
			break;
			
		case EVALUATING:
			if (newState == TRAINED) isChangeAllowed = true;
			else if (newState == PAUSED) isChangeAllowed = true;
			break;
			
		case CLASSIFYING:
			if (newState == TRAINED) isChangeAllowed = true;
			else if (newState == PAUSED) isChangeAllowed = true;
			break;
			
		case PAUSED:
			if (newState != PAUSED) isChangeAllowed = true; // always allowed to pause except if already paused
			break;
			
		default:
			break;
		}
		
		if (!isChangeAllowed) {
//			if (FrameworkContext.WARN) Log.w(TAG, "Switching state from " + currentState.toString() + " to " + newState.toString() + " is not allowed.");
		}

		return isChangeAllowed;
	}
	
	public static boolean isFeatureCalculationState(FrameworkState state) {
		if (state == TRAINING || state == EVALUATING || state == CLASSIFYING) {
			return true;
		} else {
			return false;
		}
	}
}
