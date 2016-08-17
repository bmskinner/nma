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
package gui.dialogs;

import java.awt.Dimension;
import java.io.File;
import java.util.logging.Level;

import analysis.AnalysisDataset;
import analysis.AnalysisOptions;
import analysis.signals.NuclearSignalOptions;
import analysis.signals.SignalProberWorker;
import gui.ImageType;


@SuppressWarnings("serial")
public class SignalDetectionImageProber extends ImageProber {
	
	private AnalysisDataset dataset;
	private int channel;
	private NuclearSignalOptions testOptions;


	
	public enum SignalImageType implements ImageType {
		DETECTED_OBJECTS ("Detected objects",  0);
		
		private String name;
		private int position; // the order in which the processed images should be displayed
		
		SignalImageType(String name, int position){
			this.name = name;
			this.position = position;
		}
		public String toString(){
			return this.name;
		}
		
		public ImageType[] getValues(){
			return SignalImageType.values();
		}
		@Override
		public int getPosition() {
			return position;
		}
	}
	
	public SignalDetectionImageProber(AnalysisOptions options, File folder, AnalysisDataset dataset, int channel, NuclearSignalOptions testOptions) {
		super(options, SignalImageType.DETECTED_OBJECTS, folder);
		
		
		
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
			
			table.setModel(createEmptyTableModel(rows, cols));
			
			for(int col=0; col<cols; col++){
	        	table.getColumnModel().getColumn(col).setCellRenderer(new IconCellRenderer());
	        }
			
			SignalProberWorker worker = new SignalProberWorker(imageFile, 
					options, 
					SignalImageType.DETECTED_OBJECTS, 
					table.getModel(),
					dataset,
					channel,
					testOptions);
			
			worker.setSmallIconSize(new Dimension(500, table.getRowHeight()-30));
			
			worker.addPropertyChangeListener(this);
			progressBar.setVisible(true);
			worker.execute();

//			finest("Importing image "+imageFile.getAbsolutePath());
//			
//			// Import the image as a stack
//			ImageStack stack = ImageImporter.getInstance().importImage(imageFile);
//
//			// Find the processor number in the stack to use
//			int stackNumber = Constants.rgbToStack(channel);
//			
//			finest("Converting image");
//			// Get the greyscale processor for the signal channel
//			ImageProcessor greyProcessor = stack.getProcessor(stackNumber);
//			
//			// Convert to an RGB processor for annotation
//			ImageProcessor openProcessor = ImageExporter.getInstance().convertTORGBGreyscale(greyProcessor);
//			
//			String imageName = imageFile.getName();
//
////			ImageProcessor openProcessor = ImageExporter.getInstance().makeGreyRGBImage(stack).getProcessor();
//			openProcessor.invert();
//			procMap.put(SignalImageType.DETECTED_OBJECTS, openProcessor);
//
//			// Store the options
//			double minSize  = testOptions.getMinSize();
//			double maxFract = testOptions.getMaxFraction();
//			int threshold   = testOptions.getThreshold();
//			
//			testOptions.setMinSize(5);
//			testOptions.setMaxFraction(1d);
//			
//			// Create the finder
//			SignalFinder finder = new SignalFinder(testOptions, channel);
//
//			Map<Nucleus, List<NuclearSignal>> map = new HashMap<Nucleus, List<NuclearSignal>>();
//
//			// Get the cells matching the desred imageFile, and find signals
//			log(Level.FINEST, "Detecting signals in image");
//
//			for(Nucleus n : dataset.getCollection().getNuclei()){
////				log(Level.FINEST, "Testing nucleus "+n.getImageName()+" against "+imageName);
//				if(n.getSourceFileName().equals(imageName)){
////					log(Level.FINEST, "  Nucleus is in image; finding signals");
//					List<NuclearSignal> list = finder.detectSignal(imageFile, stack, n);
//					log(Level.FINEST, "  Detected "+list.size()+" signals");
//					map.put(n, list);
//				}
//			}
//			
//			// Reset the test options
//			testOptions.setMinSize(minSize);
//			testOptions.setMaxFraction(maxFract);
//			testOptions.setThreshold(threshold);
//
//			log(Level.FINEST, "Drawing signals");
//			// annotate detected signals onto the imagefile
//			drawSignals(map, openProcessor);
//
//			updateImageThumbnails();
//
//			this.setLoadingLabelText("Showing signals in "+imageFile.getAbsolutePath());
//			this.setStatusLoaded();
//			headerLabel.setIcon(null);
//			headerLabel.repaint();
		} catch(Exception e){
				log(Level.SEVERE, "Error in signal probing", e);
			}
		}
	
//	protected void drawSignals(Map<Nucleus, List<NuclearSignal>> map, ImageProcessor ip) throws Exception{
//		if(map==null){
//			throw new IllegalArgumentException("Input cell is null");
//		}
//		
//		ip.setLineWidth(2);
//		for(Nucleus n : map.keySet()){
//			double[] positions = n.getPosition();
//			List<NuclearSignal> list = map.get(n);
//			
//			// Draw the nucleus
//			ip.setColor(Color.BLUE);
//			FloatPolygon npolygon = n.createPolygon();
//			PolygonRoi nroi = new PolygonRoi(npolygon, PolygonRoi.POLYGON);
//			nroi.setLocation(positions[CellularComponent.X_BASE], positions[CellularComponent.Y_BASE]);
//			ip.draw(nroi);
//			
//			
//		
//			for(NuclearSignal s : list){
//				if(checkSignal(s, n)){
//					ip.setColor(Color.YELLOW);
//				} else {
//					ip.setColor(Color.RED);
//				}
//
//				FloatPolygon polygon = s.createPolygon();
//				PolygonRoi roi = new PolygonRoi(polygon, PolygonRoi.POLYGON);
//				
//				// Offset the roi to the nucleus bouding box
//				double x = positions[CellularComponent.X_BASE]+s.getCentreOfMass().getX() - ( roi.getBounds().getWidth() /2);
//				double y = positions[CellularComponent.Y_BASE]+s.getCentreOfMass().getY()- ( roi.getBounds().getHeight() /2);
//				
//				roi.setLocation( x,y );
//				ip.draw(roi);
//			}
//		}
//	}
//	
//	private boolean checkSignal(NuclearSignal s, Nucleus n) throws Exception{
//		boolean result = true;
//		
//		if(s.getStatistic(SignalStatistic.AREA) < testOptions.getMinSize()){
//			
//			result = false;
//		}
//		
//		if(s.getStatistic(SignalStatistic.AREA) > (testOptions.getMaxFraction() * n.getStatistic(NucleusStatistic.AREA) )   ){
//			
//			result = false;
//		}		
//		return result;
//	}

}
