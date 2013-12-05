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


public class EntropyCalculatorKernel implements EntropyCalculator {

	protected KernelEstimatorSingleVariate svke = null;
	protected int totalObservations = 0;
	protected boolean debug = false;
	protected double[] observations;
	
	private boolean normalise = true;
	public static final String NORMALISE_PROP_NAME = "NORMALISE";
	
	/**
	 * Default value for epsilon
	 */
	public static final double DEFAULT_EPSILON = 0.25;
	/**
	 * Kernel width
	 */
	private double epsilon = DEFAULT_EPSILON;
	public static final String EPSILON_PROP_NAME = "EPSILON";
	
	public EntropyCalculatorKernel() {
		svke = new KernelEstimatorSingleVariate();
		svke.setDebug(debug);
		svke.setNormalise(normalise);
	}

	public void initialise() {
		initialise(epsilon);
	}

	public void initialise(double epsilon) {
		this.epsilon = epsilon;
		svke.initialise(epsilon);
	}

	/**
	 * Set the observations for the PDFs.
	 * Should only be called once, the last call contains the
	 *  observations that are used (they are not accumulated). 
	 * 
	 * @param observations
	 */
	public void setObservations(double observations[]) {
		this.observations = observations;
		svke.setObservations(observations);
		totalObservations = observations.length;
	}
	
	public double computeAverageLocalOfObservations() {
		double entropy = 0.0;
		for (int t = 0; t < observations.length; t++) {
			double prob = svke.getProbability(observations[t]);
			double cont = Math.log(prob);
			entropy -= cont;
			if (debug) {
				System.out.println(t + ": p(" + observations[t] + ")= " +
						prob + " -> " + (cont/Math.log(2.0)) + " -> sum: " +
						(entropy/Math.log(2.0)));
			}
		}
		return entropy / totalObservations / Math.log(2.0);
	}
	
	public void setDebug(boolean debug) {
		this.debug = debug;
		if (svke != null) {
			svke.setDebug(debug);
		}
	}
	
	/**
	 * Allows the user to set properties for the underlying calculator implementation
	 * These can include:
	 * <ul>
	 * 		<li>{@link #EPSILON_PROP_NAME}</li>
	 * 		<li>{@link #NORMALISE_PROP_NAME}</li>
	 * </ul> 
	 * 
	 * @param propertyName
	 * @param propertyValue
	 * @throws Exception
	 */
	public void setProperty(String propertyName, String propertyValue) throws Exception {
		boolean propertySet = true;

		//  If we implement a dynamic correlation exclusion property,
		//  then we will need to call getProbability(double, int) instead of
		//  just getProbability(double) above.
		
		if (propertyName.equalsIgnoreCase(EPSILON_PROP_NAME)) {
			epsilon = Double.parseDouble(propertyValue);
		} else if (propertyName.equalsIgnoreCase(NORMALISE_PROP_NAME)) {
			normalise = Boolean.parseBoolean(propertyValue);
			svke.setNormalise(normalise);
		} else {
			// No property was set
			propertySet = false;
		}
		if (debug && propertySet) {
			System.out.println(this.getClass().getSimpleName() + ": Set property " + propertyName +
					" to " + propertyValue);
		}
	}

}
