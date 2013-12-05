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

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemSelectedListener;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.RelativeLayout;
import android.widget.Spinner;
import android.widget.Toast;
import edu.teco.context.R;
import edu.teco.context.configuration.FrameworkConfiguration;
import edu.teco.context.configuration.FrameworkContext;
import edu.teco.context.configuration.FrameworkKeys.IFeatureKeys;
import edu.teco.context.util.DataConnectionManager;

public class Tab1Fragment extends Fragment implements OnItemSelectedListener {
	
	/** Tag string for debug logs. */
	private static final String TAG = "Tab1Fragment";
	
	private String mProbeKey = null; 
	
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
		
		View v = inflater.inflate(R.layout.tab_frag1_layout, container, false);
		
		List<String> probeNames = FrameworkConfiguration.getInstance().getSupportedProbeNames();
	
		final Spinner spinner = (Spinner) v.findViewById(R.id.spinner_sensors);
		// Create an ArrayAdapter using the string array and a default spinner
		// layout
		ArrayAdapter<String> adapter = new ArrayAdapter<String>(this.getActivity(),
				android.R.layout.simple_spinner_item, probeNames);
		// Specify the layout to use when the list of choices appears
		adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
		// Apply the adapter to the spinner
		spinner.setAdapter(adapter);
		// Sets the listener for the spinner to this activity.
		spinner.setOnItemSelectedListener(this);
		if (probeNames.size() > 0) {
			mProbeKey = probeNames.get(0);
		}
		
		final Button addButton = (Button) v.findViewById(R.id.buttonAddProbe);
		addButton.setOnClickListener(new View.OnClickListener() {
		    public void onClick(View v) {
		    	addSensorFeatureCombination(v);
		    }
		});
		
		return v;
	}
	
	@Override
	public void onResume() {
		super.onResume();
		
		boolean isDataConnectionEnabled = DataConnectionManager.isDataConectionEnabled(getActivity().getApplicationContext());
		if (FrameworkContext.INFO) Log.i(TAG,"Fragment returned and Data Connection enabled is : " + isDataConnectionEnabled);
		CheckBox gpsCheckbox = (CheckBox) getActivity().findViewById(R.id.checkBoxDataConnection);
		if (gpsCheckbox != null) {
			gpsCheckbox.setChecked(isDataConnectionEnabled);
		}
	}
	

	public void onItemSelected(AdapterView<?> parent, View view, int pos, long id) {
		mProbeKey = FrameworkConfiguration.getInstance().getSupportedProbeNames().get(pos);
	}

	public void onNothingSelected(AdapterView<?> arg0) {
		// nothing to do here
	}
	
	private void addSensorFeatureCombination(View v) {
		
    	if (mProbeKey != null) {
    		
    		FrameworkConfiguration conf = FrameworkConfiguration.getInstance();
    		
    		List<String> features = new ArrayList<String>();
    		
    		if (((CheckBox) getView().findViewById(R.id.checkBoxFeatureMean)).isChecked()) {
    			features.add(IFeatureKeys.MEAN);
    		}
    		if (((CheckBox) getView().findViewById(R.id.checkBoxFeatureMedian)).isChecked()) {
    			features.add(IFeatureKeys.MEDIAN);
    		}
    		if (((CheckBox) getView().findViewById(R.id.checkBoxFeatureVariance)).isChecked()) {
    			features.add(IFeatureKeys.VARIANCE);
    		}
    		if (((CheckBox) getView().findViewById(R.id.checkBoxFeatureStandardDeviation)).isChecked()) {
    			features.add(IFeatureKeys.STANDARD_DEVIATION);
    		}
    		if (((CheckBox) getView().findViewById(R.id.checkBoxFeatureDifferenceMaxMin)).isChecked()) {
    			features.add(IFeatureKeys.DIFFERENCE_MAX_MIN);
    		}
    		if (((CheckBox) getView().findViewById(R.id.checkBoxFeatureFrequencyPeak)).isChecked()) {
    			features.add(IFeatureKeys.FREQUENCY_PEAK);
    		}
    		if (((CheckBox) getView().findViewById(R.id.checkBoxFeatureEntropy)).isChecked()) {
    			features.add(IFeatureKeys.ENTROPY);
    		}
    		if (((CheckBox) getView().findViewById(R.id.checkBoxFeatureFrequencyEntropy)).isChecked()) {
    			features.add(IFeatureKeys.FREQUENCY_DOMAIN_ENTROPY);
    		}
    		
    		String[] featureArray = new String[features.size()];
    		conf.addSensorFeaturesCombination(mProbeKey, features.toArray(featureArray));
    		Toast.makeText(getActivity().getApplicationContext(), "Added " + mProbeKey 
    				+ " with features " + Arrays.toString(featureArray) , Toast.LENGTH_SHORT).show();
    	}
	}

}
