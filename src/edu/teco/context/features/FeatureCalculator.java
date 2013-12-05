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
package edu.teco.context.features;

import java.util.Arrays;

import edu.teco.context.features.entropy.EntropyCalculator;
import edu.teco.context.features.entropy.EntropyCalculatorKernel;
import edu.teco.context.features.fourier.FFT;

public class FeatureCalculator {
	
	/**
	 * 
	 * @param values
	 * @return
	 */
	public static double mean(float[] values) {
		
		double mean = 0.0;
		for (double value : values) {
			mean += value;
		}
		mean /= values.length;
		
		return mean;
	}
	
	/**
	 * 
	 * @param values
	 * @return
	 */
	public static double median(float[] values) {
		
		Arrays.sort(values);
		
		int middle = values.length/2;
	    if (values.length%2 == 1) {
	        return values[middle];
	    } else {
	        return (values[middle-1] + values[middle]) / 2.0;
	    }
	}
	
	public static double variance(float[] values, double mean) {
		
		double variance = 0.0;
		double sum = 0.0;
		
		for (double value : values) {
			sum += (value - mean) * (value - mean);
		}
		variance = sum/values.length;
		
		return variance;
	}
	
	public static double variance(float[] values) {
		return variance(values, mean(values));
	}
	
	/**
	 * 
	 * @param values
	 * @return
	 */
	public static double standardDeviation(float[] values, double mean) {
		
		double standardDeviation = 0.0;
		double sum = 0.0;
		
		for (double value : values) {
			sum += (value - mean) * (value - mean);
		}
		standardDeviation = Math.sqrt(sum/values.length);
		
		return standardDeviation;
	}
	
	/**
	 * only use if mean has not been calculated before, this operations calculates the mean by itself
	 * @param values
	 * @return
	 */
	public static double standardDeviation(float[] values) {
		return (standardDeviation(values, mean(values)));
	}
	
	public static double standardDeviation(double variance) {
		return Math.sqrt(variance);
	}
	
	public static double differenceMaxMin(float[] values) {
		
		Arrays.sort(values);
		double minValue = values[0];
		double maxValue = values[values.length - 1];
		
		return maxValue - minValue;
	}
	
	/**
	 * 
	 * http://stackoverflow.com/questions/4364823/how-to-get-frequency-from-fft-result?lq=1
	 * 
	 * @param values
	 * @param sampleWindow
	 * @return
	 */
	public static double fftFrequencyPeak(float[] values, double sampleWindow) {
		
		int n = values.length;
		
		double exponent = Math.log(n) / Math.log(2);
//		double exponent = FastMath.log(2, (double) n);
		double maxExponent = Math.ceil(exponent);
		int newLenght = (int) Math.pow(2.0 , maxExponent);
		
		double[] real = new double[newLenght];
		double[] imaginary = new double[newLenght];
		
		for (int i = 0 ; i < newLenght; i++)
		{
			if (i < n) {
				real[i] = values[i];
			} else {
				// zero padding the other values
				real[i] = 0.0;
			}
			imaginary[i] = 0.0;
		}
		
		FFT featureFFT = new FFT(newLenght);
		
		featureFFT.fft(real, imaginary);
		
		double maxValue = 0.0;
		int maxValueIndex = 0;
		
		int nyquistRate = newLenght/2;
		
		for (int i = 0; i < nyquistRate; i++) {
			double magnitude = Math.sqrt(real[i] * real[i] + imaginary[i] * imaginary[i]);
			
			if (magnitude > maxValue) {
				maxValue = magnitude;
				maxValueIndex = i;
			}
		}
		
		// calculate the max frequency peak
//		double samplingFrequency = n / sampleWindow;
//		double frequencyPeak = samplingFrequency * maxValueIndex / newLenght;
		
		return maxValueIndex / sampleWindow;
	}
	
	
	
	public static double entropy(float[] values) {
		// http://whaticode.com/2010/05/24/a-java-implementation-for-shannon-entropy/
		
		// Problem continuous variable --> use Differential entropy
		// http://en.wikipedia.org/wiki/Differential_entropy
		// http://www.mtm.ufsc.br/~taneja/book/node13.html
		// http://code.google.com/p/information-dynamics-toolkit/
		// http://code.google.com/p/information-dynamics-toolkit/source/browse/trunk/java/source/infodynamics/measures/continuous/kernel/EntropyCalculatorKernel.java?r=3
		
		
		// first normalize distribution
		float valueSum = 0;
		
		for (int i = 0; i < values.length; i++) {
		
			// for log value cannot be < 0
			if (values[i] < 0) {
				values[i] = -1 * values[i];
			}
			
			valueSum += values[i];
		}
		
		double normalizingFactor = 1 / valueSum;
		// for logging
//		double normalizedSum = 0;
		// sum over all i of -(dist[i] * Math.log(dist[i])) 
		double entropy = 0;
		
		double[] normalizedValues = new double[values.length];
		for (int i = 0; i < values.length; i++) {
			normalizedValues[i] = values[i] * normalizingFactor;
//			if (FrameworkContext.INFO) Log.i("ENTROPY", "Normalized Value = " + normalizedValues[i]);
//			normalizedSum += normalizedValues[i];
			
//			entropy -= normalizedValues[i] * Math.log(normalizedValues[i]);
		}
		
		EntropyCalculator entropyCalc = new EntropyCalculatorKernel();
		try {
			entropyCalc.initialise();
//			entropyCalc.setDebug(true);
			entropyCalc.setObservations(normalizedValues);
			entropy = entropyCalc.computeAverageLocalOfObservations();
		} catch (Exception e) {
			e.printStackTrace();
		}
		
		
		// logging
//		if (FrameworkContext.INFO) Log.i("ENTROPY", "Sum of Values = " + valueSum + " Sum of Normalized Values = " + normalizedSum + " Entropy = " + entropy);

		// flache Verteilung (Gleichverteilung angeben) Uniform distribution annehmen 
		// Daten plus Verteilung angeben eine Bibliothek finden
		
		return entropy;
	}
	
	/**
	 * 
	 * http://www.pervasive.jku.at/Teaching/_2012SS/EmbeddedSystems/Uebungen/UE52/2004_Activity%20Recognition%20from%20User-Annotated%20Acceleration%20Data_Intille.pdf
	 * 
	 * @param values
	 * @return
	 */
	public static double frequencyDomainEntropy(float[] values, double sampleWindow) {
		
		int n = values.length;
		
		double exponent = Math.log(n) / Math.log(2);
//		double exponent = FastMath.log(2, (double) n);
		double maxExponent = Math.ceil(exponent);
		int newLenght = (int) Math.pow(2.0 , maxExponent);
		
		double[] real = new double[newLenght];
		double[] imaginary = new double[newLenght];
		
		for (int i = 0 ; i < newLenght; i++)
		{
			if (i < n) {
				real[i] = values[i];
			} else {
				// zero padding the other values
				real[i] = 0.0;
			}
			imaginary[i] = 0.0;
		}
		
		FFT featureFFT = new FFT(newLenght);
		
		featureFFT.fft(real, imaginary);
		
		int nyquistRate = newLenght/2;
		
		double[] magnitude = new double[nyquistRate];
		double sumMagnitude = 0.0;
		
		for (int i = 0; i < nyquistRate; i++) {
			magnitude[i] = Math.sqrt(real[i] * real[i] + imaginary[i] * imaginary[i]);
			sumMagnitude += magnitude[i];
		}
		
//		if (FrameworkContext.INFO) Log.i("Entropy", "Sum magnitude = " + sumMagnitude);

		double entropy = 0.0;
		
		for (int i = 0; i < magnitude.length; i++) {
			double normalizedValue = magnitude[i] / sumMagnitude;
			if (normalizedValue > 0) {
				entropy -= normalizedValue * (Math.log(normalizedValue) / Math.log(2));
			}
		}
		
//		if (FrameworkContext.INFO) Log.i("Entropy", "normalized values = " + Arrays.toString(doubleValues));
//		if (FrameworkContext.INFO) Log.i("Entropy", "sum normalized values = " + sumAllNormalizedValues);
//		if (FrameworkContext.INFO) Log.i("Entropy", "entropy = " + entropy);

		return entropy;
	}
	
	/**
	 * source: http://nayuki.eigenstate.org/page/how-to-implement-the-discrete-fourier-transform
	 * 
	 * @param values
	 * @param sampleWindow
	 * @return
	 */
	public static double frequencyDomainEntropyWithDFT(float[] values, double sampleWindow) {
		
		int n = values.length;
	    
		// imaginery part is always 0
//	    double[] inimag = new double[n];
//	    Arrays.fill(inimag, 0);
	    
	    // Signals sampled at Fs are only able to accurately represent frequencies up to, but not including Fs/2
	    // http://en.wikipedia.org/wiki/Nyquist%E2%80%93Shannon_sampling_theorem
	    // http://stackoverflow.com/questions/4364823/how-to-get-frequency-from-fft-result?lq=1
	    int nyquistRate;
	    if (n % 2 == 0) {
	    	// even number
	    	nyquistRate = n/2 - 1;
	    } else {
	    	// odd number
	    	nyquistRate = (n-1) / 2;
	    }

	    double magnitude[] = new double[nyquistRate+1];
	    double sumMagnitude = 0;
	    
	    for (int k = 0; k <= nyquistRate; k++) {  // For output element till nyquistRate
	        double sumreal = 0;
	        double sumimag = 0;
	        for (int t = 0; t < n; t++) {  // For each input element
	            sumreal +=  values[t]*Math.cos(2*Math.PI * t * k / n);
	            sumimag += -values[t]*Math.sin(2*Math.PI * t * k / n);
//	            sumreal +=  values[t]*Math.cos(2*Math.PI * t * k / n) + inimag[t]*Math.sin(2*Math.PI * t * k / n);
//	            sumimag += -values[t]*Math.sin(2*Math.PI * t * k / n) + inimag[t]*Math.cos(2*Math.PI * t * k / n);
	        }
	        
	        magnitude[k] = Math.sqrt(sumreal * sumreal + sumimag * sumimag);
	        sumMagnitude += magnitude[k];
	    }
		
		double entropy = 0.0;
		
		for (int i = 0; i < magnitude.length; i++) {
			double normalizedValue = magnitude[i] / sumMagnitude;
			if (normalizedValue > 0) {
				entropy -= normalizedValue * (Math.log(normalizedValue) / Math.log(2));
			}
		}
		
		return entropy;
	}
	
	/**
	 * Calculates the frequency peak of given sensor values by maximum DFT magnitude.
	 * 
	 * see references:
	 * http://nayuki.eigenstate.org/page/how-to-implement-the-discrete-fourier-transform
	 * http://stackoverflow.com/questions/2921674/determining-the-magnitude-of-a-certain-frequency-on-the-iphone/2937762#2937762
	 * 
	 * @param values the real sensor values
	 * @param sampleWindow the sample window in seconds
	 * @return the peak frequency recognized in sensor values (by magnitude)
	 */
	public static double dftFrequencyPeak(float[] values, double sampleWindow) {
		
	    int n = values.length;
	    
	    // imaginery part is always 0
//	    double[] inimag = new double[n];
//	    Arrays.fill(inimag, 0);
	    
	    // Signals sampled at Fs are only able to accurately represent frequencies up to, but not including Fs/2
	    // http://en.wikipedia.org/wiki/Nyquist%E2%80%93Shannon_sampling_theorem
	    // http://stackoverflow.com/questions/4364823/how-to-get-frequency-from-fft-result?lq=1
	    int nyquistRate;
	    if (n % 2 == 0) {
	    	// even number
	    	nyquistRate = n/2 - 1;
	    } else {
	    	// odd number
	    	nyquistRate = (n-1) / 2;
	    }

	    double maxValue = 0.0;
		int maxValueIndex = 0;
	    
	    for (int k = 0; k <= nyquistRate; k++) {  // For each output element
	        double sumreal = 0;
	        double sumimag = 0;
	        for (int t = 0; t < n; t++) {  // For each input element
	            sumreal +=  values[t]*Math.cos(2*Math.PI * t * k / n);
	            sumimag += -values[t]*Math.sin(2*Math.PI * t * k / n);
//	            sumreal +=  values[t]*Math.cos(2*Math.PI * t * k / n) + inimag[t]*Math.sin(2*Math.PI * t * k / n);
//	            sumimag += -values[t]*Math.sin(2*Math.PI * t * k / n) + inimag[t]*Math.cos(2*Math.PI * t * k / n);
	        }
	        
	        double magnitude = Math.sqrt(sumreal * sumreal + sumimag * sumimag);
	        
	        if (magnitude > maxValue) {
				maxValue = magnitude;
				maxValueIndex = k;
			}
	    }
	    
		// calculate the max frequency peak
//		double frequencyPeak = samplingFrequency * maxValueIndex / n;
		return maxValueIndex / sampleWindow;
	}
	
//	private static void fft(float[] values, double realOut[], double[] imaginaryOut) {
//		
//		int n = values.length;
//		int newLength = realOut.length;
//		
//		for (int i = 0 ; i < newLength; i++)
//		{
//			if (i < n) {
//				realOut[i] = (double) values[i];
//			} else {
//				// zero padding the other values
//				realOut[i] = 0.0;
//			}
//			imaginaryOut[i] = 0.0;
//		}
//		
//		FFT featureFFT = new FFT(newLength);
//		
//		featureFFT.fft(realOut, imaginaryOut);
//	}
	
// 	not used anymore because org.apache.math3 not included in build path anymore and slow behavior
//	
//	/**
//	 * 
//	 * see http://www.analog.com/static/imported-files/tech_docs/dsp_book_Ch12.pdf for details about FFT
//	 * 
//	 * @param values
//	 * @param sampleWindow
//	 * @return
//	 */
//	public static double fftFrequencyPeakOld(float[] values, double sampleWindow) {
//		
//		FastFourierTransformer transformer = new FastFourierTransformer(DftNormalization.STANDARD);
//		
//		// The current implementation of the discrete Fourier transform as a fast Fourier transform 
//		// requires the length of the data set to be a power of 2. This greatly simplifies and speeds 
//		// up the code. Users can pad the data with zeros to meet this requirement.
//		// see http://commons.apache.org/math/apidocs/org/apache/commons/math3/transform/FastFourierTransformer.html
//		
//		// see this for detail information how to use FFT data for an accelerometer
//		// http://stackoverflow.com/questions/6794772/some-signal-processing-fft-questions
//		// http://stackoverflow.com/questions/12049407/build-sample-data-for-apache-commons-fast-fourier-transform-algorithm
//		// http://de.wikipedia.org/wiki/Fourieranalyse#Praktische_Anwendungen_der_Fouriertransformation
//		// http://www.public.iastate.edu/~e_m.350/FFT%205.pdf
//		// http://math.stackexchange.com/questions/77118/non-power-of-2-ffts
//		// http://math.stackexchange.com/questions/9416/extracting-exact-frequencies-from-fft-output?rq=1
//		// http://math.stackexchange.com/questions/41984/fft-bins-from-exact-frequencies?rq=1
//		
//		// calculate length of new 2^n array for sensor values (necessary for FFT)
//		double exponent = FastMath.log(2, (double) values.length);
//		double maxExponent = FastMath.ceil(exponent);
//		int newLenght = (int) FastMath.pow(2.0 , maxExponent);
//		
//		// frequency spectrum of new zero padded sample window
//		double samplingFrequency = values.length / sampleWindow;
//		
//		// fill with sensor values and zero pad the new array with 2^n length
//		double[] zeroPaddedValues = new double[newLenght];
//		for (int i = 0 ; i < newLenght; i++)
//		{
//			if (i < values.length) {
//				zeroPaddedValues[i] = (double) values[i];
//			} else {
//				zeroPaddedValues[i] = 0.0;
//			}
//		}
//		
//		// calculate the forward FFT results
//		Complex[] result = transformer.transform(zeroPaddedValues, TransformType.FORWARD);
//		
//		// Nyquist frequency = 1/2 sampling frequency
//		// only take half of all values because other half is mirrored (as just real values as input are used)
//		int halfLenght = newLenght / 2;
//		
//		// max calculated FFT value
//		double maxValue = 0.0;
//		// index of max calculated FFT value (from FFT result array)
//		int maxValueIndex = 0;
//		// only half of all result values are used (other half is mirror of first half)
//		// and the absolute value of the complex number: sqrt(real^2 + imaginary^2)
//		double[] absoluteValues = new double[halfLenght + 1];
//				
//		for (int i = 0; i <= halfLenght; i++) {
//			absoluteValues[i] = result[i].abs();
//			
//			if (absoluteValues[i] > maxValue) {
//				maxValue = absoluteValues[i];
//				maxValueIndex = i;
//			}
//		}
//		
//		// calculate the max frequency peak
//		double frequencyPeak = samplingFrequency * maxValueIndex / newLenght;
//		
////		if (FrameworkContext.INFO) Log.i("FFT", "__________ NEW FFT ____________");
////		if (FrameworkContext.INFO)Log.i("FFT", "Values array: " + Arrays.toString(values));
////		if (FrameworkContext.INFO) Log.i("FFT", "values: " + values.length + "; exponoent: " + exponent + "; maxExponent: " + maxExponent + "; new Length: " + newLenght);
////		if (FrameworkContext.INFO) Log.i("FFT", "original sample window (s) = " + sampleWindow + " new zero padded sample window (s) = " + zeroPaddedSampleWindow + " frequency (Hz) = " + frequency + " half (Nyquist) frequency (Hz) = " + halfFrequency + " half lenght = " + halfLenght);
////		if (FrameworkContext.INFO) Log.i("FFT", "result lenght: " + result.length);
////		if (FrameworkContext.INFO) Log.i("FFT", "result: MaxValue = " + maxValue + " MaxValueIndex = " + maxValueIndex + " Frequency Peak (Hz) =" + frequencyPeak);
////		if (FrameworkContext.INFO) Log.i("FFT", "_____________________________");
//		
//		/*
//		 * to show all calculated values (real, imaginary and absolute
//		 * 
//		double[] absoluteResult = new double[result.length];
//		double[] realResult = new double[result.length];
//		double[] imaginaryResult = new double[result.length];
//		
//		
//		int j = 0;
//		for (Complex complex : result) {
//			double absolute = complex.abs();
//			double imaginary = complex.getImaginary();
//			double real = complex.getReal();
//			if (FrameworkContext.INFO) Log.i("FFT", "Result absolute: " + absolute + "; imaginary: " + imaginary + "; real: " + real);
//		}
//		
//		if (FrameworkContext.INFO) Log.i("FFT", "Result real array: " + Arrays.toString(realResult));
//		if (FrameworkContext.INFO) Log.i("FFT", "Result imaginary array: " + Arrays.toString(imaginaryResult));
//		if (FrameworkContext.INFO) Log.i("FFT", "Result absolute array: " + Arrays.toString(absoluteResult));
//		*/
//		
//		return frequencyPeak;
//	}
	
	
	
//	/**
//	 * 
//	 * http://www.pervasive.jku.at/Teaching/_2012SS/EmbeddedSystems/Uebungen/UE52/2004_Activity%20Recognition%20from%20User-Annotated%20Acceleration%20Data_Intille.pdf
//	 * 
//	 * @param values
//	 * @return
//	 */
//	public static double frequencyDomainEntropy(float[] values, double sampleWindow) {
//		
//		
//		FastFourierTransformer transformer = new FastFourierTransformer(DftNormalization.STANDARD);
//		
//		// calculate length of new 2^n array for sensor values (necessary for FFT)
//		double exponent = FastMath.log(2, (double) values.length);
//		double maxExponent = FastMath.ceil(exponent);
//		int newLenght = (int) FastMath.pow(2.0 , maxExponent);
//		
//		// fill with sensor values and zero pad the new array with 2^n length
//		double[] zeroPaddedValues = new double[newLenght];
//		for (int i = 0 ; i < newLenght; i++)
//		{
//			if (i < values.length) {
//				zeroPaddedValues[i] = (double) values[i];
//			} else {
//				zeroPaddedValues[i] = 0.0;
//			}
//		}
//		
//		// calculate the forward FFT results
//		Complex[] result = transformer.transform(zeroPaddedValues, TransformType.FORWARD);
//		
//		// only take half of all values because other half is mirrored (just real values as input)
//		int halfLenght = newLenght / 2;
//		
//		// only half of all result values are used (other half is mirror of first half)
//		// and the absolute value of the complex number: sqrt(real^2 + imaginary^2)
//		double[] absoluteValues = new double[halfLenght+1];
//		
//		double sumMagnitude = 0.0;
//		
//		for (int i = 0; i <= halfLenght; i++) {
//			absoluteValues[i] = result[i].abs();
//			sumMagnitude += absoluteValues[i];
//		}
//		
////		if (FrameworkContext.INFO) Log.i("Entropy", "Sum magnitude = " + sumMagnitude);
//
//		double entropy = 0.0;
//		
//		for (int i = 0; i < absoluteValues.length; i++) {
//			double normalizedValue = absoluteValues[i] / sumMagnitude;
//			if (normalizedValue > 0) {
//				entropy -= normalizedValue * (Math.log(normalizedValue) / Math.log(2));
//			}
//		}
////		if (FrameworkContext.INFO) Log.i("Entropy", "normalized values = " + Arrays.toString(doubleValues));
////		if (FrameworkContext.INFO) Log.i("Entropy", "sum normalized values = " + sumAllNormalizedValues);
////		if (FrameworkContext.INFO) Log.i("Entropy", "entropy = " + entropy);
//
//		return entropy;
//	}
	
}
