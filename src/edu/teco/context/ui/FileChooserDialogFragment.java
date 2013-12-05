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

import java.io.File;
import java.io.FilenameFilter;

import android.app.AlertDialog;
import android.app.AlertDialog.Builder;
import android.app.Dialog;
import android.content.DialogInterface;
import android.os.Bundle;
import android.os.Environment;
import android.support.v4.app.DialogFragment;
import android.util.Log;
import edu.teco.context.configuration.FrameworkConfiguration;
import edu.teco.context.configuration.FrameworkContext;

/**
 * 
 * @author Sven Frauen
 * 
 * for more information see:
 * http://developer.android.com/guide/topics/ui/dialogs.html
 * http://stackoverflow.com/questions/3592717/choose-file-dialog
 */
public class FileChooserDialogFragment extends DialogFragment {

	/** Tag string for debug logs. */
	private static final String TAG = "FileChooserDialogFragment";
	
	public interface FileChooserDialogListener {
        public void onFileSelected(String file);
    }
    
    // Use this instance of the interface to deliver action events
	private FileChooserDialogListener mListener;
	
	// for handling files
	private String[] mFileList;
	private File mPath = null;
	private String mChosenFile;
	private String mFileType = null;
	
	public void addListener(FileChooserDialogListener listener) {
		mListener = listener;
	}
	
	public void removeListener(FileChooserDialogListener listener) {
		mListener = null;
	}

	private void loadFileList() {
		try {
			mPath.mkdirs();
		} catch (SecurityException e) {
			if (FrameworkContext.ERROR) Log.e(TAG, "unable to write on the sd card " + e.toString());
		}
		if (mPath.exists()) {
			FilenameFilter filter = new FilenameFilter() {
				public boolean accept(File dir, String filename) {
					File sel = new File(dir, filename);
					return filename.contains(mFileType) || sel.isDirectory();
				}
			};
			mFileList = mPath.list(filter);
		} else {
			mFileList = new String[0];
		}
	}

	@Override
	public Dialog onCreateDialog(Bundle savedInstanceState) {
		
		String path = getArguments().getString("path");
		String fileType = getArguments().getString("fileType");
		
		if (path == null || fileType == null) {
			mFileType = ".arff";
			String arffDirectory = FrameworkConfiguration.getInstance().getArffDirectory();
			mPath = new File(Environment.getExternalStorageDirectory() + arffDirectory);
		} else {
			mFileType = fileType;
			mPath = new File(Environment.getExternalStorageDirectory() + path);
		}
		
		loadFileList();
		
		AlertDialog.Builder builder = new Builder(getActivity());

		builder.setTitle("Choose your file");
		
		builder.setNegativeButton("Cancel", new DialogInterface.OnClickListener() {
			public void onClick(DialogInterface dialog, int which) {
				if (FrameworkContext.INFO) Log.i(TAG, "User canceled dialog.");
			}
		});
		
		if (mFileList == null) {
			if (FrameworkContext.ERROR) Log.e(TAG, "Showing file picker before loading the file list");
			return builder.create();
		}
		builder.setItems(mFileList, new DialogInterface.OnClickListener() {
			public void onClick(DialogInterface dialog, int which) {
				mChosenFile = mFileList[which];
				mListener.onFileSelected(mChosenFile);
			}
		});

		return builder.create();
	}

}
