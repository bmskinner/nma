package cell.analysis;

import java.awt.Color;
import java.awt.image.BufferedImage;
import java.io.File;
import java.util.ArrayList;
import java.util.List;

import Skeletonize3D_.Skeletonize3D_;
import utility.CannyEdgeDetector;
import utility.Constants;
import utility.Logger;
import utility.Utils;
import cell.Cell;
import mmorpho.MorphoProcessor;
import mmorpho.StructureElement;
import no.analysis.AnalysisDataset;
import no.analysis.Detector;
import no.components.AnalysisOptions;
import no.components.AnalysisOptions.CannyOptions;
import no.imports.ImageImporter;
import no.nuclei.Nucleus;
import ij.IJ;
import ij.ImagePlus;
import ij.ImageStack;
import ij.gui.Roi;
import ij.process.BinaryProcessor;
import ij.process.ByteProcessor;
import ij.process.FloatPolygon;
import ij.process.ImageProcessor;
import components.SpermTail;



/**
 * This class is to test ideas for detecting sperm tails stained with
 * anti-tubulin
 */
public class TubulinTailDetector {
	
	private static Logger logger;
	
	/**
	 * Run the analysis on a dataset with nuclei defined. Take tubulin-stained
	 * images from a folder, and analyse the given channel
	 * @param dataset the cells to add a tail to
	 * @param folder the images with the tubulin stain
	 * @param channel the channel with the stain
	 * @return success or failure
	 */
	public static boolean run(AnalysisDataset dataset, File folder, int channel){

		boolean result = true;
		logger = new Logger(dataset.getDebugFile(), "TubulinTailDetector");
		logger.log("Beginning tail detection", Logger.INFO);

		try{

			for(Cell c : dataset.getCollection().getCells()){

				Nucleus n = c.getNucleus();
				logger.log("Looking for tails associated with nucleus "+n.getImageName()+"-"+n.getNucleusNumber(), Logger.DEBUG);
				// get the image in the folder with the same name as the
				// nucleus source image
				File imageFile = new File(folder + File.separator + n.getImageName());
				logger.log("Tail in: "+imageFile.getAbsolutePath(), Logger.DEBUG);
				SpermTail tail = null;
				try{
					tail = detectTail(imageFile, channel, n, dataset.getAnalysisOptions());
				} catch(Exception e){
					logger.log("Error detectng tail: "+e.getMessage(), Logger.ERROR);
					for(StackTraceElement el : e.getStackTrace()){
						logger.log(el.toString(), Logger.STACK);
					}
				}

				if(tail!=null){
					c.setTail(tail);
				} else {
					logger.log("No tail found for cell");
				}
			}
		} catch (Exception e){
			logger.log("Error in tubulin tail detection: "+e.getMessage(), Logger.ERROR);
			for(StackTraceElement el : e.getStackTrace()){
				logger.log(el.toString(), Logger.STACK);
			}
			return false;
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
	private static SpermTail detectTail(File f, int channel, Nucleus n, AnalysisOptions options){
		
		SpermTail tail = null;
		
		// import image with tubulin in  channel
		ImageStack stack = ImageImporter.importImage(f, logger.getLogfile());
		
//		ImagePlus searchImage = new ImagePlus(null, stack);
//		searchImage.show();
		
		int stackNumber = channel == Constants.RGB_BLUE 
						? Constants.COUNTERSTAIN
						: channel == Constants.RGB_RED
							? Constants.FIRST_SIGNAL_CHANNEL
							: Constants.FIRST_SIGNAL_CHANNEL+1;
		
		
		// file must match dimensions of existing nucleus image file
		if( checkDimensions(stack, n)){
			// edge / threshold to find tubulin stain
			ImageStack edges = runEdgeDetector(stack, options, stackNumber);
			
			// get objects found by edge detector
			List<Roi> borderRois = getROIs(edges, true, 1);
			logger.log("Found "+borderRois.size()+" potential tails in image", Logger.INFO);
			
			// fill rois with white
			ByteProcessor bp = (ByteProcessor) edges.getProcessor(1);
			bp.setColor(Color.WHITE);
			for(Roi r : borderRois){
				logger.log("Filling roi", Logger.DEBUG);
				bp.fill(r);
			}
			ImagePlus bpImage = new ImagePlus(n.getImageName()+"-"+n.getNucleusNumber()+" byte image", bp);
//			bpImage.show();
			
			
			BinaryProcessor binp = new BinaryProcessor((ByteProcessor) bp.duplicate());
			ImagePlus binpImage = new ImagePlus(n.getImageName()+"-"+n.getNucleusNumber()+" binary image", binp);
//			binpImage.show();
			// skeletonise the filled rois
			
			ByteProcessor binp2 = (ByteProcessor) bp.duplicate();
			BinaryProcessor skp = new BinaryProcessor((ByteProcessor) binp2);
			
			Skeletonize3D_ skelly = new Skeletonize3D_();
			skelly.setup("", binpImage);
			skelly.run(skp);
			
			logger.log("Skeletonized the image", Logger.DEBUG);

			
			ImageStack skeletonStack = ImageStack.create(binp.getWidth(), binp.getHeight(), 0, 8);
			skeletonStack.addSlice("skeleton", binp);
			skeletonStack.deleteSlice(1); // remove the blank first slice
			
			
			// detect particles area 0-Infinity
			List<Roi> skeletons = getROIs(skeletonStack, false, 1);
			
			
			// Get rois of the correct size
			List<Roi> usableSkeletons = new ArrayList<Roi>(0);

			for(Roi r : skeletons){
				if(r.getLength()>1000){
					usableSkeletons.add(r);
				}
			}
			
			// print the roi details to the log
			logger.log("Found "+usableSkeletons.size()+" potential tails in image", Logger.INFO);
			
			// ensure that the positions of the nucleus are corrected to
			// match the original image
			FloatPolygon nucleusOutline = Utils.createOriginalPolygon(n);

			
			for(Roi skeleton : usableSkeletons){
				
				boolean tailOverlap = false;
								
				logger.log("Assessing skeleton: Length : "+skeleton.getLength(), Logger.DEBUG);

				float[] xpoints = skeleton.getFloatPolygon().xpoints;
				float[] ypoints = skeleton.getFloatPolygon().ypoints;
							
				// objects that overlap with the nucleus are kept
				for(int i=0;i<xpoints.length;i++){
										
					if(nucleusOutline.contains(xpoints[i], ypoints[i])){
						tailOverlap = true;
					}
				}
				
				
				if(tailOverlap){
					// if a tail overlaps the nucleus, get the correstponding border roi
					Roi tailBorder = null;
					for(Roi border : borderRois){
						if (border.getFloatPolygon().contains(xpoints[0], ypoints[0])
								&& border.getFloatPolygon().contains(xpoints[xpoints.length-1], ypoints[ypoints.length-1])){
							tailBorder = border;
						}
					}
					if(tailBorder!=null){
						tail = new SpermTail(f, channel, skeleton, tailBorder);
						logger.log("Found matching tail", Logger.DEBUG);
					}
				}
			}
			
		} else {
			logger.log("Dimensions of image do not match");
		}

		return tail;
	}
	
	/**
	 * Check that the dimensions of the input image are the same as the 
	 * image used to detect the nucleus. 
	 * @param stack the imported stack
	 * @param n the source nucleus
	 * @return an ok
	 */
	private static boolean checkDimensions(ImageStack stack, Nucleus n ){
		
		File baseFile = n.getSourceFile();
		logger.log("Nucleus in "+baseFile.getAbsolutePath(), Logger.DEBUG);
		ImageStack baseStack = ImageImporter.importImage(baseFile, logger.getLogfile());
		
		boolean ok = true;
		if(stack.getHeight() != baseStack.getHeight()){
			ok = false;
			logger.log("Fail on height check", Logger.DEBUG);
		}
		if(stack.getWidth() != baseStack.getWidth()){
			logger.log("Fail on width check", Logger.DEBUG);
			ok = false;
		}
		return ok;
	}
	
	/**
	 * Use Canny edge detection to produce an image with potential edges highlighted
	 * for the detector
	 * @param image the stack to process
	 * @return a stack with edges highlighted
	 */
	private static ImageStack runEdgeDetector(ImageStack image, AnalysisOptions options, int stackNumber){

		//		bi.show();
		ImageStack searchStack = null;
		try {
			// using canny detector
			CannyOptions tailCannyOptions = options.getTailCannyOptions();

			logger.log("Creating edge detector", Logger.DEBUG);
			CannyEdgeDetector canny = new CannyEdgeDetector();
			canny.setSourceImage(image.getProcessor(stackNumber).getBufferedImage());
			canny.setLowThreshold( tailCannyOptions.getLowThreshold() );
			canny.setHighThreshold( tailCannyOptions.getHighThreshold());
			canny.setGaussianKernelRadius(tailCannyOptions.getKernelRadius());
			canny.setGaussianKernelWidth(tailCannyOptions.getKernelWidth());

			canny.process();
			BufferedImage edges = canny.getEdgesImage();
			ImagePlus searchImage = new ImagePlus(null, edges);


			// add morphological closing
			ByteProcessor bp = searchImage.getProcessor().convertToByteProcessor();

			morphologyClose( bp, tailCannyOptions);
			ImagePlus bi= new ImagePlus(null, bp);
			searchStack = ImageImporter.convert(bi);

			bi.close();
//			searchImage.show();
			searchImage.close();

			logger.log("Edge detection complete", Logger.DEBUG);
		} catch (Exception e) {
			logger.log("Error in dege detection: "+e.getMessage(), Logger.ERROR);
			for(StackTraceElement el : e.getStackTrace()){
				logger.log(el.toString(), Logger.STACK);
			}
		}
		return searchStack;
	}
	
	private static void morphologyClose(ImageProcessor ip, CannyOptions options){
		try {
			
			int shift=1;
			int radius = options.getClosingObjectRadius();
			int[] offset = {0,0};
			int eltype = 0; //circle
			logger.log("Closing objects with circle of radius "+radius, Logger.DEBUG);
			
			StructureElement se = new StructureElement(eltype,  shift,  radius, offset);
//			IJ.log("Made se");
			MorphoProcessor mp = new MorphoProcessor(se);
//			IJ.log("Made mp");
			mp.fclose(ip);
			logger.log("Objects closed", Logger.DEBUG);
//			IJ.log("Closed");
		} catch (Exception e) {
			IJ.log("Error in closing: "+e.getMessage());
			for(StackTraceElement el : e.getStackTrace()){
				IJ.log(el.toString());
			}
		}
		
	}
	
	/**
	 * Detects nuclei within the given image.
	 *
	 * @param image the ImagePlus to be analysed
	 * @param closed should the detector get only closed polygons, or open lines
	 * @param channel the stach of the image to search
	 * @return the Map linking an roi to its stats
	 */
	private static List<Roi> getROIs(ImageStack image, boolean closed, int channel){
		Detector detector = new Detector();
		detector.setMaxSize(100000);
		
		if(closed){
			detector.setMinSize(1000); // get polygon rois
		} else {
			detector.setMinSize(0); // get line rois
		}
		detector.setMinCirc(0);
		detector.setMaxCirc(0.5);
		detector.setThreshold(30);
		detector.setChannel(channel);
		try{
			detector.run(image);
		} catch(Exception e){
			logger.log("Error in tail detection: "+e.getMessage(), Logger.ERROR);
		}
		return detector.getRoiList();
	}
	

}
