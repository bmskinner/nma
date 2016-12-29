package com.bmskinner.nuclear_morphology.gui.dialogs.prober.workers;

import java.awt.Color;
import java.io.File;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import javax.swing.table.TableModel;

import com.bmskinner.nuclear_morphology.analysis.image.ImageConverter;
import com.bmskinner.nuclear_morphology.analysis.signals.SignalDetector;
import com.bmskinner.nuclear_morphology.components.CellularComponent;
import com.bmskinner.nuclear_morphology.components.nuclear.INuclearSignal;
import com.bmskinner.nuclear_morphology.components.nuclei.Nucleus;
import com.bmskinner.nuclear_morphology.components.options.IDetectionOptions;
import com.bmskinner.nuclear_morphology.components.options.IMutableNuclearSignalOptions;
import com.bmskinner.nuclear_morphology.components.options.INuclearSignalOptions;
import com.bmskinner.nuclear_morphology.components.stats.NucleusStatistic;
import com.bmskinner.nuclear_morphology.components.stats.SignalStatistic;
import com.bmskinner.nuclear_morphology.gui.dialogs.prober.DetectionImageType;
import com.bmskinner.nuclear_morphology.gui.dialogs.prober.ImageProberTableCell;
import com.bmskinner.nuclear_morphology.gui.dialogs.prober.ImageSet;
import com.bmskinner.nuclear_morphology.io.ImageImporter;
import com.bmskinner.nuclear_morphology.utility.Constants;

import ij.ImageStack;
import ij.gui.PolygonRoi;
import ij.process.FloatPolygon;
import ij.process.ImageProcessor;

/**
 * Detect signals within nuclei in a given image
 * @author ben
 * @since 1.13.4
 *
 */
public class SignalProberWorker  extends ImageProberWorker {
	
	final private Set<Nucleus> nuclei;
	
	public SignalProberWorker(final File f, final IDetectionOptions options, final ImageSet type, final Set<Nucleus> nuclei, final TableModel model) {
		super(f, options, type, model);
		this.nuclei = nuclei;
	}
	
	
	protected void analyseImages() throws Exception {
		
		IMutableNuclearSignalOptions testOptions = (IMutableNuclearSignalOptions) options.duplicate(); 
		
		finer("Importing image "+file.getAbsolutePath());
		
		// Import the image as a stack
		ImageStack stack = new ImageImporter(file).importImage();

		// Find the processor number in the stack to use
		int stackNumber = Constants.rgbToStack(options.getChannel());
		
		finer("Converting image");
		// Get the greyscale processor for the signal channel
		ImageProcessor greyProcessor = stack.getProcessor(stackNumber);
		
		// Convert to an RGB processor for annotation
		ImageProcessor openProcessor = new ImageConverter(greyProcessor)
			.convertToGreyscale()
			.invert()
			.toProcessor();
		
		String imageName = file.getName();


		// Store the options
		
		
		double minSize  = testOptions.getMinSize();
		double maxFract = testOptions.getMaxFraction();
		int threshold   = testOptions.getThreshold();
		
		testOptions.setMinSize(5);
		testOptions.setMaxFraction(1d);
		
		// Create the finder
		SignalDetector finder = new SignalDetector(testOptions, testOptions.getChannel());

		Map<Nucleus, List<INuclearSignal>> map = new HashMap<Nucleus, List<INuclearSignal>>();

		// Get the cells matching the desred imageFile, and find signals
		finer("Detecting signals in image");

		for(Nucleus n : nuclei){
//			log(Level.FINEST, "Testing nucleus "+n.getImageName()+" against "+imageName);
			if(n.getSourceFileName().equals(imageName)){
//				log(Level.FINEST, "  Nucleus is in image; finding signals");
				List<INuclearSignal> list = finder.detectSignal(file, stack, n);
				finest("  Detected "+list.size()+" signals");
				map.put(n, list);
			}
		}
		
		// Reset the test options
		testOptions.setMinSize(minSize);
		testOptions.setMaxFraction(maxFract);
		testOptions.setThreshold(threshold);

		finer("Drawing signals");
		// annotate detected signals onto the imagefile
		drawSignals(map, openProcessor);
		
		ImageProberTableCell iconCell = makeIconCell(openProcessor, true, DetectionImageType.DETECTED_OBJECTS);
		publish(iconCell);

//		updateImageThumbnails();
//
//		this.setLoadingLabelText("Showing signals in "+imageFile.getAbsolutePath());
//		this.setStatusLoaded();
	}
	
	protected void drawSignals(Map<Nucleus, List<INuclearSignal>> map, ImageProcessor ip) throws Exception{
		if(map==null){
			throw new IllegalArgumentException("Input cell is null");
		}
		
		ip.setLineWidth(2);
		for(Nucleus n : map.keySet()){
			int[] positions = n.getPosition();
			List<INuclearSignal> list = map.get(n);
			
			// Draw the nucleus
			ip.setColor(Color.BLUE);
			FloatPolygon npolygon = n.createPolygon();
			PolygonRoi nroi = new PolygonRoi(npolygon, PolygonRoi.POLYGON);
			nroi.setLocation(positions[CellularComponent.X_BASE], positions[CellularComponent.Y_BASE]);
			ip.draw(nroi);
			
			
		
			for(INuclearSignal s : list){
				if(checkSignal(s, n)){
					ip.setColor(Color.YELLOW);
				} else {
					ip.setColor(Color.RED);
				}

				FloatPolygon polygon = s.createPolygon();
				PolygonRoi roi = new PolygonRoi(polygon, PolygonRoi.POLYGON);
				
				// Offset the roi to the nucleus bouding box
				double x = positions[CellularComponent.X_BASE]+s.getCentreOfMass().getX() - ( roi.getBounds().getWidth() /2);
				double y = positions[CellularComponent.Y_BASE]+s.getCentreOfMass().getY()- ( roi.getBounds().getHeight() /2);
				
				roi.setLocation( x,y );
				ip.draw(roi);
			}
		}
	}
	
	private boolean checkSignal(INuclearSignal s, Nucleus n) throws Exception{
		
		INuclearSignalOptions testOptions = (INuclearSignalOptions) options; 
		
		
		boolean result = true;
		
		if(s.getStatistic(SignalStatistic.AREA) < testOptions.getMinSize()){
			
			result = false;
		}
		
		if(s.getStatistic(SignalStatistic.AREA) > (testOptions.getMaxFraction() * n.getStatistic(NucleusStatistic.AREA) )   ){
			
			result = false;
		}		
		return result;
	}

}
