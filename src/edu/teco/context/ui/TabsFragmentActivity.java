/**
 * 
 */
package edu.teco.context.ui;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import android.app.AlertDialog;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.Bundle;
import android.os.PowerManager;
import android.os.PowerManager.WakeLock;
import android.support.v4.app.DialogFragment;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentActivity;
import android.support.v4.app.FragmentTransaction;
import android.util.Log;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.EditText;
import android.widget.Spinner;
import android.widget.TabHost;
import android.widget.TabHost.OnTabChangeListener;
import android.widget.TabHost.TabContentFactory;
import android.widget.TextView;
import android.widget.Toast;
import edu.teco.context.IContextFramework;
import edu.teco.context.FrameworkManager;
import edu.teco.context.FrameworkManager.ContextListener;
import edu.teco.context.R;
import edu.teco.context.configuration.FrameworkConfiguration;
import edu.teco.context.configuration.FrameworkContext;
import edu.teco.context.configuration.FrameworkKeys.IBroadcastActions;
import edu.teco.context.configuration.FrameworkState;
import edu.teco.context.smartwatch.SmartWatchMessengerService;
import edu.teco.context.ui.FileChooserDialogFragment.FileChooserDialogListener;
import edu.teco.context.util.DataConnectionManager;

/**
 * @author Sven Frauen
 * 
 */
public class TabsFragmentActivity extends FragmentActivity implements OnTabChangeListener, ContextListener,
		FileChooserDialogListener, IBroadcastActions {

	/** Tag string for debug logs. */
	private static final String TAG = "TabsFragmentActivity";
	
	/** In order to keep CPU running while screen turns black.
	 * @see <a href="https://github.com/jamesonwilliams/AndroidPersistentSensors">
	 https://github.com/jamesonwilliams/AndroidPersistentSensors</a>
	 * (also for how to implement a service)
	 */
	private WakeLock mWakeLock﻿ = null;
	
	private static String mCurrentContextLabel = null;
	
	private Intent mSmartWatchServiceIntent = null;
	

	
	private BroadcastReceiver mBroadcastReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
        	
        	String action = intent.getAction();
        	if (action.equals(SmartWatchMessengerService.BROADCAST_ACTION_CONTEXT_LABEL_CHANGE)) {
            	
        		String contextLabel = intent.getStringExtra("contextLabel");
            	
            	if (contextLabel != null) {
            		if (FrameworkContext.INFO) Log.i(TAG, "Context label from SmartWatch: " + contextLabel + " received.");
                	setCurrentContextLabel(contextLabel);
            	}
        	} else if (action.equals(SmartWatchMessengerService.BROADCAST_ACTION_CONTEXT_TRAINING)) {
        		toggleTraining(null);
        		
        	} else if (action.equals(SmartWatchMessengerService.BROADCAST_ACTION_CONTEXT_LIVE)) {
        		toggleLiveClassification(null);
        		
        	} else if (action.equals(SmartWatchMessengerService.BROADCAST_ACTION_CONTEXT_DATA)) {
        		toggleDataConnection();

        	}
        }
	};

	// ******************************************************************** //
	// Activity Handling
	// ******************************************************************** //

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.tabs_layout);
		initialiseTabHost(savedInstanceState);
		if (savedInstanceState != null) {
			// set the tab as per the saved state
			mTabHost.setCurrentTabByTag(savedInstanceState.getString("tab"));
		}
		
		framework = new FrameworkManager(getApplicationContext());

		setTitle("FrameworkControl state: " + framework.getCurrentState());

		PowerManager manager = (PowerManager) getSystemService(Context.POWER_SERVICE);
	    mWakeLock﻿ = manager.newWakeLock(PowerManager.PARTIAL_WAKE_LOCK, TAG);
	    mWakeLock﻿.acquire();
	    
	    if (FrameworkConfiguration.getInstance().getContextLabels().size() > 0) {
	    	String contextLabel = FrameworkConfiguration.getInstance().getContextLabels().get(0);
		    setCurrentContextLabel(contextLabel);
	    }
	    
	    // start SmartWatchMessengerService as an interface for the Sony SmartWatch
	    mSmartWatchServiceIntent = new Intent(this, SmartWatchMessengerService.class);
	    startService(mSmartWatchServiceIntent);
	    
	    IntentFilter filter = new IntentFilter(SmartWatchMessengerService.BROADCAST_ACTION_CONTEXT_LABEL_CHANGE);
	    filter.addAction(SmartWatchMessengerService.BROADCAST_ACTION_CONTEXT_TRAINING);
	    filter.addAction(SmartWatchMessengerService.BROADCAST_ACTION_CONTEXT_LIVE);
	    filter.addAction(SmartWatchMessengerService.BROADCAST_ACTION_CONTEXT_DATA);
	    
		registerReceiver(mBroadcastReceiver, filter);
		

	}

	@Override
	protected void onResume() {
		super.onResume();
		// framework.resume();
	}

	@Override
	protected void onPause() {
		super.onPause();
		// framework.pause();
	}
	
	@Override
	protected void onDestroy() {
		framework.destroyFramework();
		mWakeLock﻿.release();
		unregisterReceiver(mBroadcastReceiver);
		stopService(mSmartWatchServiceIntent);
		super.onDestroy();
	};

	// ******************************************************************** //
	// ContextFramework Handling
	// ******************************************************************** //

	IContextFramework framework = null;

	/** Called when the user clicks buttons */
	
	public void editContextLabelsClicked(View v) {
		if (framework.getCurrentState() == FrameworkState.INITIAL || framework.getCurrentState() == FrameworkState.INITIALIZED) {
			AlertDialog.Builder alert = new AlertDialog.Builder(this);

			alert.setTitle("Change Context Labels");
			alert.setMessage("Insert comma (,) seperated list.");

			StringBuilder sb = new StringBuilder();
			for (String contextLabel : FrameworkConfiguration.getInstance().getContextLabels()) {
				sb.append(contextLabel).append(",");
			}
			if (sb.length() > 0) {
				sb.deleteCharAt(sb.length()-1);
			}
			
			
			// Set an EditText view to get user input 
			final EditText input = new EditText(this);
			input.setText(sb.toString());
			alert.setView(input);

			alert.setPositiveButton("Ok", new DialogInterface.OnClickListener() {
				public void onClick(DialogInterface dialog, int whichButton) {
					
					String value = input.getText().toString();
					String[] contextLabels = value.split(",");
					if (FrameworkContext.INFO) Log.i(TAG, "New context labels from user input: " + Arrays.toString(contextLabels));
					try {
						FrameworkConfiguration.getInstance().setContextLabels(Arrays.asList(contextLabels));
						changeContextLabels();
					} catch (Exception e) {
						Toast.makeText(getApplicationContext(), e.getMessage(), Toast.LENGTH_LONG).show();
					}
				}
			});

			alert.setNegativeButton("Cancel", new DialogInterface.OnClickListener() {
				public void onClick(DialogInterface dialog, int whichButton) {
			    // Canceled.
			  }
			});

			alert.show();
		} else {
			Toast.makeText(getApplicationContext(), "Could not change labels as framework is already configured." +
					" Reset Framework first.", Toast.LENGTH_SHORT).show();
		}
	}

	public void resetFrameworkClicked(View v) {
		boolean stateChanged = framework.resetFramework(getApplicationContext());
		if (!stateChanged) {
			Toast.makeText(getApplicationContext(),
					"Could not reset framework from state " + framework.getCurrentState().toString(),
					Toast.LENGTH_SHORT).show();
		} else {
			Toast.makeText(getApplicationContext(), "Framework reset sucess.", Toast.LENGTH_SHORT).show();
		}
	}

	public void configureFrameworkClicked(View v) {
		FrameworkConfiguration conf = FrameworkConfiguration.getInstance();

		conf.setConfigurationName("Context Framework");
		// TODO: Checkbox for this
		framework.setLogAll(true);
		framework.addContextListener(this);

		boolean stateChanged = framework.configure(conf);
		if (!stateChanged) {
			Toast.makeText(getApplicationContext(),
					"Could not change sptate from state " + framework.getCurrentState().toString(), Toast.LENGTH_SHORT)
					.show();
		} else {
			Toast.makeText(getApplicationContext(), "Configured: " + 
		FrameworkConfiguration.getInstance().toString(), Toast.LENGTH_LONG).show();
		}
	}

	public void trainFromFile(View view) {
		// file on SDCard
		DialogFragment newFragment = new FileChooserDialogFragment();
		Bundle args = new Bundle();
		args.putString("fileType", ".arff");
		args.putString("path", FrameworkConfiguration.getInstance().getArffDirectory());
		newFragment.setArguments(args);
		((FileChooserDialogFragment) newFragment).addListener(this);
		
		newFragment.show(getSupportFragmentManager(), "fileDialog");
	}

	public void onFileSelected(String file) {
		if (FrameworkContext.INFO) Log.i(TAG, "Selected File: " + file);

		FrameworkConfiguration conf = FrameworkConfiguration.getInstance();
		conf.setConfigurationName("Context Framework");
		// TODO: Checkbox for this
		framework.setLogAll(true);
		framework.addContextListener(this);

		// file on SDCard
		boolean stateChanged = framework.configureAndTrainWithARFF(file);
		if (!stateChanged) {
			Toast.makeText(getApplicationContext(),
					"Could not change state from state " + framework.getCurrentState().toString(), Toast.LENGTH_SHORT)
					.show();
		} else {
			changeContextLabels();
		}

	}

	public void toggleRecording(View view) {
		Button recordButton = (Button) view;

		FrameworkState state = framework.getCurrentState();
		boolean stateChanged = false;

		if (state == FrameworkState.LOGGING) {
			stateChanged = framework.stopLogging();
			if (stateChanged) {
				recordButton.setText(R.string.button_start_recording);
			}
		} else {
			stateChanged = framework.startLogging();
			if (stateChanged) {
				recordButton.setText(R.string.button_stop_recording);
			}
		}
		if (!stateChanged) {
			Toast.makeText(getApplicationContext(), "Could not change state frome state " + state.toString(),
					Toast.LENGTH_SHORT).show();
		}
	}

	public void toggleTraining(View view) {
		
		Button trainingButton = (Button) findViewById(R.id.buttonTrain);
//		Button startButton = (Button) view;

		FrameworkState state = framework.getCurrentState();
		boolean stateChanged = false;

		if (state == FrameworkState.TRAINING) {
			stateChanged = framework.stopTrainingRecording();
			if (stateChanged) {
				if (trainingButton != null) {
					trainingButton.setText(R.string.button_start_recording);
				}
			}
		} else {
			stateChanged = framework.startTrainingRecording();
			if (stateChanged) {
				if (trainingButton != null) {
					trainingButton.setText(R.string.button_stop_recording);
				}
			}
		}
		if (!stateChanged) {
			Toast.makeText(getApplicationContext(), "Could not change state from state " + state.toString(),
					Toast.LENGTH_SHORT).show();
		} else {
			broadcastFrameworkState();
		}
	}

	public void trainClassifier(View view) {
		boolean stateChanged = false;
		stateChanged = framework.trainWithRecordedData();
		if (!stateChanged) {
			Toast.makeText(getApplicationContext(),
					"Could not change state from state " + framework.getCurrentState().toString(), Toast.LENGTH_SHORT)
					.show();
		}
	}

	public void toggleTesting(View view) {
		Button trainButton = (Button) view;

		FrameworkState state = framework.getCurrentState();
		boolean stateChanged = false;

		if (state == FrameworkState.EVALUATING) {
			stateChanged = framework.stopEvaluationRecording();
			if (stateChanged) {
				trainButton.setText(R.string.button_start_recording);
			}
		} else {
			stateChanged = framework.startEvaluationRecording();
			if (stateChanged) {
				trainButton.setText(R.string.button_stop_recording);
			}
		}

		if (!stateChanged) {
			Toast.makeText(getApplicationContext(), "Could not change state from state " + state.toString(),
					Toast.LENGTH_SHORT).show();
		}
	}

	public void evaluateWeka(View view) {
		boolean stateChanged = framework.testWithRecordedData();
		if (!stateChanged) {
			Toast.makeText(getApplicationContext(),
					"Could not change state from state " + framework.getCurrentState().toString(), Toast.LENGTH_SHORT)
					.show();
		}
	}

	public void toggleLiveClassification(View view) {
//		Button classifyButton = (Button) view;
		
		Button classificationButton = (Button) findViewById(R.id.buttonLiveClassification);

		FrameworkState state = framework.getCurrentState();
		boolean stateChanged = false;

		if (state == FrameworkState.CLASSIFYING) {
			stateChanged = framework.stopLiveClassification();
			if (stateChanged) {
				if (classificationButton != null) {
					classificationButton.setText(R.string.button_start_classifying);
				}
			}
		} else {
			stateChanged = framework.startLiveClassification();
			if (stateChanged) {
				if (classificationButton != null) {
					classificationButton.setText(R.string.button_stop_classifying);
				}
			}
		}

		if (!stateChanged) {
			Toast.makeText(getApplicationContext(), "Could not change state from state " + state.toString(),
					Toast.LENGTH_SHORT).show();
		} else {
			broadcastFrameworkState();
		}
	}

	public void resetRecordedData(View view) {
		boolean stateChanged = framework.resetRecordedData();
		if (!stateChanged) {
			Toast.makeText(getApplicationContext(),
					"Could not reset data from state " + framework.getCurrentState().toString(), Toast.LENGTH_SHORT)
					.show();
		}
	}
	
	public void getPrediction(View view) {
		Map<String, Double> prediction = framework.getParameterPrediction();
		Toast.makeText(getApplicationContext(), "Prediction result: " + prediction.toString(), Toast.LENGTH_LONG).show();
	}
	

	
	public void onCheckboxDataConnectionClicked(View view) {
		toggleDataConnection();
	}
	
	public void onCheckboxGpsClicked(View view) {
		boolean isGpsEnabled = ((CheckBox) view).isChecked();
	    framework.toggleGpsLogging(isGpsEnabled);
	}
	
	private int dataConnection = 2;
	
	public void onChangeColorClicked(View view) {
		
		if (dataConnection == 2) {
			dataConnection = 0;
		} else {
			dataConnection++;
		}
		
		Intent smartWatchIntent = new Intent(BROADCAST_ACTION_CONNECTION);
	    smartWatchIntent.putExtra("dataConnection", dataConnection);
    	sendBroadcast(smartWatchIntent);
	}
	
	// ******************************************************************** //
	// ContextListener methods
	// ******************************************************************** //

	public void onContextLabelCalculated(String contextLabel) {
		TextView textView = (TextView) findViewById(R.id.textViewClassification);
		if (textView != null) {
			textView.setText(contextLabel);
		}
	}

	public void onTestCalculated(String result) {
		TextView v = (TextView) findViewById(R.id.textViewTestResult);
		if (v != null) {
			v.setText(result);
		}
	}

	public void onStateChanged(FrameworkState state) {
		setTitle("FrameworkControl state: " + state.toString());
	}
	
	private void setCurrentContextLabel(String contextLabel) {
		
		if (mCurrentContextLabel == null || !mCurrentContextLabel.equals(contextLabel)) {
			
			if (FrameworkConfiguration.getInstance().getContextLabels().contains(contextLabel)) {
				mCurrentContextLabel = contextLabel;
		    	framework.setCurrentContextLabel(contextLabel);
		    	if (FrameworkContext.INFO) Log.i(TAG, "Changed new context label: " + contextLabel);
		    	
		    	int contextLabelIndex = FrameworkConfiguration.getInstance().getContextLabels().indexOf(contextLabel);
				Spinner spinner = (Spinner) findViewById(R.id.spinner_activities_training);
				if (spinner != null) {
		    		spinner.setSelection(contextLabelIndex);
				}
				
				spinner = (Spinner) findViewById(R.id.spinner_activities_evaluation);
				if (spinner != null) {
					spinner.setSelection(contextLabelIndex);
				}
				
				Intent smartWatchIntent = new Intent(BROADCAST_ACTION_CCONFIGURATION_CONTEXT_LABEL_SELECTED);
			    smartWatchIntent.putExtra("contextLabel", contextLabel);
		    	sendBroadcast(smartWatchIntent);
				
			} else {
				if (FrameworkContext.INFO) Log.i(TAG, "This context label " + contextLabel + " does not exist in the FrameworkConfiguration.");
			}
		} else {
			if (FrameworkContext.INFO) Log.i(TAG, "New context label " + contextLabel + " received but was already set.");
		}
	}
	
	private void changeContextLabels() {
		
		List<String> contextLabels = new ArrayList<String>(FrameworkConfiguration.getInstance().getContextLabels());
		if (FrameworkContext.INFO) Log.i(TAG, "New context labels: " + contextLabels.toString());
		
		Spinner spinner = (Spinner) findViewById(R.id.spinner_activities_training);
		if (spinner != null) {
			@SuppressWarnings("unchecked")
			ArrayAdapter<String> adapter = (ArrayAdapter<String>) spinner.getAdapter();
    		if (adapter != null) {
    			adapter.clear();
    			for (String label : contextLabels) {
					adapter.add(label);
				}
    			adapter.notifyDataSetChanged();
    		}
		}
		
		spinner = (Spinner) findViewById(R.id.spinner_activities_evaluation);
		if (spinner != null) {
			@SuppressWarnings("unchecked")
			ArrayAdapter<String> adapter = (ArrayAdapter<String>) spinner.getAdapter();
    		if (adapter != null) {
    			adapter.clear();
    			for (String label : contextLabels) {
					adapter.add(label);
				}
    			adapter.notifyDataSetChanged();
    		}
		}
		
		String contextLabel = null;
		if (contextLabels.size() > 0) {
			contextLabel = contextLabels.get(0);
		} else {
			if (FrameworkContext.WARN) Log.w(TAG, "No context labels are set!");
		}
	    setCurrentContextLabel(contextLabel);
		
	    Intent smartWatchIntent = new Intent(BROADCAST_ACTION_CONFIGURATION_CONTEXT_LABELS);
	    smartWatchIntent.putStringArrayListExtra("contextLabels", (ArrayList<String>) contextLabels);
    	sendBroadcast(smartWatchIntent);

	}
	
	public static String getCurrentContextLabel() {
		return mCurrentContextLabel;
	}
	
	private void toggleDataConnection() {
		boolean shouldDataConBeEnabled = !DataConnectionManager.isDataConectionEnabled(getApplicationContext());
		
		if (FrameworkContext.INFO) Log.i(TAG, "Data connection state should be set to : " + !shouldDataConBeEnabled);
		
		DataConnectionManager.toggleDataConnection(getApplicationContext(), shouldDataConBeEnabled);
		
		CheckBox gpsCheckbox = (CheckBox) findViewById(R.id.checkBoxDataConnection);
		if (gpsCheckbox != null) {
			gpsCheckbox.setChecked(shouldDataConBeEnabled);
		}
		
		Intent smartWatchIntent = new Intent(BROADCAST_ACTION_CONFIGURATION_DATA);
	    smartWatchIntent.putExtra("isDataActive", shouldDataConBeEnabled);
    	sendBroadcast(smartWatchIntent);
	}
	
	private void broadcastFrameworkState() {
		Intent smartWatchIntent = new Intent(BROADCAST_ACTION_CONFIGURATION_STATE);
		
		FrameworkState state = framework.getCurrentState();
		boolean isTrainingActive = false;
		boolean isLiveActive = false;
		
		if (state == FrameworkState.TRAINING) isTrainingActive = true;
		if (state == FrameworkState.CLASSIFYING) isLiveActive = true;
		
	    smartWatchIntent.putExtra("isTrainingActive", isTrainingActive);
	    smartWatchIntent.putExtra("isLiveActive", isLiveActive);
    	sendBroadcast(smartWatchIntent);
	}

	// ******************************************************************** //
	// Tab Handling
	// see this example:
	// http://thepseudocoder.wordpress.com/2011/10/04/android-tabs-the-fragment-way/
	// ******************************************************************** //

	private TabHost mTabHost;
	private HashMap<String, TabInfo> mapTabInfo = new HashMap<String, TabsFragmentActivity.TabInfo>();
	private TabInfo mLastTab = null;

	private class TabInfo {
		private String tag;
		private Class<?> clss;
		private Bundle args;
		private Fragment fragment;

		TabInfo(String tag, Class<?> clazz, Bundle args) {
			this.tag = tag;
			this.clss = clazz;
			this.args = args;
		}

	}

	class TabFactory implements TabContentFactory {

		private final Context mContext;

		public TabFactory(Context context) {
			mContext = context;
		}

		public View createTabContent(String tag) {
			View v = new View(mContext);
			v.setMinimumWidth(0);
			v.setMinimumHeight(0);
			return v;
		}
	}

	@Override
	protected void onSaveInstanceState(Bundle outState) {
		outState.putString("tab", mTabHost.getCurrentTabTag()); // save the tab
																// selected
		super.onSaveInstanceState(outState);
	}

	private void initialiseTabHost(Bundle args) {
		mTabHost = (TabHost) findViewById(android.R.id.tabhost);
		mTabHost.setup();
		TabInfo tabInfo = null;
		TabsFragmentActivity.addTab(this, this.mTabHost, this.mTabHost.newTabSpec("Tab1").setIndicator("Config"),
				(tabInfo = new TabInfo("Tab1", Tab1Fragment.class, args)));
		this.mapTabInfo.put(tabInfo.tag, tabInfo);
		TabsFragmentActivity.addTab(this, this.mTabHost, this.mTabHost.newTabSpec("Tab2").setIndicator("Train"),
				(tabInfo = new TabInfo("Tab2", Tab2Fragment.class, args)));
		this.mapTabInfo.put(tabInfo.tag, tabInfo);
		TabsFragmentActivity.addTab(this, this.mTabHost, this.mTabHost.newTabSpec("Tab3").setIndicator("Live"),
				(tabInfo = new TabInfo("Tab3", Tab3Fragment.class, args)));
		this.mapTabInfo.put(tabInfo.tag, tabInfo);
		TabsFragmentActivity.addTab(this, this.mTabHost, this.mTabHost.newTabSpec("Tab4").setIndicator("Evaluate"),
				(tabInfo = new TabInfo("Tab4", Tab4Fragment.class, args)));
		this.mapTabInfo.put(tabInfo.tag, tabInfo);
		this.mapTabInfo.put(tabInfo.tag, tabInfo);
		// Default to first tab
		this.onTabChanged("Tab1");
		//
		mTabHost.setOnTabChangedListener(this);
	}

	private static void addTab(TabsFragmentActivity activity, TabHost tabHost, TabHost.TabSpec tabSpec, TabInfo tabInfo) {
		// Attach a Tab view factory to the spec
		tabSpec.setContent(activity.new TabFactory(activity));
		String tag = tabSpec.getTag();

		// Check to see if we already have a fragment for this tab, probably
		// from a previously saved state. If so, deactivate it, because our
		// initial state is that a tab isn't shown.
		tabInfo.fragment = activity.getSupportFragmentManager().findFragmentByTag(tag);
		if (tabInfo.fragment != null && !tabInfo.fragment.isDetached()) {
			FragmentTransaction ft = activity.getSupportFragmentManager().beginTransaction();
			ft.detach(tabInfo.fragment);
			ft.commit();
			activity.getSupportFragmentManager().executePendingTransactions();
		}

		tabHost.addTab(tabSpec);
	}

	public void onTabChanged(String tag) {
		TabInfo newTab = this.mapTabInfo.get(tag);
		if (mLastTab != newTab) {
			FragmentTransaction ft = this.getSupportFragmentManager().beginTransaction();
			if (mLastTab != null) {
				if (mLastTab.fragment != null) {
					ft.detach(mLastTab.fragment);
				}
			}
			if (newTab != null) {
				if (newTab.fragment == null) {
					newTab.fragment = Fragment.instantiate(this, newTab.clss.getName(), newTab.args);
					ft.add(R.id.realtabcontent, newTab.fragment, newTab.tag);
				} else {
					ft.attach(newTab.fragment);
				}
			}

			mLastTab = newTab;
			ft.commit();
			this.getSupportFragmentManager().executePendingTransactions();
		}
	}
	
	// ******************************************************************** //
	// IFragmentControlsListener methods
	// ******************************************************************** //

	public void onContextLabelSelected(View view, String contextLabel) {
		setCurrentContextLabel(contextLabel);
	}


}
