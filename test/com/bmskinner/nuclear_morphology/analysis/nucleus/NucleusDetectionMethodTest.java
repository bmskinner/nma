package com.bmskinner.nuclear_morphology.analysis.nucleus;

import static org.junit.Assert.assertEquals;

import java.io.File;
import java.util.logging.Handler;
import java.util.logging.Level;
import java.util.logging.Logger;

import org.jdom2.Element;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;

import com.bmskinner.nuclear_morphology.ComponentTester;
import com.bmskinner.nuclear_morphology.TestResources;
import com.bmskinner.nuclear_morphology.analysis.AnalysisMethodException;
import com.bmskinner.nuclear_morphology.analysis.IAnalysisResult;
import com.bmskinner.nuclear_morphology.analysis.profiles.DatasetProfilingMethod;
import com.bmskinner.nuclear_morphology.analysis.profiles.ProfileCreator;
import com.bmskinner.nuclear_morphology.components.cells.CellularComponent;
import com.bmskinner.nuclear_morphology.components.cells.ICell;
import com.bmskinner.nuclear_morphology.components.datasets.DatasetCreator;
import com.bmskinner.nuclear_morphology.components.datasets.IAnalysisDataset;
import com.bmskinner.nuclear_morphology.components.options.DefaultAnalysisOptions;
import com.bmskinner.nuclear_morphology.components.options.HashOptions;
import com.bmskinner.nuclear_morphology.components.options.IAnalysisOptions;
import com.bmskinner.nuclear_morphology.components.options.OptionsFactory;
import com.bmskinner.nuclear_morphology.components.profiles.DefaultProfile;
import com.bmskinner.nuclear_morphology.components.profiles.IProfile;
import com.bmskinner.nuclear_morphology.components.profiles.Landmark;
import com.bmskinner.nuclear_morphology.components.profiles.ProfileType;
import com.bmskinner.nuclear_morphology.logging.ConsoleFormatter;
import com.bmskinner.nuclear_morphology.logging.ConsoleHandler;
import com.bmskinner.nuclear_morphology.logging.Loggable;

/**
 * Test that nuclei in an image are detected,
 * and all parameters are calculated. For mouse
 * sperm, this includes the hook length and body
 * width
 * @author bs19022
 * @since 1.17.1
 *
 */
public class NucleusDetectionMethodTest {
	
	static final Logger LOGGER = Logger.getLogger(Loggable.PROJECT_LOGGER);
	
    @Rule
    public final ExpectedException exception = ExpectedException.none();
	
	static {
		for(Handler h : LOGGER.getHandlers())
			LOGGER.removeHandler(h);
		Handler h = new ConsoleHandler(new ConsoleFormatter());
		LOGGER.setLevel(Level.FINE);
		h.setLevel(Level.FINE);
		LOGGER.addHandler(h);
	}
		
	@Before
	public void setUp() throws Exception{
	}
	
	@Test
	public void testFailsOnNonexistentFolder() throws AnalysisMethodException {
		File testFolder = TestResources.MOUSE_INPUT_FOLDER;
    	IAnalysisOptions op = OptionsFactory.makeDefaultRodentAnalysisOptions(testFolder);
    	
    	exception.expect(AnalysisMethodException.class);
		NucleusDetectionMethod nm = new NucleusDetectionMethod(new File(""), op);
	}
	
	@Test
	public void testFailsOnNonexistentOptions() throws AnalysisMethodException {		
		// Options does not have detection options for nucleus specified
    	IAnalysisOptions op = new DefaultAnalysisOptions();
    	
    	exception.expect(AnalysisMethodException.class);
		NucleusDetectionMethod nm = new NucleusDetectionMethod(TestResources.MOUSE_INPUT_FOLDER, op);
	}
	
	@Test
	public void testNucleusDetectionReturnsADataset() throws Exception {
		File testFolder = TestResources.MOUSE_INPUT_FOLDER.getAbsoluteFile();
    	IAnalysisOptions op = OptionsFactory.makeDefaultRodentAnalysisOptions(testFolder);

    	NucleusDetectionMethod nm = new NucleusDetectionMethod(TestResources.MOUSE_INPUT_FOLDER, op);
    	IAnalysisResult r = nm.call();
    	
    	IAnalysisDataset d = r.getFirstDataset();
    	
    	// Number of nuclei in test folder with default settings
    	assertEquals("Number of cells", 63, d.getCollection().size());
	}
	
}
