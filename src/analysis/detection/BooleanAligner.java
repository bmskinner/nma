package analysis.detection;

import logging.Loggable;

public class BooleanAligner implements Loggable {
	
	Mask reference;
	
	public static final int X = 0;
	public static final int Y = 1;

	/** 
	 * The max number of pixels to move in any direction. A value of 50
	 * would mean a range of -50 to 50 x and -50 to 50 y
	 */
	private int range = 50; 
	
	public BooleanAligner(Mask reference){
		this.reference = reference;
	}
	
	/**
	 * Perform the alignment of a test mask to the reference within
	 * the aligner.
	 * @param test the mask to align
	 * @return an array with the best alignment - { x offset, y offset, score }
	 */
	public int[] align(Mask test){
		
		if(test.getWidth()!=reference.getWidth() || test.getHeight()!=reference.getHeight()){
			throw new IllegalArgumentException("Test mask does not match reference mask");
		}
	
	    int bestScore = compare(reference, test);
	    int bestX = 0;
	    int bestY = 0;

	    int interval = 5; // must be smaller than nuclear size to ensure some hits

	    // Run a coarse alignment
	    int[] coarse = compareInterval(test, bestX, bestY, range, interval, bestScore);
	    
	    bestX = coarse[0];
	    bestY = coarse[1];
	    bestScore = coarse[2];
	    
	    // Run a fine alignment
	    int[] fine = compareInterval(test, bestX, bestY, interval, 1, bestScore);
	    
	    bestX = fine[0];
	    bestY = fine[1];
	    bestScore = fine[2];

	    fine("Images aligned at: x: "+bestX+" y:"+bestY);
	    
	    int[] result = { bestX, bestY, bestScore };
	    return result;
	  }
	
	private int[] compareInterval(Mask test, int startX, int startY, int range, int step, int bestScore){
		
		int bestX = 0;
	    int bestY = 0;
	    	    
		for(int x=startX-(range-1); x<startX+range;x+=step){
			for(int y=startY-(range-1); y<startY+range; y+=step){

				int score = compare(reference, test);
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
	


	/**
	 * Calculate the overlap between two masks
	 * @param array1 the first mask
	 * @param array2
	 * @return
	 */
	public int compare(Mask array1, Mask array2){
		  int height = array1.getHeight();
		  int width  = array1.getWidth();

		  int score = 0;
		  
		  boolean[][] added = array1.and(array2).toArray();

		  for(int y=0; y<height; y++){
			  for(int x=0; x<width; x++){
				  
				  if(added[y][x]){ // if black
					  score++;
				  }
			  }
		  }
		  return score;
	  }
	  


}
