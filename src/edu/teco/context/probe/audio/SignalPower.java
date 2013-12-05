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
/**
 * dsp: various digital signal processing algorithms
 * <br>Copyright 2009 Ian Cameron Smith
 *
 * <p>This program is free software; you can redistribute it and/or modify
 * it under the terms of the GNU General Public License version 2
 * as published by the Free Software Foundation (see COPYING).
 * 
 * <p>This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 */


package edu.teco.context.probe.audio;


/**
 * A power metering algorithm.
 */
public final class SignalPower {

    // ******************************************************************** //
    // Constructor.
    // ******************************************************************** //

    /**
     * Only static methods are provided in this class.
     */
    private SignalPower() {
    }
    

    // ******************************************************************** //
    // Algorithm.
    // ******************************************************************** //

    /**
     * Calculate the bias and range of the given input signal.
     * 
     * @param   sdata       Buffer containing the input samples to process.
     * @param   off         Offset in sdata of the data of interest.
     * @param   samples     Number of data samples to process.
     * @param   out         A float array in which the results will be placed
     *                      Must have space for two entries, which will be
     *                      set to:
     *                      <ul>
     *                      <li>The bias, i.e. the offset of the average
     *                      signal value from zero.
     *                      <li>The range, i.e. the absolute value of the largest
     *                      departure from the bias level.
     *                      </ul>
     * @throws  NullPointerException    Null output array reference.
     * @throws  ArrayIndexOutOfBoundsException  Output array too small.
     */
    public final static void biasAndRange(short[] sdata, int off, int samples,
                                          float[] out)
    {
        // Find the max and min signal values, and calculate the bias.
        short min =  32767;
        short max = -32768;
        int total = 0;
        for (int i = off; i < off + samples; ++i) {
            final short val = sdata[i];
            total += val;
            if (val < min)
                min = val;
            if (val > max)
                max = val;
        }
        final float bias = (float) total / (float) samples;
        final float bmin = min + bias;
        final float bmax = max - bias;
        final float range = Math.abs(bmax - bmin) / 2f;
        
        out[0] = bias;
        out[1] = range;
    }
    
    
    /**
     * Calculate the power of the given input signal.
     * 
     * @param   sdata       Buffer containing the input samples to process.
     * @param   off         Offset in sdata of the data of interest.
     * @param   samples     Number of data samples to process.
     * @return              The calculated power in dB relative to the maximum
     *                      input level; hence 0dB represents maximum power,
     *                      and minimum power is about -95dB.  Particular
     *                      cases of interest:
     *                      <ul>
     *                      <li>A non-clipping full-range sine wave input is
     *                          about -2.41dB.
     *                      <li>Saturated input (heavily clipped) approaches
     *                          0dB.
     *                      <li>A low-frequency fully saturated input can
     *                          get above 0dB, but this would be pretty
     *                          artificial.
     *                      <li>A really tiny signal, which only occasionally
     *                          deviates from zero, can get below -100dB.
     *                      <li>A completely zero input will produce an
     *                          output of -Infinity.
     *                      </ul>
     *                      <b>You must be prepared to handle this infinite
     *                      result and results greater than zero,</b> although
     *                      clipping them off would be quite acceptable in
     *                      most cases.
     */
    public final static double calculatePowerDb(short[] sdata, int off, int samples) {
        // Calculate the sum of the values, and the sum of the squared values.
        // We need longs to avoid running out of bits.
        double sum = 0;
        double sqsum = 0;
        for (int i = 0; i < samples; i++) {
            final long v = sdata[off + i];
            sum += v;
            sqsum += v * v;
        }
        
        // sqsum is the sum of all (signal+bias)², so
        //     sqsum = sum(signal²) + samples * bias²
        // hence
        //     sum(signal²) = sqsum - samples * bias²
        // Bias is simply the average value, i.e.
        //     bias = sum / samples
        // Since power = sum(signal²) / samples, we have
        //     power = (sqsum - samples * sum² / samples²) / samples
        // so
        //     power = (sqsum - sum² / samples) / samples
        double power = (sqsum - sum * sum / samples) / samples;

        // Scale to the range 0 - 1.
        power /= MAX_16_BIT * MAX_16_BIT;

        // Convert to dB, with 0 being max power.  Add a fudge factor to make
        // a "real" fully saturated input come to 0 dB.
        return Math.log10(power) * 10f + FUDGE;
    }
    
    
    /**
     * Calculate the average power of the given input signal.
     * 
     * 
     * @param   sdata       Buffer containing the input samples to process.
     * @param   off         Offset in sdata of the data of interest.
     * @param   samples     Number of data samples to process.
     * @return				The average power of the input signal.
     * 						Sum of samples divided by samples length.
     */
    public final static double calculateAveragePower(short[] sdata, int off, int samples) {
    	
    	// Calculate the sum of the values.
        // We need longs to avoid running out of bits.
        double sum = 0;
        for (int i = 0; i < samples; i++) {
            final long v = sdata[off + i];
            sum += v;
        }
    	
    	return sum / samples;
    }
    
    public final static double calculateMaxPower(short[] sdata, int off, int samples) {
    	// Calculate the sum of the values.
        // We need longs to avoid running out of bits.
        double max = - MAX_16_BIT;
        for (int i = 0; i < samples; i++) {
            final long v = sdata[off + i];
            if (v > max) {
            	max = v;
            }
        }
    	
    	return max;
    }
    

    // ******************************************************************** //
    // Constants.
    // ******************************************************************** //

    // Maximum signal amplitude for 16-bit data.
    private static final float MAX_16_BIT = 32768;
    
    // This fudge factor is added to the output to make a realistically
    // fully-saturated signal come to 0dB.  Without it, the signal would
    // have to be solid samples of -32768 to read zero, which is not
    // realistic.  This really is a fudge, because the best value depends
    // on the input frequency and sampling rate.  We optimise here for
    // a 1kHz signal at 16,000 samples/sec.
    private static final float FUDGE = 0.6f;
  
}

