/*
-------------------------------------------------
ANALYSIS SETUP OPTIONS
-------------------------------------------------

*/
package no.gui;

import ij.gui.GenericDialog;

public class AnalysisSetup{

	private GenericDialog gd;

	public AnalysisSetup(){

//		createOptionsPanel();

//		boolean ok = run();

//		if(ok){
//			fetchResults();
//		}

	}

//	private void createOptionsPanel(){
//		gd = new GenericDialog("New analysis");
//		gd.addNumericField("Nucleus threshold: ", nucleusThreshold, 0);
//		gd.addNumericField("Signal threshold: ", signalThreshold, 0);
//		gd.addNumericField("Min nuclear size: ", minNucleusSize, 0);
//		gd.addNumericField("Max nuclear size: ", maxNucleusSize, 0);
//		gd.addNumericField("Min nuclear circ: ", minNucleusCirc, 2);
//		gd.addNumericField("Max nuclear circ: ", maxNucleusCirc, 2);
//		gd.addNumericField("Min signal size: ", minSignalSize, 0);
//		gd.addNumericField("Max signal fraction: ", maxSignalFraction, 2);
//		gd.addNumericField("Profile window size: ", angleProfileWindowSize, 0);
//
//		String[] items = this.getNucleusTypeStrings();
//		gd.addChoice("Nucleus type", items, items[1]); // default to rodent for now
//
//		Set<String> modeSet = CurveRefolder.MODES.keySet();
//		String[] modeArray = modeSet.toArray(new String[modeSet.size()]);
//		gd.addRadioButtonGroup("Consensus refolding mode:", modeArray, 1, 3, "Fast");
//
//		gd.addCheckbox("Perform re-analysis", false);
//		gd.addNumericField("X offset:      ", xoffset, 0);
//		gd.addNumericField("Y offset:      ", yoffset, 0);
//		gd.addCheckbox("Realign each image", true);
//
//	}

	private boolean run(){
		gd.showDialog();
		if (gd.wasCanceled()){
			return false;
		} else {
			return true;
		}

	}

//	public boolean fetchOptions(){
//	
//	
//
//	nucleusThreshold = (int) gd.getNextNumber();
//	signalThreshold = (int) gd.getNextNumber();
//	minNucleusSize = gd.getNextNumber();
//	maxNucleusSize = gd.getNextNumber();
//	minNucleusCirc = gd.getNextNumber();
//	maxNucleusCirc = gd.getNextNumber();
//	minSignalSize = gd.getNextNumber();
//	maxSignalFraction = gd.getNextNumber();
//	angleProfileWindowSize = (int) gd.getNextNumber();
//	performReanalysis = gd.getNextBoolean();
//	xoffset = (int)gd.getNextNumber();
//	yoffset = (int)gd.getNextNumber();
//
//	String nucleusType = gd.getNextChoice();
//	int nucleusCode = this.nucleusTypes.get(nucleusType);
//	this.collectionClass = this.collectionClassTypes.get(nucleusCode);
//	this.nucleusClass = this.nucleusClassTypes.get(nucleusCode);
//	this.refoldMode = gd.getNextRadioButton();
//	this.realignMode = gd.getNextBoolean();
//	return true;
//  }


}
