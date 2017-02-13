package com.bmskinner.nuclear_morphology.analysis.detection.pipelines;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

import com.bmskinner.nuclear_morphology.analysis.detection.Detector;
import com.bmskinner.nuclear_morphology.analysis.image.ImageConverter;
import com.bmskinner.nuclear_morphology.analysis.image.ImageFilterer;
import com.bmskinner.nuclear_morphology.components.ComponentFactory.ComponentCreationException;
import com.bmskinner.nuclear_morphology.components.options.ICannyOptions;
import com.bmskinner.nuclear_morphology.components.options.IDetectionOptions;
import com.bmskinner.nuclear_morphology.components.options.IMutableCannyOptions;
import com.bmskinner.nuclear_morphology.components.options.IMutableDetectionOptions;
import com.bmskinner.nuclear_morphology.components.options.MissingOptionException;
import com.bmskinner.nuclear_morphology.components.options.IDetectionOptions.IDetectionSubOptions;
import com.bmskinner.nuclear_morphology.components.options.PreprocessingOptions;
import com.bmskinner.nuclear_morphology.io.ImageImporter;
import com.bmskinner.nuclear_morphology.io.ImageImporter.ImageImportException;
import com.bmskinner.nuclear_morphology.logging.Loggable;

import ij.ImageStack;
import ij.gui.Roi;
import ij.plugin.filter.BackgroundSubtracter;
import ij.process.ImageProcessor;

/**
 * The abstract pipeline implementing common methods. Extending classes
 * detect the various nuclei and cells for the program by chaining the 
 * image filtering and analysis methods. Since we want to be able to
 * sample intermediate results for e.g. the ImageProber, the pipeline
 * returns itself after each operation.
 * @author ben
 * @since 1.13.4
 *
 */
public abstract class DetectionPipeline<E> extends Detector implements Loggable {
	protected File file;
	protected ImageProcessor ip;
	protected IDetectionOptions options;
	protected double proportion;
		
	public DetectionPipeline(IDetectionOptions op, File imageFile, double prop) throws ImageImportException {
		file = imageFile;
		options = op;
		proportion = prop;
		ImageStack st =  new ImageImporter(file).importImage();
		
		int stackNumber = ImageImporter.rgbToStack(options.getChannel());
		
		ip =  st.getProcessor(stackNumber);
		
	}
	
	/**
	 * Get a copy of the active processor
	 * @return
	 */
	public ImageProcessor getProcessor(){
		return ip.duplicate();
	}
	
	/**
	 * Get a copy of the active processor, and invert the copy
	 * @return an inverted copy of the processor
	 */
	public ImageProcessor getInvertedProcessor(){
		ImageProcessor result = ip.duplicate();
		result.invert();
		return result;
	}
	
	/**
	 * Add a black border to the image
	 * @param border
	 * @return
	 */
	public DetectionPipeline<E> addBorder(int border){
		
		try {
			ICannyOptions canny = options.getCannyOptions();
			
			ImageConverter conv = new ImageConverter(ip);
			
			conv = conv.convertToGreyscale();
			
			if(canny.isAddBorder()){	
				conv = conv.addBorder(border);
			}
			ip = conv.invert().toProcessor();
			
		} catch (MissingOptionException e) {
			warn("Missing canny options");
		}
		
		return this;
	}
	
	/**
	 * Remove size and circularity filters from the detection options.
	 * @return
	 */
	public DetectionPipeline<E> openSizeParameters(){
		IMutableDetectionOptions nucleusOptions = options.duplicate().unlock();
		nucleusOptions.setMinSize(50);
		nucleusOptions.setMaxSize(ip.getWidth()*ip.getHeight());// dimensions of the image
		nucleusOptions.setMinCirc(0);
		nucleusOptions.setMaxCirc(1);
		try {
			IMutableCannyOptions canny = options.getCannyOptions().unlock();
			canny.setAddBorder(false);
		} catch (MissingOptionException e) {
			warn("Missing canny options");
		}
		
		options = nucleusOptions;
		return this;
	}
	
	/**
	 * Run a Kuwahara filter with the kernel size in the options.
	 * If the options specify the Kuwahara filter is disabled, this has no effect.
	 * @return
	 */
	public DetectionPipeline<E> kuwaharaFilter(){
		try {
			ICannyOptions canny = options.getCannyOptions();
			
			if(canny.isUseKuwahara()){
				ip = new ImageFilterer(ip)
						.runKuwaharaFiltering( canny.getKuwaharaKernel())
						.toProcessor();
			}
		} catch (MissingOptionException e) {
			warn("Missing canny options");
		}
		
		return this;
	}
	
	/**
	 * Run a chromocentre flattening with the threshold in the options.
	 * If the options specify the flattening is disabled, this has no effect.
	 * @return
	 */
	public DetectionPipeline<E> flatten(){
		
		try {
			ICannyOptions canny = options.getCannyOptions();
			
			if(canny.isUseFlattenImage()){
				ip = new ImageFilterer(ip)
						.squashChromocentres( canny.getFlattenThreshold())
						.toProcessor();
			}
		} catch (MissingOptionException e) {
			warn("Missing canny options");
		}
		return this;
	}
	
	/**
	 * Run a raising with the flattening threshold in the options.
	 * If the options specify the flattening is disabled, this has no effect.
	 * @return
	 */
	public DetectionPipeline<E> raise(){
		
		try {
			ICannyOptions canny = options.getCannyOptions();
			
			if(canny.isUseFlattenImage()){
				ip = new ImageFilterer(ip)
						.raise( canny.getFlattenThreshold())
						.toProcessor();
			}
		} catch (MissingOptionException e) {
			warn("Missing canny options");
		}
		return this;
	}
	
	/**
	 * Run a Canny edge detection with the specified options.
	 * If the options specify the edge detecion is disabled, this has no effect.
	 * @return
	 */
	public DetectionPipeline<E> edgeDetect(){
		
		try {
			ICannyOptions canny = options.getCannyOptions();
			
			if(canny.isUseCanny()){
				ip = new ImageFilterer(ip)
						.runEdgeDetector( canny )
						.toProcessor();
			}
		} catch (MissingOptionException e) {
			warn("Missing canny options");
		}
		return this;
	}
	
	/**
	 * Run theImageJ rolling ball background removal with the radius specified in the options.
	 * @return
	 */
	public DetectionPipeline<E> subtractBackground(){
		
			IDetectionSubOptions bkg = options.getSubOptions(IDetectionSubOptions.BACKGROUND_OPTIONS);
			
//			if(bkg.getBoolean(PreprocessingOptions.USE_ROLLING_BALL)){
				BackgroundSubtracter bg = new BackgroundSubtracter();
				boolean presmooth = false;
				boolean correctCorners = false;
				boolean parabaloid = false;
				boolean lightBackground = true;
				boolean createBackground = false;
				double radius = 200;
				
				bg.rollingBallBackground(ip, radius, createBackground, lightBackground, parabaloid, presmooth, correctCorners);

		return this;
	}
	
	/**
	 * Run a gap closing with the closing radius specified in the options.
	 * If the options specify edge detecion is disabled, this has no effect.
	 * @return
	 */
	public DetectionPipeline<E> gapClose(){
		
		try {
			ICannyOptions canny = options.getCannyOptions();
			
			if(canny.isUseCanny()){
				ip = new ImageFilterer(ip)
						.morphologyClose( canny.getClosingObjectRadius() )
						.toProcessor();
			}
		} catch (MissingOptionException e) {
			warn("Missing canny options");
		}
		return this;
	}
	
	/**
	 * Run a gap closing with the closing radius specified in the options.
	 * If the options specify edge detecion is disabled, this has no effect.
	 * @return
	 */
	public DetectionPipeline<E> houghCircles(){
		warn("Hough not implemented");
		return this;
	}
	
	/**
	 * Find cells in the current image processor based on the size and shape options 
	 * @return
	 */
	public List<E> findInImage() {
		List<E> result = new ArrayList<E>(0);
		
		// get polygon rois of correct size

		List<Roi> roiList = getROIs(Detector.CLOSED_OBJECTS);

		if(roiList.isEmpty()){

			fine("No usable nuclei in image "+file.getAbsolutePath());

			return result;
		}

		finer("Image has "+roiList.size()+" ROIs");
		for(int i=0; i<roiList.size(); i++){

			Roi roi = roiList.get(i);

			finest( "Acquiring nucleus "+i+" in "+file.getAbsolutePath());

			E cell;
			try {
				cell = makeComponent(roi, i); // get the profile data back for the nucleus
			}catch(ComponentCreationException e){
				stack("Cannot create nucleus from ROI "+i, e);
				continue;
			}
			result.add(cell);
			finer("Cell created");

		} 
		fine("Returning list of "+result.size()+" cells");
		return result;
	}
	
	/**
	 * Create the component type from a detected roi
	 * @param roi the roi
	 * @param objectNumber the number of this object in the image
	 * @return
	 * @throws ComponentCreationException
	 */
	protected abstract E makeComponent(Roi roi, int objectNumber) 
			throws ComponentCreationException;
	
	/**
	 * Detect objects within the current image processor.
	 *
	 * @param closed should the detector get only closed polygons, or open lines
	 * @return the detected ROIs
	 */
	protected List<Roi> getROIs(int closed){
		finer("Detecting ROIs");
		
		List<Roi> roiList = new ArrayList<Roi>();
		
		setThreshold(options.getThreshold());
		setCirc(options.getMinCirc(), options.getMaxCirc());
		
		double minSize = closed==Detector.CLOSED_OBJECTS ? options.getMinSize() : 0;
		
		setSize(minSize, options.getMaxSize() );
		

		try{
			
			roiList = detectRois(ip);
		
		} catch(Exception e){
			stack("Error in nucleus detection", e);
		}
		
		finer("Detected ROIs");
		return roiList;
	}
	
	
}
