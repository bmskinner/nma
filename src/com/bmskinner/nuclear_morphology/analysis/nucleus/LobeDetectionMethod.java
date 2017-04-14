package com.bmskinner.nuclear_morphology.analysis.nucleus;

import java.awt.Rectangle;
import java.util.List;

import com.bmskinner.nuclear_morphology.analysis.AbstractAnalysisMethod;
import com.bmskinner.nuclear_morphology.analysis.DefaultAnalysisResult;
import com.bmskinner.nuclear_morphology.analysis.IAnalysisResult;
import com.bmskinner.nuclear_morphology.analysis.detection.GenericDetector;
import com.bmskinner.nuclear_morphology.analysis.detection.StatsMap;
import com.bmskinner.nuclear_morphology.analysis.image.ImageConverter;
import com.bmskinner.nuclear_morphology.analysis.image.ImageFilterer;
import com.bmskinner.nuclear_morphology.components.CellularComponent;
import com.bmskinner.nuclear_morphology.components.ComponentFactory.ComponentCreationException;
import com.bmskinner.nuclear_morphology.components.IAnalysisDataset;
import com.bmskinner.nuclear_morphology.components.ICell;
import com.bmskinner.nuclear_morphology.components.Statistical;
import com.bmskinner.nuclear_morphology.components.generic.IPoint;
import com.bmskinner.nuclear_morphology.components.nuclear.LobeFactory;
import com.bmskinner.nuclear_morphology.components.nuclear.NucleusType;
import com.bmskinner.nuclear_morphology.components.nuclei.LobedNucleus;
import com.bmskinner.nuclear_morphology.components.nuclei.Nucleus;
import com.bmskinner.nuclear_morphology.components.nuclei.Nucleus.IncorrectNucleusTypeException;
import com.bmskinner.nuclear_morphology.components.options.IAnalysisOptions;
import com.bmskinner.nuclear_morphology.components.options.IDetectionOptions;
import com.bmskinner.nuclear_morphology.components.options.IDetectionOptions.IDetectionSubOptions;
import com.bmskinner.nuclear_morphology.components.options.IDetectionOptions.IDetectionSubOptions.IPreprocessingOptions;
import com.bmskinner.nuclear_morphology.components.options.IHoughDetectionOptions;
import com.bmskinner.nuclear_morphology.components.options.MissingOptionException;
import com.bmskinner.nuclear_morphology.components.stats.PlottableStatistic;
import com.bmskinner.nuclear_morphology.io.UnloadableImageException;

import ij.ImagePlus;
import ij.ImageStack;
import ij.gui.Roi;
import ij.process.ByteProcessor;
import ij.process.ImageProcessor;
import inra.ijpb.binary.BinaryImages;
import inra.ijpb.morphology.MinimaAndMaxima3D;
import inra.ijpb.morphology.Morphology;
import inra.ijpb.morphology.Strel;
import inra.ijpb.morphology.strel.DiskStrel;
import inra.ijpb.watershed.Watershed;


/**
 * This method finds lobes within nuclei. It is designed to work on 
 * neutrophils.
 * @author ben
 * @since 1.13.4
 *
 */
public class LobeDetectionMethod extends AbstractAnalysisMethod {

	private IHoughDetectionOptions options;
	
	public LobeDetectionMethod(IAnalysisDataset dataset, IHoughDetectionOptions op) {
		super(dataset);
		options = op;
	}

	@Override
	public IAnalysisResult call() throws Exception {

		fine("Running lobe detection method");

		if(NucleusType.NEUTROPHIL.equals(dataset.getAnalysisOptions().getNucleusType())){
			run();	
		} else {
			throw new IncorrectNucleusTypeException("Not a lobed nucleus type");
		}
			
		IAnalysisResult r = new DefaultAnalysisResult(dataset);
		return r;
	}
	
	private void run() {
		
		// Clear existing lobes
		dataset.getCollection().getNuclei().stream().forEach( n -> {
			if(n instanceof LobedNucleus){
				((LobedNucleus)n).removeAllLobes();
				n.setStatistic(PlottableStatistic.LOBE_COUNT, Statistical.STAT_NOT_CALCULATED);
			}
		});
		
		// Remove existing cached stats
		dataset.getCollection().clear(PlottableStatistic.LOBE_COUNT, CellularComponent.NUCLEUS);

		for(ICell cell : dataset.getCollection().getCells()){
			detectLobes(cell);
		}
		
		
	}
	
	/**
	 * Identify lobes within the nuclei of the cell
	 * @param cell
	 */
	private void detectLobes(ICell cell){

		try {

//			detectLobesViaWatershed(cell);
			detectLobesViaHough(cell);

		} catch (UnloadableImageException e) {
			warn("Unable to load cell image");
			stack(e);
		} catch (MissingOptionException e) {
			warn("Missing nucleus detection options for thresholding");
			stack(e);
		} catch (Exception e) {
			warn("Error in lobe detection");
			stack(e.getMessage(), e);
		}
	}
	
	/**
	 * Detect lobes using the hough transform
	 * @param cell
	 * @throws UnloadableImageException
	 * @throws MissingOptionException
	 */
	private void detectLobesViaHough(ICell cell) throws UnloadableImageException, MissingOptionException{
		
		IDetectionOptions nucleusOptions = dataset.getAnalysisOptions().getDetectionOptions(IAnalysisOptions.NUCLEUS);
		IPreprocessingOptions op = (IPreprocessingOptions) nucleusOptions.getSubOptions(IDetectionSubOptions.BACKGROUND_OPTIONS);
		
		ImageProcessor ip = cell.getCytoplasm().getComponentRGBImage();
		if(op.isUseColourThreshold()){
			
			int minHue = op.getMinHue();
			int maxHue = op.getMaxHue();
			int minSat = op.getMinSaturation();
			int maxSat = op.getMaxSaturation();
			int minBri = 73;// op.getMinBrightness();
			int maxBri = 255;//op.getMaxBrightness();
			
			ImageProcessor test = new ImageFilterer(ip)
					.colorThreshold(minHue, maxHue, minSat, maxSat, minBri, maxBri)
					.convertToByteProcessor()
					.toProcessor();
			
//			
//			ICannyOptions canny = OptionsFactory.makeCannyOptions();
			
			ImageFilterer imf =  new ImageFilterer(test);
			//.runEdgeDetector(canny);
			
//			new ImagePlus(cell.getNucleus().getNameAndNumber()+": Canny", imf.toProcessor()).show();
			List<IPoint> lobes = imf.runHoughCircleDetection(options);
			addPointsToNuclei(cell, lobes);
		}
		
	}
	
	/**
	 * Identify lobes based on watershed segmentation of nuclei within cytoplasm
	 * @param cell
	 * @throws UnloadableImageException
	 * @throws ComponentCreationException
	 */
	private void detectLobesViaWatershed(ICell cell) throws UnloadableImageException, ComponentCreationException{
		Strel strel = DiskStrel.fromRadius(20);
		ImageProcessor ip = cell.getCytoplasm().getComponentRGBImage();
		
		ImageProcessor test = new ImageConverter(ip)
//				.convertToGreyscale()
				.convertToByteProcessor()
				.toProcessor();
		
		ImageProcessor th = Morphology.blackTopHat(test, strel);
		new ImagePlus("Top hat processor", th).show();
//		th = MinimaAndMaxima.extendedMinima(th, 20);
		th.setMinAndMax(20, 255);
		ImagePlus imp = new ImagePlus("Min max", th);
		imp.show();

		// Copy the process used in the MorphologicalSegmentation plugin
					
		ImageStack image = new ImageStack(th.getWidth(), th.getHeight());
		image.addSlice(th);
		boolean calculateDams  = true;
		int gradientRadius = 2;
		int connectivity = 6;
		double dynamic = 10; // is this tolerance?
		strel = Strel.Shape.SQUARE.fromRadius( gradientRadius );
		ImageProcessor gradient = Morphology.gradient( image.getProcessor( 1 ), strel );
		image = new ImageStack(image.getWidth(), image.getHeight());
		image.addSlice(gradient); 
		
		ImageStack regionalMinima = MinimaAndMaxima3D.extendedMinima( image, (int)dynamic, connectivity );


		ImageStack labeledMinima = BinaryImages.componentsLabeling( regionalMinima, connectivity, 32 );
		ImagePlus min = new ImagePlus("", labeledMinima.getProcessor(1));
		

		ImagePlus resultStack = Watershed.computeWatershed( imp, min, 
				connectivity, calculateDams );
		resultStack.show();
		final ImagePlus lines = BinaryImages.binarize( resultStack );
		lines.show();
		
		ImageProcessor lp = lines.getProcessor();
		lp.invert();
		
		ImageFilterer ft = new ImageFilterer(lp);
		ImageProcessor ws = ft.dilate(2).toProcessor();
//		
		ImagePlus wimp = new ImagePlus("Dilated", ws);	
		wimp.show();
		ws.invert();
		GenericDetector gd = new GenericDetector();
//		gd.setSize(20, 3000);
		List<Roi> rois  = gd.getRois(ws);
		addLobesToNuclei(cell, rois);
	}
	
	private void addLobesToNuclei(ICell cell, List<Roi> rois) throws UnloadableImageException, ComponentCreationException{
		
		GenericDetector gd = new GenericDetector();
		
		List<Nucleus> nuclei = cell.getNuclei();

		for(Nucleus n : nuclei){

			if(n instanceof LobedNucleus){
				LobedNucleus l = (LobedNucleus) n;
				ImageProcessor ip = l.getImage();
				LobeFactory factory = new LobeFactory(l.getSourceFile());

				for(Roi roi : rois){
					StatsMap m = gd.measure(roi, ip);
					int x = m.get(GenericDetector.COM_X).intValue();
					int y = m.get(GenericDetector.COM_Y).intValue();

					IPoint com = IPoint.makeNew(x, y);
					if(l.containsOriginalPoint(com)){
						Rectangle bounds = roi.getBounds();
						int xbase = (int) roi.getXBase();
						int ybase = (int) roi.getYBase();
						int[] originalPosition = {xbase, ybase, (int) bounds.getWidth(), (int) bounds.getHeight() };
						l.addLobe( factory.buildInstance(roi, 0, originalPosition, com)); //TODO makethe channel useful
					}

				}

				l.setStatistic(PlottableStatistic.LOBE_COUNT, l.getLobeCount());
			}
		}
	}
	
	
	private void addPointsToNuclei(ICell cell, List<IPoint> points){
//		log("Adding "+points.size()+" points to nuclei");
		
		
		
		IPoint base = cell.getCytoplasm().getOriginalBase();
		
		List<Nucleus> nuclei = cell.getNuclei();
		
		for(Nucleus n : nuclei){
			
			if(n instanceof LobedNucleus){
				LobedNucleus l = (LobedNucleus) n;
				LobeFactory factory = new LobeFactory(l.getSourceFile());
				
				// Trim the points so that the centre of a point cluster is chosen, 
				// rather than the whole cloud
				
				// make a binary mask over the points
				// get the ROIs encompassing them
				// Add the CoM of each ROI
				
				try {
//					// This is just to get the dimensions of the original image
//					// TODO - use the component size and apply an offset
					ImageProcessor ip = l.getImage();
					
				
					int w = ip.getWidth();
					int h = ip.getHeight();
					ByteProcessor bp = new ByteProcessor(w, h ); 
					

					for(int i=0; i<w*h; i++){
						bp.set(i, 0);
					}


					for(IPoint p : points){

						int oX = p.getXAsInt() + base.getXAsInt() - CellularComponent.COMPONENT_BUFFER;
						int oY = p.getYAsInt() + base.getYAsInt() - CellularComponent.COMPONENT_BUFFER;

						IPoint oP = IPoint.makeNew(oX, oY);

						bp.set(oX, oY, 255);
					}

					// Now look for ROIs in the byte processor
					GenericDetector dt = new GenericDetector();
					List<Roi> rois = dt.getRois(bp);
					for(Roi roi : rois){
						StatsMap m = dt.measure(roi, bp);
						int x = m.get(GenericDetector.COM_X).intValue();
						int y = m.get(GenericDetector.COM_Y).intValue();

						IPoint com = IPoint.makeNew(x, y);
						if(l.containsOriginalPoint(com)){
							Rectangle bounds = roi.getBounds();
							int xbase = (int) roi.getXBase();
							int ybase = (int) roi.getYBase();
							int[] originalPosition = {xbase, ybase, (int) bounds.getWidth(), (int) bounds.getHeight() };
							l.addLobe( factory.buildInstance(roi, 0, originalPosition, com)); //TODO makethe channel useful
//							l.addLobeCentre(com);
						}

					}

					
					//				log(l.getLobeCount()+" lobes");
					// Copy stat for charting
					l.setStatistic(PlottableStatistic.LOBE_COUNT, l.getLobeCount());
				} catch (UnloadableImageException | ComponentCreationException e) {
					stack(e);
				}
			}
			
			
		}
		
	}

}
