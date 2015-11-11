/*******************************************************************************
 *  	Copyright (C) 2015 Ben Skinner
 *   
 *     This file is part of Nuclear Morphology Analysis.
 *
 *     Nuclear Morphology Analysis is free software: you can redistribute it and/or modify
 *     it under the terms of the GNU General Public License as published by
 *     the Free Software Foundation, either version 3 of the License, or
 *     (at your option) any later version.
 *
 *     Nuclear Morphology Analysis is distributed in the hope that it will be useful,
 *     but WITHOUT ANY WARRANTY; without even the implied warranty of
 *     MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 *     GNU General Public License for more details.
 *
 *     You should have received a copy of the GNU General Public License
 *     along with Nuclear Morphology Analysis. If not, see <http://www.gnu.org/licenses/>.
 *******************************************************************************/
package gui.actions;

import gui.AnalysisSetupWindow;
import gui.MainWindow;

import java.io.File;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.List;
import java.util.logging.Level;

import analysis.AnalysisDataset;
import analysis.AnalysisOptions;
import analysis.nucleus.MorphologyAnalysis;
import analysis.nucleus.NucleusDetector;

/**
 * Run a new analysis
 */
public class NewAnalysisAction extends ProgressableAction {
			
	private AnalysisOptions options;
	private NucleusDetector detector;
	private Date startTime;
	private String outputFolderName;
	
	public static final int NEW_ANALYSIS = 0;
	
	public NewAnalysisAction(MainWindow mw) {
		super(null, "Nucleus detection", "Error in analysis", mw);

		AnalysisSetupWindow analysisSetup = new AnalysisSetupWindow(programLogger);
		if( analysisSetup.getOptions()!=null){

			options = analysisSetup.getOptions();

			programLogger.log(Level.INFO, "Directory: "+options.getFolder().getName());

			this.startTime = Calendar.getInstance().getTime();
			this.outputFolderName = new SimpleDateFormat("yyyy-MM-dd_HH-mm-ss").format(this.startTime);

			// craete the analysis folder early. Did not before in case folder had no images
			File analysisFolder = new File(options.getFolder().getAbsolutePath()+File.separator+outputFolderName);
			if(!analysisFolder.exists()){
				analysisFolder.mkdir();
			}
//			utility.Logger logger = new utility.Logger( new File(options.getFolder().getAbsolutePath()+File.separator+outputFolderName+File.separator+"log.debug.txt"), "AnalysisCreator");
			File logFile = new File(options.getFolder().getAbsolutePath()+File.separator+outputFolderName+File.separator+"log.debug.txt");
//			logger.log("Analysis began: "+analysisFolder.getAbsolutePath());
//			logger.log("Directory: "+options.getFolder().getName());
			mw.setStatus("New analysis in progress");
			
			detector = new NucleusDetector(this.outputFolderName, programLogger, logFile, options);
			detector.addPropertyChangeListener(this);
			detector.execute();
			analysisSetup.dispose();
			
		} else {
							
			analysisSetup.dispose();
			this.cancel();
		}
		
		
	}
	
	@Override
	public void finished(){
		
		final List<AnalysisDataset> datasets = detector.getDatasets();
		
		if(datasets.size()==0 || datasets==null){
			programLogger.log(Level.INFO, "No datasets returned");
			this.cancel();
		} else {

			// run next analysis on a new thread to avoid blocking the EDT
			Thread thr = new Thread(){
				
				public void run(){
					
					int flag = 0; // set the downstream analyses to run
					flag |= MainWindow.ADD_POPULATION;
					flag |= MainWindow.STATS_EXPORT;
					flag |= MainWindow.NUCLEUS_ANNOTATE;
					flag |= MainWindow.EXPORT_COMPOSITE;
					flag |= MainWindow.SAVE_DATASET;
					
					if(datasets.get(0).getAnalysisOptions().refoldNucleus()){
						flag |= MainWindow.CURVE_REFOLD;
					}
					// begin a recursive morphology analysis
					new MorphologyAnalysisAction(datasets, MorphologyAnalysis.MODE_NEW, flag, mw);
				}
				
			};
			thr.start();

			// do not call super finished, because there is no dataset for this action
			// allow the morphology action to update the panels
			cancel();
		}
	}
	
}
