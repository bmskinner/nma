package com.bmskinner.nuclear_morphology.analysis.profiles;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import java.io.File;

import org.junit.Before;
import org.junit.Test;

import com.bmskinner.nuclear_morphology.TestDatasetBuilder;
import com.bmskinner.nuclear_morphology.TestResources;
import com.bmskinner.nuclear_morphology.analysis.nucleus.NucleusDetectionMethod;
import com.bmskinner.nuclear_morphology.components.Statistical;
import com.bmskinner.nuclear_morphology.components.cells.CellularComponent;
import com.bmskinner.nuclear_morphology.components.cells.ICell;
import com.bmskinner.nuclear_morphology.components.datasets.IAnalysisDataset;
import com.bmskinner.nuclear_morphology.components.measure.Measurement;
import com.bmskinner.nuclear_morphology.components.measure.MeasurementScale;
import com.bmskinner.nuclear_morphology.components.nuclei.Nucleus;
import com.bmskinner.nuclear_morphology.components.options.IAnalysisOptions;
import com.bmskinner.nuclear_morphology.components.options.OptionsFactory;
import com.bmskinner.nuclear_morphology.components.profiles.IProfile;
import com.bmskinner.nuclear_morphology.components.profiles.Landmark;
import com.bmskinner.nuclear_morphology.components.profiles.ProfileType;
import com.bmskinner.nuclear_morphology.components.rules.RuleSetCollection;
import com.bmskinner.nuclear_morphology.stats.Stats;

import ij.Prefs;

/**
 * Test methods for the dataset profiling 
 * @author bms41
 * @since 1.14.0
 *
 */
public class DatasetProfilingMethodTest extends AbstractProfileMethodTest {
	
	@Before
	public void setUp(){		
		Prefs.setThreads(2); // Attempt to avoid issue 162
	}

	@Test
	public void testSingleCellDataset() throws Exception {
		IAnalysisDataset d = new TestDatasetBuilder(RNG_SEED).cellCount(1).ofType(RuleSetCollection.roundRuleSetCollection())
				.baseHeight(40).baseWidth(40).profiled().build();
		testProfilingIsConsistentBetweenMedianAndCells(d);
	}
	
	@Test
	public void testMultiCellIdenticalRectangularDataset() throws Exception {

		IAnalysisDataset d = new TestDatasetBuilder(RNG_SEED).cellCount(N_CELLS)
				.ofType(RuleSetCollection.roundRuleSetCollection()).randomOffsetProfiles(false).profiled().build();
		testProfilesAreIdenticalForAllCells(d);
		testProfilingIsConsistentBetweenMedianAndCells(d);
	}
	
	@Test
	public void testMultiCellIdenticalRectangularDatasetIsNotAffectedByFixedBorderOffset() throws Exception {
		IAnalysisDataset dataset = new TestDatasetBuilder(RNG_SEED).cellCount(N_CELLS)
				.withMaxSizeVariation(0).fixedProfileOffset(20).profiled().build();
		testProfilesAreIdenticalForAllCells(dataset);
		testProfilingIsConsistentBetweenMedianAndCells(dataset);
	}
		
	@Test
	public void testProfilingIsIndependentOfCellLocation() throws Exception {
		// create cells with different x and y bases and no variation. 
		// Check they all generate the same profiles
		
		IProfile globalMedian = null;
		Nucleus globalCell = null;
		
		for(int xBase = -10; xBase<10; xBase++) {
			for(int yBase = -10; yBase<10; yBase++) {
								
				IAnalysisDataset dataset = new TestDatasetBuilder(RNG_SEED).cellCount(N_CELLS)
						.xBase(xBase).yBase(yBase)
						.randomOffsetProfiles(false).fixedProfileOffset(20)
						.profiled()
						.build();
				
				testProfilesAreIdenticalForAllCells(dataset);
				
				Nucleus loopCell = null;
				
				// Confirm all cells are identical
				for(ICell cell : dataset.getCollection().getCells()) {
					if(globalCell==null)
						globalCell = cell.getPrimaryNucleus();
					if(loopCell==null)
						loopCell=cell.getPrimaryNucleus();
					
					Nucleus n = cell.getPrimaryNucleus();
					testNucleiHaveIdenticalPositions(n, loopCell);
					testNucleiHaveIdenticalSizes(n, globalCell);
				}
							
				
				// Check the collection
				IProfile median = dataset.getCollection()
						.getProfileCollection()
						.getProfile(ProfileType.ANGLE, Landmark.REFERENCE_POINT, Stats.MEDIAN);
				assertTrue(median!=null);
				
				if(globalMedian==null)
					globalMedian = median;
				
				// We can't expect full precision equality due to cumulative single precision interpolation
				assertTrue(equals(globalMedian.toFloatArray(), median.toFloatArray(), 0.001f));

			}
		}
	}
	
	private void testNucleiHaveIdenticalPositions(Nucleus obs, Nucleus exp) {
		assertEquals(exp.getBase().getX(), obs.getBase().getX(), 0);
		assertEquals(exp.getBase().getY(), obs.getBase().getY(), 0);
	}
	
	private void testNucleiHaveIdenticalSizes(Nucleus obs, Nucleus exp) {
		assertEquals(exp.getBounds().getWidth(), obs.getBounds().getWidth(), 0.001);
		assertEquals(exp.getBounds().getHeight(), obs.getBounds().getHeight(), 0.001);
	}
	
	@Test
	public void testProfilingIsIndependentOfCellRotation() throws Exception {
		// create cells with different x and y bases and no variation. 
		// Check they all generate the same profiles
		
		IProfile globalMedian = null;
		
		int xBase = 10;
		int yBase = 10;

		// Allow variation in angles from none to 90 degrees
		for(int angle=0; angle<90; angle++) {

			IAnalysisDataset dataset = new TestDatasetBuilder(RNG_SEED).cellCount(N_CELLS)
					.xBase(xBase).yBase(yBase).baseWidth(40).baseHeight(50)
					.maxRotation(angle).randomOffsetProfiles(false).fixedProfileOffset(20)
					.profiled()
					.build();

			// Check the collection
			IProfile median = dataset.getCollection()
					.getProfileCollection()
					.getProfile(ProfileType.ANGLE, Landmark.REFERENCE_POINT, Stats.MEDIAN);
			assertTrue(median!=null);

			if(globalMedian==null)
				globalMedian = median;

			// We can't expect full precision equality due to cumulative single precision interpolation
			assertTrue(equals(globalMedian.toFloatArray(), median.toFloatArray(), 0.001f));
		}
	}
	
	@Test
	public void testSingleSquareCellMedianProfileHasExpectedValuesForQuartiles() throws Exception {
		IAnalysisDataset dataset = new TestDatasetBuilder(RNG_SEED).cellCount(1)
				.baseHeight(40).baseWidth(40).profiled().build();

		IProfile median = dataset.getCollection()
				.getProfileCollection()
				.getProfile(ProfileType.ANGLE, Landmark.REFERENCE_POINT, Stats.MEDIAN);
		
		IProfile q1 = dataset.getCollection()
				.getProfileCollection()
				.getProfile(ProfileType.ANGLE, Landmark.REFERENCE_POINT, Stats.LOWER_QUARTILE);
		
		IProfile q3 = dataset.getCollection()
				.getProfileCollection()
				.getProfile(ProfileType.ANGLE, Landmark.REFERENCE_POINT, Stats.LOWER_QUARTILE);
		
		assertTrue(equals(median.toFloatArray(), q1.toFloatArray(), 0.0001f));
		assertTrue(equals(median.toFloatArray(), q3.toFloatArray(), 0.0001f));
	}
	
	@Test
	public void testMultipleSquareCellMedianProfileHasExpectedValuesForQuartiles() throws Exception {
		IAnalysisDataset dataset = new TestDatasetBuilder(RNG_SEED).cellCount(1)
				.baseHeight(40).baseWidth(40).profiled().build();

		IProfile median = dataset.getCollection()
				.getProfileCollection()
				.getProfile(ProfileType.ANGLE, Landmark.REFERENCE_POINT, Stats.MEDIAN);
		
		IProfile q1 = dataset.getCollection()
				.getProfileCollection()
				.getProfile(ProfileType.ANGLE, Landmark.REFERENCE_POINT, Stats.LOWER_QUARTILE);
		
		IProfile q3 = dataset.getCollection()
				.getProfileCollection()
				.getProfile(ProfileType.ANGLE, Landmark.REFERENCE_POINT, Stats.LOWER_QUARTILE);
		
		assertTrue(equals(median.toFloatArray(), q1.toFloatArray(), 0.0001f));
		assertTrue(equals(median.toFloatArray(), q3.toFloatArray(), 0.0001f));
	}
	
	@Test
	public void testAllNuclearParametersCalculated() throws Exception {
		
		File testFolder = TestResources.MOUSE_INPUT_FOLDER.getAbsoluteFile();
    	IAnalysisOptions op = OptionsFactory.makeDefaultRodentAnalysisOptions(testFolder);

    	NucleusDetectionMethod nm = new NucleusDetectionMethod(TestResources.MOUSE_INPUT_FOLDER, op);
    	IAnalysisDataset d = nm.call().getFirstDataset();
    	
		new DatasetProfilingMethod(d).call();
    	
		for(Measurement stat : op.getRuleSetCollection().getMeasurableValues()) {
			
			for(Nucleus n : d.getCollection().getNuclei()) {
				assertTrue("Nucleus should have TV", n.hasLandmark(Landmark.TOP_VERTICAL));
				assertTrue("Nucleus should have BV", n.hasLandmark(Landmark.BOTTOM_VERTICAL));
				assertTrue("Nucleus should have "+stat, n.hasStatistic(stat));
				assertFalse("Nucleus error calculating "+stat, Statistical.ERROR_CALCULATING_STAT==n.getStatistic(stat));
				assertFalse("Nucleus missing landmark "+stat, Statistical.MISSING_LANDMARK==n.getStatistic(stat));
				assertFalse("Nucleus did not calculate "+stat, Statistical.STAT_NOT_CALCULATED==n.getStatistic(stat));
				assertFalse("Not a nucleus "+stat, Statistical.INVALID_OBJECT_TYPE==n.getStatistic(stat));
			}
			
			double value = d.getCollection().getMedian(stat, CellularComponent.NUCLEUS, MeasurementScale.PIXELS);
			assertFalse("Error calculating "+stat, Statistical.ERROR_CALCULATING_STAT==value);
			assertFalse("Missing landmark "+stat, Statistical.MISSING_LANDMARK==value);
			assertFalse("Did not calculate "+stat, Statistical.STAT_NOT_CALCULATED==value);
			assertFalse("Not a nucleus "+stat, Statistical.INVALID_OBJECT_TYPE==value);
		}
	}
	
}
