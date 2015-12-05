/*******************************************************************************
 *  	Copyright (C) 2015 Ben Skinner
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
package gui;

import java.awt.Color;
import java.io.File;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;

import analysis.AnalysisDataset;
import analysis.AnalysisOptions;
import analysis.AnalysisOptions.NuclearSignalOptions;
import analysis.nucleus.SignalFinder;
import components.nuclear.NuclearSignal;
import components.nuclei.Nucleus;

import ij.ImageStack;
import ij.gui.PolygonRoi;
import ij.process.FloatPolygon;
import ij.process.ImageProcessor;
import io.ImageExporter;
import io.ImageImporter;
import utility.Utils;


@SuppressWarnings("serial")
public class SignalDetectionImageProber extends ImageProber {
	
	private AnalysisDataset dataset;
	private int channel;
	NuclearSignalOptions testOptions;

	private enum SignalImageType implements ImageType {
		DETECTED_OBJECTS ("Detected objects");
		
		private String name;
		
		SignalImageType(String name){
			this.name = name;
		}
		public String toString(){
			return this.name;
		}
		
		public ImageType[] getValues(){
			return SignalImageType.values();
		}
	}
	
	public SignalDetectionImageProber(AnalysisOptions options, Logger logger, File folder, AnalysisDataset dataset, int channel, NuclearSignalOptions testOptions) {
		super(options, logger, SignalImageType.DETECTED_OBJECTS, folder);
		
		
		
		if(dataset==null){
			throw new IllegalArgumentException("Dataset cannot be null");
		}
		
		this.dataset = dataset;
		this.channel = channel;
		this.testOptions = testOptions;
		createFileList(folder);
		this.setVisible(true);
	}
	
	@Override
	protected void importAndDisplayImage(File imageFile){

		try{
			setStatusLoading();
			this.setLoadingLabelText("Probing image "+index+": "+imageFile.getAbsolutePath()+"...");

			ImageStack stack = ImageImporter.importImage(imageFile, programLogger);

			// Import the image as a stack
			String imageName = imageFile.getName();

			programLogger.log(Level.FINEST, "Converting image");
			ImageProcessor openProcessor = ImageExporter.makeGreyRGBImage(stack).getProcessor();
			openProcessor.invert();
			procMap.put(SignalImageType.DETECTED_OBJECTS, openProcessor);

			// Store the options
			double minSize = testOptions.getMinSize();
			double maxFract = testOptions.getMaxFraction();
			int threshold = testOptions.getSignalThreshold();
			
			testOptions.setMinSize(5);
			testOptions.setMaxFraction(1d);
			
			// Create the finder
			SignalFinder finder = new SignalFinder(testOptions, programLogger, channel);

			Map<Nucleus, List<NuclearSignal>> map = new HashMap<Nucleus, List<NuclearSignal>>();

			// Get the cells matching the desred imageFile, and find signals
			programLogger.log(Level.FINEST, "Detecting signals in image");

			for(Nucleus n : dataset.getCollection().getNuclei()){
//				programLogger.log(Level.FINEST, "Testing nucleus "+n.getImageName()+" against "+imageName);
				if(n.getImageName().equals(imageName)){
//					programLogger.log(Level.FINEST, "  Nucleus is in image; finding signals");
					List<NuclearSignal> list = finder.detectSignal(imageFile, stack, n);
					programLogger.log(Level.FINEST, "  Detected "+list.size()+" signals");
					map.put(n, list);
				}
			}
			
			// Reset the test options
			testOptions.setMinSize(minSize);
			testOptions.setMaxFraction(maxFract);
			testOptions.setThreshold(threshold);

			programLogger.log(Level.FINEST, "Drawing signals");
			// annotate detected signals onto the imagefile
			drawSignals(map, openProcessor);

			updateImageThumbnails();

			this.setLoadingLabelText("Showing signals in "+imageFile.getAbsolutePath());
			this.setStatusLoaded();
//			headerLabel.setIcon(null);
//			headerLabel.repaint();
		} catch(Exception e){
				programLogger.log(Level.SEVERE, "Error in signal probing", e);
			}
		}
	
	protected void drawSignals(Map<Nucleus, List<NuclearSignal>> map, ImageProcessor ip){
		if(map==null){
			throw new IllegalArgumentException("Input cell is null");
		}
		
		ip.setLineWidth(2);
		for(Nucleus n : map.keySet()){
			double[] positions = n.getPosition();
			List<NuclearSignal> list = map.get(n);
			
			// Draw the nucleus
			ip.setColor(Color.BLUE);
			FloatPolygon npolygon = Utils.createPolygon(n.getBorderList());
			PolygonRoi nroi = new PolygonRoi(npolygon, PolygonRoi.POLYGON);
			nroi.setLocation(positions[Nucleus.X_BASE], positions[Nucleus.Y_BASE]);
			ip.draw(nroi);
			
			
		
			for(NuclearSignal s : list){
				if(checkSignal(s, n)){
					ip.setColor(Color.YELLOW);
				} else {
					ip.setColor(Color.RED);
				}

				FloatPolygon polygon = Utils.createPolygon(s.getBorder());
				PolygonRoi roi = new PolygonRoi(polygon, PolygonRoi.POLYGON);
				
				// Offset the roi to the nucleus bouding box
				double x = positions[Nucleus.X_BASE]+s.getCentreOfMass().getX() - ( roi.getBounds().getWidth() /2);
				double y = positions[Nucleus.Y_BASE]+s.getCentreOfMass().getY()- ( roi.getBounds().getHeight() /2);
				
				roi.setLocation( x,y );
				ip.draw(roi);
			}
		}
	}
	
	private boolean checkSignal(NuclearSignal s, Nucleus n){
		boolean result = true;
		
		if(s.getArea() < testOptions.getMinSize()){
			
			result = false;
		}
		
		if(s.getArea() > (testOptions.getMaxFraction() * n.getArea() )   ){
			
			result = false;
		}		
		return result;
	}

}
