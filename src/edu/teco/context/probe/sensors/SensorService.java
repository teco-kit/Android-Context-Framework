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

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.Map;

import edu.teco.context.configuration.FrameworkKeys.IProbeKeys;

import android.app.Notification;
import android.app.Service;
import android.content.BroadcastReceiver;
import android.content.IntentFilter;
import android.content.Context;
import android.content.Intent;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.os.Binder;
import android.os.Handler;
import android.os.IBinder;
import android.os.Message;
import android.os.Messenger;
import android.os.PowerManager.WakeLock;
import android.os.PowerManager;
import android.os.Process;
import android.os.RemoteException;
import android.util.Log;

public class SensorService extends Service implements SensorEventListener, IProbeKeys {
	
	public static final String TAG = SensorService.class.getName();

	
	/** for some devices the sensors have to be re-registered to keep on 
	 * receiving sensor events.
	 */
	private boolean mIsUsingScreenScreenOffReceiver = false;
	
	// Delay until the Sensor get re-registered. This is necessary to give the
	// device some time to shutdown
	public static final int SCREEN_OFF_RECEIVER_DELAY = 500;

	private SensorManager mSensorManager = null;
	private WakeLock mWakeLock = null;

	// Keeps Track of all current registered clients.
	// Makes it possible to send SensorEvents to multiple Clients, which get
	// registered by sending a message with MSG_REGISTER_CLIENT
	private ArrayList<Messenger> mClients = new ArrayList<Messenger>();
	/**
	 * Target we publish for clients to send messages to IncomingHandler.
	 */
	final Messenger mMessenger = new Messenger(new IncomingHandler());

	// MessageTypes for the messenger
	public static final int MSG_REGISTER_CLIENT = 1;
	public static final int MSG_UNREGISTER_CLIENT = 2;
	// This is used to forward the event to the Activity
	public static final int MSG_SEND_EVENT = 3;

	private static boolean isRunning = false;

	private Map<String, Sensor> mSensorMap = null;

	
	
	/*---------------------------------------------------------------------------------------
	 *
	 *Methods for Creation / Binding / Destroying
	 *
	 ----------------------------------------------------------------------------------------*/
	
	/**
	 * On Create, fired when Service is created. Creates mSensorManager
	 */
	@Override
	public void onCreate() {
		super.onCreate();

		mSensorManager = (SensorManager) getSystemService(SENSOR_SERVICE);
		mSensorMap = new LinkedHashMap<String, Sensor>();

		PowerManager manager = (PowerManager) getSystemService(Context.POWER_SERVICE);
		mWakeLock = manager.newWakeLock(PowerManager.PARTIAL_WAKE_LOCK, TAG);
		mWakeLock.acquire();
		
		if (mIsUsingScreenScreenOffReceiver) {
			registerReceiver(mReceiver, new IntentFilter(Intent.ACTION_SCREEN_OFF));
		}

		isRunning = true;
	}
	
	/**
	 * Destroy Service
	 */
	@Override
	public void onDestroy() {
		Log.d(TAG, "On Destroy is called");
		
		if (mIsUsingScreenScreenOffReceiver) {
			unregisterReceiver(mReceiver);
		}
		unregisterListener();
		// mWakeLock.release();
		stopForeground(true);
	}

	/**
	 * On bind. Set messenger for bidirectional communication of Activity and
	 * Service
	 */
	@Override
	public IBinder onBind(Intent intent) {
		return mMessenger.getBinder();
	}

	// This is the object that receives interactions from clients. See
	// RemoteService for a more complete example.
	private final IBinder mBinder = new LocalBinder();
	private String[] mSensorValues;

	public class LocalBinder extends Binder {
		SensorService getService() {
			return SensorService.this;
		}
	}

	/**
	 * Called when service is bound to activity
	 */
	@Override
	public int onStartCommand(Intent intent, int flags, int startId) {
		super.onStartCommand(intent, flags, startId);

		startForeground(Process.myPid(), new Notification());
		mSensorValues = (String[]) intent.getExtras().get("sensorKeys");
		registerListener(mSensorValues);
		mWakeLock.acquire();

		return START_STICKY;
	}
	
	/*---------------------------------------------------------------------------------------
	 *
	 *Methods for Communication and Messaging
	 *
	 ----------------------------------------------------------------------------------------*/
	
	
	/**
	 * Handler which keeps track of all incoming messages from the client
	 * 
	 * @author XXX
	 * 
	 */
	class IncomingHandler extends Handler {
		@Override
		public void handleMessage(Message msg) {
			switch (msg.what) {
			// TODO: Maybe add the possibility to unregister the listeners via a
			// the messenger
			case MSG_REGISTER_CLIENT:
				mClients.add(msg.replyTo);
				break;
			case MSG_UNREGISTER_CLIENT:
				mClients.remove(msg.replyTo);
				break;
			default:
				super.handleMessage(msg);
			}
		}
	}

	
	/**
	 * Broadcast Receiver. Receives the event of turning the screen off and
	 * re-registers the sensors.
	 */
	public BroadcastReceiver mReceiver = new BroadcastReceiver() {
		@Override
		public void onReceive(Context context, Intent intent) {
			Log.i(TAG, "onReceive(" + intent + ")");

			if (!intent.getAction().equals(Intent.ACTION_SCREEN_OFF)) {
				return;
			}

			Runnable runnable = new Runnable() {
				public void run() {
					Log.i(TAG,
							"Runnable executing. Re-register the Sensor listeners");
					// Unregistering is done in the register method
					// unregisterListener();

					registerListener(mSensorValues);
				}
			};

			new Handler().postDelayed(runnable, SCREEN_OFF_RECEIVER_DELAY);
		}
	};
	

	

	/**
	 * Called by the onSensorChanged Method. Forwards the SensorEvent to the
	 * activity via the messenger Checks the mClients and sends to each
	 * registered Client
	 * 
	 * @param event
	 * @throws RemoteException
	 */
	private void forwardEventToMainActivity(SensorEvent event) {
		for (int i = mClients.size() - 1; i >= 0; i--) {
			try {
				// Send data as object
				mClients.get(i).send(
						Message.obtain(null, MSG_SEND_EVENT, event));
			} catch (RemoteException e) {
				// The client is dead. Remove it from the list; we are going
				// through the list from back to front so this is safe to do
				// inside the loop.
				mClients.remove(i);
			}
		}
	}
	
	/*---------------------------------------------------------------------------------------
	 *
	 * Methods for registering and unregistering the Sensors
	 *
	 ----------------------------------------------------------------------------------------*/
	
	
	/**
	 * Register the Service to Listen for all Events
	 */
	private void registerListener(String[] sensorKeys) {

		for (String sensorKey : sensorKeys) {
			// unregister and delete sensor if already existing
			if (mSensorMap.containsKey(sensorKey)) {
				mSensorManager.unregisterListener(this,
						mSensorMap.get(sensorKey));
				mSensorMap.remove(sensorKey);
			}

			// add a new sensor to map and register it with the SensorManager
			if (sensorKey.equals(ACCELEROMETER)) {
				Sensor accelerometer = mSensorManager
						.getDefaultSensor(Sensor.TYPE_ACCELEROMETER);
				mSensorMap.put(sensorKey, accelerometer);
				mSensorManager.registerListener(this, accelerometer,
						SensorManager.SENSOR_DELAY_FASTEST);

			} else if (sensorKey.equals(MAGNETIC_FIELD)) {
				Sensor magneticField = mSensorManager
						.getDefaultSensor(Sensor.TYPE_MAGNETIC_FIELD);
				mSensorMap.put(sensorKey, magneticField);
				mSensorManager.registerListener(this, magneticField,
						SensorManager.SENSOR_DELAY_FASTEST);

			} else if (sensorKey.equals(LIGHT)) {
				Sensor light = mSensorManager
						.getDefaultSensor(Sensor.TYPE_LIGHT);
				mSensorMap.put(sensorKey, light);
				mSensorManager.registerListener(this, light,
						SensorManager.SENSOR_DELAY_FASTEST);

			} else if (sensorKey.equals(PROXIMITY)) {
				Sensor proximity = mSensorManager
						.getDefaultSensor(Sensor.TYPE_PROXIMITY);
				mSensorMap.put(sensorKey, proximity);
				mSensorManager.registerListener(this, proximity,
						SensorManager.SENSOR_DELAY_FASTEST);

			} else if (sensorKey.equals(ORIENTATION)) {
				Sensor orientation = mSensorManager
						.getDefaultSensor(Sensor.TYPE_ORIENTATION);
				mSensorMap.put(sensorKey, orientation);
				mSensorManager.registerListener(this, orientation,
						SensorManager.SENSOR_DELAY_FASTEST);
			
			}  else if (sensorKey.equals(GYROSCOPE)) {
				Sensor gyroscope = mSensorManager
						.getDefaultSensor(Sensor.TYPE_GYROSCOPE);
				mSensorMap.put(sensorKey, gyroscope);
				mSensorManager.registerListener(this, gyroscope,
						SensorManager.SENSOR_DELAY_FASTEST);
			}
		}
	}

	/**
	 * Unregister the Sensors
	 */
	private void unregisterListener() {
		mSensorManager.unregisterListener(this);
	}

	/*---------------------------------------------------------------------------------------
	 *
	 * Sensor Event Listeners
	 *
	 ----------------------------------------------------------------------------------------*/
	

	/**
	 * On accuracy changed Event
	 */
	public void onAccuracyChanged(Sensor sensor, int accuracy) {
		Log.i(TAG, "onAccuracyChanged().");
	}

	/**
	 * The OnSensorChanged event. Gets fired when Sensor values come in The
	 * Event is forwarded to the main Activity by the method
	 * forwardEventTomMainActivity
	 */
	public void onSensorChanged(SensorEvent event) {

		Log.d(TAG, "onSensorChanged()");

		forwardEventToMainActivity(event);

	}



	/**
	 * Check if Service is running Not used, yet
	 * 
	 * @return
	 */
	public static boolean isRunning() {
		return isRunning;
	}


}
