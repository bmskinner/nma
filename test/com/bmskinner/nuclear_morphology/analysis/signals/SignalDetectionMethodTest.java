package com.bmskinner.nuclear_morphology.analysis.signals;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import java.awt.Color;
import java.io.File;
import java.util.Arrays;
import java.util.List;

import org.junit.Before;
import org.junit.Test;

import com.bmskinner.nuclear_morphology.ComponentTester;
import com.bmskinner.nuclear_morphology.TestDatasetBuilder;
import com.bmskinner.nuclear_morphology.TestResources;
import com.bmskinner.nuclear_morphology.TestDatasetBuilder.TestComponentShape;
import com.bmskinner.nuclear_morphology.TestImageDatasetCreator;
import com.bmskinner.nuclear_morphology.analysis.IAnalysisMethod;
import com.bmskinner.nuclear_morphology.analysis.signals.shells.ShellAnalysisMethod.ShellAnalysisException;
import com.bmskinner.nuclear_morphology.analysis.signals.shells.ShellDetector.Shell;
import com.bmskinner.nuclear_morphology.charting.ImageViewer;
import com.bmskinner.nuclear_morphology.components.Imageable;
import com.bmskinner.nuclear_morphology.components.Statistical;
import com.bmskinner.nuclear_morphology.components.cells.CellularComponent;
import com.bmskinner.nuclear_morphology.components.datasets.IAnalysisDataset;
import com.bmskinner.nuclear_morphology.components.measure.Measurement;
import com.bmskinner.nuclear_morphology.components.measure.MeasurementScale;
import com.bmskinner.nuclear_morphology.components.nuclei.Nucleus;
import com.bmskinner.nuclear_morphology.components.nuclei.NucleusType;
import com.bmskinner.nuclear_morphology.components.options.DefaultShellOptions;
import com.bmskinner.nuclear_morphology.components.options.IAnalysisOptions;
import com.bmskinner.nuclear_morphology.components.options.IDetectionOptions;
import com.bmskinner.nuclear_morphology.components.options.INuclearSignalOptions;
import com.bmskinner.nuclear_morphology.components.options.OptionsFactory;
import com.bmskinner.nuclear_morphology.components.signals.INuclearSignal;
import com.bmskinner.nuclear_morphology.components.signals.ISignalGroup;
import com.bmskinner.nuclear_morphology.components.signals.SignalGroup;
import com.bmskinner.nuclear_morphology.components.signals.IShellResult.ShrinkType;
import com.bmskinner.nuclear_morphology.core.DatasetListManager;
import com.bmskinner.nuclear_morphology.gui.components.ColourSelecter;
import com.bmskinner.nuclear_morphology.io.SampleDatasetReader;
import com.bmskinner.nuclear_morphology.io.UnloadableImageException;

import ij.process.ImageProcessor;

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
		File testFolder = new File(TestResources.TESTING_MOUSE_SIGNALS_FOLDER).getAbsoluteFile();
    	IAnalysisOptions op = OptionsFactory.makeDefaultRodentAnalysisOptions(testFolder);
    	IDetectionOptions nucleus = op.getDetectionOptions(CellularComponent.NUCLEUS).get();
    	nucleus.setMaxSize(12000);
    	nucleus.setMinSize(4000);

    	// Make the dataset with no signals
    	IAnalysisDataset d = TestImageDatasetCreator.createTestDataset(testFolder.toString(), op, false);

    	// Get current hash
    	DatasetListManager.getInstance().addDataset(d);
    	long hash = d.hashCode();
    	assertFalse(DatasetListManager.getInstance().hashCodeChanged());
    	
    	
    	// Add signals from the red channel
    	INuclearSignalOptions redOptions = OptionsFactory.makeNuclearSignalOptions(testFolder);
    	redOptions.setMaxFraction(0.5);
    	redOptions.setMinSize(5);

    	ISignalGroup red = new SignalGroup(TestImageDatasetCreator.RED_SIGNAL_NAME);
    	red.setGroupColour(Color.RED);
    	d.getCollection().addSignalGroup(TestImageDatasetCreator.RED_SIGNAL_ID, red);
    	d.getAnalysisOptions().get().setDetectionOptions(TestImageDatasetCreator.RED_SIGNAL_ID.toString(), redOptions);
    	new SignalDetectionMethod(d, redOptions, TestImageDatasetCreator.RED_SIGNAL_ID).call();
    	
    	// Get the new hash
    	long newHash = d.hashCode();
    	
//    	hashs should not be the same
    	assertFalse(hash==newHash);
    	assertTrue(DatasetListManager.getInstance().hasRootDataset(d.getId()));
    	assertTrue(DatasetListManager.getInstance().hashCodeChanged());
	}
}