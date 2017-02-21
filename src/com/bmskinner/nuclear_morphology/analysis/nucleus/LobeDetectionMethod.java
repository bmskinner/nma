package com.bmskinner.nuclear_morphology.analysis.nucleus;

import ij.ImagePlus;
import ij.gui.Roi;
import ij.process.ByteProcessor;
import ij.process.ImageProcessor;

import java.util.List;

import com.bmskinner.nuclear_morphology.analysis.AbstractAnalysisMethod;
import com.bmskinner.nuclear_morphology.analysis.DefaultAnalysisResult;
import com.bmskinner.nuclear_morphology.analysis.IAnalysisResult;
import com.bmskinner.nuclear_morphology.analysis.detection.GenericDetector;
import com.bmskinner.nuclear_morphology.analysis.detection.Mask;
import com.bmskinner.nuclear_morphology.analysis.detection.StatsMap;
import com.bmskinner.nuclear_morphology.analysis.image.ImageFilterer;
import com.bmskinner.nuclear_morphology.components.CellularComponent;
import com.bmskinner.nuclear_morphology.components.IAnalysisDataset;
import com.bmskinner.nuclear_morphology.components.ICell;
import com.bmskinner.nuclear_morphology.components.generic.IPoint;
import com.bmskinner.nuclear_morphology.components.nuclear.NucleusType;
import com.bmskinner.nuclear_morphology.components.nuclei.LobedNucleus;
import com.bmskinner.nuclear_morphology.components.nuclei.Nucleus;
import com.bmskinner.nuclear_morphology.components.options.IAnalysisOptions;
import com.bmskinner.nuclear_morphology.components.options.IDetectionOptions;
import com.bmskinner.nuclear_morphology.components.options.IDetectionOptions.IDetectionSubOptions;
import com.bmskinner.nuclear_morphology.components.options.IDetectionOptions.IDetectionSubOptions.IPreprocessingOptions;
import com.bmskinner.nuclear_morphology.components.options.IHoughDetectionOptions;
import com.bmskinner.nuclear_morphology.components.options.MissingOptionException;
import com.bmskinner.nuclear_morphology.components.stats.PlottableStatistic;
import com.bmskinner.nuclear_morphology.io.UnloadableImageException;


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
			warn("Not a lobed nucleus type; cannot run lobe detection");
		}
			
		IAnalysisResult r = new DefaultAnalysisResult(dataset);
		return r;
	}
	
	private void run() {
		
		// Clear existing lobes
		for(ICell cell : dataset.getCollection().getCells()){
			for(Nucleus n : cell.getNuclei()){
				
				if(n instanceof LobedNucleus){
					LobedNucleus l = (LobedNucleus) n;
					l.removeAllLobes();
					
				}
			}
		}
		
		
		// For each cell
		// get the cytoplasm component image
		// get the circles based on threshold
		// print
		

		for(ICell cell : dataset.getCollection().getCells()){
					

			try {
				
				IDetectionOptions nucleusOptions = dataset.getAnalysisOptions().getDetectionOptions(IAnalysisOptions.NUCLEUS);
				IPreprocessingOptions op = (IPreprocessingOptions) nucleusOptions.getSubOptions(IDetectionSubOptions.BACKGROUND_OPTIONS);
				
				ImageProcessor ip = cell.getCytoplasm().getComponentRGBImage();
				
//				new ImagePlus("Color processor", ip).show();

				if(op.isUseColourThreshold()){
					
					int minHue = op.getMinHue();
					int maxHue = op.getMaxHue();
					int minSat = op.getMinSaturation();
					int maxSat = op.getMaxSaturation();
					int minBri = op.getMinBrightness();
					int maxBri = op.getMaxBrightness();
					
					ImageProcessor test = new ImageFilterer(ip)
							.colorThreshold(minHue, maxHue, minSat, maxSat, minBri, maxBri)
							.convertToByteProcessor()
							.toProcessor();
					
//					new ImagePlus("Byte processor", test).show();
					
					
					List<IPoint> lobes = new ImageFilterer(test).runHoughCircleDetection(options);
					addPointsToNuclei(cell, lobes);
				}
				
				
			} catch (UnloadableImageException e) {
				warn("Unable to load cell image");
			} catch (MissingOptionException e) {
				warn("Missing nucleus detection options for thresholding");
			} catch (Exception e) {
				warn("Error in lobe detection");
				stack(e.getMessage(), e);
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
				
				// Trim the points so that the centre of a point cluster is chosen, 
				// rather than the whole cloud
				
				// make a binary mask over the points
				// get the ROIs encompassing them
				// Add the CoM of each ROI
				
				//TODO - the hough points are offset against the nuclei. Check the hough transform probability image
				// to see where this is occurring.
				
				try {
					// This is just to get the dimensions of the original image
					// TODO - use the component size and apply an offset
					ImageProcessor ip = l.getImage();
				
				
					int w = ip.getWidth();
					int h = ip.getHeight();
					ByteProcessor bp = new ByteProcessor(w, h ); 
					

					for(int i=0; i<w*h; i++){
						bp.set(i, 0);
					}


					for(IPoint p : points){

						int oX = p.getXAsInt() + base.getXAsInt();
						int oY = p.getYAsInt() + base.getYAsInt();

//						IPoint oP = IPoint.makeNew(oX, oY);

						bp.set(oX, oY, 255);



						// Offset the points to the position of the nucleus
						//					if(l.containsOriginalPoint(oP)){
						//						l.addLobeCentre(oP);
						////						log("\tAdded lobe "+p.toString());
						//					}
					}

//					new ImagePlus(l.getNameAndNumber(), bp).show();
					// Now look for ROIs in the byte processor
					GenericDetector dt = new GenericDetector();
					List<Roi> rois = dt.getRois(bp);
					for(Roi roi : rois){
						StatsMap m = dt.measure(roi, bp);
						int x = m.get(GenericDetector.COM_X).intValue();
						int y = m.get(GenericDetector.COM_Y).intValue();

						IPoint com = IPoint.makeNew(x, y);
						if(cell.getCytoplasm().containsOriginalPoint(com)){
							l.addLobeCentre(com);
						}

					}


					//				log(l.getLobeCount()+" lobes");
					// Copy stat for charting
					l.setStatistic(PlottableStatistic.LOBE_COUNT, l.getLobeCount());
				} catch (UnloadableImageException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				}
			}
			
			
		}
		
	}

}
