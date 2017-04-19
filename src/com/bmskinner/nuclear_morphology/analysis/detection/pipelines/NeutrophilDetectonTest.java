package com.bmskinner.nuclear_morphology.analysis.detection.pipelines;

import java.awt.Color;
import java.awt.Rectangle;
import java.io.File;
import java.util.ArrayList;
import java.util.EventObject;
import java.util.Iterator;
import java.util.List;

import com.bmskinner.nuclear_morphology.analysis.detection.Detector;
import com.bmskinner.nuclear_morphology.analysis.detection.GenericDetector;
import com.bmskinner.nuclear_morphology.analysis.detection.StatsMap;
import com.bmskinner.nuclear_morphology.analysis.image.ColourMeasurometer;
import com.bmskinner.nuclear_morphology.analysis.image.ImageAnnotator;
import com.bmskinner.nuclear_morphology.analysis.image.ImageConverter;
import com.bmskinner.nuclear_morphology.analysis.image.ImageFilterer;
import com.bmskinner.nuclear_morphology.components.ComponentFactory;
import com.bmskinner.nuclear_morphology.components.ComponentFactory.ComponentCreationException;
import com.bmskinner.nuclear_morphology.components.CytoplasmFactory;
import com.bmskinner.nuclear_morphology.components.DefaultCell;
import com.bmskinner.nuclear_morphology.components.ICell;
import com.bmskinner.nuclear_morphology.components.ICytoplasm;
import com.bmskinner.nuclear_morphology.components.generic.IPoint;
import com.bmskinner.nuclear_morphology.components.nuclear.Lobe;
import com.bmskinner.nuclear_morphology.components.nuclear.LobeFactory;
import com.bmskinner.nuclear_morphology.components.nuclear.NucleusType;
import com.bmskinner.nuclear_morphology.components.nuclei.LobedNucleus;
import com.bmskinner.nuclear_morphology.components.nuclei.Nucleus;
import com.bmskinner.nuclear_morphology.components.nuclei.NucleusFactory;
import com.bmskinner.nuclear_morphology.components.options.IAnalysisOptions;
import com.bmskinner.nuclear_morphology.components.options.IDetectionOptions;
import com.bmskinner.nuclear_morphology.components.options.IDetectionOptions.IDetectionSubOptions;
import com.bmskinner.nuclear_morphology.components.options.IDetectionOptions.IDetectionSubOptions.IPreprocessingOptions;
import com.bmskinner.nuclear_morphology.components.options.MissingOptionException;
import com.bmskinner.nuclear_morphology.components.stats.PlottableStatistic;
import com.bmskinner.nuclear_morphology.io.ImageImporter;
import com.bmskinner.nuclear_morphology.io.ImageImporter.ImageImportException;
import com.bmskinner.nuclear_morphology.logging.Loggable;
import com.bmskinner.nuclear_morphology.stats.Quartile;

import ij.ImagePlus;
import ij.ImageStack;
import ij.gui.Roi;
import ij.process.ImageProcessor;
import inra.ijpb.binary.BinaryImages;
import inra.ijpb.morphology.MinimaAndMaxima3D;
import inra.ijpb.morphology.Morphology;
import inra.ijpb.morphology.Strel;
import inra.ijpb.morphology.strel.DiskStrel;
import inra.ijpb.watershed.Watershed;

/**
 * Test for the new neutrophil pipeline and image prober model
 * @author ben
 * @since 1.13.5
 *
 */
public class NeutrophilDetectonTest implements Loggable	 {
	
	/**
	 * Interface implemented by probers to be notified that a new image is available
	 * for display
	 * @author ben
	 * @since 1.13.5
	 *
	 */
	public interface DetectionEventListener{
		void detectionEventReceived(DetectionEvent e);
	}
	
	/**
	 * Fired when an image has been processed to detect components.
	 * @author ben
	 * @since 1.13.5
	 *
	 */
	public class DetectionEvent extends EventObject {
		
		final private ImageProcessor ip;
		final private String message;
		public DetectionEvent(final Object source, final ImageProcessor ip, final String message){
			super(source);
			this.ip = ip;
			this.message = message;
		}
		
		public ImageProcessor getProcessor(){
			return ip;
		}
		
		public String getMessage(){
			return message;
		}
		
	}
	
	final private IAnalysisOptions options;
	final private boolean useProber;
	final private List<Object> listeners = new ArrayList<>();
	final private ComponentFactory<ICytoplasm> cytoFactory = new CytoplasmFactory();
	final private ComponentFactory<Nucleus>    nuclFactory = new NucleusFactory(NucleusType.NEUTROPHIL);
	final private ComponentFactory<Lobe>       lobeFactory = new LobeFactory();
	
	/**
	 * Construct with an analysis options
	 * @param op
	 * @param prober should prober events be fired
	 */
	public NeutrophilDetectonTest(IAnalysisOptions op, boolean prober){
		options = op;
		useProber = prober;
	}
	
	/*
	 * EVENT HANDLING
	 * 
	 */
	
	public void addDetectionEventListener(DetectionEventListener l){
		listeners.add(l);
	}
	
	public void removeDetectionEventListener(DetectionEventListener l){
		listeners.remove(l);
	}
	
	public void fireDetectionEvent(ImageProcessor ip, String message){
		for(Object l : listeners){
			((DetectionEventListener)l).detectionEventReceived(new DetectionEvent(this, ip, message));
		}
	}
	
	/*
	 * PIPELINE HANDLING
	 * 
	 */
	
	
	public List<ICell> run() throws Exception{
		
		List<ICell> list = findInFolder(options.getDetectionOptions(IAnalysisOptions.CYTOPLASM).getFolder());
		return list;
		
	}
	
	public List<ICell> findInFolder(File folder) throws ImageImportException, ComponentCreationException{
		List<ICell> list = new ArrayList<>();
		
		for(File f : folder.listFiles()){
			if(! f.isDirectory()){
				list.addAll(findInImage(f));
			}
		}
		
		return list;
	}
	
	
	public List<ICell> findInImage(File imageFile) throws ImageImportException, ComponentCreationException{
		List<ICell> list = new ArrayList<>();
		
		
		
		List<ICytoplasm> cyto = detectCytoplasm(imageFile);
		List<Nucleus> nucl    = detectNucleus(imageFile, cyto);
		
		for(ICytoplasm c : cyto){
			
			ICell cell = new DefaultCell(c);
			
			Iterator<Nucleus> it = nucl.iterator();
			
			while(it.hasNext()){
				Nucleus n = it.next();
				if(c.containsOriginalPoint(n.getOriginalCentreOfMass())){
					cell.addNucleus(n);
					it.remove();
				}
			}
			
			if(cell.hasNucleus()){
				list.add(cell);
			}
			
		}
		
		
		if(useProber){
			// Display the final image
			ImageProcessor ip =  new ImageImporter(imageFile).importToColorProcessor();
			ImageAnnotator an = new ImageAnnotator(ip);
			
			for(ICell cell : list){
				an.annotateCellBorders(cell);
			}
			fireDetectionEvent(an.toProcessor(), "Detected");
			
		}
		
		return list;
	}
	
	private List<ICytoplasm> detectCytoplasm(File imageFile) throws ComponentCreationException, ImageImportException{
		List<ICytoplasm> list = new ArrayList<>();
		ImageProcessor ip =  new ImageImporter(imageFile).importToColorProcessor();
		ImageProcessor ann = ip.duplicate();
		
		try {
			
			IDetectionOptions main = options.getDetectionOptions(IAnalysisOptions.CYTOPLASM);
			IPreprocessingOptions op = (IPreprocessingOptions) main.getSubOptions(IDetectionSubOptions.BACKGROUND_OPTIONS);
			

			int minHue = op.getMinHue();
			int maxHue = op.getMaxHue();
			int minSat = op.getMinSaturation();
			int maxSat = op.getMaxSaturation();
			int minBri = op.getMinBrightness();
			int maxBri = op.getMaxBrightness();

			ip = new ImageFilterer(ip)
					.colorThreshold(minHue, maxHue, minSat, maxSat, minBri, maxBri)
					.convertToByteProcessor()
					.toProcessor();

			ip.invert();

			GenericDetector gd = new GenericDetector();
			gd.setCirc(main.getMinCirc(), main.getMaxCirc());
			gd.setSize(main.getMinSize(), main.getMaxSize());
			gd.setThreshold(main.getThreshold());
			List<Roi> rois = gd.getRois(ip);
			
			for(int i=0; i<rois.size(); i++){
				
				Roi roi = rois.get(i);
				ICytoplasm cyto = makeCytoplasm(roi, imageFile, main, ip, i, gd);
				list.add(cyto);
				
			}
				

		} catch (MissingOptionException e) {
			error("Missing option", e);
		}
		if(useProber){
//			fireDetectionEvent(ip, "Cytoplasm");
			
			ImageAnnotator an = new ImageAnnotator(ann);
			for(ICytoplasm c : list){
				an.annotateBorder(c, Color.CYAN);
			}
			fireDetectionEvent(an.toProcessor(), "Detected cytoplasm");
			
		}
		return list;
	}
	
		
	private ICytoplasm makeCytoplasm(Roi roi, File f, IDetectionOptions options, ImageProcessor ip, int objectNumber, Detector gd) throws ComponentCreationException {
		
		  // measure the area, density etc within the nucleus
		StatsMap values   = gd.measure(roi, ip);

		  // save the position of the roi, for later use
		int xbase = (int) roi.getXBase();
		int ybase = (int) roi.getYBase();

		Rectangle bounds = roi.getBounds();

		int[] originalPosition = {xbase, ybase, (int) bounds.getWidth(), (int) bounds.getHeight() };

		// create a Nucleus from the roi
		IPoint centreOfMass = IPoint.makeNew(values.get("XM"), values.get("YM"));

		ICytoplasm result = cytoFactory.buildInstance(roi, f, options.getChannel(), originalPosition, centreOfMass); 

		// Move the nucleus xbase and ybase to 0,0 coordinates for charting
		IPoint offsetCoM = IPoint.makeNew( centreOfMass.getX() - xbase, centreOfMass.getY() - ybase  );


		result.moveCentreOfMass(offsetCoM);

		result.setStatistic(PlottableStatistic.AREA,      values.get("Area"));
		result.setStatistic(PlottableStatistic.MAX_FERET, values.get("Feret"));
		result.setStatistic(PlottableStatistic.PERIMETER, values.get("Perim"));

		result.setScale(options.getScale());

		return result;
	}
	
	private List<Nucleus> detectNucleus(File imageFile, List<ICytoplasm> mask) throws ComponentCreationException, ImageImportException{
		List<Nucleus> list = new ArrayList<>();
		ImageProcessor ip =  new ImageImporter(imageFile).importToColorProcessor();
		ImageProcessor ann = ip.duplicate();
		try {
			int topHatRadius = 20;
			int thresholdMin = 20;
			int thresholdMax = 255;
			
			IDetectionOptions main = options.getDetectionOptions(IAnalysisOptions.NUCLEUS);

			ImageProcessor test = new ImageConverter(ip)
					.convertToByteProcessor()
					.toProcessor();
			Strel strel = DiskStrel.fromRadius(topHatRadius); // the structuring element used for black top-hat
			ip = Morphology.blackTopHat(test, strel);
			
//			if(useProber){
//				fireDetectionEvent(ip.duplicate(), "Nucleus top hat");
//			}
//			
			// Most remaining cytoplasm is weak, can can be thresholded away 
			
			ImageProcessor bin = ip.duplicate();
			ip.setMinAndMax(thresholdMin, thresholdMax);
			bin.threshold(thresholdMin);
//			ip = BinaryImages.binarize( ip ); // Converts a grayscale 2D or 3D image into a binary image by setting non-zero elements to 255.

//			if(useProber){
//				fireDetectionEvent(bin.duplicate(), "Nucleus binarized");
//			}
//			ip.invert();

			GenericDetector gd = new GenericDetector();
			gd.setCirc(main.getMinCirc(), main.getMaxCirc());
			gd.setSize(main.getMinSize(), main.getMaxSize());
			gd.setThreshold(main.getThreshold());
			List<Roi> rois = gd.getRois(bin);
			
			for(int i=0; i<rois.size(); i++){
				
				Roi roi = rois.get(i);
				Nucleus n = makeNucleus(roi, imageFile, main, bin, i, gd);
				list.add(n);
				
			}
				

		} catch (MissingOptionException e) {
			
		}
		
		if(useProber){
//			fireDetectionEvent(ip.duplicate(), "Nucleus");
			
			ImageAnnotator an = new ImageAnnotator(ann);
			for(Nucleus c : list){
				an.annotateBorder(c, Color.ORANGE);
			}
			fireDetectionEvent(an.toProcessor(), "Detected nucleus");
		}
		
		
		detectLobes(ip, list);
//		detectLobesViaSubtraction(ip, list);
//		if(useProber){
//			fireDetectionEvent(ip.duplicate(), "Lobes");
//		}
		return list;
	}
	
	
	private Nucleus makeNucleus(Roi roi, File f, IDetectionOptions options, ImageProcessor ip, int objectNumber, Detector gd) throws ComponentCreationException {
		
		  // measure the area, density etc within the nucleus
		StatsMap values   = gd.measure(roi, ip);

		  // save the position of the roi, for later use
		int xbase = (int) roi.getXBase();
		int ybase = (int) roi.getYBase();

		Rectangle bounds = roi.getBounds();

		int[] originalPosition = {xbase, ybase, (int) bounds.getWidth(), (int) bounds.getHeight() };

		// create a Nucleus from the roi
		IPoint centreOfMass = IPoint.makeNew(values.get("XM"), values.get("YM"));

		Nucleus result = nuclFactory.buildInstance(roi, f, options.getChannel(), originalPosition, centreOfMass); 

		// Move the nucleus xbase and ybase to 0,0 coordinates for charting
		IPoint offsetCoM = IPoint.makeNew( centreOfMass.getX() - xbase, centreOfMass.getY() - ybase  );


		result.moveCentreOfMass(offsetCoM);

		result.setStatistic(PlottableStatistic.AREA,      values.get("Area"));
		result.setStatistic(PlottableStatistic.MAX_FERET, values.get("Feret"));
		result.setStatistic(PlottableStatistic.PERIMETER, values.get("Perim"));

		result.setScale(options.getScale());
		result.initialise(0.05);

		return result;
	}
	
	private void detectLobes(ImageProcessor ip, List<Nucleus> list) throws ComponentCreationException{

		boolean calculateDams  = true;
		int gradientRadius     = 2;
		int connectivity       = 6;
		int bitDepth           = 32;
		int dynamic            = 10; // the minimal difference between a minima and its boundary
		int minArea            = 5;
		int maxArea            = 3000;
		
		ImagePlus imp = new ImagePlus("Input image", ip);


		// Copy the process used in the MorphoLbJ MorphologicalSegmentation plugin
					
		ImageStack image = new ImageStack(ip.getWidth(), ip.getHeight());
		image.addSlice(ip);
		
		Strel strel = Strel.Shape.SQUARE.fromRadius( gradientRadius );
		
		/*
		 * Computes the morphological gradient of the input image. 
		 * The morphological gradient is obtained from the difference
		 * of image dilation and image erosion computed with the 
		 * same structuring element.
		 * 
    	 *	This effectively gets the borders of the nuclei in the image
    	 * TODO: use the nuclei themselves as a mask 
		 */
		ImageProcessor gradient = Morphology.gradient( image.getProcessor( 1 ), strel );
		
		if(useProber){
			fireDetectionEvent(gradient.duplicate(), "Gradient");
		}
		
		// Make a stack for use in the minima detection
		image = new ImageStack(image.getWidth(), image.getHeight());
		image.addSlice(gradient); 
		
		/*
		 * Computes the extended minima in grayscale  image, keeping minima 
		 * with the specified dynamic, and using the specified connectivity.
		 *     dynamic - the difference between maxima and maxima boundary
    	 *	  conn - the connectivity for maxima, that should be either 6 or 26
    	 *
    	 *	This finds minima in the image where the difference between the minimum and maximum is greater than 
    	 * a specified value. Big enough differences. Finds the regions inside the gradient circles - the interior of the nuclei
    	 *
		 */
		ImageStack regionalMinima = MinimaAndMaxima3D.extendedMinima( image, dynamic, connectivity );

		if(useProber){
			fireDetectionEvent(regionalMinima.getProcessor(1).duplicate(), "Regional minima");
		}
		
		/*
		 * Computes the labels in the binary 2D or 3D image contained 
		 * in the given ImagePlus, and computes the maximum label 
		 * to set up the display range of the resulting ImagePlus.
		 * 
		 * Used to restrict the watershed to only the bounds of the nuclei
		 */
		ImageStack labeledMinima = BinaryImages.componentsLabeling( regionalMinima, connectivity, bitDepth );
		ImagePlus min = new ImagePlus("", labeledMinima.getProcessor(1));
		
		if(useProber){
			fireDetectionEvent(min.getProcessor().duplicate(), "Labelled minima");
		}
		
		/*
		 * Compute watershed with markers with a
		 *  binary mask to restrict the regions of application
		 */
		ImagePlus resultStack = Watershed.computeWatershed( imp, min, 
				connectivity, calculateDams );
		
		if(useProber){
			fireDetectionEvent(resultStack.getProcessor().duplicate(), "Watershed");
		}

		final ImagePlus lines = BinaryImages.binarize( resultStack );
		
		if(useProber){
			fireDetectionEvent(lines.getProcessor().duplicate(), "Binarized");
		}
		
		ImageProcessor lp = lines.getProcessor();
		lp.invert();
		
		if(useProber){
			fireDetectionEvent(lp.duplicate(), "Lines");
		}
		
		// Now take the watershed image, and detect the distinct lobes
		
		ImageFilterer ft = new ImageFilterer(lp);
		ImageProcessor ws = ft.dilate(1).toProcessor(); // separate the lobes from each other
//		ImageProcessor ws = ft.toProcessor();
		
		GenericDetector gd = new GenericDetector();
		gd.setIncludeHoles(false);
		gd.setSize(minArea, maxArea);
		List<Roi> rois  = gd.getRois(ws);
		
		for(Roi roi : rois){
			for(Nucleus n : list){
				LobedNucleus l = (LobedNucleus) n;
				StatsMap m = gd.measure(roi, ip);
				int x = m.get(GenericDetector.COM_X).intValue();
				int y = m.get(GenericDetector.COM_Y).intValue();
				IPoint com = IPoint.makeNew(x, y);
				if(n.containsOriginalPoint(com)){
					// Now adjust the roi base to match the source image
					IPoint base = IPoint.makeNew(roi.getXBase(), roi.getYBase());
					
					Rectangle bounds = roi.getBounds();
					
					roi.setLocation(base.getXAsInt(), base.getYAsInt());

					int[] originalPosition = {base.getXAsInt(), 
							base.getYAsInt(), 
							(int) bounds.getWidth(), 
							(int) bounds.getHeight() };
					
					Lobe lobe = lobeFactory.buildInstance(roi, l.getSourceFile(), 0, originalPosition, com);
					
					l.addLobe( lobe); //TODO makethe channel useful
				}
			}
		}
		
		
	}
	
	/**
	 * Subtract the median intenity of each nucleus from a greyscale image to find lobes
	 * as islands
	 * @param ip
	 * @param list
	 * @throws ComponentCreationException 
	 */
	private void detectLobesViaSubtraction(ImageProcessor ip, List<Nucleus> list) throws ComponentCreationException{
		
		if(useProber){
			fireDetectionEvent(ip.duplicate(), "Lobe input");
		}
		
		
		int minArea            = 5;
		int maxArea            = 3000;
		
		GenericDetector gd = new GenericDetector();
		gd.setIncludeHoles(false);
		gd.setSize(minArea, maxArea);
		
		int i=0;
		for(Nucleus n : list){
//			if(i>0){
//				continue;
//			}
//			i++;
			int avg = ColourMeasurometer.calculateIntensity(n, ip, Quartile.LOWER_QUARTILE);
			
			ImageProcessor test = ip.duplicate();
			
			test.subtract(avg);
//			if(useProber){
//				fireDetectionEvent(test.duplicate(), "Subtracted "+avg);
//			}
			
			test.threshold(avg);// binarise
//			if(useProber){
//				fireDetectionEvent(test.duplicate(), "Binarised "+avg);
//			}
			
			List<Roi> rois  = gd.getRois(test);
						
			LobedNucleus l = (LobedNucleus) n;
			
			for(Roi roi : rois){
				StatsMap m = gd.measure(roi, ip);
				int x = m.get(GenericDetector.COM_X).intValue();
				int y = m.get(GenericDetector.COM_Y).intValue();
				IPoint com = IPoint.makeNew(x, y);
				if(n.containsOriginalPoint(com)){
					// Now adjust the roi base to match the source image
					IPoint base = IPoint.makeNew(roi.getXBase(), roi.getYBase());

					Rectangle bounds = roi.getBounds();

					roi.setLocation(base.getXAsInt(), base.getYAsInt());

					int[] originalPosition = {base.getXAsInt(), 
							base.getYAsInt(), 
							(int) bounds.getWidth(), 
							(int) bounds.getHeight() };

					Lobe lobe = lobeFactory.buildInstance(roi, l.getSourceFile(), 0, originalPosition, com);

					l.addLobe( lobe); //TODO makethe channel useful
				}
			}
		}
	}
}
