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

import javax.swing.JOptionPane;
import javax.swing.JSpinner;
import javax.swing.SpinnerNumberModel;

import com.bmskinner.nuclear_morphology.analysis.IAnalysisDataset;
import com.bmskinner.nuclear_morphology.analysis.signals.ShellAnalysisWorker;
import com.bmskinner.nuclear_morphology.analysis.signals.ShellDetector;
import com.bmskinner.nuclear_morphology.gui.DatasetEvent;
import com.bmskinner.nuclear_morphology.gui.MainWindow;
import com.bmskinner.nuclear_morphology.gui.ThreadManager;

public class ShellAnalysisAction extends ProgressableAction {

	public ShellAnalysisAction(IAnalysisDataset dataset, MainWindow mw) {
		super(dataset, "Shell analysis", mw);

		
	}
	
	@Override
	public void run(){
		SpinnerNumberModel sModel = new SpinnerNumberModel(ShellDetector.DEFAULT_SHELL_COUNT, 2, 10, 1);
		JSpinner spinner = new JSpinner(sModel);

		int option = JOptionPane.showOptionDialog(null, 
				spinner, 
				"Select number of shells", 
				JOptionPane.OK_CANCEL_OPTION, 
				JOptionPane.QUESTION_MESSAGE, null, null, null);
		if (option == JOptionPane.CANCEL_OPTION) {
			// user hit cancel
			this.cancel();
			return;

		} else if (option == JOptionPane.OK_OPTION)	{

			int shellCount = (Integer) spinner.getModel().getValue();
			worker = new ShellAnalysisWorker(dataset,shellCount);

			worker.addPropertyChangeListener(this);
			ThreadManager.getInstance().submit(worker);
		}
	}
	
	@Override
	public void finished() {
		fireDatasetEvent(DatasetEvent.REFRESH_CACHE, dataset);
		super.finished();
	}
}