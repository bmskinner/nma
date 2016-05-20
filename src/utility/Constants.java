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
package utility;

import java.util.Calendar;

public class Constants {
	
	/**
	 * The fields for setting the version. Version will be stored in AnalysisDatasets.
	 * Backwards compatability should be maintained between bugfix increments, but is not
	 * guaranteed between revision or major version increments.
	 */
	public static final int VERSION_MAJOR     = 1;
	public static final int VERSION_MINOR     = 12;
	public static final int VERSION_REVISION  = 2;
	
	// nmd = Nuclear Morphology Dataset
	public static final String SAVE_FILE_EXTENSION = ".nmd";
	public static final String LOG_FILE_EXTENSION = ".log";
	public static final String TAB_FILE_EXTENSION = ".txt";
	public static final String LOC_FILE_EXTENSION = "cell"; // locations of cells (in a tsv format)
	
	public static final String SEGMENT_PREFIX = "Seg_";
	
	// The date and time that this was compiled
	public static final String BUILD = Calendar.getInstance().getTime().toString();
	
	
	

		// RGB colour channels
		public static final int RGB_RED = 0;
		public static final int RGB_GREEN = 1;
		public static final int RGB_BLUE = 2;	
		
		// imported images - stack positions
		public static final int COUNTERSTAIN = 1; // ImageStack slices are numbered from 1; first slice is blue
		public static final int FIRST_SIGNAL_CHANNEL = 2; // ImageStack slices are numbered from 1; first slice is blue
		
		
		// statistical testing
		public static final double TEN_PERCENT_SIGNIFICANCE_LEVEL = 0.1;
		public static final double FIVE_PERCENT_SIGNIFICANCE_LEVEL = 0.05;
		public static final double ONE_PERCENT_SIGNIFICANCE_LEVEL = 0.01;
		
		public static final double MEDIAN = 50;
		public static final double LOWER_QUARTILE = 25;
		public static final double UPPER_QUARTILE = 75;
		
		// The prefix to use when exporting images
		public static final String IMAGE_PREFIX = "export.";

		// Images with these prefixes are ignored by the image importer
		public static final String[] PREFIXES_TO_IGNORE = { IMAGE_PREFIX, "composite", "plot", "._"};

		// The file types that the program will try to open
		public static final String[] IMPORTABLE_FILE_TYPES = {".tif", ".tiff", ".jpg"};
				
				
		/**
		 * Given an RGB channel, get the ImageStack stack for internal use
		 * @param channel the channel
		 * @return the stack
		 */
		public static int rgbToStack(int channel){
			
			if(channel < 0){
				throw new IllegalArgumentException("Channel cannot be less than 0");
			}
			
			int stackNumber = channel==Constants.RGB_RED 
					? Constants.FIRST_SIGNAL_CHANNEL
					: channel==Constants.RGB_GREEN
						? Constants.FIRST_SIGNAL_CHANNEL+1
						: Constants.COUNTERSTAIN;
			return stackNumber;
		}
		
		/**
		 * Given a channel integer, return the name of the channel.
		 * Handles red (0), green (1) and blue(2). Other ints will 
		 * return a null string.
		 * @param channel
		 * @return
		 */
		public static String channelIntToName(int channel){
			if(channel == RGB_RED){
				return "Red";
			}
			if(channel == RGB_GREEN){
				return "Green";
			}
			if(channel == RGB_BLUE){
				return "Blue";
			}
			return null;
		}
		
		public enum CellComponent {
			NUCLEUS 		("Nucleus"			), 
			SPERM_TAIL 		("Sperm tail"		), 
			MITOCHONDRION 	("Mitochondrion"	), 
			NUCLEAR_SIGNAL	("Nuclear signal"	), 
			ACROSOME 		("Acrosome"			);
			
		    private final String asString;   
		    
		    CellComponent(String value) {
		        this.asString = value;
			}
		    
		    public String toString(){
		    	return this.asString;
		    }
		}
				
		public enum Cell {
			GENERIC ("Cell"), 
			ROUND 	("Round cell"),
			SPERM	("Sperm cell");
			
			private final String asString;
			
			Cell(String string) {
		        this.asString = string;
			}
		    
		    public String toString(){
		    	return this.asString;
		    }
		}
		

		/**
		 *  SwingWorker states for progress bars (can only pass ints).
		 *  This allows the bar to switch appropriately
		 *
		 */
		public enum Progress {
			FINISHED (-1),   // signal cleanup of progress bar
			ERROR 	 (-2),	// signal error occurred in analysis
			COOLDOWN (-3);	// signal switch to indeterminate bar
			
			private final int code;
			
			Progress(int code) {
		        this.code = code;
			}
		    
		    public int code(){
		    	return this.code;
		    }
		}

}
