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

import java.util.Arrays;
import java.util.Vector;

/**
 * <p>Class to maintain probability distribution function for
 *  a single variable, using kernel estimates.</p>
 * 
 * <p>
 *  For more details on kernel estimation for computing probability distribution functions,
 *  see Kantz and Schreiber (below).
 * </p>
 * 
 * @see "H. Kantz and T. Schreiber, 'Nonlinear Time Series Analysis'.
 *   Cambridge, MA: Cambridge University Press, 1997"
 * @author Joseph Lizier, <a href="mailto:joseph.lizier at gmail.com">joseph.lizier at gmail.com</>
 *
 */
public class KernelEstimatorSingleVariate {

	private double suppliedKernelWidth = 0.1;
	private double kernelWidthInUse;
	private double min = 0;
	private double max = 0;
	private int bins = 0;
	private int totalObservations = 0;
	private TimeStampedObservation[][] sortedObservations = null;
	private boolean debug = false;
	
	private boolean normalise = true;
	
	private boolean excludeDynamicCorrelations = false;
	private int timeProximityForDynamicCorrelationExclusion = 100;

	/**
	 * 
	 * Private class to store a time-stamped data point.
	 * This allows us to eliminate dynamic correlations later.
	 * 
	 * @author Joseph Lizier
	 *
	 */
	private class TimeStampedObservation implements Comparable {
		public int timeStep;
		public double observation;
		
		TimeStampedObservation(int time, double dataPoint) {
			timeStep = time;
			observation = dataPoint;
		}

		/**
		 * Compare the data values of the two time points
		 * 
		 * @param obj
		 * @return
		 */
		public int compareTo(Object obj) {
			TimeStampedObservation tso2 = (TimeStampedObservation) obj;
			if (observation < tso2.observation) {
				return -1;
			} else if (observation > tso2.observation) {
				return 1;
			}
			return 0;
		}
	}

	public KernelEstimatorSingleVariate() {
	}

	/**
	 * Initialise the estimator before passing any observations in.
	 *
	 * @param epsilon
	 */
	public void initialise(double epsilon) {
		this.suppliedKernelWidth = epsilon;
		sortedObservations = null;
	}
	
	public void setObservations(double[] data) {
		setObservations(data, 0);
	}
	
	public void setObservations(double[] data, int startTime) {
		min = MatrixUtils.minStartFromIndex(data, startTime);
		max = MatrixUtils.maxStartFromIndex(data, startTime);
		totalObservations = data.length - startTime;
		
		if (normalise) {
			// Compute what the epsilonInUse should be here:
			//  it should expand with the standard deviation.
			// This saves us from normalising all of the incoming data points!
			double std = MatrixUtils.stdDev(data);
			kernelWidthInUse = suppliedKernelWidth * std;
		} else {
			kernelWidthInUse = suppliedKernelWidth;
		}

		// Create the bins
		Vector<TimeStampedObservation>[] observations = null;
		bins = (int) Math.ceil((max - min) / kernelWidthInUse);
		if (bins == 0) {
			// The max and min are the same.
			// Should still have one bin here to put all the data in,
			//  otherwise when we go to look up which bin an element
			//  is in we would get an exception.
			// Is mathematically akin to the spread being non-zero but within
			//  epsilon anyway.
			bins = 1;
		}
		
		if (debug) {
			System.out.println("Max: " + max + ", min: " + min +
				", bins: " + bins);
		}
		
		observations = new Vector[bins];
		for (int v = 0; v < bins; v++) {
			observations[v] = new Vector<TimeStampedObservation>();
		}
		
		// Add each observation
		for (int i = startTime; i < data.length; i++) {
			int bin = getBinIndex(data[i]);
			TimeStampedObservation tso = new TimeStampedObservation(i, data[i]);
			// System.out.println(i + " " + observations.length +
			//		" " + max + " " + min + " " + epsilon);
			observations[bin].add(tso);
		}
		
		// Now sort the bins, to allow faster counting later
		sortedObservations = new TimeStampedObservation[bins][];
		int total = 0;
		for (int v = 0; v < bins; v++) {
			// The class cast here causes a run time cast exception:
			// sortedObservations[v] = (TimeStampedObservation[]) observations[v].toArray();
			// It seems crazy, but to get around this we need to do
			//  the following:
			sortedObservations[v] = new TimeStampedObservation[observations[v].size()];
			for (int o = 0; o < sortedObservations[v].length; o++) {
				sortedObservations[v][o] = observations[v].elementAt(o);
			}
			// Sort into ascending order
			Arrays.sort(sortedObservations[v]);
			total += sortedObservations[v].length;
			if (debug) {
				System.out.println("Num observations in bin " + v + ": " + sortedObservations[v].length);
			}
		}
		if (total != totalObservations) {
			throw new RuntimeException("We have not stored all observations");
		}
		
	}
	
	/**
	 * Get the probability of this observation without any dynamic correlation exclusion
	 * 
	 * @param observation
	 * @return
	 */
	public double getProbability(double observation) {
		return getProbability(observation, 0, false);
	}
	
	/**
	 * Get the probability of this observation using the existing settings for 
	 *  dynamic correlation exclusion
	 * 
	 * @param observation
	 * @param timeStep
	 * @return
	 */
	public double getProbability(double observation, int timeStep) {
		return getProbability(observation, timeStep, excludeDynamicCorrelations);
	}
	
	private double getProbability(double observation, int timeStep, 
			boolean dynCorrExclusion) {
		int bin = getBinIndex(observation);
		// First count the number of observations in the same bin
		int count = sortedObservations[bin].length;
		int totalTimePointsCompared = totalObservations;

		// If required eliminate dynamic correlations
		if (dynCorrExclusion) {
			// Need to remove any observations that were *closer* than timeProximityForDynamicCorrelationExclusion
			int closeTimePointsToCompare = (timeStep >= timeProximityForDynamicCorrelationExclusion) ?
					timeProximityForDynamicCorrelationExclusion - 1: timeStep;
			closeTimePointsToCompare += (totalObservations - timeStep >= timeProximityForDynamicCorrelationExclusion) ?
					timeProximityForDynamicCorrelationExclusion - 1: totalObservations - timeStep - 1;
			closeTimePointsToCompare++; // Add one for comparison to self
			totalTimePointsCompared -= closeTimePointsToCompare;
			for (int t = 0; t < sortedObservations[bin].length; t++) {
				if (Math.abs(sortedObservations[bin][t].timeStep - timeStep) < timeProximityForDynamicCorrelationExclusion) {
					count--;
				}
			}
		}
		if (debug) {
			System.out.println("Count from bin " + bin + " = " + count +
					(dynCorrExclusion ? "" : " no") + " dynamic correlation exclusion.");
		}
		
		// Now check the lower bin:
		if (bin > 0) {
			// Find the cut-off point where values in the lower bin
			//  are no longer within epsilon of the given value.
			int topIndex;
			for (topIndex = sortedObservations[bin-1].length;
					(topIndex > 0) && (sortedObservations[bin-1][topIndex-1].observation > observation - kernelWidthInUse);
					topIndex--) {
				// This observation is within epsilon.
				// Before adding to the count just check if it's a dynamic correlation if required:
				if (!dynCorrExclusion ||
						(Math.abs(sortedObservations[bin-1][topIndex-1].timeStep - timeStep) < timeProximityForDynamicCorrelationExclusion)) {
					count++;
				}
			}
			// Don't need to do this addition anymore, it's incorporated above:
			// Post-condition: 
			// Lower bin has (sortedObservations[bin-1].length - topIndex)
			//  values within epsilon of our observation;
			// count += sortedObservations[bin-1].length - topIndex;
		}
		if (debug) {
			System.out.println("Count after lower bin " + (bin - 1) + " = " + count);
		}
		
		// Now check the upper bin:
		if (bin < bins - 1) {
			// Find the cut-off point where values in the upper bin
			//  are no longer within epsilon of the given value
			int bottomIndex;
			for (bottomIndex = 0;
					(bottomIndex < sortedObservations[bin+1].length) &&
						(sortedObservations[bin+1][bottomIndex].observation < observation + kernelWidthInUse);
					bottomIndex++) {
				// This observation is within epsilon.
				// Before adding to the count just check if it's a dynamic correlation if required:
				if (!dynCorrExclusion ||
						(Math.abs(sortedObservations[bin+1][bottomIndex].timeStep - timeStep) < timeProximityForDynamicCorrelationExclusion)) {
					count++;
				}
			}
			// Don't need to do this addition anymore, it's incorporated above:
			// Post-condition: 
			// Upper bin has bottomIndex
			//  values within epsilon of our observation;
			// count += bottomIndex;
		}
		if (debug) {
			System.out.println("Count after upper bin " + (bin + 1) + " = " + count);
		}

		return (double) count / (double) totalTimePointsCompared;
	}
	
	private int getBinIndex(double value) {
		int bin = (int) Math.floor((value - min) / kernelWidthInUse);
		// Check for any rounding errors on the bin assignment:
		if (bin >= bins) {
			bin = bins - 1;
		}
		if (bin < 0) {
			bin = 0;
		}
		return bin;
	}
	
	public void setDebug(boolean debug) {
		this.debug = debug;
	}

	public boolean isNormalise() {
		return normalise;
	}

	public void setNormalise(boolean normalise) {
		this.normalise = normalise;
	}

	public void setDynamicCorrelationExclusion(int timeWindow) {
		excludeDynamicCorrelations = true;
		timeProximityForDynamicCorrelationExclusion = timeWindow;
	}
	
	public void clearDynamicCorrelationExclusion() {
		excludeDynamicCorrelations = false;
	}
	
	public boolean isExcludeDynamicCorrelations() {
		return excludeDynamicCorrelations;
	}
}
