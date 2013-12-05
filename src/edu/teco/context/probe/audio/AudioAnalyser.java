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
// modified version of:
/**
 * org.hermit.android.instrument: graphical instruments for Android.
 * <br>Copyright 2009 Ian Cameron Smith
 * 
 * <p>These classes provide input and display functions for creating on-screen
 * instruments of various kinds in Android apps.
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

//import org.hermit.dsp.FFTTransformer;
//import org.hermit.dsp.Window;

import edu.teco.context.configuration.FrameworkContext;
import android.os.Bundle;
import android.util.Log;


/**
 * An {@link Instrument} which analyses an audio stream in various ways.
 * 
 * <p>To use this class, your application must have permission RECORD_AUDIO.
 */
public class AudioAnalyser
{

    // ******************************************************************** //
    // Constructor.
    // ******************************************************************** //

	/**
	 * Create a WindMeter instance.
	 * 
     * @param   parent          Parent surface.
	 */
    public AudioAnalyser(VolumeListener volumeReceiver) {
    	mVolumeListener = volumeReceiver;
        
        audioReader = new AudioReader();
        
//        spectrumAnalyser = new FFTTransformer(inputBlockSize, windowFunction);
        
        // Allocate the spectrum data.
//        spectrumData = new float[inputBlockSize / 2];
//        spectrumHist = new float[inputBlockSize / 2][historyLen];
//        spectrumIndex = 0;
//
//        biasRange = new float[2];
    }


    // ******************************************************************** //
    // Configuration.
    // ******************************************************************** //

    /**
     * Set the sample rate for this instrument.
     * 
     * @param   rate        The desired rate, in samples/sec.
     */
    public void setSampleRate(int rate) {
        sampleRate = rate;
    }
    

    /**
     * Set the input block size for this instrument.
     * 
     * @param   size        The desired block size, in samples.  Typical
     *                      values would be 256, 512, or 1024.  Larger block
     *                      sizes will mean more work to analyse the spectrum.
     */
    public void setBlockSize(int size) {
        inputBlockSize = size;

//        spectrumAnalyser = new FFTTransformer(inputBlockSize, windowFunction);

        // Allocate the spectrum data.
        spectrumData = new float[inputBlockSize / 2];
        spectrumHist = new float[inputBlockSize / 2][historyLen];
    }
    

    /**
     * Set the spectrum analyser windowing function for this instrument.
     * 
     * @param   func        The desired windowing function.
     *                      Window.Function.BLACKMAN_HARRIS is a good option.
     *                      Window.Function.RECTANGULAR turns off windowing.
     */
//    public void setWindowFunc(Window.Function func) {
//        windowFunction = func;
//        spectrumAnalyser.setWindowFunc(func);
//    }
    

    /**
     * Set the decimation rate for this instrument.
     * 
     * @param   rate        The desired decimation.  Only 1 in rate blocks
     *                      will actually be processed.
     */
    public void setDecimation(int rate) {
        sampleDecimate = rate;
    }
    
    
    /**
     * Set the histogram averaging window for this instrument.
     * 
     * @param   len         The averaging interval.  1 means no averaging.
     */
    public void setAverageLen(int len) {
        historyLen = len;
        
        // Set up the history buffer.
        spectrumHist = new float[inputBlockSize / 2][historyLen];
        spectrumIndex = 0;
    }
    

    // ******************************************************************** //
    // Run Control.
    // ******************************************************************** //

    /**
     * The application is starting.  Perform any initial set-up prior to
     * starting the application.  We may not have a screen size yet,
     * so this is not a good place to allocate resources which depend on
     * that.
     */
    public void appStart() {
    }


    /**
     * We are starting the main run; start measurements.
     */
    public void measureStart() {
        audioProcessed = audioSequence = 0;
        readError = AudioReader.Listener.ERR_OK;
        
        audioReader.startReader(sampleRate, inputBlockSize * sampleDecimate, new AudioReader.Listener() {
            @Override
            public final void onReadComplete(short[] buffer) {
                receiveAudio(buffer);
            }
            @Override
            public void onReadError(int error) {
                handleError(error);
            }
        });
    }


    /**
     * We are stopping / pausing the run; stop measurements.
     */
    public void measureStop() {
        audioReader.stopReader();
    }
    

    /**
     * The application is closing down.  Clean up any resources.
     */
    public void appStop() {
    }
    
    // ******************************************************************** //
    // Audio Processing.
    // ******************************************************************** //

    /**
     * Handle audio input.  This is called on the thread of the audio
     * reader.
     * 
     * @param   buffer      Audio data that was just read.
     */
    private final void receiveAudio(short[] buffer) {
        // Lock to protect updates to these local variables.  See run().
        synchronized (this) {
            audioData = buffer;
            ++audioSequence;
        }
    }
    
    
    /**
     * An error has occurred.  The reader has been terminated.
     * 
     * @param   error       ERR_XXX code describing the error.
     */
    private void handleError(int error) {
        synchronized (this) {
            readError = error;
        }
    }


    // ******************************************************************** //
    // Main Loop.
    // ******************************************************************** //

    /**
     * Update the state of the instrument for the current frame.
     * This method must be invoked from the doUpdate() method of the
     * application's {@link SurfaceRunner}.
     * 
     * <p>Since this is called frequently, we first check whether new
     * audio data has actually arrived.
     * 
     * @param   now         Nominal time of the current frame in ms.
     */
    public final void doUpdate(long now) {
        short[] buffer = null;
        synchronized (this) {
            if (audioData != null && audioSequence > audioProcessed) {
//                statsCount(now, (int) (audioSequence - audioProcessed));
                audioProcessed = audioSequence;
                buffer = audioData;
            }
        }

        // If we got data, process it without the lock.
        if (buffer != null)
            processAudio(buffer);
        
        if (readError != AudioReader.Listener.ERR_OK)
            processError(readError);
    }
    
//    int counter = 0;
//    int lastCounter = 0;
//    long differenceTimer = 0;
//    
//    public void statsCount(long now, int val) {
//    	if (val <= 0)
//            return;
//    	else {
//    		counter += val;
//    		double difference = (now - differenceTimer) / 1000;
//    		if (difference >= 1.0 ) {
//    			differenceTimer = now;
//    			double frequency = (counter - lastCounter) / difference;
//    			if (FrameworkContext.INFO) Log.i("AudioAnalyser", "Frequency = " + frequency + " with difference = " + difference + " counter = " + counter + " lastCounter = " + lastCounter);
//    			lastCounter = counter;
//    		}
//    	}     
//    }


    /**
     * Handle audio input.  This is called on the thread of the
     * parent surface.
     * 
     * @param   buffer      Audio data that was just read.
     */
    private final void processAudio(short[] buffer) {
        // Process the buffer.  While reading it, it needs to be locked.
    	
        synchronized (buffer) {
            // Calculate the power now, while we have the input
            // buffer; this is pretty cheap.
            final int len = buffer.length;

            // calculate the signal power.
            currentPower = SignalPower.calculatePowerDb(buffer, 0, len);
            
            // calculate the average signal power
//            currentAveragePower = SignalPower.calculateAveragePower(buffer, 0, len);
            
            // find the maximum amplitude in buffer
//            currentMax = SignalPower.calculateMaxPower(buffer, 0, len);
            
            // Tell the reader we're done with the buffer.
            buffer.notify();
        }
        
        mVolumeListener.receiveVolume(currentPower);
        
        // update the signal power
//        if (FrameworkContext.INFO) Log.i("AudioAnalyser", "Signal Power: " + currentPower + " Average Power: " + currentAveragePower + " Max: " + currentMax);
//        if (FrameworkContext.INFO) Log.i("AudioAnalyser", "Max: " + currentMax);
    }
    

    /**
     * Handle an audio input error.
     * 
     * @param   error       ERR_XXX code describing the error.
     */
    private final void processError(int error) {
        // Pass the error to all the gauges we have.
    	if (FrameworkContext.WARN) Log.w("AudioAnalyser", "Error: " + error);
    }
    

    // ******************************************************************** //
    // Save and Restore.
    // ******************************************************************** //

    /**
     * Save the state of the system in the provided Bundle.
     * 
     * @param   icicle      The Bundle in which we should save our state.
     */
    protected void saveState(Bundle icicle) {
//      gameTable.saveState(icicle);
    }


    /**
     * Restore the system state from the given Bundle.
     * 
     * @param   icicle      The Bundle containing the saved state.
     */
    protected void restoreState(Bundle icicle) {
//      gameTable.pause();
//      gameTable.restoreState(icicle);
    }
    

    // ******************************************************************** //
    // Class Data.
    // ******************************************************************** //

    // Debugging tag.
	@SuppressWarnings("unused")
	private static final String TAG = "instrument";
	
	public interface VolumeListener {
		public void receiveVolume(double volume);
	}

	
	// ******************************************************************** //
	// Private Data.
	// ******************************************************************** //

    // The desired sampling rate for this analyser, in samples/sec.
    private int sampleRate = 8000;
    
    private VolumeListener mVolumeListener = null;

    // Audio input block size, in samples.
    private int inputBlockSize = 256;
    
    // The selected windowing function.
//    private Window.Function windowFunction = Window.Function.BLACKMAN_HARRIS;

    // The desired decimation rate for this analyser.  Only 1 in
    // sampleDecimate blocks will actually be processed.
    private int sampleDecimate = 1;
   
    // The desired histogram averaging window.  1 means no averaging.
    private int historyLen = 4;

    // Our audio input device.
    private final AudioReader audioReader;

    // Fourier Transform calculator we use for calculating the spectrum
    // and sonagram.
//    private FFTTransformer spectrumAnalyser;
    
    // Buffered audio data, and sequence number of the latest block.
    private short[] audioData;
    private long audioSequence = 0;
    
    // If we got a read error, the error code.
    private int readError = AudioReader.Listener.ERR_OK;
    
    // Sequence number of the last block we processed.
    private long audioProcessed = 0;
    
    // Current signal power level, in dB relative to max. input power.
    private double currentPower = 0;
    
    // Current average signal power level
    private double currentAveragePower = 0;
    
    // Current maximum amplitude in buffer
	private double currentMax = 0;

    // Analysed audio spectrum data; history data for each frequency
    // in the spectrum; index into the history data; and buffer for
    // peak frequencies.
    private float[] spectrumData;
    private float[][] spectrumHist;
    private int spectrumIndex;

    // Temp. buffer for calculated bias and range.
    private float[] biasRange = null;
    
}

