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
package edu.teco.context.ui;

import android.app.Activity;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.View.OnClickListener;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.RelativeLayout;
import edu.teco.context.R;
import edu.teco.context.configuration.FrameworkContext;
import edu.teco.context.configuration.FrameworkState;

public class Tab3Fragment extends Fragment {
	
	/** Tag string for debug logs. */
	private static final String TAG = "Tab3Fragment";
	
	private IFragmentControlsListener mFragmentControlsListener;	
	
	@Override
	public View onCreateView(LayoutInflater inflater, ViewGroup container,
			Bundle savedInstanceState) {
		if (container == null) {
            // We have different layouts, and in one of them this
            // fragment's containing frame doesn't exist.  The fragment
            // may still be created from its saved state, but there is
            // no reason to try to create its view hierarchy because it
            // won't be displayed.  Note this is not needed -- we could
            // just run the code below, where we would create and return
            // the view hierarchy; it would just never be used.
            return null;
        }
		
		View v = inflater.inflate(R.layout.tab_frag3_layout, container, false);
		
		return v;
	}
	
	@Override
	public void onAttach(Activity activity) {
		super.onAttach(activity);
		if (FrameworkContext.DEBUG) Log.d(TAG, "tab3 attached");
		try {
			mFragmentControlsListener = (IFragmentControlsListener) activity;
	    } catch (ClassCastException e) {
	    	if (FrameworkContext.WARN) Log.w(TAG, (activity.toString() + " must implement IFragmentControlsListener"));
	    }
	}
	
	@Override
	public void onResume() {
		super.onResume();
		Button liveClassificationButton = (Button) getActivity().findViewById(R.id.buttonLiveClassification);
		if (liveClassificationButton != null) {
			FrameworkState state = ((TabsFragmentActivity) getActivity()).framework.getCurrentState();
			if (state == FrameworkState.CLASSIFYING) {
				liveClassificationButton.setText(R.string.button_stop_classifying);
			} else {
				liveClassificationButton.setText(R.string.button_start_classifying);
			}
		}
	}

}
