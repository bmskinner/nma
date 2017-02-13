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

import java.awt.Dimension;
import java.awt.Window;
import java.io.File;

import com.bmskinner.nuclear_morphology.components.options.IDetectionOptions;
import com.bmskinner.nuclear_morphology.gui.dialogs.prober.workers.NeutrophilProberWorker;

@SuppressWarnings("serial")
public class NeutrophilImageProberPanel extends ImageProberPanel {
	
	private final IDetectionOptions cytoOptions, nuclOptions;
	
	public NeutrophilImageProberPanel(final Window parent, final IDetectionOptions cytoOptions, final IDetectionOptions nuclOptions, final ImageSet set){
		super(parent, cytoOptions, set);
		this.cytoOptions = cytoOptions;
		this.nuclOptions = nuclOptions;
		
		createFileList(options.getFolder());
	}
	
	/**
	 * Import the given file as an image, detect nuclei and
	 * display the image with annotated nuclear outlines
	 * @param imageFile
	 */
	@Override
	protected void importAndDisplayImage(File imageFile){

		if(imageFile==null){
			throw new IllegalArgumentException(NULL_FILE_ERROR);
		}
		
		try {
			progressBar.setValue(0);
			setImageLabel(imageFile.getAbsolutePath());
			
			
			table.setModel(createEmptyTableModel(rows, cols));
			
			for(int col=0; col<cols; col++){
	        	table.getColumnModel().getColumn(col).setCellRenderer(new IconCellRenderer());
	        }
						
			worker = new NeutrophilProberWorker(imageFile, 
					cytoOptions,
					nuclOptions, 
					imageSet, 
					table.getModel());
			
			worker.setSmallIconSize(new Dimension(SMALL_ICON_MAX_WIDTH, table.getRowHeight()-30));
			
			worker.addPropertyChangeListener(this);
			progressBar.setVisible(true);
			finer("Firing panel updating event");
			firePanelUpdatingEvent(PanelUpdatingEvent.UPDATING);
			
			
			
			worker.execute();


		} catch (Exception e) { // end try
			error(e.getMessage(), e);
		} 
	}
}
