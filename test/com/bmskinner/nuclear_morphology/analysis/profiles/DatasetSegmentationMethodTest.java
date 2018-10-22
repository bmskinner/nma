package com.bmskinner.nuclear_morphology.analysis.profiles;

import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;

import com.bmskinner.nuclear_morphology.TestDatasetBuilder;
import com.bmskinner.nuclear_morphology.components.IAnalysisDataset;
import com.bmskinner.nuclear_morphology.components.nuclear.NucleusType;

public class DatasetSegmentationMethodTest extends AbstractProfileMethodTest {
		
	@Rule
	public final ExpectedException expectedException = ExpectedException.none();
	

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
			System.out.println(String.format("Testing %s cells", i));
			IAnalysisDataset dataset = new TestDatasetBuilder(RNG_SEED).cellCount(i)
					.baseHeight(40).baseWidth(40)
					.ofType(NucleusType.ROUND)
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
			System.out.println(String.format("Testing %s cells", i));
			IAnalysisDataset dataset = new TestDatasetBuilder(RNG_SEED).cellCount(i)
					.baseHeight(40).baseWidth(40)
					.ofType(NucleusType.ROUND)
					.randomOffsetProfiles(false)
					.fixedProfileOffset(50)
					.segmented().build();
			testSegmentationIsConsistent(dataset);
		}
	}
	
	/**
	 * Test multiple identical cells segmentation, with a variable border offset
	 * @throws Exception
	 */
	@Test
	public void testSegmentationOfMultiCellDatasetWithVariableOffset() throws Exception {
		int maxCells = 50;		
		for(int i=1; i<=maxCells; i++) {
			System.out.println(String.format("Testing %s cells", i));
			IAnalysisDataset dataset = new TestDatasetBuilder(RNG_SEED).cellCount(i)
					.baseHeight(40).baseWidth(40)
					.ofType(NucleusType.ROUND)
					.randomOffsetProfiles(true)
					.segmented().build();
			testSegmentationIsConsistent(dataset);
		}
	}
	
	
	@Test
	public void testSegmentationCanAccountForVariationInCells() throws Exception {

		int cells = 50;
		int var   = 20;

		System.out.println(String.format("Testing variability %s on %s cells", var, cells));
		IAnalysisDataset dataset = new TestDatasetBuilder(RNG_SEED).cellCount(cells)
				.withMaxSizeVariation(var)
				.baseHeight(40).baseWidth(40)
				.randomOffsetProfiles(false)
				.segmented().build();
		testSegmentationIsConsistent(dataset);
	}
	
	
	@Test
	public void testSegmentationIsIndependentOfCellCountInVaryingDatasetWithKnownError() throws Exception {

		System.out.println(String.format("Testing variability %s on %s cells", 16, 3));
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
				System.out.println(String.format("Testing %s cells", nCells));
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
	 * but any segments found should be propogated to the cells.
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
				System.out.println(String.format("Testing variability %s on %s cells", var, nCells));
				IAnalysisDataset dataset = new TestDatasetBuilder(RNG_SEED).cellCount(nCells)
						.withMaxSizeVariation(var)
						.baseHeight(40).baseWidth(40)
						.randomOffsetProfiles(false)
						.segmented().build();
				testSegmentationIsConsistent(dataset);
			}
		}
	}
	
//	@Test
//	public void testSegmentationOfRodentDataset() throws Exception {
//		File f = new File(TestResources.DATASET_FOLDER, "Unsegmented_mouse.nmd");
//		IAnalysisDataset dataset = SampleDatasetReader.openDataset(f);
//		ISegmentedProfile template = dataset.getCollection()
//				.getProfileCollection().getSegmentedProfile(ProfileType.ANGLE, Tag.REFERENCE_POINT, Stats.MEDIAN).copy();
//
//		new DatasetSegmentationMethod(dataset, MorphologyAnalysisMode.NEW).call();
//		
//		ISegmentedProfile result = dataset.getCollection()
//				.getProfileCollection().getSegmentedProfile(ProfileType.ANGLE, Tag.REFERENCE_POINT, Stats.MEDIAN).copy();
//
//	}
}
