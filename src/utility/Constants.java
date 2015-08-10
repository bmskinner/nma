package utility;

public class Constants {
	
	/**
	 * The fields for setting the version. Version will be stored in AnalysisDatasets.
	 * Backwards compatability should be maintained between bugfix increments, but is not
	 * guaranteed between revision or major version increments.
	 */
	public static final int VERSION_MAJOR    = 1;
	public static final int VERSION_REVISION = 9;
	public static final int VERSION_BUGFIX   = 2;
	

		// RGB colour channels
		public static final int RGB_RED = 0;
		public static final int RGB_GREEN = 1;
		public static final int RGB_BLUE = 2;
		// thsese should no longer be used:
//		public static final int NOT_RED_CHANNEL  = 3;
//		public static final int NOT_GREEN_CHANNEL  = 4;
		
		
		// imported images - stack positions
		public static final int COUNTERSTAIN = 1; // ImageStack slices are numbered from 1; first slice is blue
		public static final int FIRST_SIGNAL_CHANNEL = 2; // ImageStack slices are numbered from 1; first slice is blue
		
		
		// statistical testing
		public static final double FIVE_PERCENT_SIGNIFICANCE_LEVEL = 0.05;
		public static final double ONE_PERCENT_SIGNIFICANCE_LEVEL = 0.01;
		
				
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
		    
		    Nucleus(String string, String referencePoint, String orientationPoint) {
		        this.asString = string;
		        this.referencePoint = referencePoint;
		        this.orientationPoint = orientationPoint;
			}
		    
		    public String string(){
		    	return this.asString;
		    }
		    
		    public String orientationPoint(){
		    	return this.orientationPoint;
		    }
		    
		    public String referencePoint(){
		    	return this.referencePoint;
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
