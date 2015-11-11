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

import gui.MainWindow;

import java.util.concurrent.CountDownLatch;
import java.util.logging.Level;

import javax.swing.SwingUtilities;

import analysis.AnalysisDataset;
import analysis.nucleus.CurveRefolder;
import analysis.nucleus.CurveRefolder.CurveRefoldingMode;

/**
 * Refold the consensus nucleus for the selected dataset using default parameters
 */
public class RefoldNucleusAction extends ProgressableAction {

	/**
	 * Refold the given selected dataset
	 */
	
	public RefoldNucleusAction(AnalysisDataset dataset, MainWindow mw, CountDownLatch doneSignal) {
		super(dataset, "Refolding", "Error refolding nucleus", mw);
		programLogger.log(Level.FINEST, "Created RefoldNucleusAction");
		try{

			this.progressBar.setIndeterminate(true);
			worker = new CurveRefolder(dataset, 
					CurveRefoldingMode.FAST, 
					doneSignal, 
					programLogger);

			worker.addPropertyChangeListener(this);
			this.setProgressMessage("Refolding: "+dataset.getName());
			
			/*
			 * The SwingWorker doInBackground is off the EDT. At this point, the EDT should be free
			 * 
			 * 
			 * What thread is waiting for a signal from the worker?
			 */
			programLogger.log(Level.FINEST, "RefoldNucleusAction init is EDT: "+SwingUtilities.isEventDispatchThread());
			
			worker.execute();
			programLogger.log(Level.FINEST, "Executed CurveRefolder");

		} catch(Exception e1){
			this.cancel();
			programLogger.log(Level.SEVERE, "Error refolding nucleus", e1);
		}
	}
	
	@Override
	public void finished(){

		programLogger.log(Level.FINE, "Refolding finished, cleaning up");
		programLogger.log(Level.FINEST, "RefoldNucleusAction.finished() is EDT: "+SwingUtilities.isEventDispatchThread());
		
		
		// ensure the bar is gone, even if the cleanup fails
		this.progressBar.setVisible(false);
		dataset.getAnalysisOptions().setRefoldNucleus(true);
		dataset.getAnalysisOptions().setRefoldMode("Fast");
		super.finished();
		
	}

}
