package com.bmskinner.nuclear_morphology.analysis;

import static org.junit.Assert.fail;
import ij.IJ;
import ij.ImagePlus;
import ij.process.ByteProcessor;
import ij.process.ImageProcessor;

import org.junit.Test;

import com.bmskinner.nuclear_morphology.analysis.image.ImageFilterer;

public class ImageFiltererTest {
	
	
	/**
	 * Create a 3x3 array of pixels at a given intensity
	 * @return
	 */
	private int[][] createPixelArray(int intensity){
		int[][] pixels = new int[3][3];
		
		for(int w =0; w<3; w++){
			
			for( int h=0; h<3; h++){
				pixels[w][h]  = intensity;
			}
		}
		return pixels;
	}
		
	/**
	 * Make a byteprocessor from a pixel array
	 * @param pixels
	 * @return
	 */
	private ByteProcessor createProcessor(int[][] pixels){
		ByteProcessor bp = new ByteProcessor(3, 3);
		bp.setIntArray(pixels);
		return bp;
	}
	
	/**
	 * Create a 3x3 processor with zero intensity
	 * @return
	 */
	private ByteProcessor makeTestEmptyProcessor(){
		int[][] pixels = createPixelArray(0);
		return createProcessor(pixels);
	}
	
	/**
	 * Create a 3x3 processor with max intensity
	 * @return
	 */
	private ByteProcessor makeTestFilledProcessor(){
		int[][] pixels = createPixelArray(255);
		return createProcessor(pixels);
	}
	
	/**
	 * Create a 3x3 processor with zero intensity in the middle vertical
	 * and filled sides
	 * 1 0 1
	 * 1 0 1
	 * 1 0 1
	 * @return
	 */
	private ByteProcessor makeTestVerticalSidesProcessor(){
		int[][] pixels = createPixelArray(255);
		
		pixels[0][1] = 0;
		pixels[1][1] = 0;
		pixels[2][1] = 0;

		return createProcessor(pixels);
	}
	
	/**
	 * Create a 3x3 processor with zero intensity in the middle horizontal
	 * and filled sides
	 * 1 1 1
	 * 0 0 0
	 * 1 1 1
	 * @return
	 */
	private ByteProcessor makeTestHorizontalSidesProcessor(){
		int[][] pixels = createPixelArray(255);
		
		pixels[1][0] = 0;
		pixels[1][1] = 0;
		pixels[1][2] = 0;

		return createProcessor(pixels);
	}
	
	/**
	 * Create a 3x3 processor with diagonal hole
	 * and filled sides
	 * 1 1 1
	 * 1 0 0
	 * 0 0 1
	 * @return
	 */
	private ByteProcessor makeTestDiagonalProcessor(){
		int[][] pixels = createPixelArray(255);
		
		pixels[1][1] = 0;
		pixels[1][2] = 0;
		pixels[2][0] = 0;
		pixels[2][1] = 0;

		return createProcessor(pixels);
	}
	
	/**
	 * Create a 3x3 processor with diagonal hole
	 * and filled sides
	 * 1 0 1
	 * 0 0 0
	 * 1 0 1
	 * @return
	 */
	private ByteProcessor makeTestSquareProcessor(){
		int[][] pixels = createPixelArray(0);
		
		pixels[0][0] = 255;
		pixels[0][2] = 255;
		pixels[2][0] = 255;
		pixels[2][2] = 255;

		return createProcessor(pixels);
	}
	
	@Test
	public void bridgePixelAcrossSquare(){
		ByteProcessor bp = makeTestSquareProcessor();
//		System.out.println("\n-------------\nSquare:");
		bridgePixels(bp);
	}
	
	@Test
	public void bridgePixelAcrossVerticals(){
		ByteProcessor bp = makeTestVerticalSidesProcessor();
//		System.out.println("\n-------------\nVertical line:");
		bridgePixels(bp);
	}
	
	@Test
	public void bridgePixelAcrossHorizontals(){
		ByteProcessor bp = makeTestHorizontalSidesProcessor();
//		System.out.println("\n-------------\nHorizontal line:");
		bridgePixels(bp);
	}
	
	@Test
	public void bridgePixelAcrossEmpty(){
		ByteProcessor bp = makeTestEmptyProcessor();
//		System.out.println("\n-------------\nEmpty:");
		bridgePixels(bp);
	}
	
	@Test
	public void bridgePixelAcrossFilled(){
		ByteProcessor bp = makeTestFilledProcessor();
//		System.out.println("\n-------------\nFilled:");
		bridgePixels(bp);
	}
	
	@Test
	public void bridgePixelAcrossDiagonal(){
		ByteProcessor bp = makeTestDiagonalProcessor();
//		System.out.println("\n-------------\nDiagonal:");
		bridgePixels(bp);
	}
	
	/**
	 * Bridge the pixels in the given processor
	 * @param bp
	 */
	private void bridgePixels(ByteProcessor bp){
		
//		System.out.println("Input image:");
		printPixelArray(bp);
		
//		System.out.println("\nKernel at 1, 1:");
		int[][] kernel = new ImageFilterer(bp).getKernel(bp.getIntArray(), 1, 1);
//		printPixelArray(kernel);
		
//		System.out.println("\nBridged image:");
		ByteProcessor bridged = (ByteProcessor) new ImageFilterer(bp).bridgePixelGaps(3).toProcessor();
//		printPixelArray(bridged);

		
	}
		
	private void printPixelArray(ImageProcessor ip){
		for(int x = 0; x<ip.getWidth(); x++){
			
			for( int y=0; y<ip.getHeight(); y++){


				System.out.print(pad(ip.get(x, y))+" ");

			}
			System.out.print("\n");
		}
		
	}
	
	private void printPixelArray(int[][] array){
		for(int x = 0; x<3; x++){
			
			for( int y=0; y<3; y++){


				System.out.print(pad(array[x][y])+" ");

			}
			System.out.print("\n");
		}
		
	}
	
	public static String pad(int s) {
		if(s>=100){
			return String.valueOf(s);
		}
		if(s>=10){
			return " "+String.valueOf(s);
		}
		return "  "+String.valueOf(s);
	}

}
