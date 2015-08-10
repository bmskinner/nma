package no.analysis;

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
import utility.Constants;
import utility.Logger;
import utility.StatsMap;
import utility.Utils;
import no.collections.CellCollection;
import no.components.AnalysisOptions.NuclearSignalOptions;
import no.components.NuclearSignal;
import no.components.SignalCollection;
import no.components.XYPoint;
import no.export.CompositeExporter;
import no.export.NucleusAnnotator;
import no.export.StatsExporter;
import no.imports.ImageImporter;
import no.nuclei.AsymmetricNucleus;
import no.nuclei.Nucleus;
import no.nuclei.RoundNucleus;


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
			
			// divide population into clusters with and without signals
			List<CellCollection> signalPopulations = dividePopulationBySignals(dataset.getCollection(), signalGroup);
			
			for(CellCollection collection : signalPopulations){
				processSubPopulation(collection);
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
				firePropertyChange("Finished", getProgress(), Constants.Progress.FINISHED.code());			
				
			} else {
				firePropertyChange("Error", getProgress(), Constants.Progress.ERROR.code());
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
	 * Create child datasets for signal populations
	 * and perform basic analyses
	 * @param collection
	 */
	private void processSubPopulation(CellCollection collection){

		AnalysisDataset subDataset = new AnalysisDataset(collection, dataset.getSavePath());
		subDataset.setAnalysisOptions(dataset.getAnalysisOptions());

		logger.log("Sub-population: "+collection.getType()+" : "+collection.getNucleusCount()+" nuclei");

		// use the same segmentation from the initial analysis
		MorphologyAnalysis.reapplyProfiles(collection, dataset.getCollection());


//		 measure general nuclear organisation
//		SignalAnalysis.run(collection);

		// Perform shell analysis with 5 shells by default
//		if(collection.getNucleusClass() == RoundNucleus.class){
//			ShellAnalysis.run(subDataset, 5);
//		}
		dataset.addChildDataset(subDataset);
	}
	
	/*
    Given a complete collection of nuclei, split it into up to 4 populations;
      nuclei with red signals, with green signals, without red signals and without green signals
    Only include the 'without' populations if there is a 'with' population.
	 */
	private List<CellCollection> dividePopulationBySignals(CellCollection r, int signalGroup){

		List<CellCollection> signalPopulations = new ArrayList<CellCollection>(0);
		logger.log("Dividing population by signals...");
		try{

			List<Cell> list = r.getCellsWithNuclearSignals(signalGroup, true);
			if(!list.isEmpty()){
				logger.log("Found nuclei with signals in group "+signalGroup);
				CellCollection listCollection = new CellCollection(r.getFolder(), 
						r.getOutputFolderName(), 
						"Signals_in_group_"+signalGroup, 
						r.getDebugFile(), 
						r.getNucleusClass());

				for(Cell c : list){
					listCollection.addCell( c );
				}
				signalPopulations.add(listCollection);

				List<Cell> notList = r.getCellsWithNuclearSignals(signalGroup, false);
				if(!notList.isEmpty()){
					logger.log("Found nuclei without signals in group "+signalGroup);
					CellCollection notListCollection = new CellCollection(r.getFolder(), 
							r.getOutputFolderName(), 
							"No_signals_in_group_"+signalGroup, 
							r.getDebugFile(), 
							r.getNucleusClass());

					for(Cell c : notList){
						notListCollection.addCell( c );
					}
					signalPopulations.add(notListCollection);
				}

			}

		} catch(Exception e){
			logger.log("Cannot create collection: "+e.getMessage(), Logger.ERROR);
		}

		return signalPopulations;
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
		

		signalCollection.addSignalGroup(signals, signalGroup, sourceFile, channel);
		signalCollection.setSignalGroupName(signalGroup, channelName);
		n.calculateSignalDistancesFromCoM();
		n.calculateFractionalSignalDistancesFromCoM();

		if(AsymmetricNucleus.class.isAssignableFrom(n.getClass())){
			if(n.getBorderTag(Constants.Nucleus.ASYMMETRIC.orientationPoint())!=null){

				n.calculateSignalAnglesFromPoint(n.getBorderTag(Constants.Nucleus.ASYMMETRIC.orientationPoint()));
			}
		}

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
		
		
		// only use the calculated threshold if it is larger than
		// the given minimum
		if(threshold > options.getSignalThreshold()){
			logger.log("Threshold set at: "+threshold);
			options.setThreshold(threshold);
		} else {
			logger.log("Threshold kept at minimum: "+options.getSignalThreshold());
		}
		
		// now we have the reverse threshold value, do the thresholding 
		// and find signal rois
		detectForwardThresholdSignal(sourceFile, stack, n);

	}
}
