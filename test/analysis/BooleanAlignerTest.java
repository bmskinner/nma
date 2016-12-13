package analysis;

import org.junit.Test;

import com.bmskinner.nuclear_morphology.analysis.detection.BooleanAligner;
import com.bmskinner.nuclear_morphology.analysis.detection.BooleanMask;
import com.bmskinner.nuclear_morphology.analysis.detection.Mask;

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
	
	private Mask createReferenceArray(int w, int h){
		boolean[][] array = createBlankArray(w, h);
		
		/*
		 * Make a vertical stripe in the middle,
		 * assuming a 3x3
		 */
		array[0][1] = true;
		array[1][1] = true;
		array[2][1] = true;
		array[0][2] = true;

		return new BooleanMask(array);
		
	}
	
	private Mask createTestArray(int w, int h){
		boolean[][] array = createBlankArray(w, h);
		
		/*
		 * Make a vertical stripe in the middle
		 * 
		 */
		
		array[1][1] = true;
		array[2][1] = true;
		array[3][1] = true;
		array[1][2] = true;

		return new BooleanMask(array);
		
	}
		
	private void printArray(Mask mask){
		boolean[][] array = mask.toArray();
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
		Mask reference = createReferenceArray(3, 4);
		System.out.println("\n-------------\nReference:");
		printArray(reference);
	}
	
	@Test
	public void testRenders(){
		Mask test = createTestArray(3, 4);
		System.out.println("\n-------------\nTest:");
		printArray(test);
	}
	
	@Test
	public void alignCompletes(){
		Mask reference = createReferenceArray(3, 4);
		Mask test      = createTestArray(3, 4);
		 
		BooleanAligner aln = new BooleanAligner(reference);
		 
		System.out.println("\n-------------\nAnd:");
		Mask and = reference.and(test);
		printArray(and);
		
		System.out.println("\n-------------\nCompare:");
		int score = aln.compare(reference, test);
		System.out.println("Score: "+score+"\n");
		 
		int[] result = aln.align(test);
		
		
		System.out.println("\n-------------\nOffset by x 1 y 0");
		Mask offset = test.offset( 0, 1);
		printArray(offset);
		
			
		System.out.println("\n-------------\nAligned:");
		Mask aligned = test.offset(result[1], result[0]);
		printArray(aligned);
		
		
		
		
		
		System.out.println("\n-------------\n x offset:"+result[0]
				+"\n y offset:"+result[1]
				+"\n Score:"+result[2]
				+"\n-------------\n");
		
		
	}

}
