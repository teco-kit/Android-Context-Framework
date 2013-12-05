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

import edu.teco.context.configuration.FrameworkKeys.IAudioKeys;
import edu.teco.context.probe.sensors.AbstractSensorHandler;

public class MaxAmplitudeHandler extends AbstractSensorHandler implements IAudioKeys {

	@Override
	public int getValueSize() {
		return 1;
	}

	@Override
	public int getSensorType() {
		return 0;
	}

	@Override
	public String[] getValueNames() {
		// http://stackoverflow.com/questions/10668470/what-is-the-unit-of-the-returned-amplitude-of-getmaxamplitude-method
		// The MediaRecorder.getMaxAmplitude() function returns unsigned 16-bit
		// integer values (0-32767). Those values are probably calculated by
		// using abs() on -32768 � +32767, similar to the normal CD-quality
		// sample values. Negative amplitudes are just mirrored and therefore
		// the amplitude is always positive
		// The values are NOT RELATED to any concrete calibrated physical
		// property. The values are therefore, just 16-bit digitalization of
		// electrical output from 0-100% (maximum voltage range of that
		// microphone).
		// Microphones convert sound pressure (Pascal) linearly to voltage.
		// Therefore, values reported by the API correlate with sound pressure
		// BUT they are different on each device used and depends heavily on
		// brand, model, and specific device (circuits, amplifier, etc.) This
		// means that it is extremely hard to judge the values without
		// calibrating the phone microphone to a reliable sound pressure meter.
		return new String[] { MAX_AMPLITUDE };
	}

	@Override
	public double getMaxSamplingRate() {
		// this is currently 10 Hz but the buffer length should at least be 2x
		return 2 * 10;
	}

}
