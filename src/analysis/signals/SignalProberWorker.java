/*******************************************************************************
 *  	Copyright (C) 2016 Ben Skinner
 *   
 *     This file is part of Nuclear Morphology Analysis.
 *
 *     Nuclear Morphology Analysis is free software: you can redistribute it and/or modify
 *     it under the terms of the GNU General Public License as published by
 *     the Free Software Foundation, either version 3 of the License, or
 *     (at your option) any later version.
 *
 *     Nuclear Morphology Analysis is distributed in the hope that it will be useful,
 *     but WITHOUT ANY WARRANTY; without even the implied warranty of
 *     MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 *     GNU General Public License for more details.
 *
 *     You should have received a copy of the GNU General Public License
 *     along with Nuclear Morphology Analysis. If not, see <http://www.gnu.org/licenses/>.
 *******************************************************************************/

package analysis.signals;

import gui.ImageType;
import gui.tabs.signals.SignalDetectionImageProber.SignalImageType;
import ij.ImageStack;
import ij.gui.PolygonRoi;
import ij.process.FloatPolygon;
import ij.process.ImageProcessor;
import io.ImageImporter;

import java.awt.Color;
import java.io.File;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.swing.table.TableModel;

import stats.NucleusStatistic;
import stats.SignalStatistic;
import utility.Constants;
import components.CellularComponent;
import components.nuclear.INuclearSignal;
import components.nuclear.NuclearSignal;
import components.nuclei.Nucleus;
import analysis.IAnalysisDataset;
import analysis.IAnalysisOptions;
import analysis.detection.IconCell;
import analysis.detection.ImageProberWorker;
import analysis.image.ImageConverter;

public class SignalProberWorker extends ImageProberWorker {
	
	private IAnalysisDataset dataset;
	private int channel;
	private NuclearSignalOptions testOptions;

	public SignalProberWorker(File f, IAnalysisOptions options, ImageType type, TableModel model, IAnalysisDataset dataset, int channel, NuclearSignalOptions testOptions) {
		super(f, options, type, model);
		this.dataset = dataset;
		this.channel = channel;
		this.testOptions = testOptions;
		
	}
	
	protected void analyseImages() throws Exception {
//		setStatusLoading();
//		this.setLoadingLabelText("Probing image "+index+": "+imageFile.getAbsolutePath()+"...");

		finer("Importing image "+file.getAbsolutePath());
		
		// Import the image as a stack
		ImageStack stack = new ImageImporter(file).importImage();
		
		if(options.hasCannyOptions("nucleus")){
			if(options.getCannyOptions("nucleus").isAddBorder()){
				ImageConverter conv = new ImageConverter(stack);
				stack = conv.addBorder(10).toStack();
			}
		}

		// Find the processor number in the stack to use
		int stackNumber = Constants.rgbToStack(channel);
		
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
		SignalDetector finder = new SignalDetector(testOptions, channel);

		Map<Nucleus, List<INuclearSignal>> map = new HashMap<Nucleus, List<INuclearSignal>>();

		// Get the cells matching the desred imageFile, and find signals
		finer("Detecting signals in image");

		for(Nucleus n : dataset.getCollection().getNuclei()){
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
		
		IconCell iconCell = makeIconCell(openProcessor, SignalImageType.DETECTED_OBJECTS);
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
