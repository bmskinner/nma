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

import java.util.HashMap;
import java.util.Map;

public class Constants {
	
	/**
	 * The fields for setting the version. Version will be stored in AnalysisDatasets.
	 * Backwards compatability should be maintained between bugfix increments, but is not
	 * guaranteed between revision or major version increments.
	 */
	public static final int VERSION_MAJOR    = 1;
	public static final int VERSION_REVISION = 11;
	public static final int VERSION_BUGFIX   = 0;
	
	// nmd = Nuclear Morphology Dataset
	public static final String SAVE_FILE_EXTENSION = ".nmd";
	public static final String LOG_FILE_EXTENSION = ".log";
	

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
		
		// The prefix to use when exporting images
		public static final String IMAGE_PREFIX = "export.";

		// Images with these prefixes are ignored by the image importer
		public static final String[] PREFIXES_TO_IGNORE = { IMAGE_PREFIX, "composite", "plot"};

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
		    
		    public String string(){
		    	return this.asString;
		    }
		}
		
		/**
		 * The types of nuclei we are able to analyse,
		 * with the reference and orientation points to be used.
		 * The reference point is the best identifiable point on the
		 * nucleus when aligning profiles. The orientation point is the point
		 * placed at the bottom when rotating a consensus nucleus.
		 *
		 */
		public enum Nucleus {
			ROUND 		 ("Round nucleus"		 , "head", "tail"), 
			ASYMMETRIC 	 ("Asymmetric nucleus"	 , "head", "tail"),
			RODENT_SPERM ("Rodent sperm nucleus" , "tip" , "tail"), 
			PIG_SPERM 	 ("Pig sperm nucleus"	 , "head", "tail");
			
		    private final String asString;   
		    private final String referencePoint;
		    private final String orientationPoint;
		    
		    private final Map<BorderTag, String> map = new HashMap<BorderTag, String>();
		    
		    Nucleus(String string, String referencePoint, String orientationPoint) {
		        this.asString = string;
		        this.referencePoint = referencePoint;
		        this.orientationPoint = orientationPoint;
		        this.map.put(BorderTag.REFERENCE_POINT, referencePoint);
		        this.map.put(BorderTag.ORIENTATION_POINT, orientationPoint);
			}
		    
		    public String string(){
		    	return this.asString;
		    }
		    
		    
		    /**
		     * Get the name of the given border tag, if present
		     * @param point
		     * @return
		     */
		    public String getPoint(BorderTag point){
		    	return this.map.get(point);
		    }
		    
		    public String orientationPoint(){
		    	return this.orientationPoint;
		    }
		    
		    public String referencePoint(){
		    	return this.referencePoint;
		    }
		}
		
		// use in charting
		public enum BorderTag {
			ORIENTATION_POINT ("Orientation point"),
			REFERENCE_POINT ("Reference point");
			
			private final String name;
			
			BorderTag(String name){
				this.name = name;
			}
			
			public String toString(){
				return this.name;
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
		    
		    public String string(){
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
