package com.bmskinner.nma.analysis.signals;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import java.io.File;

import org.junit.Before;
import org.junit.Test;

import com.bmskinner.nma.ComponentTester;
import com.bmskinner.nma.TestImageDatasetCreator;
import com.bmskinner.nma.TestResources;
import com.bmskinner.nma.analysis.signals.SignalDetectionMethod;
import com.bmskinner.nma.components.cells.CellularComponent;
import com.bmskinner.nma.components.datasets.IAnalysisDataset;
import com.bmskinner.nma.components.options.HashOptions;
import com.bmskinner.nma.components.options.IAnalysisOptions;
import com.bmskinner.nma.components.options.OptionsFactory;
import com.bmskinner.nma.core.DatasetListManager;

/**
 * Test the shell detector is functioning
 * @author ben
 *
 */
public class SignalDetectionMethodTest extends ComponentTester {
	
	@Before
	public void setUp() throws Exception{
	}
	
	/**
	 * Adding nuclear signals should change the dataset hash.
	 * If not, the save prompt will not display properly
	 * @throws Exception
	 */
	@Test
	public void testAddingSignalChangesDatasetHash() throws Exception {
		File testFolder = TestResources.MOUSE_SIGNALS_INPUT_FOLDER;
    	IAnalysisOptions op = OptionsFactory.makeDefaultRodentAnalysisOptions(testFolder);
    	HashOptions nucleus = op.getDetectionOptions(CellularComponent.NUCLEUS).get();
    	nucleus.setInt(HashOptions.MAX_SIZE_PIXELS, 12000);
    	nucleus.setInt(HashOptions.MIN_SIZE_PIXELS, 4000);

    	// Make the dataset with no signals
    	IAnalysisDataset d = TestImageDatasetCreator.createTestDataset(testFolder, op, false);

    	// Get current hash
    	DatasetListManager.getInstance().addDataset(d);
    	long hash = d.hashCode();
    	assertFalse(DatasetListManager.getInstance().hashCodeChanged());
    	
    	
    	// Add signals from the red channel
    	HashOptions redOptions = OptionsFactory.makeNuclearSignalOptions(testFolder)
    			.withValue(HashOptions.SIGNAL_MAX_FRACTION, 0.5)
    			.withValue(HashOptions.MIN_SIZE_PIXELS, 5)
    			.withValue(HashOptions.SIGNAL_GROUP_NAME, TestImageDatasetCreator.RED_SIGNAL_NAME)
    			.withValue(HashOptions.SIGNAL_GROUP_ID, TestImageDatasetCreator.RED_SIGNAL_ID.toString())
    			.build();

    	new SignalDetectionMethod(d, redOptions).call();
    	
    	// Get the new hash
    	long newHash = d.hashCode();
    	
//    	hashs should not be the same
    	assertFalse(hash==newHash);
    	assertTrue(DatasetListManager.getInstance().hasRootDataset(d.getId()));
    	assertTrue(DatasetListManager.getInstance().hashCodeChanged());
	}
	
	@Test
	public void testAddingSignalGroupAddsDetectionOptionsToDataset() throws Exception {
		File testFolder = TestResources.MOUSE_SIGNALS_INPUT_FOLDER;
    	IAnalysisOptions op = OptionsFactory.makeDefaultRodentAnalysisOptions(testFolder);
    	HashOptions nucleus = op.getDetectionOptions(CellularComponent.NUCLEUS).get();
    	nucleus.setInt(HashOptions.MAX_SIZE_PIXELS, 12000);
    	nucleus.setInt(HashOptions.MIN_SIZE_PIXELS, 4000);

    	// Make the dataset with no signals
    	IAnalysisDataset d = TestImageDatasetCreator.createTestDataset(testFolder, op, false);
    	
    	// Add signals from the red channel
    	HashOptions redOptions = OptionsFactory.makeNuclearSignalOptions(testFolder)
    			.withValue(HashOptions.SIGNAL_MAX_FRACTION, 0.5)
    			.withValue(HashOptions.MIN_SIZE_PIXELS, 5)
    			.withValue(HashOptions.SIGNAL_GROUP_NAME, TestImageDatasetCreator.RED_SIGNAL_NAME)
    			.withValue(HashOptions.SIGNAL_GROUP_ID, TestImageDatasetCreator.RED_SIGNAL_ID.toString())
    			.build();

    	new SignalDetectionMethod(d, redOptions).call();
    	
    	// confirm the signal options were added
    	assertTrue(d.getAnalysisOptions().get().hasSignalDetectionOptions(TestImageDatasetCreator.RED_SIGNAL_ID));
	}
	
}