package com.bmskinner.nuclear_morphology.analysis.detection.pipelines;

import java.awt.Color;
import java.awt.Rectangle;
import java.io.File;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

import com.bmskinner.nuclear_morphology.analysis.detection.Detector;
import com.bmskinner.nuclear_morphology.analysis.detection.GenericDetector;
import com.bmskinner.nuclear_morphology.analysis.detection.StatsMap;
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

import ij.ImagePlus;
import ij.ImageStack;
import ij.gui.Roi;
import ij.process.ImageProcessor;
import inra.ijpb.binary.BinaryImages;
import inra.ijpb.binary.ChamferWeights;
import inra.ijpb.binary.distmap.DistanceTransform;
import inra.ijpb.binary.distmap.DistanceTransform5x5Float;
import inra.ijpb.morphology.MinimaAndMaxima;
import inra.ijpb.morphology.MinimaAndMaxima3D;
import inra.ijpb.morphology.Morphology;
import inra.ijpb.morphology.Strel;
import inra.ijpb.morphology.attrfilt.AreaOpening;
import inra.ijpb.morphology.attrfilt.AreaOpeningNaive;
import inra.ijpb.morphology.strel.DiskStrel;
import inra.ijpb.watershed.ExtendedMinimaWatershed;
import inra.ijpb.watershed.Watershed;

/**
 * Detect neutrophils in H&E stained images
 * @author ben
 * @since 1.13.5
 *
 */
public class NeutrophilFinder extends AbstractFinder {
	
	final private ComponentFactory<ICytoplasm> cytoFactory = new CytoplasmFactory();
	final private ComponentFactory<Nucleus>    nuclFactory = new NucleusFactory(NucleusType.NEUTROPHIL);
	final private ComponentFactory<Lobe>       lobeFactory = new LobeFactory();
	
	final private static int CONNECTIIVITY  = 4;
	final private static boolean IS_VERBOSE = false;
	final private static boolean NORMALISE_DISTANCE_MAP      = true;
	
	/**
	 * Construct with an analysis options
	 * @param op
	 * @param prober should prober events be fired
	 */
	public NeutrophilFinder(IAnalysisOptions op){
		super(op);
	}
		
	/*
	 * METHODS IMPLEMENTING THE FINDER INTERFACE
	 * 
	 */
		
	
	@Override
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
		
		
		if( ! listeners.isEmpty()){
			// Display the final image
			ImageProcessor ip =  new ImageImporter(imageFile).importToColorProcessor();
			ImageAnnotator an = new ImageAnnotator(ip);
			
			for(ICell cell : list){
				an.annotateCellBorders(cell);
			}
			fireDetectionEvent(an.toProcessor(), "Detected cells");
			
		}
		
		return list;
	}
	
	/*
	 * PRIVATE METHODS
	 */
	
	private List<ICytoplasm> detectCytoplasmBySegmentation(File imageFile) throws ComponentCreationException, ImageImportException{
		List<ICytoplasm> list = new ArrayList<>();
		ImageProcessor ip =  new ImageImporter(imageFile).importToColorProcessor();
		ImageProcessor ann = ip.duplicate();
		
		// Based on http://blogs.mathworks.com/steve/2013/11/19/watershed-transform-question-from-tech-support/
		try {
			IDetectionOptions main = options.getDetectionOptions(IAnalysisOptions.CYTOPLASM);
			IPreprocessingOptions op = (IPreprocessingOptions) main.getSubOptions(IDetectionSubOptions.BACKGROUND_OPTIONS);
			
			// Use colour thresholds to get rough cytoplasms

			int minHue = op.getMinHue();
			int maxHue = op.getMaxHue();
			int minSat = op.getMinSaturation();
			int maxSat = op.getMaxSaturation();
			int minBri = op.getMinBrightness();
			int maxBri = op.getMaxBrightness();
			int connectivity = 8;

			ip = new ImageFilterer(ip)
					.colorThreshold(minHue, maxHue, minSat, maxSat, minBri, maxBri)
					.convertToByteProcessor()
					.toProcessor();

			ip.invert();

			fireDetectionEvent(ip.duplicate(), "Colour threshold");
			
			// Fill area holes
			AreaOpening ao = new AreaOpeningNaive();
			ip = ao.process(ip, (int) main.getMinSize());

			fireDetectionEvent(ip.duplicate(), "Area opening");
						
			// Calculate a distance map
			float[] floatWeights = ChamferWeights.CHESSKNIGHT.getFloatWeights();
			boolean normalize = true;
			DistanceTransform dt = new DistanceTransform5x5Float(floatWeights, normalize);
			ImageProcessor distance = dt.distanceMap(ip);
//			distance.invert();
			fireDetectionEvent(distance.duplicate(), "Distance map");
			
			// Calculate extended minima
			int dynamic = 2;
			ImageProcessor minima = MinimaAndMaxima.extendedMinima( distance, dynamic, connectivity );
//			minima.invert();

			fireDetectionEvent(minima.duplicate(), "Extended minima");
			
			// Impose the minima to the distnace map
			ImageProcessor minimaDistance = MinimaAndMaxima.imposeMinima(distance, minima);
			
			
			
			// Watershed the image
//			WatershedTransform2D wt = new WatershedTransform2D(result, null, connectivity);
//			ImageProcessor watersheded = wt.apply();
			
			ImageProcessor watersheded = Watershed.computeWatershed(minimaDistance, null, connectivity);
			
			fireDetectionEvent(watersheded.duplicate(), "Watershed");

			
			
			ImageProcessor lines = BinaryImages.binarize( watersheded );
				
			fireDetectionEvent(lines.duplicate(), "Binarized");
			
			
		} catch (MissingOptionException e) {
			error("Missing option", e);
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
			int erosionDiameter = 1;
			int dynamic         = 2;
			
			ip = new ImageFilterer(ip)
					.colorThreshold(minHue, maxHue, minSat, maxSat, minBri, maxBri)
					.convertToByteProcessor()
					.toProcessor();
//			fireDetectionEvent(ip.duplicate(), "Colour threshold");
			ip.invert();
//			
//			
//			
//			// Calculate a distance map on the binarised input
//			float[] floatWeights = ChamferWeights.CHESSKNIGHT.getFloatWeights();
//			ImageProcessor dist =	BinaryImages.distanceMap( ip, floatWeights, NORMALISE_DISTANCE_MAP );
//			dist.invert();
//			fireDetectionEvent(dist.duplicate(), "Distance map");
//
//			// Watershed the inverted map
//			ImageProcessor watersheded = ExtendedMinimaWatershed.extendedMinimaWatershed(
//					dist, ip, dynamic, CONNECTIIVITY, IS_VERBOSE );
//			fireDetectionEvent(watersheded.duplicate(), "Distance transform watershed");
//
//			// Binarise for object detection
//			ImageProcessor lines = BinaryImages.binarize( watersheded );
//			fireDetectionEvent(lines.duplicate(), "Binarized");
//			
//			// Erode by 1 pixel to better separate lobes
//			Strel erosionStrel = Strel.Shape.DISK.fromDiameter(erosionDiameter);
//			lines = Morphology.erosion(lines, erosionStrel);
//			fireDetectionEvent(lines.duplicate(), "Eroded");

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
		if( ! listeners.isEmpty()){
			
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
			
			IDetectionOptions main = options.getDetectionOptions(IAnalysisOptions.NUCLEUS);
			int topHatRadius = main.getInt(IDetectionOptions.TOP_HAT_RADIUS);
			
			int thresholdMin = main.getThreshold();
//			int thresholdMax = 255;
			
			

			ImageProcessor test = new ImageConverter(ip)
					.convertToByteProcessor()
					.toProcessor();
			Strel strel = DiskStrel.fromRadius(topHatRadius); // the structuring element used for black top-hat
			ip = Morphology.blackTopHat(test, strel);
//			fireDetectionEvent(ip.duplicate(), "Nucleus top hat");
//			
			
			// Most remaining cytoplasm is weak, can can be thresholded away 
			ImageProcessor bin = ip.duplicate();
//			ip.setMinAndMax(thresholdMin, thresholdMax);
			bin.threshold(thresholdMin);


			GenericDetector gd = new GenericDetector();
			gd.setCirc(main.getMinCirc(), main.getMaxCirc());
			gd.setSize(main.getMinSize(), main.getMaxSize());
			gd.setThreshold(thresholdMin);
			List<Roi> rois = gd.getRois(bin);
			
			for(int i=0; i<rois.size(); i++){
				
				Roi roi = rois.get(i);
				Nucleus n = makeNucleus(roi, imageFile, main, bin, i, gd);
				list.add(n);
				
			}
				

		} catch (MissingOptionException e) {
			
		}
		
		if( ! listeners.isEmpty()){
//			fireDetectionEvent(ip.duplicate(), "Nucleus");
			
			ImageAnnotator an = new ImageAnnotator(ann);
			for(Nucleus c : list){
				an.annotateBorder(c, Color.ORANGE);
			}
			fireDetectionEvent(an.toProcessor(), "Detected nucleus");
		}
		
		
//		detectLobes(ip, list);
		detectLobesViaWatershed(ip, list);
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
		result.initialise(this.options.getProfileWindowProportion());
		result.findPointsAroundBorder();
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
		
		fireDetectionEvent(gradient.duplicate(), "Gradient");
		
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

		fireDetectionEvent(regionalMinima.getProcessor(1).duplicate(), "Regional minima");
		
		/*
		 * Computes the labels in the binary 2D or 3D image contained 
		 * in the given ImagePlus, and computes the maximum label 
		 * to set up the display range of the resulting ImagePlus.
		 * 
		 * Used to restrict the watershed to only the bounds of the nuclei
		 */
		ImageStack labeledMinima = BinaryImages.componentsLabeling( regionalMinima, connectivity, bitDepth );
		ImagePlus min = new ImagePlus("", labeledMinima.getProcessor(1));
		fireDetectionEvent(min.getProcessor().duplicate(), "Labelled minima");
		
		/*
		 * Compute watershed with markers with a
		 *  binary mask to restrict the regions of application
		 */
		ImagePlus resultStack = Watershed.computeWatershed( imp, min, 
				connectivity, calculateDams );
			fireDetectionEvent(resultStack.getProcessor().duplicate(), "Watershed");

		final ImagePlus lines = BinaryImages.binarize( resultStack );

		fireDetectionEvent(lines.getProcessor().duplicate(), "Binarized");

		
		ImageProcessor lp = lines.getProcessor();
		lp.invert();
		fireDetectionEvent(lp.duplicate(), "Lines");

		
		// Now take the watershed image, and detect the distinct lobes
		
		ImageFilterer ft = new ImageFilterer(lp);
		ImageProcessor ws = ft.dilate(1).toProcessor(); // separate the lobes from each other
//		ImageProcessor ws = ft.toProcessor();
		
		makeLobes(ws, list);
		
		
	}
	
	/* 
	 * 	Uses the Distance Transform watershed	
     * Take the distance map from the input.
	 * Invert it, and perform watershed using the binary mask (dynamic of 1 and 4-connectivity).	 
	 * @param ip
	 * @param list
	 * @throws ComponentCreationException
	 */
	private void detectLobesViaWatershed(ImageProcessor ip, List<Nucleus> list) throws ComponentCreationException{

		int erosionDiameter    = 1;
		int dynamic            = 1; // the minimal difference between a minima and its boundary
		
//		fireDetectionEvent(ip.duplicate(), "Lobe detection input");
		ImageProcessor mask = ip.duplicate();		
		mask.threshold(20);// binarise
//		fireDetectionEvent(mask.duplicate(), "Binarised input");
					
		// Calculate a distance map on the binarised input
		float[] floatWeights = ChamferWeights.CHESSKNIGHT.getFloatWeights();
		ImageProcessor dist =	BinaryImages.distanceMap( mask, floatWeights, NORMALISE_DISTANCE_MAP );
		dist.invert();
//		fireDetectionEvent(dist.duplicate(), "Distance map");

		// Watershed the inverted map
		ImageProcessor watersheded = ExtendedMinimaWatershed.extendedMinimaWatershed(
				dist, mask, dynamic, CONNECTIIVITY, IS_VERBOSE );
//		fireDetectionEvent(watersheded.duplicate(), "Distance transform watershed");

		// Binarise for object detection
		ImageProcessor lines = BinaryImages.binarize( watersheded );
//		fireDetectionEvent(lines.duplicate(), "Binarized");
		
		// Erode by 1 pixel to better separate lobes
		Strel erosionStrel = Strel.Shape.DISK.fromDiameter(erosionDiameter);
		lines = Morphology.erosion(lines, erosionStrel);
//		fireDetectionEvent(lines.duplicate(), "Eroded");
		
		
		// Now take the watershed image, and detect the distinct lobes
		makeLobes(lines, list);

	}
	
	/**
	 * Detect lobes in the given processed image, and assign them to nuclei
	 * @param ip the binary image with lobe objects
	 * @param list the nuclei to which lobes in this image belong
	 * @throws ComponentCreationException 
	 */
	private void makeLobes(ImageProcessor ip, List<Nucleus> list) throws ComponentCreationException{
		
		int minArea            = 5;
		int maxArea            = 3000;
		
		GenericDetector gd = new GenericDetector();
		gd.setIncludeHoles(false);
		gd.setSize(minArea, maxArea);
		List<Roi> rois  = gd.getRois(ip);
		
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
	
}
