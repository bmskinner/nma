package no.analysis;

import ij.IJ;
import ij.ImageStack;
import ij.gui.Roi;
import ij.process.FloatPolygon;
import ij.process.ImageProcessor;

import java.io.File;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ExecutionException;

import javax.swing.SwingWorker;

import cell.Cell;
import components.SpermTail;
import utility.Constants;
import utility.Logger;
import utility.StatsMap;
import utility.Utils;
import no.components.AnalysisOptions.NuclearSignalOptions;
import no.components.NuclearSignal;
import no.components.SignalCollection;
import no.components.XYPoint;
import no.imports.ImageImporter;
import no.nuclei.AsymmetricNucleus;
import no.nuclei.Nucleus;


public class SignalDetector extends SwingWorker<Boolean, Integer> {
	
	protected NuclearSignalOptions options = null;
	protected Logger logger;
	protected File folder;
	protected AnalysisDataset dataset;
	protected int channel;
	protected int signalGroup;
	protected String channelName;

	/**
	 * Empty constructor. Detector will have default values
	 */
	public SignalDetector(){
		
	}
	
	/**
	 * For use when running on an existing dataset
	 * @param d the dataset to add signals to
	 * @param folder the folder of images
	 * @param channel the RGB channel to search
	 * @param options the analysis options
	 * @param group the signal group to add signals to
	 */
	public SignalDetector(AnalysisDataset d, File folder, int channel, NuclearSignalOptions options, int group, String channelName){
		this.options	 = options;
		this.logger		 = new Logger(d.getDebugFile(), "SignalDetector");
		this.folder		 = folder;
		this.channel	 = channel;
		this.signalGroup = group;
		this.channelName = channelName;
		this.dataset	 = d;
	}
	
	

	/**
	 * Constructor for use in AnalysisCreator.
	 * @param options the analysis options
	 * @param debugFile the log file
	 */
	public SignalDetector(NuclearSignalOptions options, File debugFile){
		this.options = options;
		this.logger = new Logger(debugFile, "SignalDetector");
		logger.log("Created signal detector", Logger.DEBUG);
	}
	
	@Override
	protected void process( List<Integer> integers ) {
		//update the number of entries added
		int amount = integers.get( integers.size() - 1 );
		int totalCells = dataset.getCollection().getNucleusCount();
		int percent = (int) ( (double) amount / (double) totalCells * 100);
		setProgress(percent); // the integer representation of the percent
	}
	
	@Override
	protected Boolean doInBackground() throws Exception {
		boolean result = true;
		logger.log("Beginning signal detection in channel "+channel, Logger.INFO);

		try{
			int progress = 0;
			for(Cell c : dataset.getCollection().getCells()){

				Nucleus n = c.getNucleus();
				logger.log("Looking for signals associated with nucleus "+n.getImageName()+"-"+n.getNucleusNumber(), Logger.DEBUG);
				
				// get the image in the folder with the same name as the
				// nucleus source image
				File imageFile = new File(folder + File.separator + n.getImageName());
				logger.log("Source file: "+imageFile.getAbsolutePath(), Logger.DEBUG);

				try{
					
					ImageStack stack = ImageImporter.importImage(imageFile, logger.getLogfile());
					
					detectSignal(imageFile, stack, n);
					
					
				} catch(Exception e){
					logger.log("Error detecting signal: "+e.getMessage(), Logger.ERROR);
					for(StackTraceElement el : e.getStackTrace()){
						logger.log(el.toString(), Logger.STACK);
					}
				}
				
				progress++;
				publish(progress);
			}
		} catch (Exception e){
			logger.log("Error in signal detection: "+e.getMessage(), Logger.ERROR);
			for(StackTraceElement el : e.getStackTrace()){
				logger.log(el.toString(), Logger.STACK);
			}
			return false;
		}

		return result;
	}
	
	@Override
	public void done() {

		try {
			if(this.get()){
				firePropertyChange("Finished", getProgress(), Constants.PROGRESS_FINISHED);
			} else {
				firePropertyChange("Error", getProgress(), Constants.PROGRESS_ERROR);
			}
		} catch (InterruptedException e) {
			logger.log("Error in signal detection: "+e.getMessage(), Logger.ERROR);
			for(StackTraceElement el : e.getStackTrace()){
				logger.log(el.toString(), Logger.STACK);
			}
		} catch (ExecutionException e) {
			logger.log("Error in signal detection: "+e.getMessage(), Logger.ERROR);
			for(StackTraceElement el : e.getStackTrace()){
				logger.log(el.toString(), Logger.STACK);
			}
		}

	} 
	
	
	/**
	 * Call the appropriate signal detection method based on the analysis options
	 * @param sourceFile the file the image came from
	 * @param stack the imagestack
	 * @param n the nucleus
	 */
	private void detectSignal(File sourceFile, ImageStack stack, Nucleus n){
		
		if(options==null || !options.isReverseThreshold()){
			detectForwardThresholdSignal(sourceFile, stack, n);
		} else {
			detectReverseThresholdSignal(sourceFile, stack, n);
		}
		
		
	}
	
	/**
	 * Detect a signal in a given stack by standard forward thresholding
	 * and add to the given nucleus
	 * @param sourceFile the file the image came from
	 * @param stack the imagestack
	 * @param n the nucleus
	 */
	private void detectForwardThresholdSignal(File sourceFile, ImageStack stack, Nucleus n){
		SignalCollection signalCollection = n.getSignalCollection();
		
		// choose the right stack number for the channel
		int stackNumber = Constants.rgbToStack(channel);
		
		// create a new detector
		Detector detector = new Detector();
		detector.setMaxSize(n.getArea() * options.getMaxFraction());
		detector.setMinSize(options.getMinSize());
		detector.setMinCirc(options.getMinCirc());
		detector.setMaxCirc(options.getMaxCirc());
		detector.setThreshold(options.getSignalThreshold());
		detector.setStackNumber(stackNumber);
		try{
			detector.run(stack);
		} catch(Exception e){
			logger.log("Error in signal detection: "+e.getMessage(), Logger.ERROR);
			for(StackTraceElement el : e.getStackTrace()){
				logger.log(el.toString(), Logger.STACK);
			}
		}
		List<Roi> roiList = detector.getRoiList();

		ArrayList<NuclearSignal> signals = new ArrayList<NuclearSignal>(0);

		if(!roiList.isEmpty()){
			
			logger.log(roiList.size()+" signals in stack "+stackNumber, Logger.DEBUG);

			for( Roi r : roiList){
				
				StatsMap values = detector.measure(r, stack);
				NuclearSignal s = new NuclearSignal( r, 
						values.get("Area"), 
						values.get("Feret"), 
						values.get("Perim"), 
						new XYPoint(values.get("XM")-n.getPosition()[Nucleus.X_BASE], 
									values.get("YM")-n.getPosition()[Nucleus.Y_BASE]),
						n.getImageName()+"-"+n.getNucleusNumber());

				// only keep the signal if it is within the nucleus
				if(Utils.createPolygon(n).contains(	(float) s.getCentreOfMass().getX(), 
													(float) s.getCentreOfMass().getY())){
					signals.add(s);
				}
				
			}
		} else {
			logger.log("No signal in stack "+stackNumber, Logger.DEBUG);
		}
		
//		if(!signals.isEmpty()){ // only add groups if they contain  signals
			signalCollection.addSignalGroup(signals, signalGroup, sourceFile, channel);
			signalCollection.setSignalGroupName(signalGroup, channelName);
			n.calculateSignalDistancesFromCoM();
			n.calculateFractionalSignalDistancesFromCoM();

			if(AsymmetricNucleus.class.isAssignableFrom(n.getClass())){
				if(n.getBorderTag(Constants.ASYMMETRIC_NUCLEUS_ORIENTATION_POINT)!=null){

					n.calculateSignalAnglesFromPoint(n.getBorderTag(Constants.ASYMMETRIC_NUCLEUS_ORIENTATION_POINT));
				}
			}
//		}
	}
	
	/**
	 * Detect a signal in a given stack by reverse thresholding
	 * and add to the given nucleus. Find the brightest pixels in
	 * the nuclear roi. If < maxSignalFraction, get dimmer pixels and
	 * remeasure. Continue until signal size is met. Works best with 
	 * maxSignalFraction of ~0.1 for a chromosome paint
	 * @param sourceFile the file the image came from
	 * @param stack the imagestack
	 * @param n the nucleus
	 */
	private void detectReverseThresholdSignal(File sourceFile, ImageStack stack, Nucleus n){
		
//		SignalCollection signalCollection = n.getSignalCollection();
		logger.log("Beginning reverse detection for nucleus");
		// choose the right stack number for the channel
		int stackNumber = Constants.rgbToStack(channel);
		
		ImageProcessor ip = stack.getProcessor(stackNumber);
		FloatPolygon polygon = Utils.createOriginalPolygon(n);
		
		// map brightness to count
		Map<Integer, Integer> counts = new HashMap<Integer, Integer>(0);
		for(int i=0;i<256;i++){
			counts.put(i, 0);
		}
		
		
//		get the region bounded by the nuclear roi
//		for max intensity (255) , downwards, count pixels with that intensity
//		if count / area < fraction, continue
		
		//sort the pixels in the roi to bins
		for(int width = 0; width<ip.getWidth();width++){
			for(int height = 0; height<ip.getHeight();height++){
				
				if(polygon.contains( (float) width, (float) height)){
					int brightness = ip.getPixel(width, height);
					int oldCount = counts.get(brightness);
					counts.put(brightness, oldCount+1);
				}
				
			}
		}
		
//		logger.log("Counts created", Logger.DEBUG);
//		for(int i=0;i<256;i++){
//			logger.log("Level "+i+": "+counts.get(i), Logger.DEBUG);
//		}
		
		// find the threshold from the bins
		int area = (int) ( n.getArea() * options.getMaxFraction());
		int total = 0;
		int threshold = 0; // the value to threshold at
		
		for(int brightness = 255; brightness>0; brightness--){
			
			total += counts.get(brightness); 
			
			if(total>area){
				threshold = brightness+1;
				break;
			}
		}
		logger.log("Threshold set at: "+threshold);
		
		// now we have the reverse threshold value, do the thresholding 
		// and find signal rois
		options.setThreshold(threshold);
		detectForwardThresholdSignal(sourceFile, stack, n);

	}

	/**
	 * Find signals in the given ImageStack, and add them to the given nucleus
	 * Called by AnalysisCreator when SwingWorker functions not needed
	 * TODO: remove dependency on this method
	 * @param n - the nucleus to add signals to
	 * @param stack - the ImageStack with signal channels
	 */
	public void run(Nucleus n, ImageStack stack, File sourceFile){

		for(int i = Constants.RGB_RED; i< Constants.RGB_BLUE; i++){
			logger.log("Running signal detector on channel "+i, Logger.DEBUG);
			this.channel	 = i;
			this.signalGroup = i;
			this.channelName = i==Constants.RGB_RED ? "Red" : "Green";
			detectSignal(sourceFile, stack, n);
		}
	}
}
