package com.bmskinner.nuclear_morphology.analysis.profiles;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import java.util.logging.Level;
import java.util.logging.Logger;

import org.eclipse.jdt.annotation.NonNull;
import org.junit.Before;
import org.junit.Test;

import com.bmskinner.nuclear_morphology.FloatArrayTester;
import com.bmskinner.nuclear_morphology.components.IAnalysisDataset;
import com.bmskinner.nuclear_morphology.components.ICell;
import com.bmskinner.nuclear_morphology.components.TestDatasetBuilder;
import com.bmskinner.nuclear_morphology.components.generic.IProfile;
import com.bmskinner.nuclear_morphology.components.generic.ISegmentedProfile;
import com.bmskinner.nuclear_morphology.components.generic.ProfileType;
import com.bmskinner.nuclear_morphology.components.generic.Tag;
import com.bmskinner.nuclear_morphology.components.nuclear.NucleusType;
import com.bmskinner.nuclear_morphology.components.nuclei.Nucleus;
import com.bmskinner.nuclear_morphology.logging.ConsoleHandler;
import com.bmskinner.nuclear_morphology.logging.LogPanelFormatter;
import com.bmskinner.nuclear_morphology.logging.Loggable;
import com.bmskinner.nuclear_morphology.stats.Stats;

/**
 * Test methods for the dataset profiling 
 * @author bms41
 * @since 1.14.0
 *
 */
public class DatasetProfilingMethodTest extends AbstractProfileMethodTest {

	@Test
	public void testSingleCellDataset() throws Exception {
		IAnalysisDataset d = new TestDatasetBuilder(RNG_SEED).cellCount(1).ofType(NucleusType.ROUND)
				.baseHeight(40).baseWidth(40).profiled().build();
		testProfilingIsConsistentBetweenMedianAndCells(d);
	}
	
	@Test
	public void testMultiCellIdenticalRectangularDataset() throws Exception {

		IAnalysisDataset d = new TestDatasetBuilder(RNG_SEED).cellCount(N_CELLS)
				.ofType(NucleusType.ROUND).randomOffsetProfiles(false).profiled().build();
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
				
				System.out.println("xBase "+xBase+"; yBase "+yBase);
				
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
						globalCell = cell.getNucleus();
					if(loopCell==null)
						loopCell=cell.getNucleus();
					
					Nucleus n = cell.getNucleus();
					testNucleiHaveIdenticalPositions(n, loopCell);
					testNucleiHaveIdenticalSizes(n, globalCell);
				}
							
				
				// Check the collection
				IProfile median = dataset.getCollection()
						.getProfileCollection()
						.getProfile(ProfileType.ANGLE, Tag.REFERENCE_POINT, Stats.MEDIAN);
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
					.getProfile(ProfileType.ANGLE, Tag.REFERENCE_POINT, Stats.MEDIAN);
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
				.getProfile(ProfileType.ANGLE, Tag.REFERENCE_POINT, Stats.MEDIAN);
		
		IProfile q1 = dataset.getCollection()
				.getProfileCollection()
				.getProfile(ProfileType.ANGLE, Tag.REFERENCE_POINT, Stats.LOWER_QUARTILE);
		
		IProfile q3 = dataset.getCollection()
				.getProfileCollection()
				.getProfile(ProfileType.ANGLE, Tag.REFERENCE_POINT, Stats.LOWER_QUARTILE);
		
		assertTrue(equals(median.toFloatArray(), q1.toFloatArray(), 0.0001f));
		assertTrue(equals(median.toFloatArray(), q3.toFloatArray(), 0.0001f));
	}
	
	@Test
	public void testMultipleSquareCellMedianProfileHasExpectedValuesForQuartiles() throws Exception {
		IAnalysisDataset dataset = new TestDatasetBuilder(RNG_SEED).cellCount(1)
				.baseHeight(40).baseWidth(40).profiled().build();

		IProfile median = dataset.getCollection()
				.getProfileCollection()
				.getProfile(ProfileType.ANGLE, Tag.REFERENCE_POINT, Stats.MEDIAN);
		
		IProfile q1 = dataset.getCollection()
				.getProfileCollection()
				.getProfile(ProfileType.ANGLE, Tag.REFERENCE_POINT, Stats.LOWER_QUARTILE);
		
		IProfile q3 = dataset.getCollection()
				.getProfileCollection()
				.getProfile(ProfileType.ANGLE, Tag.REFERENCE_POINT, Stats.LOWER_QUARTILE);
		
		assertTrue(equals(median.toFloatArray(), q1.toFloatArray(), 0.0001f));
		assertTrue(equals(median.toFloatArray(), q3.toFloatArray(), 0.0001f));
	}
	
}
