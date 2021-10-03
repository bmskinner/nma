package com.bmskinner.nuclear_morphology;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import java.awt.Color;
import java.io.File;
import java.nio.file.Files;
import java.nio.file.StandardCopyOption;
import java.util.UUID;
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
import com.bmskinner.nuclear_morphology.components.cells.CellularComponent;
import com.bmskinner.nuclear_morphology.components.datasets.IAnalysisDataset;
import com.bmskinner.nuclear_morphology.components.options.HashOptions;
import com.bmskinner.nuclear_morphology.components.options.IAnalysisOptions;
import com.bmskinner.nuclear_morphology.components.options.IClusteringOptions;
import com.bmskinner.nuclear_morphology.components.options.OptionsFactory;
import com.bmskinner.nuclear_morphology.components.rules.RuleSetCollection;
import com.bmskinner.nuclear_morphology.components.signals.DefaultSignalGroup;
import com.bmskinner.nuclear_morphology.components.signals.ISignalGroup;
import com.bmskinner.nuclear_morphology.io.DatasetExportMethod;
import com.bmskinner.nuclear_morphology.io.DatasetExportMethod.ExportFormat;
import com.bmskinner.nuclear_morphology.io.xml.OptionsXMLWriter;

import ij.Prefs;

/**
 * Generate the test datasets for the current version, which will be used for comparison
 * testing with older versions by other classes. This class should be run first in test
 * suites.
 * @author ben
 * @since 1.14.0
 *
 */
public class TestImageDatasetCreator {
	
	protected Logger LOGGER = Logger.getLogger(TestImageDatasetCreator.class.getName());
	
	public static final UUID RED_SIGNAL_ID   = UUID.fromString("00000000-0000-0000-0000-100000000001");
	public static final UUID GREEN_SIGNAL_ID = UUID.fromString("00000000-0000-0000-0000-100000000002");
	
	public static final String RED_SIGNAL_NAME   = "Test red";
	public static final String GREEN_SIGNAL_NAME = "Test green";

	@Before
	public void setUp(){		
		Prefs.setThreads(2); // Attempt to avoid issue 162
	}

	
    @Test
    public void createMouseDataset() throws Exception{

    	File testFolder = new File(TestResources.TESTING_MOUSE_FOLDER).getAbsoluteFile();
    	IAnalysisOptions op = OptionsFactory.makeDefaultRodentAnalysisOptions(testFolder);
    	File saveFile = new File(TestResources.MOUSE_TEST_DATASET).getAbsoluteFile();;
    	IAnalysisDataset d = createTestDataset(TestResources.UNIT_TEST_FOLDERNAME, op, false);
    	saveTestDataset(d, saveFile);
    }
    
    @Test
    public void createPigDataset() throws Exception{

    	File testFolder = new File(TestResources.TESTING_PIG_FOLDER).getAbsoluteFile();
    	IAnalysisOptions op = OptionsFactory.makeDefaultPigAnalysisOptions(testFolder);
    	File saveFile = new File(TestResources.PIG_TEST_DATASET).getAbsoluteFile();;
    	IAnalysisDataset d = createTestDataset(TestResources.UNIT_TEST_FOLDERNAME, op, false);
    	saveTestDataset(d, saveFile);
    }
    
    @Test
    public void createRoundDataset() throws Exception{

    	File testFolder = new File(TestResources.TESTING_ROUND_FOLDER).getAbsoluteFile();
    	IAnalysisOptions op = OptionsFactory.makeDefaultRoundAnalysisOptions(testFolder);
    	File saveFile = new File(TestResources.ROUND_TEST_DATASET).getAbsoluteFile();;
    	IAnalysisDataset d = createTestDataset(TestResources.UNIT_TEST_FOLDERNAME, op, false);
    	saveTestDataset(d, saveFile);
    }
    
    @Test
    public void createMultipleSource1Dataset() throws Exception{

    	File testFolder = new File(TestResources.TESTING_MULTIPLE_SOURCE_1_FOLDER).getAbsoluteFile();
    	IAnalysisOptions op = OptionsFactory.makeDefaultRodentAnalysisOptions(testFolder);
    	File saveFile = new File(TestResources.MULTIPLE1_TEST_DATASET).getAbsoluteFile();;
    	IAnalysisDataset d = createTestDataset(TestResources.UNIT_TEST_FOLDERNAME, op, false);
    	saveTestDataset(d, saveFile);
    }
    
    @Test
    public void createMultipleSource2Dataset() throws Exception{

    	File testFolder = new File(TestResources.TESTING_MULTIPLE_SOURCE_2_FOLDER).getAbsoluteFile();
    	IAnalysisOptions op = OptionsFactory.makeDefaultRodentAnalysisOptions(testFolder);
    	File saveFile = new File(TestResources.MULTIPLE2_TEST_DATASET).getAbsoluteFile();;
    	IAnalysisDataset d = createTestDataset(TestResources.UNIT_TEST_FOLDERNAME, op, false);
    	saveTestDataset(d, saveFile);
    }
    
    
    @Test
    public void createMouseWithClustersDataset() throws Exception{

    	File testFolder = new File(TestResources.TESTING_MOUSE_CLUSTERS_FOLDER).getAbsoluteFile();
    	IAnalysisOptions op = OptionsFactory.makeDefaultRodentAnalysisOptions(testFolder);
    	File saveFile = new File(TestResources.MOUSE_CLUSTERS_DATASET).getAbsoluteFile();;
    	IAnalysisDataset d = createTestDataset(TestResources.UNIT_TEST_FOLDERNAME, op, true);
    	saveTestDataset(d, saveFile);
    }
    
    @Test
    public void createPigWithClustersDataset() throws Exception{

    	File testFolder = new File(TestResources.TESTING_PIG_CLUSTERS_FOLDER).getAbsoluteFile();
    	IAnalysisOptions op = OptionsFactory.makeDefaultPigAnalysisOptions(testFolder);
    	File saveFile = new File(TestResources.PIG_CLUSTERS_DATASET).getAbsoluteFile();;
    	IAnalysisDataset d = createTestDataset(TestResources.UNIT_TEST_FOLDERNAME, op, true);
    	saveTestDataset(d, saveFile);
    }
    
    @Test
    public void createRoundWithClustersDataset() throws Exception{

    	File testFolder = new File(TestResources.TESTING_ROUND_CLUSTERS_FOLDER).getAbsoluteFile();
    	IAnalysisOptions op = OptionsFactory.makeDefaultRoundAnalysisOptions(testFolder);
    	File saveFile = new File(TestResources.ROUND_CLUSTERS_DATASET).getAbsoluteFile();;
    	IAnalysisDataset d = createTestDataset(TestResources.UNIT_TEST_FOLDERNAME, op, true);
    	saveTestDataset(d, saveFile);
    }
    
    @Test
    public void createMouseWithSignalsDataset() throws Exception {

    	File testFolder = new File(TestResources.TESTING_MOUSE_SIGNALS_FOLDER).getAbsoluteFile();
    	IAnalysisOptions op = OptionsFactory.makeDefaultRodentAnalysisOptions(testFolder);
    	HashOptions nucleus = op.getDetectionOptions(CellularComponent.NUCLEUS).get();
    	nucleus.setInt(HashOptions.MIN_SIZE_PIXELS, 4000);
    	nucleus.setInt(HashOptions.MAX_SIZE_PIXELS, 12000);
    	File saveFile = new File(TestResources.MOUSE_SIGNALS_DATASET).getAbsoluteFile();;
    	IAnalysisDataset d = createTestSignalDataset(op, true, false);
    	saveTestDataset(d, saveFile);
    }
    
    @Test
    public void createPigWithSignalsDataset() throws Exception {

    	File testFolder = new File(TestResources.TESTING_PIG_SIGNALS_FOLDER).getAbsoluteFile();
    	IAnalysisOptions op = OptionsFactory.makeDefaultPigAnalysisOptions(testFolder);
    	HashOptions nucleus = op.getDetectionOptions(CellularComponent.NUCLEUS).get();
    	nucleus.setInt(HashOptions.MIN_SIZE_PIXELS, 4000);
    	nucleus.setInt(HashOptions.MAX_SIZE_PIXELS, 15000);
    	File saveFile = new File(TestResources.PIG_SIGNALS_DATASET).getAbsoluteFile();;
    	
    	IAnalysisDataset d = createTestSignalDataset(op, false, true);
    	saveTestDataset(d, saveFile);
    }
    
    @Test
    public void createRoundWithSignalsDataset() throws Exception {

    	File testFolder = new File(TestResources.TESTING_ROUND_SIGNALS_FOLDER).getAbsoluteFile();
    	IAnalysisOptions op = OptionsFactory.makeDefaultRoundAnalysisOptions(testFolder);
    	File saveFile = new File(TestResources.ROUND_SIGNALS_DATASET).getAbsoluteFile();;
    	
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
    	
    	File testFolder = op.getDetectionOptions(CellularComponent.NUCLEUS).get().getFile(HashOptions.DETECTION_FOLDER);
    	if(!testFolder.exists()){
            throw new IllegalArgumentException("Detection folder does not exist");
        }
    	
    	IAnalysisDataset d = new NucleusDetectionMethod(testFolder, op).call().getFirstDataset();
        
        new DatasetProfilingMethod(d)
	    	.then(new DatasetSegmentationMethod(d, MorphologyAnalysisMode.NEW))
	    	.then(op.getRuleSetCollection().equals(RuleSetCollection.roundRuleSetCollection())
	    			? new ProfileRefoldMethod(d, CurveRefoldingMode.FAST)
	    		    : new ConsensusAveragingMethod(d))
	    	.call();

        if(addRed) {
        	HashOptions redOptions = OptionsFactory.makeNuclearSignalOptions(testFolder);
        	redOptions.setInt(HashOptions.MIN_SIZE_PIXELS, 5);
        	redOptions.setDouble(HashOptions.MAX_FRACTION, 0.5);

        	ISignalGroup red = new DefaultSignalGroup(RED_SIGNAL_NAME);
        	red.setGroupColour(Color.RED);
        	d.getCollection().addSignalGroup(RED_SIGNAL_ID, red);
        	d.getAnalysisOptions().get().setDetectionOptions(RED_SIGNAL_ID.toString(), redOptions);
        	new SignalDetectionMethod(d, redOptions, RED_SIGNAL_ID).call();
        }
        
        if(addGreen) {
        	HashOptions greenOptions = OptionsFactory.makeNuclearSignalOptions(testFolder);
             greenOptions.setInt(HashOptions.CHANNEL, 1);
             ISignalGroup green = new DefaultSignalGroup(GREEN_SIGNAL_NAME);
             green.setGroupColour(Color.GREEN);
             d.getCollection().addSignalGroup(GREEN_SIGNAL_ID, green);
             d.getAnalysisOptions().get().setDetectionOptions(GREEN_SIGNAL_ID.toString(), greenOptions);
             new SignalDetectionMethod(d, greenOptions, GREEN_SIGNAL_ID).call();
        }

    	new ShellAnalysisMethod(d, OptionsFactory.makeShellAnalysisOptions()).call();
    	return d;
    }
    
    /**
     * Create and run an analysis
     * @param folder the folder path of images to analyse
     * @param op the analysis options
     * @param makeClusters should clusters be created
     * @return a dataset with the results of the analysis
     * @throws Exception if anything goes wrong
     */
    public static IAnalysisDataset createTestDataset(String folder, IAnalysisOptions op, boolean makeClusters) throws Exception {
    	 if(!op.getDetectionOptions(CellularComponent.NUCLEUS).get().getFile(HashOptions.DETECTION_FOLDER).exists())
             throw new IllegalArgumentException("Detection folder does not exist");

         IAnalysisDataset d = new NucleusDetectionMethod(folder, op).call().getFirstDataset();
         
         IClusteringOptions clusterOptions = OptionsFactory.makeClusteringOptions();
         
         new DatasetProfilingMethod(d)
 	    	.then(new DatasetSegmentationMethod(d, MorphologyAnalysisMode.NEW))
 	    	.then(op.getRuleSetCollection().equals(RuleSetCollection.roundRuleSetCollection())
	    			? new ProfileRefoldMethod(d, CurveRefoldingMode.FAST)
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
    	new DatasetExportMethod(d, saveFile, ExportFormat.JAVA).call();
        assertTrue("Expecting file saved to "+saveFile.getAbsolutePath(), saveFile.exists());
        
        // Copy the saved file into backup file for comparison and conversion testing in the next version.
        String bakName = saveFile.getAbsolutePath().replaceAll(".nmd$", ".bak");
        File bakFile = new File(bakName);
        if(!bakFile.getParentFile().exists())
        	bakFile.getParentFile().mkdirs();
        if(bakFile.exists())
        	bakFile.delete();
        assertFalse("Expecting backup file to be deleted: "+bakFile.getAbsolutePath(), bakFile.exists());
        Files.copy(saveFile.getAbsoluteFile().toPath(), bakFile.getAbsoluteFile().toPath(), StandardCopyOption.COPY_ATTRIBUTES);
        assertTrue("Expecting backup copied to "+bakFile.getAbsolutePath(), bakFile.exists());
        
        // Create an xml representation of the analysis options for pipeline testing
        String xmlName = saveFile.getAbsolutePath().replaceAll(".nmd$", ".xml");
        File xmlFile = new File(xmlName);
        if(xmlFile.exists())
        	xmlFile.delete();
        assertFalse("Expecting xml file to be deleted: "+xmlFile.getAbsolutePath(), xmlFile.exists());
        new OptionsXMLWriter().write(d, xmlFile);
        assertTrue("Expecting xml exported to "+xmlFile.getAbsolutePath(), xmlFile.exists());
        
    }
}
