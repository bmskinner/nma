package no.analysis;

import ij.IJ;
import ij.gui.Roi;

import java.util.ArrayList;
import java.util.List;

import no.components.NuclearSignal;
import no.components.SignalCollection;
import no.components.XYPoint;
import no.nuclei.INuclearFunctions;
import no.utility.ImageImporter;
import no.utility.StatsMap;

public class SignalDetector {

	private  int    signalThreshold = 70;
	private  double   minSignalSize = 5;
	private  double   maxSignalFraction = 0.5;

	public SignalDetector(){

	}

	public void setSignalThreshold(int i){
		if(i<0){
			throw new IllegalArgumentException("Value must be positive");
		}
		this.signalThreshold = i;
	}

	public void setMinSignalSize(double d){
		if(d<0){
			throw new IllegalArgumentException("Value must be positive");
		}
		this.minSignalSize = d;
	}

	public void setMaxSignalFraction(double d){
		if(d<0 || d>1){
			throw new IllegalArgumentException("Value must be between 0 and 1");
		}
		this.maxSignalFraction = d;
	}

	public void run(INuclearFunctions n){

		SignalCollection signalCollection = n.getSignalCollection();

		// find the signals
		// within nuclear roi, analyze particles in colour channels
		// the nucleus is in index 1, so from 2 to end
		for(int channel=ImageImporter.FIRST_SIGNAL_CHANNEL;channel<=n.getImagePlanes().getSize();channel++){

			Detector detector = new Detector();
			detector.setMaxSize(n.getArea() * this.maxSignalFraction);
			detector.setMinSize(this.minSignalSize);
			detector.setMinCirc(0);
			detector.setMaxCirc(1);
			detector.setThreshold(this.signalThreshold);
			detector.setChannel(channel);
			try{
				detector.run(n.getImagePlanes());
			} catch(Exception e){
				IJ.log("Error in signal detection: "+e.getMessage());
			}
			List<Roi> roiList = detector.getRoiList();

			ArrayList<NuclearSignal> signals = new ArrayList<NuclearSignal>(0);

			if(!roiList.isEmpty()){

				for( Roi r : roiList){

					StatsMap values = detector.measure(r, n.getImagePlanes());
					NuclearSignal s = new NuclearSignal( r, 
							values.get("Area"), 
							values.get("Feret"), 
							values.get("Perim"), 
							new XYPoint(values.get("XM"), values.get("YM")),
							n.getImageName()+"-"+n.getNucleusNumber());

					signals.add(s);
				}
			} 
			signalCollection.addChannel(signals, channel);
		} 
	}
}
