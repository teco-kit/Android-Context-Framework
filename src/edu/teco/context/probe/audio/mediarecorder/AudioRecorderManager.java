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
package edu.teco.context.probe.audio.mediarecorder;

import java.io.IOException;
import java.util.Date;

import android.media.MediaRecorder;
import android.os.Environment;
import edu.teco.context.probe.sensors.SensorDataWriter;

public class AudioRecorderManager {

// 	Singleton currently not in use
//	/** Singleton because only one writer should be open. */
//	private static AudioRecorderManager singletonInstance = null;
//	
//	private AudioRecorderManager() {
//		super();
//	}
//	
//	/**
//	 * Creates a singleton AudioRecorderManager or returns the already created
//	 * object. Before recording start() must be called.
//	 * 
//	 * @return The singleton object.
//	 */
//	public static AudioRecorderManager getInstance() {
//		if (singletonInstance == null) {
//			singletonInstance = new AudioRecorderManager();
//		}
//		return singletonInstance;
//	}

	private MediaRecorder mRecorder = null;
	private boolean mIsRecordedToFile = false;
	
	public boolean isRecordedToFile() {
		return mIsRecordedToFile;
	}
	
	public void setIsRecordedToFile(boolean isRecordedToFile) {
		mIsRecordedToFile = isRecordedToFile;
	}
	
	public void start() {
		if (mRecorder == null) {
			mRecorder = new MediaRecorder();
			mRecorder.setAudioSource(MediaRecorder.AudioSource.MIC);
			mRecorder.setOutputFormat(MediaRecorder.OutputFormat.THREE_GPP);
			mRecorder.setAudioEncoder(MediaRecorder.AudioEncoder.AMR_NB);
			
			// only record to a file if set and external storage is available
			if (mIsRecordedToFile) {
				if (SensorDataWriter.isExternalStorageAvailable()) {
					Date currentDate = new Date();
					String filePath = Environment.getExternalStorageDirectory().getPath() + "/AudioRecord_" + currentDate.getTime() + ".3gp";
					mRecorder.setOutputFile(filePath);
				} else {
					mRecorder.setOutputFile("/dev/null");
				}
			} else  {
				mRecorder.setOutputFile("/dev/null");
			}
			
			try {
				mRecorder.prepare();
			} catch (IllegalStateException e) {
				e.printStackTrace();
			} catch (IOException e) {
				e.printStackTrace();
			}
			mRecorder.start();
		}
	}

	public void stop() {
		if (mRecorder != null) {
			mRecorder.stop();
			mRecorder.release();
			mRecorder = null;
		}
	}

	public double getMaxAmplitude() {
		if (mRecorder != null)
			return (mRecorder.getMaxAmplitude());
		else
			return 0;
	}
	
	public float getMaxAmplitudeFloat() {
		if (mRecorder != null)
			return mRecorder.getMaxAmplitude();
		else
			return 0;
	}
}
