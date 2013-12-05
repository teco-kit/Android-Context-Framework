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
package edu.teco.context.probe.location;

import java.util.Arrays;

import edu.teco.context.configuration.FrameworkContext;

import android.util.Log;

public class LocationBuffer {
	
	private static String TAG = "LocationBuffer";
	
	private double[] mLatitudeBuffer = null;
	private double[] mLongitudeBuffer = null;
	
	private int mSamplingRate = 100;
	
	private int mCurrentPosition = 0;

	public LocationBuffer(double sampleWindow) {
		int capacity = (int) Math.ceil(mSamplingRate * sampleWindow);
		
		mLatitudeBuffer = new double[capacity];
		mLongitudeBuffer = new double[capacity];
		
		Arrays.fill(mLatitudeBuffer, 0.0);
		Arrays.fill(mLongitudeBuffer, 0.0);
	}
	
	public void clearBuffer() {
		
		Arrays.fill(mLatitudeBuffer, 0, mCurrentPosition, 0.0);
		Arrays.fill(mLongitudeBuffer, 0, mCurrentPosition, 0.0);
		
		mCurrentPosition = 0;
	}
	
	public void putCoordinates(double latitude, double longitude) {
		if (mLatitudeBuffer.length > mCurrentPosition) {
			mLatitudeBuffer[mCurrentPosition] = latitude;
			mLongitudeBuffer[mCurrentPosition] = longitude;
			
			mCurrentPosition++;
		} else {
			if (FrameworkContext.WARN) Log.w(TAG, "Cannot put values into buffer, because max position is reached. Increase buffer size.");
		}
	}
	
	
	public double[] getMeanCoordinates() {

		double latitudeMean = 0.0;
		double longitudeMean = 0.0;
		
		if (mCurrentPosition > 0) {
			
			for (int i = 0; i <= mCurrentPosition; i++) {
				latitudeMean += mLatitudeBuffer[i];
				longitudeMean += mLongitudeBuffer[i];
			}
			
			latitudeMean = latitudeMean / (mCurrentPosition + 1 );
			longitudeMean = longitudeMean / (mCurrentPosition + 1 );
			
		} else {
			if (FrameworkContext.WARN) Log.w(TAG, "No location values to calculate the mean. These are zero values");
		}

		return new double[] {latitudeMean, longitudeMean};
	}

}
