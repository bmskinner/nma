package no.analysis;

import ij.IJ;
import ij.ImageStack;
import ij.gui.Roi;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

import utility.Constants;
import utility.Logger;
import utility.StatsMap;
import no.components.NuclearSignal;
import no.components.SignalCollection;
import no.components.XYPoint;
import no.imports.ImageImporter;
import no.nuclei.Nucleus;


public class SignalDetector {

	private  int    signalThreshold = 70;
	private  double   minSignalSize = 5;
	private  double   maxSignalFraction = 0.5;
	
	protected Logger logger;

	/**
	 * Empty constructor. Detector will have default values
	 */
	public SignalDetector(){
		
	}
	
	
	/**
	 * Constructor specifying detection parameters.
	 * @param threshold - the signal threshold level
	 * @param minSize - the minumum size of a signal in pixels
	 * @param maxFraction - the maximum fractional area of the nucleus covered
	 */
	public SignalDetector(int threshold, double minSize, double maxFraction, File debugFile){
		this.setSignalThreshold(threshold);
		this.setMaxSignalFraction(maxFraction);
		this.setMinSignalSize(minSize);
		this.logger = new Logger(debugFile, "SignalDetector");
		logger.log("Created signal detector", Logger.DEBUG);
	}

	/**
	 * Set the threshold level
	 * @param i the new threshold
	 */
	public void setSignalThreshold(int i){
		if(i<0){
			throw new IllegalArgumentException("Value must be positive");
		}
		this.signalThreshold = i;
	}

	/**
	 * Set the minimum size
	 * @param d - the minumum size of a signal in pixels
	 */
	public void setMinSignalSize(double d){
		if(d<0){
			throw new IllegalArgumentException("Value must be positive");
		}
		this.minSignalSize = d;
	}

	/**
	 * Set the maximum fraction
	 * @param d - the maximum fractional area of the nucleus covered
	 */
	public void setMaxSignalFraction(double d){
		if(d<0 || d>1){
			throw new IllegalArgumentException("Value must be between 0 and 1");
		}
		this.maxSignalFraction = d;
	}

	/**
	 * Find signals in the given ImageStack, and add them to the given nucleus
	 * @param n - the nucleus to add signals to
	 * @param stack - the ImageStack with signal channels
	 */
	public void run(Nucleus n, ImageStack stack, File sourceFile){

		logger.log("Running signal detector", Logger.DEBUG);
		SignalCollection signalCollection = n.getSignalCollection();
		

		// find the signals
		// within nuclear roi, analyze particles in colour channels
		// the nucleus is in index 1, so from 2 to end
		for(int stackNumber=Constants.FIRST_SIGNAL_CHANNEL;stackNumber<=stack.getSize();stackNumber++){

			// assume rgb image, with blue as counterstain for now
			int channel = stackNumber==Constants.FIRST_SIGNAL_CHANNEL ? 0 : 1;
			String channelName = stackNumber==Constants.FIRST_SIGNAL_CHANNEL ? "Red" : "Green";
			
			// create a new detector to find the signals
			Detector detector = new Detector();
			detector.setMaxSize(n.getArea() * this.maxSignalFraction);
			detector.setMinSize(this.minSignalSize);
			detector.setMinCirc(0);
			detector.setMaxCirc(1);
			detector.setThreshold(this.signalThreshold);
			detector.setStackNumber(stackNumber);
			try{
				detector.run(stack);
			} catch(Exception e){
				IJ.log("Error in signal detection: "+e.getMessage());
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
							new XYPoint(values.get("XM"), values.get("YM")),
							n.getImageName()+"-"+n.getNucleusNumber());

					signals.add(s);
				}
			} else {
				logger.log("No signal in stack "+stackNumber, Logger.DEBUG);
			}
			signalCollection.addSignalGroup(signals, channel, sourceFile, channel);
			signalCollection.setSignalGroupName(channel, channelName);
		} 
		n.calculateSignalDistancesFromCoM();
		n.calculateFractionalSignalDistancesFromCoM();
	}
}
