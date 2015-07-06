package utility;

public class Constants {

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
		
		
		// structural components of the cell
		public static final int COMPONENT_NUCLEUS 		= 0;
		public static final int COMPONENT_SPERM_TAIL	= 1;
		public static final int COMPONENT_MITOCHONDRION = 2;
		
		
		
		
}
