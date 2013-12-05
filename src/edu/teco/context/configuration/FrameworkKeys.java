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
package edu.teco.context.configuration;

public class FrameworkKeys {

	public static interface IProbeKeys {
		public static final String
		ACCELEROMETER = "ACCELEROMETER",
		GYROSCOPE = "GYROSCOPE",
		LINEAR_ACCELERATION = "LINEAR_ACCELERATION",
		GRAVITY = "GRAVITY",
		MAGNETIC_FIELD = "MAGNETIC_FIELD",
		LIGHT = "LIGHT",
		PROXIMITY = "PROXIMITY",
		ORIENTATION = "ORIENTATION",
		ROTATION_VECTOR = "ROTATION_VECTOR",
		LOCATION = "LOCATION",
		AUDIO_VOLUME = "AUDIO_VOLUME";
	}
	
	public static interface IFeatureKeys {
		public static final String
		MEAN = "MEAN",
		MEDIAN = "MEDIAN",
		VARIANCE = "VARIANCE",
		STANDARD_DEVIATION = "STANDARD_DEVIATION",
		DIFFERENCE_MAX_MIN = "DIFFERENCE_MAX_MIN",
		FREQUENCY_PEAK = "FREQUENCY_PEAK",
		ENTROPY = "ENTROPY",
		FREQUENCY_DOMAIN_ENTROPY = "FREQUENCY_DOMAIN_ENTROPY";
	}
	
	public static interface IMetaDataTags {
		public static final String
		CONFIG_BEGIN = "<config>",
		CONFIG_END = "</config>",
		SAMPLE_WINDOW_BEGIN = "<sample_window>",
		SAMPLE_WINDOW_END = "<sample_window>",
		OVERLAP_BEGIN = "<overlap>",
		OVERLAP_END = "</overlap>",
		PROBE_BEGIN = "<probe>",
		PROBE_END = "</probe>",
		FEATURE_BEGIN = "<feature>",
		FEATURE_END = "</feature>",
		CONTEXT_LABEL_BEGIN = "<context_label>",
		CONTEXT_LABEL_END = "</context_label>";
	}
	
	public static interface ILocationKeys {
		public static final String 
		PROVIDER_NETWORK = "PROVIDER_NETWORK",
		PROVIDER_GPS = "PROVIDER_GPS",
		PROVIDER_GPS_NETWORK = "PROVIDER_GPS_NETWORK",
		STRATEGY_BEST_LOCATION = "STRATEGY_BEST_LOCATION",
		STRATEGY_MEAN_LOCATIONS = "STRATEGY_AVERAGE_LOCATIONS",
		LONGITUDE = "LONGITUDE",
		LATITUDE = "LATITUDE";
	}
	
	public static interface ISensorAccelerometerKeys {
		public static final String
		X = "X",
		Y = "Y",
		Z = "Z";
	}
	
	public static interface ISensorMagneticFieldKeys {
		public static final String
		X = "X",
		Y = "Y",
		Z = "Z";
	}
	
	public static interface ISensorLightKeys {
		public static final String
		LUX = "LUX";
	}
	
	public static interface ISensorProximityKeys {
		public static final String
		DISTANCE = "DISTANCE";
	}
	
	public static interface ISensorOrientationKeys {
		public static final String 
		AZIMUTH = "AZIMUTH", 
		PITCH = "PITCH", 
		ROLL = "ROLL";
	}
	
	public static interface ISensorRotationVectorKeys {
		public static final String 
		X_ROTATION = "X_ROTATION", 
		Y_ROTATION = "Y_ROTATION", 
		Z_ROTATION = "Z_ROTATION";
	}
	
	public static interface IAudioKeys {
		public static final String
		MAX_AMPLITUDE = "MAX_AMPLITUDE",
		AVERAGE_AMPLITUDE = "AVERAGE_AMPLITUDE",
		DECIBEL = "DECIBEL";
	}
	
	public static interface IParameterKeys {
		public static final String
		NETWORK_CONNECTIVITY = "NETWORK_QUALITY";
	}
	
	public static interface IBroadcastActions {
		public static final String
		// EXTRA "dataConnection" integer 0 aus = rot
		// EXTRA "dataConnection" integer 1 wird bald ausgehen = orange
		// EXTRA "dataConnection" integer 2 ist an = grün
		BROADCAST_ACTION_CONNECTION = "edu.teco.contextframework.CONNECTION",
		BROADCAST_ACTION_LIVE_CLASSIFICATION = "edu.teco.contextframework.LIVE_CLASSIFICATION",
		
		BROADCAST_ACTION_CONFIGURATION_CONTEXT_LABELS = "edu.teco.contextframework.configuration.CONTEXT_LABELS",
		BROADCAST_ACTION_CCONFIGURATION_CONTEXT_LABEL_SELECTED = "edu.teco.contextframework.configuration.CONTEXT_LABEL_SELECTED",
		BROADCAST_ACTION_CONFIGURATION_STATE = "edu.teco.contextframework.configuration.STATE",
		BROADCAST_ACTION_CONFIGURATION_DATA = "edu.teco.contextframework.configuration.DATA";
	}

	
	public static interface IBroadcastReceiverActions {
		public static final String
		BROADCAST_RECEIVER_TRAINING_CONTEXT_LABEL = "edu.teco.contextframework.training.CONTEXT_LABEL",
		BROADCAST_RECEIVER_TRAINING_START = "edu.teco.contextframework.training.START",
		BROADCAST_RECEIVER_TRAINING_STOP = "edu.teco.contextframework.training.START",
		BROADCAST_RECEIVER_LIVE_START = "edu.teco.contextframework.live.START",
		BROADCAST_RECEIVER_LIVE_STOP = "edu.teco.contextframework.live.STOP",
		BROADCAST_RECEIVER_CONFIGURATION_DATA_CONNECTION_ON = "edu.teco.contextframework.configuration.DATA_CONNECTION_ON",
		BROADCAST_RECEIVER_CONFIGURATION_DATA_CONNECTION_OFF = "edu.teco.contextframework.configuration.DATA_CONNECTION_OFF";
	}
	
}
