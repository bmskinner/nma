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
package com.bmskinner.nuclear_morphology.utility;

public class Constants {
	
	/**
	 * The fields for setting the version. Version will be stored in AnalysisDatasets.
	 * Backwards compatability should be maintained between bugfix increments, but is not
	 * guaranteed between revision or major version increments.
	 */
	public static final int VERSION_MAJOR     = 1;
	public static final int VERSION_MINOR     = 13;
	public static final int VERSION_REVISION  = 4;
	
	// nmd = Nuclear Morphology Dataset
	public static final String SAVE_FILE_EXTENSION = ".nmd";
	public static final String LOG_FILE_EXTENSION = ".log";
	public static final String TAB_FILE_EXTENSION = ".txt";
	public static final String LOC_FILE_EXTENSION = "cell"; // locations of cells (in a tsv format)
	public static final String BAK_FILE_EXTENSION = ".bak"; // backup files made in conversions
	public static final String WRK_FILE_EXTENSION = ".wrk"; // workspace files for multiple nmds
	
	public static final String SEGMENT_PREFIX = "Seg_";
	
	
	public static final String CLUSTER_GROUP_PREFIX = "Group";
	
		
		// imported images - stack positions
		public static final int COUNTERSTAIN = 1; // ImageStack slices are numbered from 1; first slice is blue
		public static final int FIRST_SIGNAL_CHANNEL = 2; // ImageStack slices are numbered from 1; first slice is blue
		
		
		// statistical testing
		public static final double TEN_PERCENT_SIGNIFICANCE_LEVEL = 0.1;
		public static final double FIVE_PERCENT_SIGNIFICANCE_LEVEL = 0.05;
		public static final double ONE_PERCENT_SIGNIFICANCE_LEVEL = 0.01;
				
		// The prefix to use when exporting images
		public static final String IMAGE_PREFIX = "export.";

		// Images with these prefixes are ignored by the image importer
		public static final String[] PREFIXES_TO_IGNORE = { IMAGE_PREFIX, "composite", "plot", "._"};

		// The file types that the program will try to open
		public static final String[] IMPORTABLE_FILE_TYPES = {".tif", ".tiff", ".jpg"};
						
}
