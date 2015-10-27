package analysis;

import ij.ImagePlus;
import ij.process.FloatProcessor;
import ij.process.ImageProcessor;

public class LocalBinaryPatterner {
	
	/**
	 * Run the LBP on a copy the given image, and return 
	 * the image processor
	 * @param image
	 * @return
	 */
	public static ImageProcessor run(ImagePlus image){		
		return getLBP(image.getProcessor());
	}
	
	/**
	 * Run the LBP on a copy of the given processor, and return 
	 * the image processor
	 * @param image
	 * @return
	 */
	public static ImageProcessor run(ImageProcessor image){
		return getLBP(image);
	}
	
	private static ImageProcessor getLBP(ImageProcessor ip){
		
		int[][] input = ip.getIntArray();
		int[][] array = new int[ip.getWidth()][ip.getHeight()];
		
//		// zero the array
		for(int x=1; x< ip.getWidth()-1; x++){
			
			for(int y=1; y< ip.getHeight()-1; y++){
				
				array[x][y] = 0;

			}
		}
		
		// Start from index 1, to avoid image edges
		for(int x=1; x< ip.getWidth()-1; x++){
			
			for(int y=1; y< ip.getHeight()-1; y++){
				
				int b = getPixelLBP(x, y, input);
				array[x][y] = b;

			}
		}
		FloatProcessor bp = new FloatProcessor( array );
		ImagePlus img = new ImagePlus("", bp);
		img.show();
		return bp;
	}
	
	private static Integer getPixelLBP(int x, int y, int[][] input){
		
		String byteString = "";
		int pixel = input[x][y];
		
		/*
		 *  1 2 3
		 *  8   4
		 *  7 6 5
		 */
		
		// Top row
		int j = y-1;
		for(int i=x-1;i<=x+1; i++){
			byteString += testValue(i, j, input, pixel);
		}
		
		// Middle row right
		int i = x+1;
		j = y;
		byteString += testValue(i, j, input, pixel);
		
//		Bottom row - work backwards
		j = y+1;
		for(i=x+1;i>=x-1; i--){
			byteString += testValue(i, j, input, pixel);
		}
		
		// Middle row left
		i = x-1;
		j = y;
		byteString += testValue(i, j, input, pixel);
		
		int value = Integer.parseInt(byteString, 2); // radix 2 for binary
		return value;
	}
	
	private static int testValue(int i, int j, int[][] array, int pixel){
		int test = array[i][j];
		if(test >= pixel){
			return 1;
		} else {
			return 0;
		}
	}

}
