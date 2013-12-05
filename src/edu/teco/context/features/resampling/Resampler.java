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
package edu.teco.context.features.resampling;

import java.util.List;

import edu.teco.context.configuration.FrameworkContext;

import android.util.Log;

public class Resampler {
	
	public static void resampleData(List<SensorData> events, int sampleRateHz) {

		for (int i = 0; i < events.size(); i++) {
			if (FrameworkContext.INFO) Log.i("RESAMPLE", i + ". " + events.get(i).toString());
		}

		double timeFrameRatio = 1000000 / sampleRateHz;

		long firstTimeStamp = events.get(0).mTimeStamp;
		long lastTimeStamp = events.get(events.size() - 1).mTimeStamp;

		long timeDifference = lastTimeStamp - firstTimeStamp;

		int size = (int) Math.round((timeDifference) / timeFrameRatio);

		double newTS = (Math.ceil(firstTimeStamp / timeFrameRatio))
				* timeFrameRatio;
		
		if (FrameworkContext.INFO) Log.i("RESAMPLE", "Original frequency: " + ((double) events.size() / timeDifference) * 1000000);

		if (FrameworkContext.INFO) Log.i("RESAMPLE", "ratio = " + timeFrameRatio + "(ms) first = "
				+ firstTimeStamp + "(ms) last = " + lastTimeStamp
				+ "(ms) time difference = " + timeDifference + "(ms) size = "
				+ size + " newTS = " + (long) newTS + "(ms)");

		SensorData[] data = new SensorData[size];

		int index = 0;
		SensorData measurement = events.get(index);

		for (int i = 0; i < data.length; i++) {

			for (int j = index; j < events.size(); j++) {
				if (events.get(j).mTimeStamp > newTS) {
					break;
				} else {
					measurement = events.get(j);
					index++;
				}
			}

			if (FrameworkContext.INFO) Log.i("RESAMPLE", "Index: " + index);
			data[i] = new SensorData();
			data[i].addData((long) newTS, measurement.mValues);
			newTS += timeFrameRatio;
			if (FrameworkContext.INFO) Log.i("RESAMPLE", i + ". " + data[i].toString());
		}
	}

}
