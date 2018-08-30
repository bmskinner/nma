package com.bmskinner.nuclear_morphology.analysis.profiles;

import static org.junit.Assert.assertTrue;

import java.io.File;
import java.util.ArrayList;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;

import org.eclipse.jdt.annotation.NonNull;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;

import com.bmskinner.nuclear_morphology.analysis.DatasetValidator;
import com.bmskinner.nuclear_morphology.analysis.profiles.DatasetSegmentationMethod.MorphologyAnalysisMode;
import com.bmskinner.nuclear_morphology.charting.ChartFactoryTest;
import com.bmskinner.nuclear_morphology.charting.OutlineChartFactoryTest;
import com.bmskinner.nuclear_morphology.components.IAnalysisDataset;
import com.bmskinner.nuclear_morphology.components.TestDatasetBuilder;
import com.bmskinner.nuclear_morphology.components.generic.IProfile;
import com.bmskinner.nuclear_morphology.components.generic.ISegmentedProfile;
import com.bmskinner.nuclear_morphology.components.generic.ProfileType;
import com.bmskinner.nuclear_morphology.components.generic.Tag;
import com.bmskinner.nuclear_morphology.components.generic.UnavailableBorderTagException;
import com.bmskinner.nuclear_morphology.components.generic.UnavailableProfileTypeException;
import com.bmskinner.nuclear_morphology.components.generic.UnsegmentedProfileException;
import com.bmskinner.nuclear_morphology.components.nuclear.NucleusType;
import com.bmskinner.nuclear_morphology.io.SampleDatasetReader;
import com.bmskinner.nuclear_morphology.logging.ConsoleHandler;
import com.bmskinner.nuclear_morphology.logging.LogPanelFormatter;
import com.bmskinner.nuclear_morphology.logging.Loggable;
import com.bmskinner.nuclear_morphology.stats.Stats;

public class DatasetSegmentationMethodTest {
	
	private Logger logger;
	
	@Rule
	public final ExpectedException expectedException = ExpectedException.none();
	
	@Before
	public void setUp(){
		logger = Logger.getLogger(Loggable.PROGRAM_LOGGER);
		logger.setLevel(Level.FINE);
		logger.addHandler(new ConsoleHandler(new LogPanelFormatter()));
	}
	
	private void testDatasetMedianAndCellsAreSegmentedConsistently(@NonNull IAnalysisDataset d) throws UnavailableBorderTagException, UnavailableProfileTypeException, ProfileException, UnsegmentedProfileException, InterruptedException {

		DatasetValidator v = new DatasetValidator();
		boolean ok = v.validate(d);
		for(String s : v.getErrors()){
			System.out.println(s);
		}
		
		if(!ok)
			ChartFactoryTest.showProfiles(v.getErrorCells(), d);
				
		assertTrue(ok);

		ISegmentedProfile median = d.getCollection()
				.getProfileCollection().getSegmentedProfile(ProfileType.ANGLE, Tag.REFERENCE_POINT, Stats.MEDIAN);
		
		if(!median.hasSegments() || median.getSegmentCount()==1)
			ChartFactoryTest.showProfiles(d.getCollection().getCells(), d);
		assertTrue(median.hasSegments() && median.getSegmentCount()>1);	
	}
	
	/**
	 * Test a single cell dataset segmentation
	 * @throws Exception
	 */
	@Test
	public void testSegmentationOfSingleCellDataset() throws Exception {
		long seed = 1234;
		IAnalysisDataset dataset = new TestDatasetBuilder(seed).cellCount(1)
				.baseHeight(40).baseWidth(40).randomOffsetProfiles(true).segmented().build();
		testDatasetMedianAndCellsAreSegmentedConsistently(dataset);

	}
		
	/**
	 * Test multiple identical cells segmentation, with the same (zero) border offset
	 * @throws Exception
	 */
	@Test
	public void testSegmentationOfMultiCellDataset() throws Exception {

		long seed = 1234;
		int maxCells = 50;		
		for(int i=1; i<=maxCells; i++) {
			System.out.println(String.format("Testing %s cells", i));
			IAnalysisDataset dataset = new TestDatasetBuilder(seed).cellCount(i)
					.baseHeight(40).baseWidth(40)
					.ofType(NucleusType.ROUND)
					.randomOffsetProfiles(false)
					.segmented().build();
			testDatasetMedianAndCellsAreSegmentedConsistently(dataset);
		}
	}
	
	/**
	 * Test multiple identical cells segmentation, with the same (fixed) border offset
	 * @throws Exception
	 */
	@Test
	public void testSegmentationOfMultiCellDatasetWithFixedOffset() throws Exception {

		long seed = 1234;
		int maxCells = 50;		
		for(int i=1; i<=maxCells; i++) {
			System.out.println(String.format("Testing %s cells", i));
			IAnalysisDataset dataset = new TestDatasetBuilder(seed).cellCount(i)
					.baseHeight(40).baseWidth(40)
					.ofType(NucleusType.ROUND)
					.randomOffsetProfiles(false)
					.fixedProfileOffset(50)
					.segmented().build();
			testDatasetMedianAndCellsAreSegmentedConsistently(dataset);
		}
	}
	
	/**
	 * Test multiple identical cells segmentation, with a variable border offset
	 * @throws Exception
	 */
	@Test
	public void testSegmentationOfMultiCellDatasetWithVariableOffset() throws Exception {
		long seed = 1234;
		int maxCells = 50;		
		for(int i=1; i<=maxCells; i++) {
			System.out.println(String.format("Testing %s cells", i));
			IAnalysisDataset dataset = new TestDatasetBuilder(seed).cellCount(i)
					.baseHeight(40).baseWidth(40)
					.ofType(NucleusType.ROUND)
					.randomOffsetProfiles(true)
					.segmented().build();
			testDatasetMedianAndCellsAreSegmentedConsistently(dataset);
			if(i==maxCells)
				ChartFactoryTest.showProfiles(dataset.getCollection().getCells(), dataset);
		}
	}
	
	
	@Test
	public void testSegmentationCanAccountForVariationInCells() throws Exception {
		long seed = 1234;
		int cells = 50;
		int var   = 20;

		System.out.println(String.format("Testing variability %s on %s cells", var, cells));
		IAnalysisDataset dataset = new TestDatasetBuilder(seed).cellCount(cells)
				.withMaxSizeVariation(var)
				.baseHeight(40).baseWidth(40)
				.randomOffsetProfiles(false)
				.segmented().build();
		testDatasetMedianAndCellsAreSegmentedConsistently(dataset);

		ChartFactoryTest.showProfiles(dataset.getCollection().getCells(), dataset);


	}
	
	
	@Test
	public void testSegmentationIsIndependentOfCellCountInVaryingDatasetWithKnownError() throws Exception {
		long seed = 1234;
		System.out.println(String.format("Testing variability %s on %s cells", 16, 3));
		IAnalysisDataset dataset = new TestDatasetBuilder(seed).cellCount(3)
				.withMaxSizeVariation(16)
				.baseHeight(40).baseWidth(40)
				.randomOffsetProfiles(false)
				.segmented().build();
		OutlineChartFactoryTest.generateOutlineChartsForAllCells(dataset, "Known error");
		testDatasetMedianAndCellsAreSegmentedConsistently(dataset);
		
		ChartFactoryTest.showProfiles(dataset.getCollection().getCells(), dataset);


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
	public void testSegmentationIsIndependentOfCellCountInVaryingDataset() throws Exception {
		long seed = 1234;
		int maxCells = 50;		
		for(int i=1; i<=maxCells; i++) {
			for(int var=0; var<=20; var++) {
				System.out.println(String.format("Testing variability %s on %s cells", var, i));
				IAnalysisDataset dataset = new TestDatasetBuilder(seed).cellCount(i)
						.withMaxSizeVariation(var)
						.baseHeight(40).baseWidth(40)
						.randomOffsetProfiles(false)
						.segmented().build();
				testDatasetMedianAndCellsAreSegmentedConsistently(dataset);
				if(i==maxCells)
					ChartFactoryTest.showProfiles(dataset.getCollection().getCells(), dataset);
			}
		}
	}
	
	@Test
	public void testMedianFindingInRodentDataset() throws Exception {
		File f = new File(SampleDatasetReader.SAMPLE_DATASET_PATH, "Unsegmented_mouse.nmd");
		IAnalysisDataset dataset = SampleDatasetReader.openDataset(f);
		ISegmentedProfile template = dataset.getCollection()
				.getProfileCollection().getSegmentedProfile(ProfileType.ANGLE, Tag.REFERENCE_POINT, Stats.MEDIAN).copy();

		new DatasetSegmentationMethod(dataset, MorphologyAnalysisMode.NEW).call();
		
		ISegmentedProfile result = dataset.getCollection()
				.getProfileCollection().getSegmentedProfile(ProfileType.ANGLE, Tag.REFERENCE_POINT, Stats.MEDIAN).copy();
				
		List<IProfile> profiles = new ArrayList<>();
		profiles.add(template);
		profiles.add(result);
		
		List<String> names = new ArrayList<>();
		names.add("Overall median");
		names.add("Final median");
		
//		if(!template.equals(result))
			ChartFactoryTest.showProfiles(profiles, names, "Messy mouse dataset");
	}
}
