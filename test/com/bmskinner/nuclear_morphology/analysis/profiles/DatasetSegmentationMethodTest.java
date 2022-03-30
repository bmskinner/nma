package com.bmskinner.nuclear_morphology.analysis.profiles;

import java.io.File;
import java.util.logging.Handler;
import java.util.logging.Level;
import java.util.logging.Logger;

import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;

import com.bmskinner.nuclear_morphology.TestDatasetBuilder;
import com.bmskinner.nuclear_morphology.TestDatasetBuilder.TestComponentShape;
import com.bmskinner.nuclear_morphology.TestResources;
import com.bmskinner.nuclear_morphology.analysis.nucleus.NucleusDetectionMethod;
import com.bmskinner.nuclear_morphology.analysis.profiles.DatasetSegmentationMethod.MorphologyAnalysisMode;
import com.bmskinner.nuclear_morphology.charting.ChartFactoryTest;
import com.bmskinner.nuclear_morphology.components.datasets.IAnalysisDataset;
import com.bmskinner.nuclear_morphology.components.options.IAnalysisOptions;
import com.bmskinner.nuclear_morphology.components.options.OptionsFactory;
import com.bmskinner.nuclear_morphology.components.rules.RuleSetCollection;
import com.bmskinner.nuclear_morphology.logging.ConsoleFormatter;
import com.bmskinner.nuclear_morphology.logging.ConsoleHandler;
import com.bmskinner.nuclear_morphology.logging.Loggable;

import ij.Prefs;

public class DatasetSegmentationMethodTest extends AbstractProfileMethodTest {
	
	private static final Logger LOGGER = Logger.getLogger(Loggable.PROJECT_LOGGER);
		
	@Rule
	public final ExpectedException expectedException = ExpectedException.none();
	
	@Before
	public void setUp(){		
		Prefs.setThreads(2); // Attempt to avoid issue 162
		for(Handler h : LOGGER.getHandlers())
			LOGGER.removeHandler(h);
		Handler h = new ConsoleHandler(new ConsoleFormatter());
		LOGGER.setLevel(Level.FINE);
		h.setLevel(Level.FINE);
		LOGGER.addHandler(h);
	}
	

	/**
	 * Test a single cell dataset segmentation
	 * @throws Exception
	 */
	@Test
	public void testSegmentationOfSingleCellDataset() throws Exception {
		
		IAnalysisDataset dataset = new TestDatasetBuilder(RNG_SEED).cellCount(1)
				.baseHeight(40).baseWidth(40)
				.randomOffsetProfiles(true).segmented().build();
		testSegmentationIsConsistent(dataset);
	}
		
	/**
	 * Test multiple identical cells segmentation, with the same (zero) border offset
	 * @throws Exception
	 */
	@Test
	public void testSegmentationOfMultiCellDataset() throws Exception {

		int maxCells = 50;		
		for(int i=1; i<=maxCells; i++) {
			IAnalysisDataset dataset = new TestDatasetBuilder(RNG_SEED).cellCount(i)
					.baseHeight(40).baseWidth(40)
					.ofType(RuleSetCollection.roundRuleSetCollection())
					.randomOffsetProfiles(false)
					.segmented().build();
			testSegmentationIsConsistent(dataset);
		}
	}
	
	/**
	 * Test multiple identical cells segmentation, with the same (fixed) border offset
	 * @throws Exception
	 */
	@Test
	public void testSegmentationOfMultiCellDatasetWithFixedOffset() throws Exception {

		int maxCells = 50;		
		for(int i=1; i<=maxCells; i++) {
			IAnalysisDataset dataset = new TestDatasetBuilder(RNG_SEED).cellCount(i)
					.baseHeight(40).baseWidth(40)
					.ofType(RuleSetCollection.roundRuleSetCollection())
					.randomOffsetProfiles(false)
					.fixedProfileOffset(50)
					.segmented().build();
			testSegmentationIsConsistent(dataset);
		}
	}
	
	/**
	 * Test multiple identical cells segmentation, with a variable border offset. 
	 * These should produce a multi-segment profile, but depending on the offset may
	 * only have the default segment.
	 * @throws Exception
	 */
	@Test
	public void testSegmentationOfMultiCellDatasetWithVariableOffset() throws Exception {
		int maxCells = 50;		
		for(int i=1; i<=maxCells; i++) {
			IAnalysisDataset dataset = new TestDatasetBuilder(RNG_SEED).cellCount(i)
					.baseHeight(40).baseWidth(40)
					.ofType(RuleSetCollection.roundRuleSetCollection())
					.randomOffsetProfiles(true)
					.segmented().build();
			testSegmentationIsConsistent(dataset);
		}
	}
	
	@Test
	public void testSegmentationCanAccountForVariationInCells() throws Exception {

		int cells = 50;
		int var   = 20;

		IAnalysisDataset dataset = new TestDatasetBuilder(RNG_SEED).cellCount(cells)
				.withMaxSizeVariation(var)
				.baseHeight(40).baseWidth(40)
				.randomOffsetProfiles(false)
				.segmented().build();
		testSegmentationIsConsistent(dataset);
	}
	
	
	/**
	 * This specific dataset caused an error during development. This test exists just to confirm that
	 * the error is gone.
	 * @throws Exception
	 */
	@Test
	public void testSegmentationIsIndependentOfCellCountInVaryingDatasetWithKnownError() throws Exception {

		IAnalysisDataset dataset = new TestDatasetBuilder(RNG_SEED).cellCount(3)
				.withMaxSizeVariation(16)
				.baseHeight(40).baseWidth(40)
				.randomOffsetProfiles(false)
				.segmented().build();
		testSegmentationIsConsistent(dataset);
	}
	
	/**
	 * Test that increasing numbers of varying cells does not affect segment fitting.
	 * The number of segments may change in variable dataset, since the median may become distorted,
	 * but any segments found should be propogated to the cells.
	 * 
	 * This test ranges from variation of zero (identical cells) to 20 (highly variable dataset)
	 * across up to 50 cells. 
	 * @throws Exception
	 */
	@Test
	public void testSegmentationIsIndependentOfCellCountInIdenticalDataset() throws Exception {
		int maxCells = 50;		
		for(int nCells=1; nCells<=maxCells; nCells++) {
				IAnalysisDataset dataset = new TestDatasetBuilder(RNG_SEED).cellCount(nCells)
						.baseHeight(40).baseWidth(40)
						.randomOffsetProfiles(false)
						.segmented().build();
				testSegmentationIsConsistent(dataset);
		}
	}
	
	/**
	 * Test that increasing numbers of varying cells does not affect segment fitting.
	 * The number of segments may change in variable dataset, since the median may become distorted,
	 * but any segments found should be propagated to the cells.
	 * 
	 * This test ranges from variation of zero (identical cells) to 20 (highly variable dataset)
	 * across up to 50 cells. 
	 * @throws Exception
	 */
	@Test
	public void testSegmentationIsIndependentOfCellCountInVaryingDataset() throws Exception {

		int maxCells = 50;		
		for(int nCells=1; nCells<=maxCells; nCells++) {
			for(int var=0; var<=20; var++) {
				IAnalysisDataset dataset = new TestDatasetBuilder(RNG_SEED).cellCount(nCells)
						.withMaxSizeVariation(var)
						.baseHeight(40).baseWidth(40)
						.randomOffsetProfiles(false)
						.segmented().build();
				testSegmentationIsConsistent(dataset);
			}
		}
	}
	
	
	/**
	 * A perfectly round cell will not be segmentable. Test that this does not impair
	 * the positioning of the RP, or the ability to assign the single default segment.
	 * @throws Exception
	 */
	@Test
	public void testRoundDatasetGeneratesSegmentableProfiles() throws Exception {
		
		IAnalysisDataset dataset = new TestDatasetBuilder(RNG_SEED).cellCount(10)
				.baseHeight(40).baseWidth(40)
				.withNucleusShape(TestComponentShape.ROUND)
				.randomOffsetProfiles(false)
				.segmented().build();
		testSegmentationIsConsistent(dataset);

	}
	
	@Test
	public void testSegmentationOfRealData() throws Exception {
		File testFolder = TestResources.MOUSE_INPUT_FOLDER.getAbsoluteFile();
    	IAnalysisOptions op = OptionsFactory.makeDefaultRodentAnalysisOptions(testFolder);
    	
    	IAnalysisDataset d = new NucleusDetectionMethod(TestResources.UNIT_TEST_FOLDER.getAbsoluteFile(), op).call().getFirstDataset();
    	new DatasetProfilingMethod(d).call();    	
    	new DatasetSegmentationMethod(d, MorphologyAnalysisMode.NEW).call();
    	testSegmentationIsConsistent(d);
	}

}
