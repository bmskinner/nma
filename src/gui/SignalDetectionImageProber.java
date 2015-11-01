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
			headerLabel.setText("Probing image "+index+": "+imageFile.getAbsolutePath()+"...");

			programLogger.log(Level.FINEST, "Importing image "+imageFile.getAbsolutePath());
			ImageStack stack = ImageImporter.importImage(imageFile, programLogger);

			// Import the image as a stack
			String imageName = imageFile.getName();

			programLogger.log(Level.FINEST, "Converting image");
			ImageProcessor openProcessor = ImageExporter.convert(stack).getProcessor();
			procMap.put(SignalImageType.DETECTED_OBJECTS, openProcessor);

			// Store the options
			double minSize = testOptions.getMinSize();
			double maxFract = testOptions.getMaxFraction();
			
			testOptions.setMinSize(5);
			testOptions.setMaxFraction(1);
			
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

			programLogger.log(Level.FINEST, "Drawing signals");
			// annotate detected signals onto the imagefile
			drawSignals(map, openProcessor);

			updateImageThumbnails();

			headerLabel.setText("Showing signals in "+imageFile.getAbsolutePath());
			headerLabel.setIcon(null);
			headerLabel.repaint();
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
