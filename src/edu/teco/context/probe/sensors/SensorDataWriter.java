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
package edu.teco.context.probe.sensors;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.util.Date;

import edu.teco.context.configuration.FrameworkContext;

import android.os.Environment;
import android.util.Log;

public class SensorDataWriter {

	/** Singleton because only one writer should be open. */
	private static SensorDataWriter singletonInstance = null;

	/** Tag string for debug logs */
	private static final String TAG = "SensorDataWriter";

	private boolean isExternalStorageAvailable = false;
	private boolean isExternalStorageWriteable = false;

	private BufferedWriter writer = null;

	private SensorDataWriter() {
		super();
	}

	/**
	 * Creates a singleton SensorDataWriter or returns the already created
	 * object. Before writing to a file first openWriter() must be called.
	 * 
	 * @return The singleton object.
	 */
	public static SensorDataWriter getInstance() {
		if (singletonInstance == null) {
			singletonInstance = new SensorDataWriter();
		}
		return singletonInstance;
	}

	/**
	 * Creates a new BufferedWriter or does nothing if BufferedWriter is already
	 * open. Must be called before writing to a file.
	 * 
	 * @param fileName
	 *            The name of the .txt file where the data values will be saved.
	 */
	public void openWriter(String fileName) {

		if (!isWriterOpen()) {
			if (isStorageAvailable()) {
				Date currentDate = new Date();

				// check for null or empty string
				if (fileName == null)
					fileName = "SensorData_" + currentDate.getTime();
				else if (fileName.equals(""))
					fileName = "SensorData_" + currentDate.getTime();

				try {
					File file = new File(Environment.getExternalStorageDirectory(), fileName + ".txt");
					writer = new BufferedWriter(new FileWriter(file));
					writer.write("Systemtime in ms; Sensor Name; Value(s)");
					writer.newLine();
					if (FrameworkContext.INFO) Log.i(TAG, "Writer was opened with filename: " + fileName + ".txt");
				} catch (IOException e) {
					if (FrameworkContext.WARN) Log.w(TAG, "Writer could not be opened.");
					e.printStackTrace();
				}
			} else {
				if (FrameworkContext.WARN) Log.w(TAG, "Storage is not available. Nothing was done.");
			}
		} else {
			if (FrameworkContext.INFO) Log.i(TAG, "Writer is already open. Nothing was done.");
		}
	}

	/**
	 * Creates a new BufferedWriter with the filename SensorData_{current Date
	 * in nanoseconds} as .txt file or does nothing if BufferedWriter is already
	 * open. Must be called before writing to a file.
	 */
	public void openWriter() {
		openWriter(null);
	}

	/**
	 * Closes the BufferedWriter. Does nothing if already closed.
	 */
	public void closeWriter() {
		if (isWriterOpen()) {
			try {
				writer.flush();
				writer.close();
				if (FrameworkContext.INFO) Log.i(TAG, "Writer has been closed.");
				writer = null;
			} catch (IOException e) {
				e.printStackTrace();
			}
		}
	}

	/**
	 * Tells if BufferedWriter is open or not.
	 * 
	 * @return False if BufferedWriter is closed otherwise true.
	 */
	public boolean isWriterOpen() {
		if (writer == null)
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
			if (FrameworkContext.WARN) Log.w(TAG, "Media readonly.");
		} else {
			// Something else is wrong. It may be one of many other states, but
			// all we need to know is we can neither read nor write.
			isExternalStorageAvailable = isExternalStorageWriteable = false;
			if (FrameworkContext.WARN) Log.w(TAG, "Media neither read or write.");
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
			if (FrameworkContext.WARN) Log.w(TAG, "Media readonly.");
		} else {
			// Something else is wrong. It may be one of many other states, but
			// all we need to know is we can neither read nor write.
			externalStorageAvailable = externalStorageWriteable = false;
			if (FrameworkContext.WARN) Log.w(TAG, "Media neither read or write.");
		}
		
		if (externalStorageAvailable && externalStorageWriteable)
			return true;
		else
			return false;
		
	}

	/**
	 * Writes a line of sensor data to the .txt file on the sdCard.
	 * 
	 * @param time
	 * @param sensor
	 * @param values
	 */
	public void writeData(String time, String sensor, float[] values) {
		writeData(time, null, sensor, values);
	}
	
	public void writeData(String time, String sensor, double[] values) {
		// use of StringBuilder has better performance for loops
		StringBuilder builder = new StringBuilder();
		builder.append(time).append(";").append(sensor);
		for (double value : values) {
			builder.append(";").append(value);
		}
		
		writeDataLine(builder.toString());
	}
	
	/**
	 * Writes a line of sensor data to the .txt file on the sdCard.
	 * 
	 * @param time
	 * @param classLabel
	 * @param sensor
	 * @param values
	 */
	public void writeData(String time, String classLabel, String sensor, float[] values) {
		// use of StringBuilder has better performance for loops
		StringBuilder builder = new StringBuilder();
		builder.append(time).append(";").append(sensor);
		if (classLabel != null) {
			builder.append(";").append(classLabel);
		}
		for (float value : values) {
			builder.append(";").append(value);
		}
		
		writeDataLine(builder.toString());
	}
	
	public void writeDataLine(String line) {
		if (isWriterOpen()) {
			try {
				writer.write(line);
				writer.newLine();
			} catch (IOException e) {
				e.printStackTrace();
			}
		} else {
			if (FrameworkContext.WARN) Log.w(TAG, "Could not write data. Writer is not open.");
		}
	}

}
