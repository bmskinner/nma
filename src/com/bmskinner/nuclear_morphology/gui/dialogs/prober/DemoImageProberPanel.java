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

package com.bmskinner.nuclear_morphology.gui.dialogs.prober;

import java.awt.Window;
import java.io.File;

import com.bmskinner.nuclear_morphology.analysis.detection.pipelines.NeutrophilFinder;
import com.bmskinner.nuclear_morphology.components.options.IAnalysisOptions;
import com.bmskinner.nuclear_morphology.components.options.MissingOptionException;

/**
 * This class is to test the new image prober using Finders rather than ImageProberWorkers
 * @author bms41
 * @since 1.13.5
 *
 */
@SuppressWarnings("serial")
public class DemoImageProberPanel extends AbstractImageProberPanel {

	public DemoImageProberPanel(File folder, IAnalysisOptions options, Window parent) throws MissingOptionException{

		super(folder, options, parent);	
		
		try {
						
			test = new NeutrophilFinder(options);
			
			createUI();

		} catch (Exception e) {
			error("Error in prober", e);
			stack(e);
		}
	}

	
	@Override
	protected void importAndDisplayImage(File imageFile){
		if(imageFile==null){
			throw new IllegalArgumentException(NULL_FILE_ERROR);
		}
		
		try {
			finer("Firing panel updating event");
			
			progressBar.setVisible(true);
			test.removeAllDetectionEventListeners();
			ProberTableModel model = new ProberTableModel();
			
			test.addDetectionEventListener(model);
			table.setModel(model);
			table.getColumnModel().getColumn(1).setCellRenderer(new IconCellRenderer());
			
//			progressBar.setValue(0);
			setImageLabel(imageFile.getAbsolutePath());
			
			
			test.findInImage(imageFile);
			progressBar.setVisible(false);
			firePanelUpdatingEvent(PanelUpdatingEvent.COMPLETE);
		} catch (Exception e) { // end try
			error(e.getMessage(), e);
		} 
	}

}
