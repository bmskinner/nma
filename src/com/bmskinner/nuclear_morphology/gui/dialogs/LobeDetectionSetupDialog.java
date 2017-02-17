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
 * The setup for lobe detection in neutrophils
 * @author bms41
 * @since 1.13.4
 *
 */
@SuppressWarnings("serial")
public class LobeDetectionSetupDialog extends SettingsDialog {
	
	private static final String DIALOG_TITLE = "Lobe detection options";
	
	protected IAnalysisDataset dataset;

	public LobeDetectionSetupDialog(final MainWindow mw, final IAnalysisDataset dataset) {

		// modal dialog
		super( mw, true);
		this.dataset = dataset;
		this.setTitle(DIALOG_TITLE);

		createUI();
		
		this.pack();
		this.setLocationRelativeTo(null);
		this.setVisible(true);

	}

	private void createUI() {
		
		
	}
	
	

}
