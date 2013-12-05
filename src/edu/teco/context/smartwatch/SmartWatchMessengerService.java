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
package edu.teco.context.smartwatch;

import java.lang.ref.WeakReference;

import edu.teco.context.configuration.FrameworkConfiguration;
import edu.teco.context.configuration.FrameworkContext;

import android.app.Service;
import android.content.Intent;
import android.os.Handler;
import android.os.IBinder;
import android.os.Message;
import android.os.Messenger;
import android.util.Log;
import android.widget.Toast;

/**
 * Service to communicate with the Sony SmartWatch.
 * 
 * References:
 * Service: http://developer.android.com/guide/components/services.html
 * Messenger (used to communicate with SmartWatch) http://developer.android.com/guide/components/bound-services.html#Messenger
 * Intents: http://developer.android.com/guide/components/intents-filters.html
 * AIDL (alternative superior interface description) http://developer.android.com/guide/components/aidl.html
 * 
 * Communication with UI Activity of App:
 * Example used to communicate with activity: http://www.websmithing.com/2011/02/01/how-to-update-the-ui-in-an-android-activity-using-data-from-a-background-service/
 * 
 * http://stackoverflow.com/questions/2468874/how-can-i-update-information-in-an-android-activity-from-a-background-service
 * http://stackoverflow.com/questions/4111398/android-notify-activity-from-service
 * http://developer.android.com/reference/android/app/Service.html#LocalServiceSample
 * http://stackoverflow.com/questions/4300291/example-communication-between-activity-and-service-using-messaging
 * http://androidtrainningcenter.blogspot.in/2012/08/updating-ui-from-background-service-in.html
 * 
 * @author Sven Frauen
 *
 */
public class SmartWatchMessengerService extends Service {
	
	public static final String TAG = "SmartWatchMessengerService";
    
	/** Commands to the service depending on the button clicked on the SmartWatch. */
//    private static final int CLICKED_BUTTON_1 = 1;
//    private static final int CLICKED_BUTTON_2 = 2;
//    private static final int CLICKED_BUTTON_3 = 3;
//    private static final int CLICKED_BUTTON_4 = 4;
//    private static final int CLICKED_BUTTON_5 = 5;
//    private static final int CLICKED_BUTTON_6 = 6;
//    private static final int CLICKED_BUTTON_7 = 7;
//    private static final int CLICKED_BUTTON_8 = 8;
//    private static final int CLICKED_BUTTON_9 = 9;
    
    private String[] mContextLabels = null;

    
    public static final String BROADCAST_ACTION_CONTEXT_LABEL_CHANGE = "edu.teco.context.smartwatch.contextlabelchange";
    public static final String BROADCAST_ACTION_CONTEXT_TRAINING = "edu.teco.context.smartwatch.training";
    public static final String BROADCAST_ACTION_CONTEXT_LIVE = "edu.teco.context.smartwatch.live";
    public static final String BROADCAST_ACTION_CONTEXT_DATA = "edu.teco.context.smartwatch.data";
    Intent labelIntent = null;
    Intent trainingIntent = null;
    Intent liveIntent = null;
    Intent dataIntent = null;
    
    /**
     * Handler of incoming messages from clients.
     */
    static class IncomingHandler extends Handler {
        
    	private final WeakReference<SmartWatchMessengerService> mService;
    	
    	IncomingHandler(SmartWatchMessengerService service) {
            mService = new WeakReference<SmartWatchMessengerService>(service);
        }
    	
    	@Override
        public void handleMessage(Message msg) {
    		
    		SmartWatchMessengerService service = mService.get();
    		
    		if (service != null) {
//                switch (msg.what) {
//                case CLICKED_BUTTON_1:
//                    Toast.makeText(service.getApplicationContext(), "Button 1", Toast.LENGTH_SHORT).show();
//                    break;
//                case CLICKED_BUTTON_2:
//                    Toast.makeText(service.getApplicationContext(), "Button 2", Toast.LENGTH_SHORT).show();
//                    break;
//                case CLICKED_BUTTON_3:
//                    Toast.makeText(service.getApplicationContext(), "Button 3", Toast.LENGTH_SHORT).show();
//                    break;
//                case CLICKED_BUTTON_4:
//                    Toast.makeText(service.getApplicationContext(), "Button 4", Toast.LENGTH_SHORT).show();
//                    break;
//                case CLICKED_BUTTON_5:
//                    Toast.makeText(service.getApplicationContext(), "Button 5", Toast.LENGTH_SHORT).show();
//                    break;
//                case CLICKED_BUTTON_6:
//                    Toast.makeText(service.getApplicationContext(), "Button 6", Toast.LENGTH_SHORT).show();
//                    break;
//                case CLICKED_BUTTON_7:
//                    Toast.makeText(service.getApplicationContext(), "Button 7", Toast.LENGTH_SHORT).show();
//                    
//                    break;
//                case CLICKED_BUTTON_8:
//                    Toast.makeText(service.getApplicationContext(), "Button 8", Toast.LENGTH_SHORT).show();
//                    break;
//                case CLICKED_BUTTON_9:
//                    Toast.makeText(service.getApplicationContext(), "Button 9", Toast.LENGTH_SHORT).show();
//                    break;
//            	  default:
//                    super.handleMessage(msg);
//                }               
                if (msg.what > 6) {
                	boolean isActive = false;
                	if (msg.arg1 == 1) {
                		isActive = true;
                	}
                	
                	if (msg.what == 7) {
                		if (FrameworkContext.INFO) Log.i(TAG, "Training: " + isActive);
                		service.trainingIntent.putExtra("training", isActive);
                    	service.sendBroadcast(service.trainingIntent);
                	} else if (msg.what == 8) {
                		if (FrameworkContext.INFO) Log.i(TAG, "Live: " + isActive);
                		service.liveIntent.putExtra("live", isActive);
                    	service.sendBroadcast(service.liveIntent);
                	} else if (msg.what == 9) {
                		if (FrameworkContext.INFO) Log.i(TAG, "Data: " + isActive);
                		service.dataIntent.putExtra("data", isActive);
                    	service.sendBroadcast(service.dataIntent);
                	}
//                } else if (service.mContextLabels.length >= msg.what) {
//                	FrameworkConfiguration.getInstance().getContextLabelArray();
//                	if (FrameworkContext.INFO) Log.i(TAG, "Context label: " + service.mContextLabels[msg.what-1]);
//                	service.labelIntent.putExtra("contextLabel", service.mContextLabels[msg.what-1]);
//                	service.sendBroadcast(service.labelIntent);
//                }
                } else if (msg.what > 0 && msg.what <= 6) {
                	int index = msg.what-1;
            		if (index < FrameworkConfiguration.getInstance().getContextLabels().size()) {
            			String contextLabel = FrameworkConfiguration.getInstance().getContextLabels().get(index);
            			if (FrameworkContext.INFO) Log.i(TAG, "Context label: " + contextLabel);
            			service.labelIntent.putExtra("contextLabel", contextLabel);
                    	service.sendBroadcast(service.labelIntent);
            		}
                }
           }
        }
    }

    /**
     * Target we publish for clients to send messages to IncomingHandler.
     */
    final Messenger mMessenger = new Messenger(new IncomingHandler(this));

    /**
     * When binding to the service, we return an interface to our messenger
     * for sending messages to the service.
     */
    @Override
    public IBinder onBind(Intent intent) {
        Toast.makeText(getApplicationContext(), "binding", Toast.LENGTH_SHORT).show();
        return mMessenger.getBinder();
    }
    
    @Override
    public void onCreate() {
    	super.onCreate();
    	mContextLabels = FrameworkConfiguration.getInstance().getContextLabelArray();
    	labelIntent = new Intent(BROADCAST_ACTION_CONTEXT_LABEL_CHANGE);
    	trainingIntent = new Intent(BROADCAST_ACTION_CONTEXT_TRAINING);
    	liveIntent = new Intent(BROADCAST_ACTION_CONTEXT_LIVE);
    	dataIntent = new Intent(BROADCAST_ACTION_CONTEXT_DATA);
    }
    
    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        Toast.makeText(this, "service starting", Toast.LENGTH_SHORT).show();
        // If we get killed, after returning from here, restart
        return START_STICKY;
    }
    
    @Override
    public void onDestroy() {
      Toast.makeText(this, "service done", Toast.LENGTH_SHORT).show(); 
    }
}
