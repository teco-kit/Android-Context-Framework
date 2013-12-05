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
package edu.teco.context.util;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;

import android.content.Context;
import android.telephony.TelephonyManager;

/**
 * Class to turn on and off the Data Connection (UMTS, EDGE...).
 * 
 * see:
 * http://stackoverflow.com/questions/3644144/how-to-disable-mobile-data-on-android
 * http://stackoverflow.com/questions/5416585/why-2-3-version-of-android-does-not-hava-android-permission-modify-phone-state
 * http://stackoverflow.com/questions/4715250/how-to-grant-modify-phone-state-permission-for-apps-ran-on-gingerbread
 * 
 * @author Sven Frauen
 *
 */
public class DataConnectionManager {
	

    
    public static void toggleDataConnection(Context context, boolean switchOn) {
    	
    	Method dataConnSwitchmethod;
        Class<?> telephonyManagerClass;
        Object ITelephonyStub;
        Class<?> ITelephonyClass;
        
        TelephonyManager telephonyManager = (TelephonyManager) context
                .getSystemService(Context.TELEPHONY_SERVICE);
        
        boolean isEnabled = DataConnectionManager.isDataConectionEnabled(context);
        
        if (switchOn == isEnabled) return;
        
        try {
			telephonyManagerClass = Class.forName(telephonyManager.getClass().getName());
	        Method getITelephonyMethod = telephonyManagerClass.getDeclaredMethod("getITelephony");
	        getITelephonyMethod.setAccessible(true);
	        ITelephonyStub = getITelephonyMethod.invoke(telephonyManager);
	        ITelephonyClass = Class.forName(ITelephonyStub.getClass().getName());

	        if (isEnabled) {
	            dataConnSwitchmethod = ITelephonyClass
	                    .getDeclaredMethod("disableDataConnectivity");
	        } else {
	            dataConnSwitchmethod = ITelephonyClass
	                    .getDeclaredMethod("enableDataConnectivity");   
	        }
	        dataConnSwitchmethod.setAccessible(true);
	        dataConnSwitchmethod.invoke(ITelephonyStub);
		} catch (ClassNotFoundException e) {
			e.printStackTrace();
		} catch (SecurityException e) {
			e.printStackTrace();
		} catch (NoSuchMethodException e) {
			e.printStackTrace();
		} catch (IllegalArgumentException e) {
			e.printStackTrace();
		} catch (IllegalAccessException e) {
			e.printStackTrace();
		} catch (InvocationTargetException e) {
			e.printStackTrace();
		}

    }
    
    public static boolean isDataConectionEnabled(Context context) {
        boolean isEnabled;
    	
        TelephonyManager telephonyManager = (TelephonyManager) context
                .getSystemService(Context.TELEPHONY_SERVICE);

        if(telephonyManager.getDataState() == TelephonyManager.DATA_CONNECTED){
            isEnabled = true;
        }else{
            isEnabled = false;  
        }
        
        return isEnabled;
    }

 



}
