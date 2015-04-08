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
import no.collections.NucleusCollection;
import no.collections.PigSpermNucleusCollection;
import no.collections.RodentSpermNucleusCollection;
import no.nuclei.Nucleus;
import no.nuclei.sperm.PigSpermNucleus;
import no.nuclei.sperm.RodentSpermNucleus;
import ij.gui.GenericDialog;
import ij.io.DirectoryChooser;
import ij.io.OpenDialog;

public class AnalysisSetup{

	private GenericDialog gd;

	private static final int RODENT_SPERM_NUCLEUS = 0;
	private static final int PIG_SPERM_NUCLEUS = 1;
	private static final int ROUND_NUCLEUS = 2;

	private  int    nucleusThreshold = 36;
	private  int    signalThreshold  = 70;
	private  double minNucleusSize   = 500;
	private  double maxNucleusSize   = 10000;
	private  double minNucleusCirc   = 0.0;
	private  double maxNucleusCirc   = 1.0;

	private int angleProfileWindowSize = 15;

	private Class<?> nucleusClass;

	/**
	 * The class of NucleusCollection to use in the analysis
	 */
	private Class<?> collectionClass;

	/**
	 * Should a reanalysis be performed?
	 */
	private boolean performReanalysis = false;

	/**
	 * Should images for a reanalysis be aligned
	 * beyond the offsets provided?
	 */
	private boolean realignMode = true;
	
	private File folder;
	private File mappingFile;

	private String refoldMode = "Fast";

	private int xoffset = 0;
	private int yoffset = 0;

	private  double minSignalSize = 5;
	private  double maxSignalFraction = 0.5;

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
		collectionClassTypes.put(RODENT_SPERM_NUCLEUS, new RodentSpermNucleusCollection().getClass());
		collectionClassTypes.put(PIG_SPERM_NUCLEUS, new PigSpermNucleusCollection().getClass());
		collectionClassTypes.put(ROUND_NUCLEUS, new NucleusCollection().getClass());

		nucleusClassTypes = new HashMap<Integer, Class<?>>();
		nucleusClassTypes.put(RODENT_SPERM_NUCLEUS, new RodentSpermNucleus().getClass());
		nucleusClassTypes.put(PIG_SPERM_NUCLEUS, new PigSpermNucleus().getClass());
		nucleusClassTypes.put(ROUND_NUCLEUS, new Nucleus().getClass());
	}

	public AnalysisSetup(){

		createOptionsPanel();

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
	    this.folder = new File(folderName);

	    if(performReanalysis){
	      OpenDialog fileDialog = new OpenDialog("Select a mapping file...");
	      String fileName = fileDialog.getPath();
	      if(fileName==null) return false;
	      this.mappingFile = new File(fileName);
	    }
	    return true;

	}


	/*
    -----------------------
    Getters
    -----------------------
	 */
	
	public File getFolder(){
		return this.folder;
	}
	
	public File getMappingFile(){
		return this.mappingFile;
	}

	public int getNucleusThreshold(){
		return this.nucleusThreshold;
	}

	public int getSignalThreshold(){
		return this.signalThreshold;
	}

	public double getMinNucleusSize(){
		return this.minNucleusSize;
	}

	public double getMaxNucleusSize(){
		return this.maxNucleusSize;
	}

	public double getMinNucleusCirc(){
		return this.minNucleusCirc;
	}

	public double getMaxNucleusCirc(){
		return this.maxNucleusCirc;
	}

	public double getMinSignalSize(){
		return this.minSignalSize;
	}

	public double getMaxSignalFraction(){
		return this.maxSignalFraction;
	}

	public int getAngleProfileWindowSize(){
		return this.angleProfileWindowSize;
	}
	
	public Class<?> getNucleusClass(){
		return this.nucleusClass;
	}
	
	public Class<?> getCollectionClass(){
		return this.collectionClass;
	}
	
	public String getRefoldMode(){
		return this.refoldMode;
	}
	
	public boolean isReanalysis(){
		return this.performReanalysis;
	}
	
	public boolean realignImages(){
		return this.realignMode;
	}
	
	public int getXOffset(){
		return  this.xoffset;
	}
	
	public int getYOffset(){
		return  this.yoffset;
	}

	private String[] getNucleusTypeStrings(){
		return AnalysisSetup.nucleusTypes.keySet().toArray(new String[0]);
	}

	private void createOptionsPanel(){
		gd = new GenericDialog("New analysis");
		gd.addNumericField("Nucleus threshold: ", nucleusThreshold, 0);
		gd.addNumericField("Signal threshold: ", signalThreshold, 0);
		gd.addNumericField("Min nuclear size: ", minNucleusSize, 0);
		gd.addNumericField("Max nuclear size: ", maxNucleusSize, 0);
		gd.addNumericField("Min nuclear circ: ", minNucleusCirc, 2);
		gd.addNumericField("Max nuclear circ: ", maxNucleusCirc, 2);
		gd.addNumericField("Min signal size: ", minSignalSize, 0);
		gd.addNumericField("Max signal fraction: ", maxSignalFraction, 2);
		gd.addNumericField("Profile window size: ", angleProfileWindowSize, 0);

		String[] items = this.getNucleusTypeStrings();
		gd.addChoice("Nucleus type", items, items[1]); // default to rodent for now

		Set<String> modeSet = CurveRefolder.MODES.keySet();
		String[] modeArray = modeSet.toArray(new String[modeSet.size()]);
		gd.addRadioButtonGroup("Consensus refolding mode:", modeArray, 1, 3, "Fast");

		gd.addCheckbox("Perform re-analysis", false);
		gd.addNumericField("X offset:      ", xoffset, 0);
		gd.addNumericField("Y offset:      ", yoffset, 0);
		gd.addCheckbox("Realign each image", true);

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

		nucleusThreshold = (int) gd.getNextNumber();
		signalThreshold = (int) gd.getNextNumber();
		minNucleusSize = gd.getNextNumber();
		maxNucleusSize = gd.getNextNumber();
		minNucleusCirc = gd.getNextNumber();
		maxNucleusCirc = gd.getNextNumber();
		minSignalSize = gd.getNextNumber();
		maxSignalFraction = gd.getNextNumber();
		angleProfileWindowSize = (int) gd.getNextNumber();
		performReanalysis = gd.getNextBoolean();
		xoffset = (int)gd.getNextNumber();
		yoffset = (int)gd.getNextNumber();

		String nucleusType = gd.getNextChoice();
		int nucleusCode = AnalysisSetup.nucleusTypes.get(nucleusType);
		this.collectionClass = AnalysisSetup.collectionClassTypes.get(nucleusCode);
		this.nucleusClass = AnalysisSetup.nucleusClassTypes.get(nucleusCode);
		this.refoldMode = gd.getNextRadioButton();
		this.realignMode = gd.getNextBoolean();
	}


}
