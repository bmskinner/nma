package cell.analysis;

import java.io.File;

import cell.Cell;
import cell.SpermCell;
import no.analysis.AnalysisDataset;
import no.imports.ImageImporter;
import no.nuclei.Nucleus;
import ij.ImageStack;
import components.SpermTail;



/**
 * This class is to test ideas for detecting sperm tails stained with
 * anti-tubulin
 */
public class TubulinTailDetector {
	
	public static boolean run(AnalysisDataset dataset, File folder, int channel){
		
		boolean result = true;
		
		for(Nucleus n : dataset.getCollection().getNuclei()){
			
			// get the image in the folder with the same name as the
			// nucleus source image
			File image = new File(folder + n.getImageName());
			SpermTail tail = detectTail(image, channel, n);
			
			Cell cell = new SpermCell();
			cell.setNucleus(n);
			cell.setTail(tail);
			
			
		}
		
		return result;
	}
	
	
	/**
	 * Given a file, try to find a sperm tail in the given channel.
	 * The nucleus is used to orient the tail, and separate from other
	 * tails in the image
	 * @param f the file
	 * @param channel the channel with the tail signal
	 * @param n the nucleus to which the tail should attach
	 * @return a SpermTail object
	 */
	private static SpermTail detectTail(File f, int channel, Nucleus n){
		
		// import image with tubulin in  channel
		//	Select file - must match dimensions of existing nucleus file if added secondarily.
		ImageStack stack = ImageImporter.importImage(f, null);
		
		if( checkDimensions(stack, n)){
			// edge / threshold to find tubulin stain
			
			// create a binary mask of region
			
			// skeletonise
			
			// detect particles area 0-Infinity
			
			// check each roi: print outline
			// this is the step that tells us what the roi model looks like
			
			// convert area roi to line roi if needed
			
			// create line selection
			
			// provide line roi to store in SpermTail object
			
			// straighten / measure
		}
		
		
		
		
		
		SpermTail tail = new SpermTail(f, channel);
		return tail;
	}
	
	/**
	 * Check that the dimensions of the input image are the same as the 
	 * image used to detect the nucleus. 
	 * @param stack the imported stack
	 * @param n the source nucleus
	 * @return an ok
	 */
	private static boolean checkDimensions(ImageStack stack, Nucleus n){
		
		File baseFile = n.getSourceFile();
		ImageStack baseStack = ImageImporter.importImage(baseFile, null);
		
		boolean ok = true;
		if(stack.getHeight() != baseStack.getHeight()){
			ok = false;
		}
		if(stack.getWidth() != baseStack.getWidth()){
			ok = false;
		}
		return ok;
	}
	

}
