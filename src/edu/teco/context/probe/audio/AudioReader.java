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
 * org.hermit.android.io: Android utilities for accessing peripherals.
 * 
 * These classes provide some basic utilities for accessing the audio
 * interface, at present.
 *
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


import edu.teco.context.configuration.FrameworkContext;
import android.media.AudioFormat;
import android.media.AudioRecord;
import android.media.MediaRecorder;
import android.util.Log;


/**
 * A class which reads audio input from the mic in a background thread and
 * passes it to the caller when ready.
 * 
 * <p>To use this class, your application must have permission RECORD_AUDIO.
 */
public class AudioReader
{

    // ******************************************************************** //
    // Public Classes.
    // ******************************************************************** //

    /**
     * Listener for audio reads.
     */
    public static abstract class Listener {
        /**
         * Audio read error code: no error.
         */
        public static final int ERR_OK = 0;
        
        /**
         * Audio read error code: the audio reader failed to initialise.
         */
        public static final int ERR_INIT_FAILED = 1;
        
        /**
         * Audio read error code: an audio read failed.
         */
        public static final int ERR_READ_FAILED = 2;
        
        /**
         * An audio read has completed.
         * @param   buffer      Buffer containing the data.
         */
        public abstract void onReadComplete(short[] buffer);
        
        /**
         * An error has occurred.  The reader has been terminated.
         * @param   error       ERR_XXX code describing the error.
         */
        public abstract void onReadError(int error);
    }
    
    
    // ******************************************************************** //
    // Constructor.
    // ******************************************************************** //

	/**
	 * Create an AudioReader instance.
	 */
    public AudioReader() {
//        audioManager = (AudioManager) app.getSystemService(Context.AUDIO_SERVICE);
    }


    // ******************************************************************** //
    // Run Control.
    // ******************************************************************** //

    /**
     * Start this reader.
     * 
     * @param   rate        The audio sampling rate, in samples / sec.
     * @param   block       Number of samples of input to read at a time.
     *                      This is different from the system audio
     *                      buffer size.
     * @param   listener    Listener to be notified on each completed read.
     */
    public void startReader(int rate, int block, Listener listener) {
    	if (FrameworkContext.INFO) Log.i(TAG, "Reader: Start Thread");
        synchronized (this) {
            // Calculate the required I/O buffer size.
            int audioBuf = AudioRecord.getMinBufferSize(rate,
                                         AudioFormat.CHANNEL_CONFIGURATION_MONO,
                                         AudioFormat.ENCODING_PCM_16BIT) * 2;

            // Set up the audio input.
            audioInput = new AudioRecord(MediaRecorder.AudioSource.MIC,
                                         rate,
                                         AudioFormat.CHANNEL_CONFIGURATION_MONO,
                                         AudioFormat.ENCODING_PCM_16BIT,
                                         audioBuf);
            inputBlockSize = block;
            sleepTime = (long) (1000f / ((float) rate / (float) block));
            inputBuffer = new short[2][inputBlockSize];
            inputBufferWhich = 0;
            inputBufferIndex = 0;
            inputListener = listener;
            running = true;
            readerThread = new Thread(new Runnable() {
                
				public void run() { readerRun(); }
            }, "Audio Reader");
            readerThread.start();
        }
    }
    

    /**
     * Stop this reader.
     */
    public void stopReader() {
    	if (FrameworkContext.INFO) Log.i(TAG, "Reader: Signal Stop");
        synchronized (this) {
            running = false;
        }
        try {
            if (readerThread != null)
                readerThread.join();
        } catch (InterruptedException e) {
            ;
        }
        readerThread = null;
        
        // Kill the audio input.
        synchronized (this) {
            if (audioInput != null) {
                audioInput.release();
                audioInput = null;
            }
        }
        
        if (FrameworkContext.INFO) Log.i(TAG, "Reader: Thread Stopped");
    }


    // ******************************************************************** //
    // Main Loop.
    // ******************************************************************** //
    
    /**
     * Main loop of the audio reader.  This runs in our own thread.
     */
    private void readerRun() {
        short[] buffer;
        int index, readSize;
        
        int timeout = 200;
        try {
            while (timeout > 0 && audioInput.getState() != AudioRecord.STATE_INITIALIZED) {
                Thread.sleep(50);
                timeout -= 50;
            }
        } catch (InterruptedException e) { }

        if (audioInput.getState() != AudioRecord.STATE_INITIALIZED) {
        	if (FrameworkContext.ERROR) Log.e(TAG, "Audio reader failed to initialize");
            readError(Listener.ERR_INIT_FAILED);
            running = false;
            return;
        }

        try {
        	if (FrameworkContext.INFO) Log.i(TAG, "Reader: Start Recording");
            audioInput.startRecording();
            while (running) {
                long stime = System.currentTimeMillis();

                if (!running)
                    break;

                readSize = inputBlockSize;
                int space = inputBlockSize - inputBufferIndex;
                if (readSize > space)
                    readSize = space;
                buffer = inputBuffer[inputBufferWhich];
                index = inputBufferIndex;

                synchronized (buffer) {
                    int nread = audioInput.read(buffer, index, readSize);

                    boolean done = false;
                    if (!running)
                        break;
                    
                    if (nread < 0) {
                    	if (FrameworkContext.ERROR) Log.e(TAG, "Audio read failed: error " + nread);
                        readError(Listener.ERR_READ_FAILED);
                        running = false;
                        break;
                    }
                    int end = inputBufferIndex + nread;
                    if (end >= inputBlockSize) {
                        inputBufferWhich = (inputBufferWhich + 1) % 2;
                        inputBufferIndex = 0;
                        done = true;
                    } else
                        inputBufferIndex = end;

                    if (done) {
                        readDone(buffer);

                        // Because our block size is way smaller than the audio
                        // buffer, we get blocks in bursts, which messes up
                        // the audio analyzer.  We don't want to be forced to
                        // wait until the analysis is done, because if
                        // the analysis is slow, lag will build up.  Instead
                        // wait, but with a timeout which lets us keep the
                        // input serviced.
                        long etime = System.currentTimeMillis();
                        long sleep = sleepTime - (etime - stime);
                        if (sleep < 5)
                            sleep = 5;
                        try {
                            buffer.wait(sleep);
                        } catch (InterruptedException e) { }
                    }
                }
            }
        } finally {
        	if (FrameworkContext.INFO) Log.i(TAG, "Reader: Stop Recording");
            if (audioInput.getState() == AudioRecord.RECORDSTATE_RECORDING)
                audioInput.stop();
        }
    }

    
    /**
     * Notify the client that a read has completed.
     * 
     * @param   buffer      Buffer containing the data.
     */
    private void readDone(short[] buffer) {
        inputListener.onReadComplete(buffer);
    }
    
    
    /**
     * Notify the client that an error has occurred.  The reader has been
     * terminated.
     * 
     * @param   error       ERR_XXX code describing the error.
     */
    private void readError(int code) {
        inputListener.onReadError(code);
    }
    

    // ******************************************************************** //
    // Class Data.
    // ******************************************************************** //

    // Debugging tag.
	private static final String TAG = "WindMeter";

	
	// ******************************************************************** //
	// Private Data.
	// ******************************************************************** //
    
    // Our audio input device.
    private AudioRecord audioInput;

    // Our audio input buffer, and the index of the next item to go in.
    private short[][] inputBuffer = null;
    private int inputBufferWhich = 0;
    private int inputBufferIndex = 0;

    // Size of the block to read each time.
    private int inputBlockSize = 0;
    
    // Time in ms to sleep between blocks, to meter the supply rate.
    private long sleepTime = 0;
    
    // Listener for input.
    private Listener inputListener = null;
    
    // Flag whether the thread should be running.
    private boolean running = false;
    
    // The thread, if any, which is currently reading.  Null if not running.
    private Thread readerThread = null;
    
}

