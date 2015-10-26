package analysis;

import ij.ImagePlus;
import ij.process.ByteProcessor;
import ij.process.ImageProcessor;

public class LocalBinaryPatterner {
	
	public ImagePlus run(ImagePlus image){
		
		ImageProcessor ip = image.getProcessor();
		
		ImagePlus result = getLBP(ip);
		return result;
		
	}
	
	private ImagePlus getLBP(ImageProcessor ip){
		
		int[][] input = ip.getIntArray();
		byte[] array = new byte[ip.getWidth()*ip.getHeight()];
		
		int count = 0;
		
		// Start from index 1, to avoid image edges
		for(int x=1; x< ip.getWidth()-1; x++){
			for(int y=1; y< ip.getHeight()-1; y++){
				Byte b = getPixelLBP(x, y, input);
				array[count] = b.byteValue();
				count++;
			}
		}
		ByteProcessor bp = new ByteProcessor(ip.getWidth(), ip.getHeight(), array);
		return new ImagePlus(null, bp);
	}
	
	private Byte getPixelLBP(int x, int y, int[][] input){
		
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

		return new Byte(byteString);
	}
	
	private int testValue(int i, int j, int[][] array, int pixel){
		int test = array[i][j];
		if(test >= pixel){
			return 1;
		} else {
			return 0;
		}
	}

}
