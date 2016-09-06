/*******************************************************************************
 *  	Copyright (C) 2016 Ben Skinner
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

import gui.DatasetListManager;
import gui.MainWindow;
import gui.ThreadManager;
import gui.DatasetEvent.DatasetMethod;
import gui.dialogs.AnalysisSetupDialog;

import java.io.File;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.List;
import java.util.logging.Level;

import analysis.AnalysisDataset;
import analysis.AnalysisOptions;
import analysis.nucleus.NucleusDetector;

/**
 * Run a new analysis
 */
public class NewAnalysisAction extends ProgressableAction {
			
	private AnalysisOptions options;
	private Date startTime;
	private String outputFolderName;
	
	public static final int NEW_ANALYSIS = 0;
	
	public NewAnalysisAction(MainWindow mw) {
		this(mw, null);
		
	}
	
	/**
	 * Create a new analysis, specifying the initial directory of images
	 * @param mw
	 * @param folder
	 */
	public NewAnalysisAction(MainWindow mw, File folder) {
		super("Nucleus detection", mw);

		this.cooldown();
		
		fine("Making analysis options");
		AnalysisSetupDialog analysisSetup = new AnalysisSetupDialog(DatasetListManager.getInstance().getRootDatasets(), folder);
		
		if( analysisSetup.getOptions()!=null){

			options = analysisSetup.getOptions();

			log("Directory: "+options.getFolder().getName());

			this.startTime = Calendar.getInstance().getTime();
			this.outputFolderName = new SimpleDateFormat("yyyy-MM-dd_HH-mm-ss").format(this.startTime);

			// craete the analysis folder early. Did not before in case folder had no images
			File analysisFolder = new File(options.getFolder().getAbsolutePath()+File.separator+outputFolderName);
			if(!analysisFolder.exists()){
				analysisFolder.mkdir();
			}
//			
			File logFile = new File(options.getFolder().getAbsolutePath()
					+ File.separator
					+ outputFolderName
					+ File.separator+options.getFolder().getName()+".log");

//			mw.setStatus("New analysis in progress");
			
			worker = new NucleusDetector(this.outputFolderName, logFile, options);
			worker.addPropertyChangeListener(this);
			ThreadManager.getInstance().submit(worker);
			log(Level.FINEST, "Worker has executed");
			analysisSetup.dispose();
			
		} else {				
			analysisSetup.dispose();
			log(Level.FINE, "Analysis cancelled");
			this.cancel();
		}
	}
	
	@Override
	public void finished(){
		
		final List<AnalysisDataset> datasets = ((NucleusDetector) worker).getDatasets();
		
		if(datasets.size()==0 || datasets==null){
			log(Level.INFO, "No datasets returned");
		} else {
			fireDatasetEvent(DatasetMethod.PROFILING_ACTION, datasets);
			
		}
		super.finished();
	}
	
}
