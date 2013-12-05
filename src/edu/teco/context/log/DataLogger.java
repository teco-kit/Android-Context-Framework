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
package edu.teco.context.log;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.util.Date;

import android.os.Environment;
import android.util.Log;
import edu.teco.context.configuration.FrameworkConfiguration;
import edu.teco.context.configuration.FrameworkContext;

/*for detailed logging solutions in android look at
 * 
 * sensor logging
 * http://stackoverflow.com/questions/1916660/need-to-read-android-sensors-really-fast
 * http://stackoverflow.com/questions/12246942/how-to-log-data-from-android-motion-sensors-at-a-fixed-rate
 * http://stackoverflow.com/questions/6682060/android-sensor-recording-values-simultaneously
 * http://stackoverflow.com/questions/5216147/how-to-use-multithreading-in-android-for-an-event-handling-function-sensorliste
 * http://stackoverflow.com/questions/7701569/collecting-storing-and-retrieving-sensor-data
 * http://stackoverflow.com/questions/590069/how-would-you-code-an-efficient-circular-buffer-in-java-or-c-sharp
 * 
 * log to text file
 * http://stackoverflow.com/questions/1756296/android-writing-logs-to-text-file
 * http://stackoverflow.com/questions/2116260/logging-to-a-file-on-android
 * 
 * java.util.logging
 * http://stackoverflow.com/questions/4561345/how-to-configure-java-util-logging-on-android
 * http://stackoverflow.com/questions/7819649/android-util-log-vs-java-util-logging-for-writing-log-to-a-file
 */

public class DataLogger {

	/** Singleton because only one writer should be open. */
	private static DataLogger singletonInstance = null;

	/** Tag string for debug logs */
	private static final String TAG = "DataLog";

	private boolean isExternalStorageAvailable = false;
	private boolean isExternalStorageWriteable = false;

	private BufferedWriter mWriter = null;
	
	private String mFileName = null;
	
	private String mCommentToken = "% ";

	private DataLogger() {
		super();
	}

	/**
	 * Creates a singleton DataLogger or returns the already created
	 * object. Before writing to a file first openWriter() must be called.
	 * 
	 * @return The singleton object.
	 */
	public static DataLogger getInstance() {
		if (singletonInstance == null) {
			singletonInstance = new DataLogger();

			singletonInstance.mFileName = "ProbeLog_" + new Date().getTime();
			singletonInstance.openWriter();
		}
		return singletonInstance;
	}
	
	/**
	 * Creates a new BufferedWriter with the filename ProbeLog_{current Date
	 * in nanoseconds} as .txt file or does nothing if BufferedWriter is already
	 * open. Must be called before writing to a file.
	 */
	public void openLogger() {
		openWriter();
	}
	
	public void createNewLogFile() {
		mFileName = "ProbeLog_" + new Date().getTime();
		closeWriter();
		openWriter();
	}

	/**
	 * Creates a new BufferedWriter or does nothing if BufferedWriter is already
	 * open. Must be called before writing to a file.
	 * 
	 * @param fileName
	 *            The name of the .txt file where the data values will be saved.
	 */
	private void openWriter() {
		if (isStorageAvailable()) {

			try {
				String logDirectory = FrameworkConfiguration.getInstance().getLogDirectory();
				File logDir = new File(Environment.getExternalStorageDirectory() + logDirectory);
				logDir.mkdirs();
				
				File file = new File(logDir, mFileName + ".txt");
				
				mWriter = new BufferedWriter(new FileWriter(file, true), 32768);
				
				mWriter.write(mCommentToken + "This is the probe log file for all probe data from " + new Date().toString());
				mWriter.newLine();
				if (FrameworkContext.INFO) Log.i(TAG, "Writer was opened with filename: " + mFileName + ".txt");
			} catch (IOException e) {
				if (FrameworkContext.INFO) Log.i(TAG, "Writer could not be opened.");
				e.printStackTrace();
			}
		} else {
			if (FrameworkContext.INFO) Log.i(TAG, "Storage is not available. Nothing was done.");
		}
	}
	
	public void flushWriter() {
		if (isWriterOpen()) {
			try {
				mWriter.flush();
			} catch (IOException e) {
				e.printStackTrace();
			}
		} else {
			if (FrameworkContext.INFO) Log.i(TAG, "Writer was not open.");
		}
	}

	/**
	 * Closes the BufferedWriter. Does nothing if already closed.
	 */
	public void closeWriter() {
		if (isWriterOpen()) {
			try {
				mWriter.flush();
				mWriter.close();
				if (FrameworkContext.INFO) Log.i(TAG, "Writer has been closed.");
				mWriter = null;
			} catch (IOException e) {
				e.printStackTrace();
			}
		} else {
			if (FrameworkContext.INFO) Log.i(TAG, "Writer was not open.");
		}
	}

	/**
	 * Tells if BufferedWriter is open or not.
	 * 
	 * @return False if BufferedWriter is closed otherwise true.
	 */
	public boolean isWriterOpen() {
		if (mWriter == null)
			return false;
		else
			return true;
	}

	private void checkStorageState() {

		String state = Environment.getExternalStorageState();

		if (Environment.MEDIA_MOUNTED.equals(state)) {
			// We can read and write the media
			isExternalStorageAvailable = isExternalStorageWriteable = true;
			if (FrameworkContext.INFO) Log.i(TAG, "Media is available and writeable.");
		} else if (Environment.MEDIA_MOUNTED_READ_ONLY.equals(state)) {
			// We can only read the media
			isExternalStorageAvailable = true;
			isExternalStorageWriteable = false;
			if (FrameworkContext.INFO) Log.i(TAG, "Media readonly.");
		} else {
			// Something else is wrong. It may be one of many other states, but
			// all we need to know is we can neither read nor write.
			isExternalStorageAvailable = isExternalStorageWriteable = false;
			if (FrameworkContext.INFO) Log.i(TAG, "Media neither read or write.");
		}
	}

	/**
	 * Checks if external sdCard storage is available and writable.
	 * 
	 * @return True if storage is available for writing otherwise false.
	 */
	public boolean isStorageAvailable() {
		checkStorageState();
		if (isExternalStorageAvailable && isExternalStorageWriteable)
			return true;
		else
			return false;
	}
	
	/**
	 * Method to check if external storage is available and writable.
	 * 
	 * @return true if available and writable
	 */
	public static boolean isExternalStorageAvailable() {
		
		boolean externalStorageAvailable = false;
		boolean externalStorageWriteable = false;
		
		String state = Environment.getExternalStorageState();

		if (Environment.MEDIA_MOUNTED.equals(state)) {
			// We can read and write the media
			externalStorageAvailable = externalStorageWriteable = true;
			if (FrameworkContext.INFO) Log.i(TAG, "Media is available and writeable.");
		} else if (Environment.MEDIA_MOUNTED_READ_ONLY.equals(state)) {
			// We can only read the media
			externalStorageAvailable = true;
			externalStorageWriteable = false;
			if (FrameworkContext.INFO) Log.i(TAG, "Media readonly.");
		} else {
			// Something else is wrong. It may be one of many other states, but
			// all we need to know is we can neither read nor write.
			externalStorageAvailable = externalStorageWriteable = false;
			if (FrameworkContext.INFO) Log.i(TAG, "Media neither read or write.");
		}
		
		if (externalStorageAvailable && externalStorageWriteable)
			return true;
		else
			return false;
		
	}
	
	public void log(String message) {
		if (isWriterOpen()) {
			try {
				mWriter.write(message);
				mWriter.newLine();
			} catch (IOException e) {
				e.printStackTrace();
			}
		} else {
			if (FrameworkContext.INFO) Log.i(TAG, "Could not write data. Writer is not open.");
		}
	}
	
	public void logData(String data) {
		log(data);
	}

	public void logComment(String comment) {
		log(mCommentToken + comment);
	}
}

