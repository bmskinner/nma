package analysis.detection;

import java.util.logging.Level;

import logging.Loggable;

public class BooleanAligner implements Loggable {
	
	boolean[][] reference;
	
	public static final int X = 0;
	public static final int Y = 1;

//	private int xOffset = 0;
//	private int yOffset = 0;

	/** 
	 * The max number of pixels to move in any direction. A value of 50
	 * would mean a range of -50 to 50 x and -50 to 50 y
	 */
	private int range = 50; 
	
	public BooleanAligner(boolean[][] reference){
		this.reference = reference;
	}
	
	public int[] align(boolean[][] test){
		
		if(test.length!=reference.length){
			throw new IllegalArgumentException("Test array does not match reference array");
		}
	
	    int bestScore = compare(reference, test);
	    int bestX = 0;
	    int bestY = 0;

	    int interval = 5; // must be smaller than nuclear size to ensure some hits

	    System.out.println("Coarse");
	    int[] coarse = compareInterval(test, bestX, bestY, range, interval, bestScore);
	    
	    bestX = coarse[0];
	    bestY = coarse[1];
	    bestScore = coarse[2];
	    
	    System.out.println("Fine");
	    int[] fine = compareInterval(test, bestX, bestY, interval, 1, bestScore);
	    
	    bestX = fine[0];
	    bestY = fine[1];
	    bestScore = fine[2];

	    log(Level.FINE, "Images aligned at: x: "+bestX+" y:"+bestY);
	    
	    int[] result = { bestX, bestY, bestScore };
	    return result;
	  }
	
	private int[] compareInterval(boolean[][] test, int startX, int startY, int range, int step, int bestScore){
		
		int bestX = 0;
	    int bestY = 0;
	    	    
		for(int x=startX-(range-1); x<startX+range;x+=step){
			for(int y=startY-(range-1); y<startY+range; y+=step){

				boolean[][] offsetImage = offset(test, y, x);
				int score = compare(reference, offsetImage);
				System.out.println(x+"  "+y+"  Score: "+score);
				if(score>bestScore){
					bestScore = score;
					bestX = x;
					bestY = y;
				}
			}
		}
		
		int[] result = { bestX, bestY, bestScore };
		return result;
	}
	
	private void zeroArray(boolean[][] array){
		int height = array.length;
		int width  = array[0].length;
		for(int y=0; y<height; y++){
			for(int x=0; x<width; x++){

				array[y][x] = false;
			}
		}

	}

	public boolean[][] offset(boolean[][] array, int xOffset, int yOffset){

		int height = array.length;
		int width  = array[0].length;
		boolean[][] result = new boolean[height][width];
		zeroArray(result); 

		for(int y=0; y<height; y++){
			
			if(y-yOffset<0 || y-yOffset >= height){
				continue;
			}
			
			for(int x=0; x<width; x++){

				if(x-xOffset<0 || x-xOffset >= width){
					continue;
				}
				result[y][x] = array[y-yOffset][x-xOffset];
			}
		}
		return result;
	}

	public int compare(boolean[][] array1, boolean[][] array2){
		  int height = array1.length;
		  int width  = array1[0].length;

		  int score = 0;
		  
		  boolean[][] added = and(array1, array2);

		  for(int y=0; y<height; y++){
			  for(int x=0; x<width; x++){
				  
				  if(added[y][x]){ // if black
					  score++;
				  }
			  }
		  }
		  return score;
	  }
	  
	  public boolean[][] and(boolean[][] array1, boolean[][] array2){
		  int height = array1.length;
		  int width  = array1[0].length;
		  		  
		  boolean[][] result = new boolean[height][width];

		  for(int y=0; y<height; y++){
			  for(int x=0; x<width; x++){
				  result[y][x] = array1[y][x]  &&  array2[y][x];
			  }
		  }
		  return result;
	  }

}
