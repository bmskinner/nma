package analysis;

import org.junit.Test;

import analysis.detection.BooleanAligner;

public class BooleanAlignerTest {
	
	/**
	 * Create a 3x4 array of pixels
	 * @return
	 */
	private boolean[][] createBlankArray(int w, int h){
		boolean[][] pixels = new boolean[h][w];
		
		for( int x=0; x<w; x++){
			
			for(int y=0; y<h; y++){

				pixels[y][x] = false;
			}
		}
		return pixels;
	}
	
	private boolean[][] createReferenceArray(int w, int h){
		boolean[][] array = createBlankArray(w, h);
		
		/*
		 * Make a vertical stripe in the middle,
		 * assuming a 3x3
		 */
		array[0][1] = true;
		array[1][1] = true;
		array[2][1] = true;
		array[0][2] = true;

		return array;
		
	}
	
	private boolean[][] createTestArray(int w, int h){
		boolean[][] array = createBlankArray(w, h);
		
		/*
		 * Make a vertical stripe in the middle
		 * 
		 */
		
		array[1][1] = true;
		array[2][1] = true;
		array[3][1] = true;
		array[1][2] = true;

		return array;
		
	}
		
	private void printArray(boolean[][] array){
		
		int h  = array.length;
		int w = array[0].length;
		
		for( int x=0; x<w; x++){
			
			for(int y=0; y<h; y++){

				if(array[y][x]){
					System.out.print("1 ");
				} else {
					System.out.print("0 ");
				}
				
			}
			System.out.print("\n");
		}
	}
	
	@Test
	public void referenceRenders(){
		 boolean[][] reference = createReferenceArray(3, 4);
		System.out.println("\n-------------\nReference:");
		printArray(reference);
	}
	
	@Test
	public void testRenders(){
		 boolean[][] test = createTestArray(3, 4);
		System.out.println("\n-------------\nTest:");
		printArray(test);
	}
	
	@Test
	public void alignCompletes(){
		boolean[][] reference = createReferenceArray(3, 4);
		 boolean[][] test = createTestArray(3, 4);
		 
		BooleanAligner aln = new BooleanAligner(reference);
		 
		System.out.println("\n-------------\nAnd:");
		boolean[][] and = aln.and(reference, test);
		printArray(and);
		
		System.out.println("\n-------------\nCompare:");
		int score =aln.compare(reference, test);
		System.out.println("Score: "+score+"\n");
		 
		int[] result = aln.align(test);
		
		
		System.out.println("\n-------------\nOffset by x 1 y 0");
		boolean[][] offset = aln.offset(test, 0, 1);
		printArray(offset);
		
			
		System.out.println("\n-------------\nAligned:");
		boolean[][] aligned = aln.offset(test, result[1], result[0]);
		printArray(aligned);
		
		
		
		
		
		System.out.println("\n-------------\n x offset:"+result[0]
				+"\n y offset:"+result[1]
				+"\n Score:"+result[2]
				+"\n-------------\n");
		
		
	}
	

	
	
//	{
//		int width  = array1.length;
//		  int height = array1[0].length;
//
//		  int score = 0;
//		  
//		  boolean[][] added = and(array1, array2);
//
//		  for(int y=0; y<height; y++){
//			  for(int x=0; x<width; x++){
//				  
//				  if(added[y][x]){ // if black
//					  score++;
//				  }
//			  }
//		  }
//	}
}
