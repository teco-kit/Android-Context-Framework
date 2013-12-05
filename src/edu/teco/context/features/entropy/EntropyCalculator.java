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
package edu.teco.context.features.entropy;

public interface EntropyCalculator {

	/**
	 * Initialise the calculator using the existing or default value of calculator-specific parameters
	 * 
	 */
	public void initialise() throws Exception;
	
	public void setObservations(double observations[]);
	
	public double computeAverageLocalOfObservations();

	public void setDebug(boolean debug);

	/**
	 * Allows the user to set properties for the underlying calculator implementation
	 * 
	 * @param propertyName
	 * @param propertyValue
	 * @throws Exception
	 */
	public void setProperty(String propertyName, String propertyValue) throws Exception;
}
