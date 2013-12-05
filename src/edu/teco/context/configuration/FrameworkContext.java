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

import edu.teco.context.log.DataLogger;

// Singleton Pattern
// http://www.techrepublic.com/blog/programming-and-development/using-the-singleton-pattern-in-java/449

// Global modified Android Application Object
// http://savagelook.com/blog/android/android-extending-application-to-handle-global-configuration
// http://stackoverflow.com/questions/9244583/using-getapplicationcontext-vs-referencing-to-custom-application-class-in-and
// http://stackoverflow.com/questions/5018545/getapplication-vs-getapplicationcontext
// http://stackoverflow.com/questions/708012/android-how-to-declare-global-variables
// http://stackoverflow.com/questions/2002288/static-way-to-get-context-on-android

// Other configuration options
// http://stackoverflow.com/questions/6311364/configuration-file-in-android-does-that-exist

public class FrameworkContext {

	private final static FrameworkContext INSTANCE = new FrameworkContext();

	// Private constructor suppresses generation of
	// a (public) default constructor
	private FrameworkContext() {
	}

	// thread safe
	public static synchronized FrameworkContext getInstance() {
		return INSTANCE;
	}

	private FrameworkState mCurrentFrameworkState = FrameworkState.INITIAL;

	private FrameworkState mPreviousFrameworkState = FrameworkState.INITIAL;

	// should all data for TRAINING, TESTING, CLASSIFICATION be logged
	private boolean mLogAllData = true;
	
	/** If the configuration is loaded from the ARFF file it should not be saved again. */
	private boolean mIsConfigurationAlreadySaved = false;
	
	// for prediction starting and stopping
	private boolean mIsPredictionTrainingForTraining = false;
	private boolean mIsPredictionTrainingForEvalutation = false;
	private boolean mIsPredictionTrainingForLiveClassification = false;

	// http://stackoverflow.com/questions/2018263/android-logging
	public static int LOGLEVEL = 0;
	public static boolean ASSERT = LOGLEVEL < 6;
	public static boolean ERROR = LOGLEVEL < 5;
	public static boolean WARN = LOGLEVEL < 4;
	public static boolean INFO = LOGLEVEL < 3;
	public static boolean DEBUG = LOGLEVEL < 2;
	public static boolean VERBOSE = LOGLEVEL < 1;
	
	public FrameworkState getFrameworkState() {
		return mCurrentFrameworkState;
	}

	public boolean changeFrameworkState(FrameworkState newState) {
		if (FrameworkState.isChangeAllowed(mCurrentFrameworkState, newState)) {

			// Log state change to LOGGGING or if logging is on to
			// FeatureCalculationState
			if (newState == FrameworkState.LOGGING
					|| (mLogAllData && FrameworkState.isFeatureCalculationState(newState))) {
				DataLogger.getInstance().logComment("---------------- State " + newState.toString() + " started.");
			}

			mPreviousFrameworkState = mCurrentFrameworkState;
			mCurrentFrameworkState = newState;

			return true;

		} else {
			return false;
		}
	}

	public boolean resumeToPreviousState() {
		if (FrameworkState.isChangeAllowed(mCurrentFrameworkState, mPreviousFrameworkState)) {

			FrameworkState tempState = mCurrentFrameworkState;
			mCurrentFrameworkState = mPreviousFrameworkState;
			mPreviousFrameworkState = tempState;

			return true;
		} else {
			return false;
		}
	}

	public boolean resetToConfiguredFrameworkState() {
		if (mCurrentFrameworkState == FrameworkState.INITIAL || mCurrentFrameworkState == FrameworkState.INITIALIZED
				|| mCurrentFrameworkState == FrameworkState.CONFIGURED) {
			return false;
		} else {
			FrameworkState tempState = mCurrentFrameworkState;
			mCurrentFrameworkState = FrameworkState.CONFIGURED;
			mPreviousFrameworkState = tempState;
			
			mIsConfigurationAlreadySaved = false;
			
			return true;
		}
	}
	
	public boolean resetToInitialState() {
		mCurrentFrameworkState = FrameworkState.INITIAL;
		mPreviousFrameworkState = FrameworkState.INITIAL;
		
		mIsConfigurationAlreadySaved = false;
		
		return true;
	}
	
	public void resetToInitializedState() {
		mCurrentFrameworkState = FrameworkState.INITIAL;
		mPreviousFrameworkState = FrameworkState.INITIAL;
		mLogAllData = true;
		mIsConfigurationAlreadySaved = false;
		mIsPredictionTrainingForTraining = false;
		mIsPredictionTrainingForEvalutation = false;
		mIsPredictionTrainingForLiveClassification = false;
	}

	public FrameworkState getPreviousFrameworkState() {
		return mPreviousFrameworkState;
	}

	public boolean isLogging() {
		if (mCurrentFrameworkState == FrameworkState.LOGGING) {
			return true;
		} else if (mLogAllData && FrameworkState.isFeatureCalculationState(mCurrentFrameworkState)) {
			return true;
		} else {
			return false;
		}
	}

	@Override
	public Object clone() throws CloneNotSupportedException {
		throw new CloneNotSupportedException();
	}

	public boolean isLogAllData() {
		return mLogAllData;
	}

	public void setLogAllData(boolean logAllData) {
		mLogAllData = logAllData;
	}
	
	public boolean isConfigurationAlreadySaved() {
		return mIsConfigurationAlreadySaved;
	}
	
	public void setIsConfigurationAlreadySaved(boolean alreadySaved) {
		mIsConfigurationAlreadySaved = alreadySaved;
	}
	
	public boolean isPredictionTrainingForTraining() {
		return mIsPredictionTrainingForTraining;
	}

	public void setPredictionTrainingForTraining(boolean isPredictionTrainingOn) {
		this.mIsPredictionTrainingForTraining = isPredictionTrainingOn;
	}

	public boolean isPredictionTrainingForEvaluation() {
		return mIsPredictionTrainingForEvalutation;
	}

	public void setPredictionTrainingForEvaluation(boolean isPredictionTrainingOn) {
		this.mIsPredictionTrainingForEvalutation = isPredictionTrainingOn;
	}

	public boolean isPredictionTrainingForLiveClassification() {
		return mIsPredictionTrainingForLiveClassification;
	}

	public void setPredictionTrainingForLiveClassification(boolean isPredictionTrainingOn) {
		this.mIsPredictionTrainingForLiveClassification = isPredictionTrainingOn;
	}

}
