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
package com.bmskinner.nuclear_morphology.gui.actions;

import java.util.List;
import java.util.concurrent.CountDownLatch;
import com.bmskinner.nuclear_morphology.analysis.DefaultAnalysisWorker;
import com.bmskinner.nuclear_morphology.analysis.IAnalysisMethod;
import com.bmskinner.nuclear_morphology.analysis.profiles.DatasetProfilingMethod;
import com.bmskinner.nuclear_morphology.analysis.profiles.DatasetSegmentationMethod.MorphologyAnalysisMode;
import com.bmskinner.nuclear_morphology.components.IAnalysisDataset;
import com.bmskinner.nuclear_morphology.gui.MainWindow;
import com.bmskinner.nuclear_morphology.gui.ThreadManager;

public class RunProfilingAction extends ProgressableAction {
	
	public RunProfilingAction(IAnalysisDataset dataset, int downFlag, MainWindow mw){
		super(dataset, "Profiling", mw, downFlag);
		fine("Creating new profiling analysis");
	}
	
	public RunProfilingAction(List<IAnalysisDataset> list, int downFlag, MainWindow mw){
		super(list, "Profiling", mw, downFlag);
		fine("Creating new profiling analysis");
	}
	
	public RunProfilingAction(IAnalysisDataset dataset, int downFlag, MainWindow mw, CountDownLatch latch){
		super(dataset, "Profiling", mw, downFlag);
		fine("Creating new profiling analysis");
		this.setLatch(latch);
		
	}
	
	public RunProfilingAction(List<IAnalysisDataset> list, int downFlag, MainWindow mw, CountDownLatch latch){
		super(list, "Profiling", mw, downFlag);
		fine("Creating new profiling analysis");
		this.setLatch(latch);
		
	}
	
	public void run(){
		fine("Running new profiling analysis");
		runNewAnalysis();
	}
	
	private void runNewAnalysis(){
		try{
			String message = "Profiling: "+dataset.getName();
			fine("Beginning profliling action");

			this.setProgressMessage(message);
			IAnalysisMethod method = new DatasetProfilingMethod(dataset);
			worker = new DefaultAnalysisWorker(method);
			

			worker.addPropertyChangeListener(this);
			fine("Running morphology analysis");
			ThreadManager.getInstance().submit(worker);
		} catch(Exception e){
			this.cancel();
			error("Error in morphology analysis", e);
		}
	}
	
	@Override
	public void finished() {

		// ensure the progress bar gets hidden even if it is not removed
		this.setProgressBarVisible(false);
//		cleanup();
		// The analysis takes place in a new thread to accomodate refolding.
		// See specific comment in RunSegmentationAction
		Runnable task = () -> {

//			public void run(){

				if(  (downFlag & ASSIGN_SEGMENTS) == ASSIGN_SEGMENTS){
					
					final CountDownLatch latch = new CountDownLatch(1);
					Runnable r = new RunSegmentationAction(dataset, MorphologyAnalysisMode.NEW, downFlag, mw, latch);
					r.run();
					try {
						latch.await();
					} catch (InterruptedException e) {
						error("Interruption in segmentation thread", e);
					}
				}


				// if no list was provided, or no more entries remain,
				// call the finish
				if( ! hasRemainingDatasetsToProcess()){

					cancel();		
					RunProfilingAction.this.removeInterfaceEventListener(mw);
					RunProfilingAction.this.removeDatasetEventListener(mw);					
//					
					RunProfilingAction.this.countdownLatch();
					
				} else {
					// otherwise analyse the next item in the list
					cancel(); // remove progress bar

					Runnable p = new RunProfilingAction(getRemainingDatasetsToProcess(), downFlag, mw);
					p.run();

				}			
//			}
		};
//		thr.start();
//		task.run();
		ThreadManager.getInstance().execute(task);

	}

}
