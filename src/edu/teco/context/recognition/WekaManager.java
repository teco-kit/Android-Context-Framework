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
package edu.teco.context.recognition;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;
import java.util.List;
import java.util.Map.Entry;

import weka.classifiers.Evaluation;
import weka.classifiers.bayes.NaiveBayes;
import weka.core.Attribute;
import weka.core.DenseInstance;
import weka.core.Instance;
import weka.core.Instances;
import weka.core.converters.ArffLoader;
import android.os.Environment;
import android.util.Log;
import edu.teco.context.configuration.FrameworkConfiguration;
import edu.teco.context.configuration.FrameworkContext;
import edu.teco.context.configuration.FrameworkKeys.IMetaDataTags;
import edu.teco.context.log.DataLogger;
import edu.teco.context.probe.sensors.StaticSensorHandlerFactory;

// Example how to create a WEKA dataset in java https://svn.scms.waikato.ac.nz/svn/weka/trunk/wekaexamples/src/main/java/wekaexamples/core/CreateInstances.java
// http://weka.wikispaces.com/Programmatic+Use

public class WekaManager implements IMetaDataTags {

	/** Tag string for debug logs */
	private static final String TAG = "WekaManager";

	/** Singleton because only one WekaManager should be used. */
	private static WekaManager singletonInstance = null;

	private Instances trainingData;
	private Instances testingData;
	private ArrayList<Attribute> atts;
	private ArrayList<Attribute> attsTesting;
	private ArrayList<String> attClassVals;
	private List<String> metaData;
	
	private String mPreviousCalculatedClassName = null;

	/** the classifier used internally */
	public NaiveBayes classifier = null;

	private List<IWekaListener> mListeners;
	
	/** If this option is set, the training data will be logged and not saved in an array at first. */
	private boolean isLogDirectlyToFile = true;
	
	private boolean isTrainingDataReceived = false;
	
	/** The file for directly logging the training data. */
	private File mArffFile = null;
	
	/** BufferedWriter for ARFF file writing.*/
	private BufferedWriter mWriter = null;

	private WekaManager() {
		super();

		// set a Classifier (Naives Bayes)
		classifier = new NaiveBayes();
		classifier.setUseKernelEstimator(true);
		
		atts = new ArrayList<Attribute>();
		attsTesting = new ArrayList<Attribute>();
		attClassVals = new ArrayList<String>();
		metaData= new ArrayList<String>();
		
		mListeners = new ArrayList<IWekaListener>();
		
		isTrainingDataReceived = false;
	}

	/**
	 * Creates a singleton WekaManager or returns the already created object.
	 * 
	 * @return The singleton object.
	 */
	public static WekaManager getInstance() {
		if (singletonInstance == null) {
			singletonInstance = new WekaManager();
		}
		return singletonInstance;
	}

	/**
	 * Configures WEKA with numeric attributes and nominal class values. Any old
	 * values are discarded.
	 * 
	 * @param features
	 * @param classValues
	 */
	public void configureArff(List<String> features, List<String> classValues) {
		configureArff("MyAccelerometerTestingData", features, classValues);
	}

	/**
	 * Configures WEKA with numeric attributes and nominal class values. Any old
	 * values are discarded.
	 * 
	 * @param features
	 * @param classValues
	 */
	public void configureArff(String name, List<String> features, List<String> classValues) {
		atts.clear();
		attsTesting.clear();

		// add feature attributes (numeric)
		for (String feature : features) {
			atts.add(new Attribute(feature));
			attsTesting.add(new Attribute(feature));
		}

		// add class attributes (nominal)
		attClassVals = new ArrayList<String>(classValues);
		atts.add(new Attribute("Class", attClassVals));
		attsTesting.add(new Attribute("Class", attClassVals));

		// create instances object
		trainingData = new Instances(name + "_TrainingData", atts, 0);
		testingData = new Instances(name + "_TestingData", atts, 0);
		
		if (isLogDirectlyToFile) {
			if (!FrameworkContext.getInstance().isConfigurationAlreadySaved()) {
				storeArffFile(trainingData);
			}
		}
	}

	public void configureWithArffFile(File file) {

		if (FrameworkContext.INFO) Log.i(TAG, "Loading from path: " + file.getPath());

		try {
			ArffLoader loader = new ArffLoader();
			loader.setSource(file);
			trainingData = loader.getDataSet();

			if (FrameworkContext.INFO) Log.i(TAG,
					"WEKA configuration from ARFF File: " + file.getPath() + "Training Data Information:\n" + trainingData.toSummaryString());
			buildClassifier();
		} catch (Exception e) {
			e.printStackTrace();
		}
	}

	public void configure(FrameworkConfiguration configuration) {
		
		// write down configuration in machine readable format
		metaData.clear();
		addMetaData(CONFIG_BEGIN);
		addMetaData(SAMPLE_WINDOW_BEGIN + String.valueOf(configuration.getSampleWindow()) + SAMPLE_WINDOW_END);
		addMetaData(OVERLAP_BEGIN + String.valueOf(configuration.getOverlap()) + OVERLAP_END);
		for (Entry<String, String[]> entry : configuration.getSensorFeaturesSet()) {
			String probeKey = entry.getKey();
			String[] featureKeys = entry.getValue();
			StringBuilder sb = new StringBuilder().append(PROBE_BEGIN).append(probeKey).append(PROBE_END);
			for (String featureKey : featureKeys) {
				sb.append(FEATURE_BEGIN).append(featureKey).append(FEATURE_END);
			}
			addMetaData(sb.toString());
		}
		
		StringBuilder sb = new StringBuilder();
		for (String contextLabel : configuration.getContextLabels()) {
			sb.append(CONTEXT_LABEL_BEGIN).append(contextLabel).append(CONTEXT_LABEL_END);
		}
		addMetaData(sb.toString());
		
		addMetaData(CONFIG_END);
		
		
		addMetaData("Sample Window in seconds: " + String.valueOf(configuration.getSampleWindow()));
		addMetaData("Relative overlap: " + String.valueOf(configuration.getOverlap()));

		// create a WekaManager configuration � list of features (numeric) and
		// class values (nominal)
		List<String> features = new ArrayList<String>();

		for (Entry<String, String[]> entry : configuration.getSensorFeaturesSet()) {

			String sensorKey = entry.getKey();
			String[] sensorValueKeys = StaticSensorHandlerFactory.getSensorValueKeys(sensorKey);

			addMetaData("Sensor: " + sensorKey + " with values: " + Arrays.toString(sensorValueKeys));

			String[] featureKeys = entry.getValue();

			for (String featureKey : featureKeys) {
				for (String sensorValueKey : sensorValueKeys) {
					features.add(sensorKey + "_" + featureKey + "_" + sensorValueKey);
				}
			}
		}

		// TODO: GPS as feature
		configureArff(configuration.getConfigurationName(), features, configuration.getContextLabels());
	}

	/**
	 * sets the classifier to use
	 * 
	 * @param name
	 *            the classname of the classifier
	 * @param options
	 *            the options for the classifier
	 */
	public void setClassifier(String name, String[] options) {
		// classifier = AbstractClassifier.forName(name, options);

		// set a Classifier (Naives Bayes)
		classifier = new NaiveBayes();
	}

	public void addMetaData(String metaDataArgument) {
		metaData.add(metaDataArgument);
	}

	public void removeAllMetaData() {
		metaData.clear();
	}

	public void addTrainingData(double[] featureValues, String className) {
		isTrainingDataReceived = true;
		fillData(featureValues, className, trainingData);
	}

	public void addEvaluationData(double[] featureValues, String className) {
		fillData(featureValues, className, testingData);
	}

	private void fillData(double[] featureValues, String className, Instances data) {

		double[] vals = new double[data.numAttributes()];

		if (vals.length != (featureValues.length + 1)) {
			if (FrameworkContext.WARN) Log.w(TAG, "Number of feature values and weka instance values differs.");
		}

		for (int i = 0; i < featureValues.length; i++) {
			vals[i] = featureValues[i];
		}

		vals[vals.length - 1] = attClassVals.indexOf(className);

		DenseInstance instance = new DenseInstance(1.0, vals);
		
		if (isLogDirectlyToFile) {
			instance.setDataset(data);
			logArffData(instance.toString());
		} else {
			// add
			data.add(instance);
		}
	}
	
	public String getArffResult() {
		return trainingData.toString();
	}

	public void trainClassifier(boolean storeArffFile) {
		
		if (isLogDirectlyToFile) {
			try {
				mWriter.flush();
				mWriter.close();
				configureWithArffFile(mArffFile);
			} catch (IOException e) {
				e.printStackTrace();
			}
		} else {
			if (storeArffFile) storeArffFile(trainingData);
//			if (FrameworkContext.INFO) Log.i("WekaData", "Training data:\n" + trainingData.toString());
			buildClassifier();
		}
	}
	
	private void buildClassifier() {
		// set class attribute (last attribute)
		trainingData.setClassIndex(trainingData.numAttributes() - 1);
		try {
//			classifier.setDebug(true);
			classifier.buildClassifier(trainingData);
			
			// if too large String Log will not show everything
//			if (FrameworkContext.INFO) Log.i(TAG, "Classifier description: " + classifier.toString());
			if (FrameworkContext.INFO) Log.i(TAG, "Classifier Global Info: " + classifier.globalInfo());
		} catch (Exception e) {
			e.printStackTrace();
		}
	}

	public void testClassification() {
		// set class attribute (last attribute)
		testingData.setClassIndex(testingData.numAttributes() - 1);

		if (FrameworkContext.INFO) Log.i("WekaData", "Testing data:\n" + testingData.toString());

		// Test the model
		Evaluation eTest;
		try {
			eTest = new Evaluation(trainingData);
			eTest.evaluateModel(classifier, testingData);

			if (FrameworkContext.INFO) Log.i("WekaData", "\nClass detail:\n\n" + eTest.toClassDetailsString());

			// Print the result à la Weka explorer:
			String strSummary = eTest.toSummaryString();
			if (FrameworkContext.INFO) Log.i("WekaData", "----- Summary -----\n" + strSummary);

			// print the confusion matrix
			if (FrameworkContext.INFO) Log.i("WekaData", "----- Confusion Matrix -----\n" + eTest.toMatrixString());

			// print class details
			if (FrameworkContext.INFO) Log.i("WekaData", "----- Class Detail -----\n" + eTest.toClassDetailsString());
			
			notifyTestCalculated(strSummary);

		} catch (Exception e) {
			e.printStackTrace();
		}

	}

	public void classifyInstance(double[] featureValues) {

		/*
		 * // Create empty instance with three attribute values Instance inst =
		 * new DenseInstance(3);
		 * 
		 * // Set instance's values for the attributes "length", "weight", and
		 * "position" inst.setValue(length, 5.3); inst.setValue(weight, 300);
		 * inst.setValue(position, "first");
		 * 
		 * // Set instance's dataset to be the dataset "race"
		 * inst.setDataset(race);
		 */

		Instance instance = new DenseInstance(1.0, featureValues);
		boolean check = trainingData.checkInstance(instance);
		if (FrameworkContext.INFO) Log.i("WekaData", "Result of Instance check: " + check);
		instance.setDataset(trainingData);

		if (FrameworkContext.INFO) Log.i("WekaData", "Try to classify FeatureVector.");

		try {
			double classValue = classifier.classifyInstance(instance);
			double[] classDistribution = classifier.distributionForInstance(instance);

			Attribute classAttribute = trainingData.classAttribute();
			String className = classAttribute.value((int) classValue);
			double classProbability = classDistribution[(int) classValue];

			StringBuilder logString = new StringBuilder();
			logString.append("----- Classification Result -----\nClass Value = ").append(classValue)
					.append("\nClass Distribution = {");
			for (double value : classDistribution) {
				logString.append(value).append(";");
			}
			logString.deleteCharAt(logString.length() - 1);
			logString.append("}\nClass Name = ").append(className);

			if (FrameworkContext.INFO) Log.i("WekaData", logString.toString());
			
			WekaEvent wekaEvent = new WekaEvent(this, className, classProbability, mPreviousCalculatedClassName, classDistribution);
			
			notifyClassCalculated(wekaEvent);
			
			if (mPreviousCalculatedClassName != null) {
				if (mPreviousCalculatedClassName.equals(className)) {
					notifyClassChanged(wekaEvent);
				}
			}
			mPreviousCalculatedClassName = className;
			
		} catch (Exception e) {
			e.printStackTrace();
		}
	}
	
	public void removeAllData() {
		if (trainingData != null) {
			trainingData.clear();
			isTrainingDataReceived = false;
		}
		if (testingData != null) {
			testingData.clear();
		}
	}
	
	public void reset() {
		removeAllData();
		singletonInstance = new WekaManager();
	}

	private void storeArffFile(Instances dataSet) {

		// see http://weka.wikispaces.com/Save+Instances+to+an+ARFF+File
		// better performance with ArffSaver but no comments supported?

		if (DataLogger.isExternalStorageAvailable()) {
			// ArffSaver saver = new ArffSaver();
			// saver.setInstances(dataSet);
			try {
				
				String arffDirectory = FrameworkConfiguration.getInstance().getArffDirectory();
				File arffDir = new File(Environment.getExternalStorageDirectory() + arffDirectory);
				arffDir.mkdirs();
				
				String arffFileName = "Recognition_" + new Date().getTime() + ".arff";
				
				mArffFile = new File(arffDir, arffFileName);
				
				if (isLogDirectlyToFile) {
					mWriter = new BufferedWriter(new FileWriter(mArffFile, true), 8192);
					
					// add the metadata comments
					for (String metaDataLine : metaData) {
						mWriter.write("% " + metaDataLine);
						mWriter.newLine();
					}
					mWriter.write(dataSet.toString());
					mWriter.flush();
					
				} else {
					BufferedWriter writer = new BufferedWriter(new FileWriter(mArffFile));

					// add the metadata comments
					for (String metaDataLine : metaData) {
						writer.write("% " + metaDataLine);
						writer.newLine();
					}

					writer.write(dataSet.toString());
					writer.flush();
					writer.close();
				}

			} catch (IOException e) {
				e.printStackTrace();
			}
		}
	}
	
	private void logArffData(String data) {
		if (mWriter != null) {
			try {
				mWriter.write(data);
				mWriter.newLine();
				mWriter.flush();
			} catch (IOException e) {
				e.printStackTrace();
			}
		}
	}
	
	// ******************************************************************** //
    // sanity check methods
    // ******************************************************************** //
	
	public boolean isTrainingDataAvailable() {
		if (isLogDirectlyToFile && isTrainingDataReceived) {
			return true;
		}
		if (trainingData == null || trainingData.isEmpty()) {
			return false;
		}
		return true;
	}
	
	public boolean isTestingDataAvailable() {
		if (testingData == null || testingData.isEmpty()) {
			return false;
		}
		return true;
	}
	
	public boolean isLogDirectlyToFile() {
		return this.isLogDirectlyToFile;
	}
	
	public void setLogDirectlyToFile(boolean isLogDirectlyToFile) {
		this.isLogDirectlyToFile = isLogDirectlyToFile;
	}

	// ******************************************************************** //
    // WekaEvent & WekaListener methods
    // ******************************************************************** //

	public void addWekaListener(IWekaListener listener) {
		if (!mListeners.contains(listener)) {
			mListeners.add(listener);
		}
	}

	public void removeWekaListener(IWekaListener listener) {
		mListeners.remove(listener);
	}

	private void notifyClassCalculated(WekaEvent event) {
		for (IWekaListener listener : mListeners) {
			listener.onClassCalculated(event);
		}
	}

	private void notifyClassChanged(WekaEvent event) {
		for (IWekaListener listener : mListeners) {
			listener.onClassChanged(event);
		}
	}
	
	private void notifyTestCalculated(String result) {
		for (IWekaListener listener : mListeners) {
			listener.onTestCalculated(result);
		}
	}

}
