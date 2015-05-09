/*
-------------------------------------------------
ANALYSIS SETUP OPTIONS
-------------------------------------------------

*/
package no.gui;

import java.io.File;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;

import no.analysis.CurveRefolder;
import no.collections.RoundNucleusCollection;
import no.collections.PigSpermNucleusCollection;
import no.collections.RodentSpermNucleusCollection;
import no.components.AnalysisOptions;
import no.nuclei.RoundNucleus;
import no.nuclei.sperm.PigSpermNucleus;
import no.nuclei.sperm.RodentSpermNucleus;
import ij.gui.GenericDialog;
import ij.io.DirectoryChooser;
import ij.io.OpenDialog;

public class AnalysisSetup{

	private GenericDialog gd;
		
	private AnalysisOptions analysisOptions = new AnalysisOptions();

	private static final int RODENT_SPERM_NUCLEUS = 0;
	private static final int PIG_SPERM_NUCLEUS = 1;
	private static final int ROUND_NUCLEUS = 2;

	private static Map<String, Integer> nucleusTypes;
	private static Map<Integer, Class<?>>  collectionClassTypes;
	private static Map<Integer, Class<?>>  nucleusClassTypes;

	static
	{
		nucleusTypes = new HashMap<String, Integer>();
		nucleusTypes.put("Rodent sperm" , RODENT_SPERM_NUCLEUS);
		nucleusTypes.put("Pig sperm"    , PIG_SPERM_NUCLEUS);
		nucleusTypes.put("Round nucleus", ROUND_NUCLEUS);

		collectionClassTypes = new HashMap<Integer, Class<?>>();
		collectionClassTypes.put(RODENT_SPERM_NUCLEUS, RodentSpermNucleusCollection.class);
		collectionClassTypes.put(PIG_SPERM_NUCLEUS, PigSpermNucleusCollection.class);
		collectionClassTypes.put(ROUND_NUCLEUS, RoundNucleusCollection.class);

		nucleusClassTypes = new HashMap<Integer, Class<?>>();
		nucleusClassTypes.put(RODENT_SPERM_NUCLEUS, new RodentSpermNucleus().getClass());
		nucleusClassTypes.put(PIG_SPERM_NUCLEUS, new PigSpermNucleus().getClass());
		nucleusClassTypes.put(ROUND_NUCLEUS, new RoundNucleus().getClass());
	}

	public AnalysisSetup(){

		setDefaultOptions();
		createOptionsPanel();

	}
	
	public void setDefaultOptions(){
		analysisOptions.setNucleusThreshold(36);
		analysisOptions.setSignalThreshold(70);
		
		analysisOptions.setMinNucleusSize(500);
		analysisOptions.setMaxNucleusSize(10000);
		
		analysisOptions.setMinNucleusCirc(0.0);
		analysisOptions.setMaxNucleusCirc(1.0);
		
		analysisOptions.setMinSignalSize(5);
		analysisOptions.setMaxSignalFraction(0.5);

		analysisOptions.setAngleProfileWindowSize(15);

		analysisOptions.setPerformReanalysis(false);
		analysisOptions.setRealignMode(true);

		analysisOptions.setRefoldNucleus(true);
		analysisOptions.setRefoldMode("Fast");
		
		analysisOptions.setXoffset(0);
		analysisOptions.setYoffset(0);
	}
	
	public boolean run(){
		boolean ok = showPanel();

		if(ok){
			fetchOptions();
			if(getFiles()){
				return true;
			} else {
				return false;
			}
		} else {
			return false;
		}
	}
	
	private boolean getFiles(){

	    DirectoryChooser localOpenDialog = new DirectoryChooser("Select directory of images...");
	    String folderName = localOpenDialog.getDirectory();

	    if(folderName==null) return false; // user cancelled
	    analysisOptions.setFolder( new File(folderName));

	    if(analysisOptions.isReanalysis()){
	      OpenDialog fileDialog = new OpenDialog("Select a mapping file...");
	      String fileName = fileDialog.getPath();
	      if(fileName==null) return false;
	      analysisOptions.setMappingFile(new File(fileName));
	    }
	    return true;
	}


	/*
    -----------------------
    Getters
    -----------------------
	 */
	public AnalysisOptions getOptions(){
		return this.analysisOptions;
	}
	
	private String[] getNucleusTypeStrings(){
		return AnalysisSetup.nucleusTypes.keySet().toArray(new String[0]);
	}

	private void createOptionsPanel(){
		
//		AnalysisSetupWindow demoWindow = new AnalysisSetupWindow(); // will not work - needs >= java 7
		
		gd = new GenericDialog("New analysis");
	
		gd.addNumericField("Nucleus threshold: ", analysisOptions.getNucleusThreshold(), 0);
		gd.addNumericField("Signal threshold: ", analysisOptions.getSignalThreshold(), 0);
		gd.addNumericField("Min nuclear size: ", analysisOptions.getMinNucleusSize(), 0);
		gd.addNumericField("Max nuclear size: ", analysisOptions.getMaxNucleusSize(), 0);
		gd.addNumericField("Min nuclear circ: ", analysisOptions.getMinNucleusCirc(), 2);
		gd.addNumericField("Max nuclear circ: ", analysisOptions.getMaxNucleusCirc(), 2);
		gd.addNumericField("Min signal size: ", analysisOptions.getMinSignalSize(), 0);
		gd.addNumericField("Max signal fraction: ", analysisOptions.getMaxSignalFraction(), 2);
		gd.addNumericField("Profile window size: ", analysisOptions.getAngleProfileWindowSize(), 0);

		String[] items = this.getNucleusTypeStrings();
		gd.addChoice("Nucleus type", items, items[1]); // default to rodent for now
		
		gd.addCheckbox("Refold consensus nucleus?", analysisOptions.refoldNucleus());

		Set<String> modeSet = CurveRefolder.MODES.keySet();
		String[] modeArray = modeSet.toArray(new String[modeSet.size()]);
		gd.addRadioButtonGroup("Consensus refolding mode:", modeArray, 1, 3, analysisOptions.getRefoldMode());

		gd.addCheckbox("Perform re-analysis", analysisOptions.isReanalysis());
		gd.addNumericField("X offset:      ", analysisOptions.getXOffset(), 0);
		gd.addNumericField("Y offset:      ", analysisOptions.getYOffset(), 0);
		gd.addCheckbox("Realign each image", analysisOptions.realignImages());

	}

	private boolean showPanel(){
		gd.showDialog();
		if (gd.wasCanceled()){
			return false;
		} else {
			return true;
		}

	}

	private void fetchOptions(){
		
		analysisOptions.setNucleusThreshold((int) gd.getNextNumber());
		analysisOptions.setSignalThreshold((int) gd.getNextNumber());
		
		analysisOptions.setMinNucleusSize(gd.getNextNumber());
		analysisOptions.setMaxNucleusSize(gd.getNextNumber());
		
		analysisOptions.setMinNucleusCirc(gd.getNextNumber());
		analysisOptions.setMaxNucleusCirc(gd.getNextNumber());
		
		analysisOptions.setMinSignalSize(gd.getNextNumber());
		analysisOptions.setMaxSignalFraction(gd.getNextNumber());

		analysisOptions.setAngleProfileWindowSize((int) gd.getNextNumber());

		analysisOptions.setRefoldNucleus(gd.getNextBoolean());
		analysisOptions.setPerformReanalysis(gd.getNextBoolean());
				
		analysisOptions.setXoffset((int)gd.getNextNumber());
		analysisOptions.setYoffset((int)gd.getNextNumber());
		
		
		String nucleusType = gd.getNextChoice();
		int nucleusCode = AnalysisSetup.nucleusTypes.get(nucleusType);
		
		analysisOptions.setCollectionClass(AnalysisSetup.collectionClassTypes.get(nucleusCode));
		analysisOptions.setNucleusClass(AnalysisSetup.nucleusClassTypes.get(nucleusCode));
		
		analysisOptions.setRefoldMode(gd.getNextRadioButton());
		analysisOptions.setRealignMode(gd.getNextBoolean());

//		nucleusThreshold = 
//		signalThreshold = 
//		minNucleusSize = 
//		maxNucleusSize = 
//		minNucleusCirc = 
//		maxNucleusCirc = 
//		minSignalSize = 
//		maxSignalFraction = 
//		angleProfileWindowSize = ;
//		this.refoldNucleus = ;
//		
//		
//		performReanalysis = ;
//		xoffset = ;
//		yoffset = ;
//
//		
//		
//		this.collectionClass = 
//		this.nucleusClass = 
//		
//		
//		this.refoldMode = ;
//		this.realignMode = ;
	}


}
