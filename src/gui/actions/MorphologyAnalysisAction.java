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

import gui.DatasetEvent.DatasetMethod;
import gui.MainWindow;
import io.CompositeExporter;
import io.NucleusAnnotator;
import io.PopulationExporter;
import io.StatsExporter;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.logging.Level;

import javax.swing.SwingUtilities;

import analysis.AnalysisDataset;
import analysis.nucleus.MorphologyAnalysis;

public class MorphologyAnalysisAction extends ProgressableAction {

	private int mode = MorphologyAnalysis.MODE_NEW;
	private List<AnalysisDataset> processList = null;
	private AnalysisDataset source 			= null;
	
                
	/**
	 * Carry out a morphology analysis on a dataset, giving the mode
	 * @param dataset the dataset to work on 
	 * @param mode the type of morphology analysis to carry out
	 * @param downFlag the next analyses to perform
	 */
	public MorphologyAnalysisAction(AnalysisDataset dataset, int mode, int downFlag, MainWindow mw){
		super(dataset, "Morphology analysis", "Error in analysis", mw, downFlag);
		programLogger.log(Level.FINE, "Creating morphology analysis");
		this.mode = mode;
		runNewAnalysis();
	}
	
	/**
	 * Carry out a morphology analysis on a dataset, giving the mode
	 * @param list the datasets to work on 
	 * @param mode the type of morphology analysis to carry out
	 * @param downFlag the next analyses to perform
	 */
	public MorphologyAnalysisAction(List<AnalysisDataset> list, int mode, int downFlag, MainWindow mw){
		super(list.get(0), "Morphology analysis", "Error in analysis", mw, downFlag);
		programLogger.log(Level.FINE, "Creating morphology analysis");
		this.mode = mode;
		this.processList = list;
		processList.remove(0); // remove the first entry
		
		runNewAnalysis();
	}
	
	private void runNewAnalysis(){

		try{
			String message = null;
			switch (this.mode) {
			case MorphologyAnalysis.MODE_COPY:  message = "Copying morphology";
			break;

			case MorphologyAnalysis.MODE_REFRESH: message = "Refreshing morphology";
			break;

			default: message = "Morphology analysis: "+dataset.getName();
			break;  
			}

			this.setProgressMessage(message);
			this.cooldown();

			worker = new MorphologyAnalysis(this.dataset, mode, programLogger);
			worker.addPropertyChangeListener(this);
			programLogger.log(Level.FINE, "Running morphology analysis");
			worker.execute();
		} catch(Exception e){
			this.cancel();
			programLogger.log(Level.SEVERE, "Error in morphology analysis", e);
		}
	}


	/**
	 * Copy the morphology information from the source dataset to the dataset
	 * @param dataset the target
	 * @param source the source
	 */
	public MorphologyAnalysisAction(AnalysisDataset dataset, AnalysisDataset source, Integer downFlag, MainWindow mw){
		super(dataset, "Copying morphology to "+dataset.getName(), "Error in analysis", mw);

		this.mode = MorphologyAnalysis.MODE_COPY;
		this.source = source;
		if(downFlag!=null){
			this.downFlag = downFlag;
		}
		
		// always copy when a source is given
		worker = new MorphologyAnalysis(dataset, source.getCollection(), programLogger);
		worker.addPropertyChangeListener(this);
		worker.execute();
	}
	
	/**
	 * Copy the morphology information from the source dataset to each dataset in a list
	 * @param list
	 * @param source
	 */
	public MorphologyAnalysisAction(List<AnalysisDataset> list, AnalysisDataset source, Integer downFlag, MainWindow mw){
		this(list.get(0), source, downFlag, mw ); // take the first entry
		this.processList = list;
		processList.remove(0); // remove the first entry
	}
  
	@Override
	public void finished() {
//		final utility.Logger logger = new utility.Logger(dataset.getDebugFile(), "MainWindow");
//		logger.log(Level.INFO, "Morphology analysis finished");
//		logger.log("Morphology analysis finished");

		// ensure the progress bar gets hidden even if it is not removed
		this.progressBar.setVisible(false);

		// The analysis takes place in a new thread to accomodate refolding.
		// See specific comment below
		Thread thr = new Thread(){

			public void run(){

				if(  (downFlag & MainWindow.STATS_EXPORT) == MainWindow.STATS_EXPORT){
//					logger.log(Level.FINE, "Running stats export");
//					logger.log("Running stats export", utility.Logger.DEBUG);
					programLogger.log(Level.INFO, "Exporting stats...");
					boolean ok = StatsExporter.run(dataset);
					if(ok){
						programLogger.log(Level.INFO, "OK");
					} else {
						programLogger.log(Level.INFO, "Error");
					}
				}

				// annotate the nuclei in the population
				if(  (downFlag & MainWindow.NUCLEUS_ANNOTATE) == MainWindow.NUCLEUS_ANNOTATE){
//					logger.log("Running annotation", utility.Logger.DEBUG);
					programLogger.log(Level.INFO, "Annotating nuclei...");
					boolean ok = NucleusAnnotator.run(dataset);
					if(ok){
						programLogger.log(Level.INFO, "OK");
					} else {
						programLogger.log(Level.INFO, "Error");
					}
				}

				// make a composite image of all nuclei in the collection
				if(  (downFlag & MainWindow.EXPORT_COMPOSITE) == MainWindow.EXPORT_COMPOSITE){
//					logger.log("Running compositor", utility.Logger.DEBUG);
					programLogger.log(Level.INFO, "Exporting composite...");
					boolean ok = CompositeExporter.run(dataset);
					if(ok){
						programLogger.log(Level.INFO, "OK");
					} else {
						programLogger.log(Level.INFO, "Error");
					}
				}

				// The new refold action is a progressable action, so must not block
				// the EDT. Also, the current action must wait for refolding to complete,
				// otherwise the next MorphologyAnalysisAction in the chain will block the
				// refold from firing a done signal. Hence, put a latch on the refold to 
				// make this thread wait until the refolding is complete.
				if(  (downFlag & MainWindow.CURVE_REFOLD) == MainWindow.CURVE_REFOLD){

					final CountDownLatch latch = new CountDownLatch(1);
//					logger.log("Running curve refolder", utility.Logger.DEBUG);
					programLogger.log(Level.FINEST, "Morphology finished() process thread is EDT: "+SwingUtilities.isEventDispatchThread());
					
					new RefoldNucleusAction(dataset, mw, latch);
					try {
						latch.await();
					} catch (InterruptedException e) {
						programLogger.log(Level.SEVERE, "Interruption to thread", e);
					}
				}

				if(  (downFlag & MainWindow.SAVE_DATASET) == MainWindow.SAVE_DATASET){
//					logger.log("Saving dataset", utility.Logger.DEBUG);
					PopulationExporter.saveAnalysisDataset(dataset);
				}

				if(  (downFlag & MainWindow.ADD_POPULATION) == MainWindow.ADD_POPULATION){
					List<AnalysisDataset> list = new ArrayList<AnalysisDataset>();
					list.add(dataset);
					fireDatasetEvent(DatasetMethod.ADD_DATASET, list);
				}

				// if no list was provided, or no more entries remain,
				// call the finish
				if(processList==null){
//					logger.log("Analysis complete, process list null, cleaning up", utility.Logger.DEBUG);
					MorphologyAnalysisAction.super.finished();
				} else if(processList.isEmpty()){
//					logger.log("Analysis complete, process list empty, cleaning up", utility.Logger.DEBUG);
					MorphologyAnalysisAction.super.finished();
				} else {
//					logger.log("Morphology analysis continuing; removing progress bar", utility.Logger.DEBUG);
					// otherwise analyse the next item in the list
					cancel();
					if(mode == MorphologyAnalysis.MODE_COPY){

						SwingUtilities.invokeLater(new Runnable(){
							public void run(){
								new MorphologyAnalysisAction(processList, source, downFlag, mw);
							}});
					} else {
						SwingUtilities.invokeLater(new Runnable(){
							public void run(){
								new MorphologyAnalysisAction(processList, mode, downFlag, mw);
							}});
					}
				}
//				removeDatasetEventListener(mw);
//				removeInterfaceEventListener(mw);
			
			}
		};
		thr.start();

	}
}
