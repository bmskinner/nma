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

package com.bmskinner.nuclear_morphology.gui.dialogs;

import com.bmskinner.nuclear_morphology.components.IAnalysisDataset;
import com.bmskinner.nuclear_morphology.gui.MainWindow;

/**
 * A base class for the sub analyses setup options
 * @author bms41
 * @since 1.13.4
 *
 */
@SuppressWarnings("serial")
public abstract class SubAnalysisSetupDialog extends SettingsDialog {
	
	final protected IAnalysisDataset dataset;
	
	/**
	 * Construct with a main program window to listen for actions, and a dataset to operate on
	 * @param mw
	 * @param dataset
	 */
	public SubAnalysisSetupDialog(final MainWindow mw, final IAnalysisDataset dataset, final String title) {

		// modal dialog
		super( mw, true);
		this.dataset = dataset;
		this.setTitle(title);

		createUI();
		
		this.pack();
		this.setLocationRelativeTo(null);
		this.setVisible(true);

	}

	protected abstract void createUI();
	
	protected abstract void runAnalysis();

}
