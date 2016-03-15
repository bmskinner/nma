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

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.logging.Level;

import javax.swing.SwingUtilities;

import analysis.AnalysisDataset;
import analysis.nucleus.DatasetSegmenter;
import analysis.nucleus.DatasetSegmenter.MorphologyAnalysisMode;
import gui.InterfaceEvent.InterfaceMethod;
import gui.MainWindow;
import gui.DatasetEvent.DatasetMethod;
import io.CompositeExporter;
import io.NucleusAnnotator;
import io.StatsExporter;

public class RunSegmentationAction extends ProgressableAction {
	
	private MorphologyAnalysisMode mode = MorphologyAnalysisMode.NEW;
	
	private AnalysisDataset source = null;
	private CountDownLatch latch   = null;
	
	/**
	 * Carry out a segmentation on a dataset
	 * @param dataset the dataset to work on 
	 * @param mode the type of morphology analysis to carry out
	 * @param downFlag the next analyses to perform
	 */
	public RunSegmentationAction(AnalysisDataset dataset, MorphologyAnalysisMode mode, int downFlag, MainWindow mw, CountDownLatch latch){
		super(dataset, "Segmentation analysis", mw, downFlag);
		this.mode = mode;
		this.latch = latch;
		log(Level.FINE, "Creating segmentation analysis");
		runNewAnalysis();
	}
	
	public RunSegmentationAction(List<AnalysisDataset> list, MorphologyAnalysisMode mode, int downFlag, MainWindow mw){
		super(list, "Segmentation analysis", mw, downFlag);
		this.mode = mode;
		log(Level.FINE, "Creating segmentation analysis");
		runNewAnalysis();
	}
	
	
	public RunSegmentationAction(AnalysisDataset dataset, AnalysisDataset source, Integer downFlag, MainWindow mw, CountDownLatch latch){
		super(dataset, "Copying morphology to "+dataset.getName(), mw);
		this.downFlag = downFlag;
		this.latch = latch;
		this.mode = MorphologyAnalysisMode.COPY;
		this.source = source;
		log(Level.FINE, "Creating segmentation copying analysis");
		runCopyAnalysis();
	}
	
	/**
	 * Copy the morphology information from the source dataset to each dataset in a list
	 * @param list
	 * @param source
	 */
	public RunSegmentationAction(List<AnalysisDataset> list, AnalysisDataset source, Integer downFlag, MainWindow mw){
		super(list, "Segmentation analysis", mw);
		this.downFlag = downFlag;
		this.mode = MorphologyAnalysisMode.COPY;
		this.source = source;
		log(Level.FINE, "Creating segmentation copying analysis");
		runCopyAnalysis();
	}
	
	private void runCopyAnalysis(){
		worker = new DatasetSegmenter(dataset, source.getCollection());
		worker.addPropertyChangeListener(this);
		worker.execute();
	}
	
	private void runNewAnalysis(){
		try{
			String message = null;
			switch (this.mode) {
			case COPY:  message = "Copying segmentation";
			break;

			case REFRESH: message = "Refreshing segmentation";
			break;

			default: message = "Morphology analysis: "+dataset.getName();
			break;  
			}

			this.setProgressMessage(message);
			this.cooldown();

			worker = new DatasetSegmenter(this.dataset, mode);
			worker.addPropertyChangeListener(this);
			log(Level.FINE, "Running morphology analysis");
			worker.execute();
		} catch(Exception e){
			this.cancel();
			logError("Error in morphology analysis", e);
		}
	}
	
	@Override
	public void finished() {

		// ensure the progress bar gets hidden even if it is not removed
		this.setProgressBarVisible(false);

		// The analysis takes place in a new thread to accomodate refolding.
		// See specific comment below
		Thread thr = new Thread(){

			public void run(){

				/*
				 * The refold action is a progressable action, so must not block
				 * the EDT. Also, the current action must wait for refolding to complete,
				 * otherwise the next RunSegmentationAction in the chain will block the
				 * refold from firing a done signal. 
				 * 
				 * Hence, put a latch on the refold to make this thread wait 
				 * until the refolding is complete.
				 */
				if(  (downFlag & MainWindow.CURVE_REFOLD) == MainWindow.CURVE_REFOLD){
					log(Level.FINEST, "Preparing to hold thread while refolding datast");
					final CountDownLatch latch = new CountDownLatch(1);
					new RefoldNucleusAction(dataset, mw, latch);
					try {
						latch.await();
					} catch (InterruptedException e) {
						logError("Interruption to thread", e);
					}
					log(Level.FINEST, "Resuming thread after refolding datast");
				}

				
				/*
				 * Save the dataset, regardless of flags
				 */
				log(Level.FINEST, "Saving the dataset");
				saveDataset(dataset);
				
				/*
				 * We should only need to recache charts if the dataset exists
				 */

				if(  (downFlag & MainWindow.ADD_POPULATION) == MainWindow.ADD_POPULATION){
					log(Level.FINEST, "Firing add dataset signal");
					fireDatasetEvent(DatasetMethod.ADD_DATASET, dataset);
					
					
				} 
				
				/*
				 * When refreshing segmnetation, the orientaition point may have changed.
				 * Update the vertical orientation nuclei for the dataset
				 */
				if(mode.equals(MorphologyAnalysisMode.REFRESH)){
					dataset.getCollection().updateVerticalNuclei();
				}
				
//				if(  (downFlag & MainWindow.SAVE_DATASET) == MainWindow.SAVE_DATASET){
//					programLogger.log(Level.FINEST, "Preparing to fire save datast request");
//					fireDatasetEvent(DatasetMethod.SAVE, dataset);
//				}
				
				/*
				 * Save the dataset, regardless of flags
				 */
//				programLogger.log(Level.FINEST, "Saving the dataset");
//				saveDataset(dataset);
//				programLogger.log(Level.FINEST, "Firing save dataset request");
				
//				fireDatasetEvent(DatasetMethod.SAVE, dataset);

				// if no list was provided, or no more entries remain,
				// call the finish
				if( ! hasRemainingDatasetsToProcess()){
					log(Level.FINEST, "No more datasets remain to process");
					if(latch!=null){
						latch.countDown();
					}
//					if(mode.equals(MorphologyAnalysisMode.REFRESH)){
					log(Level.FINEST, "Firing select dataset event");
					fireDatasetEvent(DatasetMethod.SELECT_ONE_DATASET, dataset);
//					programLogger.log(Level.FINEST, "Firing update panel interface event");
//					fireInterfaceEvent(InterfaceMethod.UPDATE_PANELS);
//					}
					RunSegmentationAction.super.finished();
					
					
					
					
					
				} else {
					
					// otherwise analyse the next item in the list
					cancel(); // remove progress bar

					SwingUtilities.invokeLater(new Runnable(){
						public void run(){

							if(mode.equals(MorphologyAnalysisMode.COPY)){

								new RunSegmentationAction(getRemainingDatasetsToProcess(), source, downFlag, mw);
								
							} else {
								
								new RunSegmentationAction(getRemainingDatasetsToProcess(), mode, downFlag, mw);
							}
						}});

				}			
			}
		};
		thr.start();
	}
	
	/**
	 * Save the given dataset. If it is root, save directly.
	 * If it is not root, find the root parent and save it.
	 * @param d
	 */	
	private void saveDataset(final AnalysisDataset d){
		
		if(d.isRoot()){

			final CountDownLatch latch = new CountDownLatch(1);
			new SaveDatasetAction(d, mw, latch, false);
			try {
				log(Level.FINEST, "Awaiting latch for save action");
				latch.await();
			} catch (InterruptedException e) {
				logError("Interruption to thread", e);
			}

			log(Level.FINE, "Root dataset saved");
		} else {

			AnalysisDataset target = null; 
			for(AnalysisDataset root : mw.getPopulationsPanel().getRootDatasets()){

				for(AnalysisDataset child : root.getAllChildDatasets()){
					if(child.getUUID().equals(d.getUUID())){
						target = root;
						break;
					}
				}
				if(target!=null){
					break;
				}
			}
			if(target!=null){
				saveDataset(target);
			}
		}

	}

}
