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

import gui.MainWindow;
import gui.ThreadManager;

import java.util.concurrent.CountDownLatch;

import analysis.IAnalysisDataset;
import analysis.nucleus.CurveRefolder;
import analysis.nucleus.CurveRefolder.CurveRefoldingMode;

/**
 * Refold the consensus nucleus for the selected dataset using default parameters
 */
public class RefoldNucleusAction extends ProgressableAction {

	/**
	 * Refold the given selected dataset
	 */
	
	public RefoldNucleusAction(IAnalysisDataset dataset, MainWindow mw, CountDownLatch doneSignal) {
		super(dataset, "Refolding", mw);
		this.setLatch(doneSignal);
		try{

			this.cooldown();
			worker = new CurveRefolder(dataset, 
					CurveRefoldingMode.FAST);

			worker.addPropertyChangeListener(this);
			this.setProgressMessage("Refolding: "+dataset.getName());
			ThreadManager.getInstance().submit(worker);
//			worker.execute();

		} catch(Exception e1){
			this.cancel();
			error("Error refolding nucleus", e1);
		}
	}
	
	@Override
	public void finished(){
		this.cancel();
		fine("Refolding finished, cleaning up");
		super.finished();
		this.countdownLatch();
		
		
		
	}

}
