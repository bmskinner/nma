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

import java.awt.Dimension;
import java.io.File;

import com.bmskinner.nuclear_morphology.analysis.IAnalysisOptions;
import com.bmskinner.nuclear_morphology.analysis.detection.NucleusProberWorker;
import com.bmskinner.nuclear_morphology.gui.ImageType;

@SuppressWarnings("serial")
public class NucleusDetectionImageProber extends ImageProber {
	
	public NucleusDetectionImageProber(IAnalysisOptions options, File folder) {
		super(options, NucleusImageType.DETECTED_OBJECTS, folder);
		createFileList(folder);
		this.setVisible(true);
	}
	
	/**
	 * Hold the stages of the detection pipeline to display 
	 */
	public enum NucleusImageType implements ImageType {
		KUWAHARA 			("Kuwahara filtering",      0),
		FLATTENED 			("Chromocentre flattening", 1),
		EDGE_DETECTION 		("Edge detection",          2),
		MORPHOLOGY_CLOSED 	("Gap closing",             3),
		DETECTED_OBJECTS 	("Detected objects",        4);
		
		private String name;
		private int position; // the order in which the processed images should be displayed
		
		NucleusImageType(String name, int position){
			this.name = name;
			this.position = position;
		}
		public String toString(){
			return this.name;
		}
		
		public ImageType[] getValues(){
			return NucleusImageType.values();
		}
		@Override
		public int getPosition() {
			return position;
		}
	}
	
	/**
	 * Import the given file as an image, detect nuclei and
	 * display the image with annotated nuclear outlines
	 * @param imageFile
	 */
	@Override
	protected void importAndDisplayImage(File imageFile){

		try {
			this.setStatusLoading();
			this.setLoadingLabelText("Looking for nuclei in "+imageFile.getAbsolutePath());
			table.setModel(createEmptyTableModel(rows, cols));
			
			for(int col=0; col<cols; col++){
	        	table.getColumnModel().getColumn(col).setCellRenderer(new IconCellRenderer());
	        }
			
			NucleusProberWorker worker = new NucleusProberWorker(imageFile, 
					options, 
					NucleusImageType.DETECTED_OBJECTS, 
					table.getModel());
			
			worker.setSmallIconSize(new Dimension(500, table.getRowHeight()-30));
			
			worker.addPropertyChangeListener(this);
			progressBar.setVisible(true);
			worker.execute();


		} catch (Exception e) { // end try
			error("Error in image processing", e);
		} 
	}

}
