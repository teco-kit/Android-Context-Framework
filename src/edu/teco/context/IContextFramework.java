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
package edu.teco.context;

import java.util.Map;

import android.content.Context;
import edu.teco.context.FrameworkManager.ContextListener;
import edu.teco.context.configuration.FrameworkConfiguration;
import edu.teco.context.configuration.FrameworkState;

public interface IContextFramework {
	
	public boolean configure(FrameworkConfiguration config);
	
	public boolean configureAndTrainWithARFF(String fileName);
	
	// just log sensor data without classification
	public boolean startLogging();
	public boolean startLogging(String contextLabel);
	public boolean stopLogging();
	
	// default is false
	public void setLogAll(boolean isLogAll);
	public boolean getLogAll();
	
	// central method to set the context label for all states
	public boolean setCurrentContextLabel(String contextLabel);
	
	// classifier training
	public boolean startTrainingRecording();
	public boolean stopTrainingRecording();
	public boolean trainWithRecordedData();
	public boolean trainWithARFF(String fileName);
	
	// for classifier evaluation
	public boolean startEvaluationRecording();
	public boolean stopEvaluationRecording();
	public boolean testWithRecordedData();
	
	// for real time live classification
	public void addContextListener(ContextListener listener);
	public void removeContextListener(ContextListener listener);
	
	public boolean startLiveClassification();
	public boolean stopLiveClassification();
	
	// for starting and stopping the service (no sensor data retrieved when stopped)
	public void resume();
	public void pause();
	
	public boolean resetRecordedData();
	public boolean resetFramework(Context context);
	public boolean destroyFramework();

	// for retrieving current state
	public FrameworkState getCurrentState();
	
	// for prediction handling
	public void togglePredictionTrainingForTraining(boolean isPredictionTrainingOn);
	public void togglePredictionTrainingForEvaluation(boolean isPredictionTrainingOn);
	public void togglePredictionTrainingForLiveClassification(boolean isPredictionTrainingOn);
	public Map<String, Double> getParameterPrediction();
	public boolean savePredictor();
	public boolean loadPredictor(String fileName);
	
	// for GPS
	public void toggleGpsLogging(boolean isEnabled);
}
