package com.bmskinner.nuclear_morphology;

import static org.junit.Assert.assertTrue;

import java.awt.Color;
import java.io.File;
import java.util.UUID;

import org.junit.Test;

import com.bmskinner.nuclear_morphology.analysis.IAnalysisMethod;
import com.bmskinner.nuclear_morphology.analysis.IAnalysisResult;
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
import com.bmskinner.nuclear_morphology.components.nuclear.ISignalGroup;
import com.bmskinner.nuclear_morphology.components.nuclear.NucleusType;
import com.bmskinner.nuclear_morphology.components.nuclear.SignalGroup;
import com.bmskinner.nuclear_morphology.components.options.DefaultShellOptions;
import com.bmskinner.nuclear_morphology.components.options.IAnalysisOptions;
import com.bmskinner.nuclear_morphology.components.options.IDetectionOptions;
import com.bmskinner.nuclear_morphology.components.options.INuclearSignalOptions;
import com.bmskinner.nuclear_morphology.components.options.OptionsFactory;
import com.bmskinner.nuclear_morphology.io.DatasetExportMethod;

/**
 * Generate the test datasets for the current version, which will be used for comparison
 * testing with older versions by other classes. This class should be run first in test
 * suites.
 * @author ben
 * @since 1.14.0
 *
 */
public class TestDatasetCreator {
	
    @Test
    public void createMouseDataset() throws Exception{

    	File testFolder = new File(TestResources.TESTING_MOUSE_FOLDER);
    	IAnalysisOptions op = OptionsFactory.makeDefaultRodentAnalysisOptions(testFolder);
    	File saveFile = new File(TestResources.MOUSE_TEST_DATASET);
    	createTestDataset(TestResources.UNIT_TEST_FOLDERNAME, op, saveFile);
    }
    
    @Test
    public void createPigDataset() throws Exception{

    	File testFolder = new File(TestResources.TESTING_PIG_FOLDER);
    	IAnalysisOptions op = OptionsFactory.makeDefaultPigAnalysisOptions(testFolder);
    	File saveFile = new File(TestResources.PIG_TEST_DATASET);
    	createTestDataset(TestResources.UNIT_TEST_FOLDERNAME, op, saveFile);
    }
    
    @Test
    public void createRoundDataset() throws Exception{

    	File testFolder = new File(TestResources.TESTING_ROUND_FOLDER);
    	IAnalysisOptions op = OptionsFactory.makeDefaultRoundAnalysisOptions(testFolder);
    	File saveFile = new File(TestResources.ROUND_TEST_DATASET);
    	createTestDataset(TestResources.UNIT_TEST_FOLDERNAME, op, saveFile);
    }
    
    @Test
    public void createMouseWitSignalsDataset() throws Exception {

    	File testFolder = new File(TestResources.TESTING_MOUSE_SIGNALS_FOLDER);
    	IAnalysisOptions op = OptionsFactory.makeDefaultRodentAnalysisOptions(testFolder);
    	IDetectionOptions nucleus = op.getDetectionOptions(IAnalysisOptions.NUCLEUS).get();
    	nucleus.setMaxSize(12000);
    	nucleus.setMinSize(4000);
    	File saveFile = new File(TestResources.MOUSE_SIGNALS_DATASET);

        IAnalysisDataset d = new NucleusDetectionMethod(testFolder, op).call().getFirstDataset();
        
        new DatasetProfilingMethod(d)
	    	.then(new DatasetSegmentationMethod(d, MorphologyAnalysisMode.NEW))
	    	.then(new ConsensusAveragingMethod(d))
	    	.call();

    	INuclearSignalOptions redOptions = OptionsFactory.makeNuclearSignalOptions(testFolder);
    	redOptions.setMaxFraction(0.5);
    	redOptions.setMinSize(5);
    	
    	UUID redId =  UUID.fromString("00000000-0000-0000-0000-100000000001");
        ISignalGroup red = new SignalGroup("Test red");
        red.setGroupColour(Color.RED);
        d.getCollection().addSignalGroup(redId, red);

        d.getAnalysisOptions().get().setDetectionOptions(redId.toString(), redOptions);
        
    	new SignalDetectionMethod(d, redOptions, redId)
    	.then(new ShellAnalysisMethod(d, new DefaultShellOptions()))
    	.then(new DatasetExportMethod(d, saveFile))
    	.call();
    	assertTrue("Expecting file saved to "+saveFile.getAbsolutePath(), saveFile.exists());
    }
    
    /**
     * Detect nuclei in the round dataset, make a consensus, add red and green signals
     * and run a shell analysis.
     * @return
     * @throws Exception
     */
    @Test
    public void createRoundWitSignalsDataset() throws Exception {

    	File testFolder = new File(TestResources.TESTING_ROUND_SIGNALS_FOLDER);
    	IAnalysisOptions op = OptionsFactory.makeDefaultRoundAnalysisOptions(testFolder);
    	File saveFile = new File(TestResources.ROUND_SIGNALS_DATASET);
    	
    	IAnalysisMethod m = new NucleusDetectionMethod(testFolder, op);
        IAnalysisResult r = m.call();
        IAnalysisDataset d = r.getFirstDataset();
        
        new DatasetProfilingMethod(d)
	    	.then(new DatasetSegmentationMethod(d, MorphologyAnalysisMode.NEW))
	    	.then(new ProfileRefoldMethod(d, CurveRefoldingMode.FAST))
	    	.call();

    	INuclearSignalOptions redOptions = OptionsFactory.makeNuclearSignalOptions(testFolder);
    	
    	UUID redId =  UUID.fromString("00000000-0000-0000-0000-100000000001");
        ISignalGroup red = new SignalGroup("Test red");
        red.setGroupColour(Color.RED);
        d.getCollection().addSignalGroup(redId, red);
        
        
        INuclearSignalOptions greenOptions = OptionsFactory.makeNuclearSignalOptions(testFolder);
        greenOptions.setChannel(1);
        UUID greenId =  UUID.fromString("00000000-0000-0000-0000-100000000002");
        ISignalGroup green = new SignalGroup("Test green");
        green.setGroupColour(Color.GREEN);
        d.getCollection().addSignalGroup(greenId, green);
        

        d.getAnalysisOptions().get().setDetectionOptions(redId.toString(), redOptions);
        d.getAnalysisOptions().get().setDetectionOptions(greenId.toString(), greenOptions);
        
    	new SignalDetectionMethod(d, redOptions, redId)
    	.then(new SignalDetectionMethod(d, greenOptions, greenId))
    	.then(new ShellAnalysisMethod(d, new DefaultShellOptions()))
    	.then(new DatasetExportMethod(d, saveFile))
    	.call();
    	assertTrue("Expecting file saved to "+saveFile.getAbsolutePath(), saveFile.exists());
    }
    
   
    /**
     * Run a new analysis on the images using the given options.
     * @param folder the name of the output folder for the nmd file
     * @param op the detection options
     * @param saveFile the full path to the nmd file
     * @return the new dataset
     * @throws Exception
     */
    private static void createTestDataset(String folder, IAnalysisOptions op, File saveFile) throws Exception {
        
        if(!op.getDetectionOptions(CellularComponent.NUCLEUS).get().getFolder().exists()){
            throw new IllegalArgumentException("Detection folder does not exist");
        }
        IAnalysisMethod m = new NucleusDetectionMethod(folder, op);
        IAnalysisResult r = m.call();
        IAnalysisDataset d = r.getFirstDataset();
        
        new DatasetProfilingMethod(d)
	    	.then(new DatasetSegmentationMethod(d, MorphologyAnalysisMode.NEW))
	    	.then( op.getNucleusType()==NucleusType.ROUND?new ProfileRefoldMethod(d,CurveRefoldingMode.FAST):new ConsensusAveragingMethod(d))
	    	.then(new DatasetExportMethod(d, saveFile))
	    	.call();
        
        assertTrue("Expecting file saved to "+saveFile.getAbsolutePath(), saveFile.exists());
    }
}
