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

import gui.DatasetEvent;
import gui.DatasetListManager;
import gui.MainWindow;
import gui.ThreadManager;
import gui.dialogs.AnalysisSetupDialog;

import java.io.File;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.List;
import java.util.logging.Level;

import analysis.IAnalysisDataset;
import analysis.IAnalysisOptions;
import analysis.IMutableAnalysisOptions;
import analysis.IMutableDetectionOptions;
import analysis.nucleus.NucleusDetectionWorker;

/**
 * Run a new analysis
 */
public class NewAnalysisAction extends ProgressableAction {
			
	private IMutableAnalysisOptions options;
	private Date startTime;
	private String outputFolderName;
	
	private File folder;
	
	public static final int NEW_ANALYSIS = 0;
	
	/**
	 * Create a new analysis. The folder of images to analyse will be
	 * requested by a dialog.
	 * @param mw the main window to which a progress bar will be attached
	 */
	public NewAnalysisAction(MainWindow mw) {
		this(mw, null);
	}
	
	/**
	 * Create a new analysis, specifying the initial directory of images
	 * @param mw the main window to which a progress bar will be attached
	 * @param folder the folder of images to analyse
	 */
	public NewAnalysisAction(MainWindow mw, final File folder) {
		super("Nucleus detection", mw);
		this.folder = folder;
	}
	
	@Override
	public void run(){

		this.cooldown();
		
		fine("Making analysis options");
		AnalysisSetupDialog analysisSetup = new AnalysisSetupDialog(DatasetListManager.getInstance().getRootDatasets(), folder);
		
		if( analysisSetup.getOptions()!=null){

			options = analysisSetup.getOptions();
			IMutableDetectionOptions nucleusOptions = options.getDetectionOptions(IAnalysisOptions.NUCLEUS);

			log("Directory: "+nucleusOptions.getFolder().getName());

			this.startTime = Calendar.getInstance().getTime();
			this.outputFolderName = new SimpleDateFormat("yyyy-MM-dd_HH-mm-ss").format(this.startTime);

			// craete the analysis folder early. Did not before in case folder had no images
			File analysisFolder = new File(nucleusOptions.getFolder().getAbsolutePath()+File.separator+outputFolderName);
			if(!analysisFolder.exists()){
				analysisFolder.mkdir();
			}
//			
			File logFile = new File(nucleusOptions.getFolder().getAbsolutePath()
					+ File.separator
					+ outputFolderName
					+ File.separator+nucleusOptions.getFolder().getName()+".log");

//			mw.setStatus("New analysis in progress");
			
			worker = new NucleusDetectionWorker(this.outputFolderName, logFile, options);
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
		
		final List<IAnalysisDataset> datasets = ((NucleusDetectionWorker) worker).getDatasets();
		
		if(datasets.size()==0 || datasets==null){
			log(Level.INFO, "No datasets returned");
		} else {
			fireDatasetEvent(DatasetEvent.PROFILING_ACTION, datasets);
			
		}
		super.finished();
	}
	
}
