package com.bmskinner.nuclear_morphology;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import java.awt.Color;
import java.io.File;
import java.nio.file.Files;
import java.nio.file.StandardCopyOption;
import java.util.UUID;
import java.util.logging.Handler;
import java.util.logging.Level;
import java.util.logging.Logger;

import org.junit.Before;
import org.junit.Test;

import com.bmskinner.nuclear_morphology.analysis.classification.NucleusClusteringMethod;
import com.bmskinner.nuclear_morphology.analysis.nucleus.ConsensusAveragingMethod;
import com.bmskinner.nuclear_morphology.analysis.nucleus.NucleusDetectionMethod;
import com.bmskinner.nuclear_morphology.analysis.nucleus.ProfileRefoldMethod;
import com.bmskinner.nuclear_morphology.analysis.nucleus.ProfileRefoldMethod.CurveRefoldingMode;
import com.bmskinner.nuclear_morphology.analysis.profiles.DatasetProfilingMethod;
import com.bmskinner.nuclear_morphology.analysis.profiles.DatasetSegmentationMethod;
import com.bmskinner.nuclear_morphology.analysis.profiles.DatasetSegmentationMethod.MorphologyAnalysisMode;
import com.bmskinner.nuclear_morphology.analysis.signals.SignalDetectionMethod;
import com.bmskinner.nuclear_morphology.analysis.signals.shells.ShellAnalysisMethod;
import com.bmskinner.nuclear_morphology.components.CellularComponent;
import com.bmskinner.nuclear_morphology.components.IAnalysisDataset;
import com.bmskinner.nuclear_morphology.components.generic.Version;
import com.bmskinner.nuclear_morphology.components.nuclear.ISignalGroup;
import com.bmskinner.nuclear_morphology.components.nuclear.NucleusType;
import com.bmskinner.nuclear_morphology.components.nuclear.SignalGroup;
import com.bmskinner.nuclear_morphology.components.options.DefaultShellOptions;
import com.bmskinner.nuclear_morphology.components.options.IAnalysisOptions;
import com.bmskinner.nuclear_morphology.components.options.IClusteringOptions;
import com.bmskinner.nuclear_morphology.components.options.IDetectionOptions;
import com.bmskinner.nuclear_morphology.components.options.INuclearSignalOptions;
import com.bmskinner.nuclear_morphology.components.options.OptionsFactory;
import com.bmskinner.nuclear_morphology.io.DatasetExportMethod;
import com.bmskinner.nuclear_morphology.io.xml.OptionsXMLWriter;
import com.bmskinner.nuclear_morphology.logging.ConsoleHandler;
import com.bmskinner.nuclear_morphology.logging.LogPanelFormatter;
import com.bmskinner.nuclear_morphology.logging.Loggable;

/**
 * Generate the test datasets for the current version, which will be used for comparison
 * testing with older versions by other classes. This class should be run first in test
 * suites.
 * @author ben
 * @since 1.14.0
 *
 */
public class TestImageDatasetCreator {
	
	protected Logger logger;
	
	public static final UUID RED_SIGNAL_ID   = UUID.fromString("00000000-0000-0000-0000-100000000001");
	public static final UUID GREEN_SIGNAL_ID = UUID.fromString("00000000-0000-0000-0000-100000000002");
	
	public static final String RED_SIGNAL_NAME   = "Test red";
	public static final String GREEN_SIGNAL_NAME = "Test green";

	@Before
	public void setUp(){
		logger = Logger.getLogger(Loggable.CONSOLE_LOGGER);
		logger.setLevel(Level.FINE);

		boolean hasHandler = false;
		for(Handler h : logger.getHandlers()) {
			if(h instanceof ConsoleHandler)
				hasHandler = true;
		}
		if(!hasHandler)
			logger.addHandler(new ConsoleHandler(new LogPanelFormatter()));
	}

	
    @Test
    public void createMouseDataset() throws Exception{

    	File testFolder = new File(TestResources.TESTING_MOUSE_FOLDER);
    	IAnalysisOptions op = OptionsFactory.makeDefaultRodentAnalysisOptions(testFolder);
    	File saveFile = new File(TestResources.MOUSE_TEST_DATASET);
    	IAnalysisDataset d = createTestDataset(TestResources.UNIT_TEST_FOLDERNAME, op, false);
    	saveTestDataset(d, saveFile);
    }
    
    @Test
    public void createPigDataset() throws Exception{

    	File testFolder = new File(TestResources.TESTING_PIG_FOLDER);
    	IAnalysisOptions op = OptionsFactory.makeDefaultPigAnalysisOptions(testFolder);
    	File saveFile = new File(TestResources.PIG_TEST_DATASET);
    	IAnalysisDataset d = createTestDataset(TestResources.UNIT_TEST_FOLDERNAME, op, false);
    	saveTestDataset(d, saveFile);
    }
    
    @Test
    public void createRoundDataset() throws Exception{

    	File testFolder = new File(TestResources.TESTING_ROUND_FOLDER);
    	IAnalysisOptions op = OptionsFactory.makeDefaultRoundAnalysisOptions(testFolder);
    	File saveFile = new File(TestResources.ROUND_TEST_DATASET);
    	IAnalysisDataset d = createTestDataset(TestResources.UNIT_TEST_FOLDERNAME, op, false);
    	saveTestDataset(d, saveFile);
    }
    
    @Test
    public void createMultipleSource1Dataset() throws Exception{

    	File testFolder = new File(TestResources.TESTING_MULTIPLE_SOURCE_1_FOLDER);
    	IAnalysisOptions op = OptionsFactory.makeDefaultRodentAnalysisOptions(testFolder);
    	File saveFile = new File(TestResources.MULTIPLE1_TEST_DATASET);
    	IAnalysisDataset d = createTestDataset(TestResources.UNIT_TEST_FOLDERNAME, op, false);
    	saveTestDataset(d, saveFile);
    }
    
    @Test
    public void createMultipleSource2Dataset() throws Exception{

    	File testFolder = new File(TestResources.TESTING_MULTIPLE_SOURCE_2_FOLDER);
    	IAnalysisOptions op = OptionsFactory.makeDefaultRodentAnalysisOptions(testFolder);
    	File saveFile = new File(TestResources.MULTIPLE2_TEST_DATASET);
    	IAnalysisDataset d = createTestDataset(TestResources.UNIT_TEST_FOLDERNAME, op, false);
    	saveTestDataset(d, saveFile);
    }
    
    
    @Test
    public void createMouseWithClustersDataset() throws Exception{

    	File testFolder = new File(TestResources.TESTING_MOUSE_CLUSTERS_FOLDER);
    	IAnalysisOptions op = OptionsFactory.makeDefaultRodentAnalysisOptions(testFolder);
    	File saveFile = new File(TestResources.MOUSE_CLUSTERS_DATASET);
    	IAnalysisDataset d = createTestDataset(TestResources.UNIT_TEST_FOLDERNAME, op, true);
    	saveTestDataset(d, saveFile);
    }
    
    @Test
    public void createPigWithClustersDataset() throws Exception{

    	File testFolder = new File(TestResources.TESTING_PIG_CLUSTERS_FOLDER);
    	IAnalysisOptions op = OptionsFactory.makeDefaultPigAnalysisOptions(testFolder);
    	File saveFile = new File(TestResources.PIG_CLUSTERS_DATASET);
    	IAnalysisDataset d = createTestDataset(TestResources.UNIT_TEST_FOLDERNAME, op, true);
    	saveTestDataset(d, saveFile);
    }
    
    @Test
    public void createRoundWithClustersDataset() throws Exception{

    	File testFolder = new File(TestResources.TESTING_ROUND_CLUSTERS_FOLDER);
    	IAnalysisOptions op = OptionsFactory.makeDefaultRoundAnalysisOptions(testFolder);
    	File saveFile = new File(TestResources.ROUND_CLUSTERS_DATASET);
    	IAnalysisDataset d = createTestDataset(TestResources.UNIT_TEST_FOLDERNAME, op, true);
    	saveTestDataset(d, saveFile);
    }
    
    @Test
    public void createMouseWithSignalsDataset() throws Exception {

    	File testFolder = new File(TestResources.TESTING_MOUSE_SIGNALS_FOLDER);
    	IAnalysisOptions op = OptionsFactory.makeDefaultRodentAnalysisOptions(testFolder);
    	IDetectionOptions nucleus = op.getDetectionOptions(IAnalysisOptions.NUCLEUS).get();
    	nucleus.setMaxSize(12000);
    	nucleus.setMinSize(4000);
    	File saveFile = new File(TestResources.MOUSE_SIGNALS_DATASET);
    	IAnalysisDataset d = createTestSignalDataset(op, true, false);
    	saveTestDataset(d, saveFile);
    }
    
    @Test
    public void createPigWithSignalsDataset() throws Exception {

    	File testFolder = new File(TestResources.TESTING_PIG_SIGNALS_FOLDER);
    	IAnalysisOptions op = OptionsFactory.makeDefaultPigAnalysisOptions(testFolder);
    	IDetectionOptions nucleus = op.getDetectionOptions(IAnalysisOptions.NUCLEUS).get();
    	nucleus.setMaxSize(15000);
    	nucleus.setMinSize(4000);
    	File saveFile = new File(TestResources.PIG_SIGNALS_DATASET);
    	
    	IAnalysisDataset d = createTestSignalDataset(op, false, true);
    	saveTestDataset(d, saveFile);
    }
    
    @Test
    public void createRoundWithSignalsDataset() throws Exception {

    	File testFolder = new File(TestResources.TESTING_ROUND_SIGNALS_FOLDER);
    	IAnalysisOptions op = OptionsFactory.makeDefaultRoundAnalysisOptions(testFolder);
    	File saveFile = new File(TestResources.ROUND_SIGNALS_DATASET);
    	
    	IAnalysisDataset d = createTestSignalDataset(op, true, true);
    	saveTestDataset(d, saveFile);
    }
    
    
    /**
     * Run a new analysis on the images using the given options.
     * @param op the nucleus detection options
     * @param saveFile the full path to the nmd file
     * @param addRed should red signals be detected with default parameters?
     * @param addGreen should green signals be detected with default parameters?
     * @throws Exception
     */
    private static IAnalysisDataset createTestSignalDataset(IAnalysisOptions op, boolean addRed, boolean addGreen) throws Exception {
    	
    	File testFolder = op.getDetectionOptions(CellularComponent.NUCLEUS).get().getFolder();
    	if(!testFolder.exists()){
            throw new IllegalArgumentException("Detection folder does not exist");
        }
    	
    	IAnalysisDataset d = new NucleusDetectionMethod(testFolder, op).call().getFirstDataset();
        
        new DatasetProfilingMethod(d)
	    	.then(new DatasetSegmentationMethod(d, MorphologyAnalysisMode.NEW))
	    	.then(op.getNucleusType().equals(NucleusType.ROUND)
	    			? new ProfileRefoldMethod(d, CurveRefoldingMode.FAST)
	    		    : new ConsensusAveragingMethod(d))
	    	.call();

        if(addRed) {
        	INuclearSignalOptions redOptions = OptionsFactory.makeNuclearSignalOptions(testFolder);
        	redOptions.setMaxFraction(0.5);
        	redOptions.setMinSize(5);

        	ISignalGroup red = new SignalGroup(RED_SIGNAL_NAME);
        	red.setGroupColour(Color.RED);
        	d.getCollection().addSignalGroup(RED_SIGNAL_ID, red);
        	d.getAnalysisOptions().get().setDetectionOptions(RED_SIGNAL_ID.toString(), redOptions);
        	new SignalDetectionMethod(d, redOptions, RED_SIGNAL_ID).call();
        }
        
        if(addGreen) {
        	 INuclearSignalOptions greenOptions = OptionsFactory.makeNuclearSignalOptions(testFolder);
             greenOptions.setChannel(1);
             ISignalGroup green = new SignalGroup(GREEN_SIGNAL_NAME);
             green.setGroupColour(Color.GREEN);
             d.getCollection().addSignalGroup(GREEN_SIGNAL_ID, green);
             d.getAnalysisOptions().get().setDetectionOptions(GREEN_SIGNAL_ID.toString(), greenOptions);
             new SignalDetectionMethod(d, greenOptions, GREEN_SIGNAL_ID).call();
        }

    	new ShellAnalysisMethod(d, new DefaultShellOptions()).call();
    	return d;
    }
    
    public static IAnalysisDataset createTestDataset(String folder, IAnalysisOptions op, boolean makeClusters) throws Exception {
    	 if(!op.getDetectionOptions(CellularComponent.NUCLEUS).get().getFolder().exists())
             throw new IllegalArgumentException("Detection folder does not exist");

         IAnalysisDataset d = new NucleusDetectionMethod(folder, op).call().getFirstDataset();
         
         IClusteringOptions clusterOptions = OptionsFactory.makeClusteringOptions();
         
         new DatasetProfilingMethod(d)
 	    	.then(new DatasetSegmentationMethod(d, MorphologyAnalysisMode.NEW))
 	    	.then( op.getNucleusType()==NucleusType.ROUND
 	    			? new ProfileRefoldMethod(d,CurveRefoldingMode.FAST)
 	    			: new ConsensusAveragingMethod(d))
 	    	.thenIf(makeClusters, new NucleusClusteringMethod(d, clusterOptions))
 	    	.call();
         return d;
    }
   
    /**
     * Save the given dataset to the desired output folder, and create a backup file
     * in the appropriate {@link TestResources#DATASET_FOLDER}.
     * @param d the dataset to save
     * @param saveFile the full path to the nmd file
     * @return the new dataset
     * @throws Exception
     */
    public static void saveTestDataset(IAnalysisDataset d, File saveFile) throws Exception {
        if(saveFile.exists())
        	saveFile.delete();
        assertFalse("Expecting output file to be deleted: "+saveFile.getAbsolutePath(), saveFile.exists());
    	new DatasetExportMethod(d, saveFile).call();
        assertTrue("Expecting file saved to "+saveFile.getAbsolutePath(), saveFile.exists());
        
        // Copy the saved file into backup file for comparison and conversion testing in the next version.
        String bakName = saveFile.getName().replaceAll(".nmd$", ".bak");
        File bakFile = new File(TestResources.DATASET_FOLDER+Version.currentVersion(), bakName);
        if(!bakFile.getParentFile().exists())
        	bakFile.getParentFile().mkdirs();
        if(bakFile.exists())
        	bakFile.delete();
        assertFalse("Expecting backup file to be deleted: "+bakFile.getAbsolutePath(), bakFile.exists());
        Files.copy(saveFile.getAbsoluteFile().toPath(), bakFile.getAbsoluteFile().toPath(), StandardCopyOption.COPY_ATTRIBUTES);
        assertTrue("Expecting backup copied to "+bakFile.getAbsolutePath(), bakFile.exists());
        
        // Create an xml representation of the analysis options for pipeline testing
        String xmlName = saveFile.getName().replaceAll(".nmd$", ".xml");
        File xmlFile = new File(TestResources.IMAGE_FOLDER, xmlName);
        if(xmlFile.exists())
        	xmlFile.delete();
        assertFalse("Expecting xml file to be deleted: "+xmlFile.getAbsolutePath(), xmlFile.exists());
        new OptionsXMLWriter().write(d, xmlFile);
        assertTrue("Expecting xml exported to "+xmlFile.getAbsolutePath(), xmlFile.exists());
        
    }
}
