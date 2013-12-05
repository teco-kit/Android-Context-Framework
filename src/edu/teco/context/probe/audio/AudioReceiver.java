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
package edu.teco.context.probe.audio;

import android.util.Log;
import edu.teco.context.configuration.FrameworkContext;
import edu.teco.context.configuration.FrameworkKeys.IProbeKeys;
import edu.teco.context.configuration.FrameworkState;
import edu.teco.context.log.DataLogger;
import edu.teco.context.probe.ProbeDataListener;
import edu.teco.context.probe.audio.AudioAnalyser.VolumeListener;

public class AudioReceiver implements VolumeListener {
	
	private static final String TAG = "AudioReceiver";
	
	private FrameworkContext mFrameworkContext = null;
	
	private ProbeDataListener mDataReceiver = null;
	
	// based on this example http://code.google.com/p/moonblink/wiki/Audalyzer
	private AudioAnalyser mAnalyzer = null;
	private boolean mRecording = false;
	
	private Thread thread = new Thread()
    {
        @Override
        public void run() {
        	while (true) {
        		if (mRecording) {
        			tick();
        		}
        	}
        }
    };
	
	public AudioReceiver(ProbeDataListener probeDataListener) {
		
		mFrameworkContext = FrameworkContext.getInstance();
		mDataReceiver = probeDataListener;
		mAnalyzer = new AudioAnalyser(this);
    	thread.start();
	}
	
	public void registerAudio() {
		mAnalyzer.measureStart();
		mRecording = true;
		logDescription();
		if (FrameworkContext.INFO) Log.i(TAG, "AudioReceiver registered");
	}
	
	public void unregisterAudio() {
		mRecording = false;
		mAnalyzer.measureStop();
		if (FrameworkContext.INFO) Log.i(TAG, "AudioReceiver unregistered");
	}
	
	private void logDescription() {
		String logMessage = IProbeKeys.AUDIO_VOLUME + " (ProbeKey name);System Time (ms);Audio volume (mean of audio frame)";
		DataLogger.getInstance().logComment(logMessage);
	}
	
	private String createAudioMessage(String audioKey, double audioValue) {
		StringBuilder sb = new StringBuilder(audioKey).append(";").append(System.currentTimeMillis()).append(";")
				.append(audioValue);
		return sb.toString();
	}
	
	private void tick() {
        try {
            // Do the application's physics.
            long now = System.currentTimeMillis();
            if (mAnalyzer != null) {
            	mAnalyzer.doUpdate(now);
            }
            // And update the screen.
        } catch (Exception e) {
        	e.printStackTrace();
        }
    }

	public void receiveVolume(double volume) {
		if (FrameworkState.isFeatureCalculationState(mFrameworkContext.getFrameworkState())) {
			mDataReceiver.onAudioVolumeUpdate(volume);
		}
		
		if (mFrameworkContext.isLogging()) {
			DataLogger.getInstance().logData(createAudioMessage(IProbeKeys.AUDIO_VOLUME, volume));
		}
	}

}
