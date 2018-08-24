package com.bmskinner.nuclear_morphology.analysis.profiles;

import static org.junit.Assert.*;

import java.util.Arrays;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.stream.Collectors;

import org.junit.Before;
import org.junit.Test;

import com.bmskinner.nuclear_morphology.analysis.FloatArrayTester;
import com.bmskinner.nuclear_morphology.analysis.IAnalysisMethod;
import com.bmskinner.nuclear_morphology.analysis.IAnalysisResult;
import com.bmskinner.nuclear_morphology.components.ComponentFactory.ComponentCreationException;
import com.bmskinner.nuclear_morphology.components.IAnalysisDataset;
import com.bmskinner.nuclear_morphology.components.ICell;
import com.bmskinner.nuclear_morphology.components.TestDatasetBuilder;
import com.bmskinner.nuclear_morphology.components.TestDatasetFactory;
import com.bmskinner.nuclear_morphology.components.generic.IProfile;
import com.bmskinner.nuclear_morphology.components.generic.ISegmentedProfile;
import com.bmskinner.nuclear_morphology.components.generic.ProfileType;
import com.bmskinner.nuclear_morphology.components.generic.Tag;
import com.bmskinner.nuclear_morphology.components.generic.UnavailableBorderTagException;
import com.bmskinner.nuclear_morphology.components.generic.UnavailableProfileTypeException;
import com.bmskinner.nuclear_morphology.components.nuclear.IBorderPoint;
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
public class DatasetProfilingMethodTest extends FloatArrayTester {
	
	@Before
	public void setUp(){
		Logger logger = Logger.getLogger(Loggable.PROGRAM_LOGGER);
		logger.setLevel(Level.FINE);
		logger.addHandler(new ConsoleHandler(new LogPanelFormatter()));
	}
	
	private void testProfilingIsConsistent(IAnalysisDataset dataset) throws Exception {

		// Check the collection
		IProfile median = dataset.getCollection()
				.getProfileCollection()
				.getProfile(ProfileType.ANGLE, Tag.REFERENCE_POINT, Stats.MEDIAN);
						
		for(ICell cell : dataset.getCollection().getCells()) {
			ISegmentedProfile cellProfile = cell.getNucleus().getProfile(ProfileType.ANGLE, Tag.REFERENCE_POINT);
			if(!equals(median.toFloatArray(), cellProfile.toFloatArray(), 0.0001f))
				fail();			
		}
	}

	@Test
	public void testSingleCellDataset() throws Exception {
		IAnalysisDataset d = new TestDatasetBuilder().cellCount(1).ofType(NucleusType.ROUND)
				.baseHeight(40).baseWidth(40).build();
		testProfilingIsConsistent(d);
	}
	
	@Test
	public void testMultiCellIdenticalRectangularDataset() throws Exception {

		IAnalysisDataset d = new TestDatasetBuilder().cellCount(10).ofType(NucleusType.ROUND).build();
		testProfilingIsConsistent(d);
	}
	
	@Test
	public void testMultiCellIdenticalRectangularDatasetIsNotAffectedByFixedBorderOffset() throws Exception {
		IAnalysisDataset dataset = new TestDatasetBuilder().cellCount(10).withMaxSizeVariation(0).build();
		testProfilingIsConsistent(dataset);
	}
	
	@Test
	public void testMultiCellVariableRectangularDataset() throws Exception {
//		TODO - this test makes no sense, the profiles will not be identical in variable cells
		IAnalysisDataset dataset = new TestDatasetBuilder().cellCount(10).withMaxSizeVariation(20).build();
		testProfilingIsConsistent(dataset);
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
				
				IAnalysisDataset dataset = new TestDatasetBuilder().cellCount(10)
						.xBase(xBase).yBase(yBase).baseWidth(40).baseHeight(50)
						.maxRotation(0).offsetProfiles(false).fixedProfileOffset(20)
						.profiled()
						.build();
				
				Nucleus loopCell = null;
				
				// Confirm all cells are identical
				for(ICell cell : dataset.getCollection().getCells()) {
					if(globalCell==null)
						globalCell = cell.getNucleus();
					if(loopCell==null)
						loopCell=cell.getNucleus();
					
					Nucleus n = cell.getNucleus();
					assertEquals(loopCell.getBase().getX(), n.getBase().getX(), 0);
					assertEquals(loopCell.getBase().getY(), n.getBase().getY(), 0);
					assertEquals(globalCell.getBounds().getWidth(), n.getBounds().getWidth(), 0.001);
					assertEquals(globalCell.getBounds().getHeight(), n.getBounds().getHeight(), 0.001);
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
	
	@Test
	public void testProfilingIsIndependentOfCellRotation() throws Exception {
		// create cells with different x and y bases and no variation. 
		// Check they all generate the same profiles
		
		IProfile globalMedian = null;
		
		int xBase = 10;
		int yBase = 10;

		// Allow variation in angles from none to 90 degrees
		for(int angle=0; angle<90; angle++) {

			IAnalysisDataset dataset = new TestDatasetBuilder().cellCount(10)
					.xBase(xBase).yBase(yBase).baseWidth(40).baseHeight(50)
					.maxRotation(angle).offsetProfiles(false).fixedProfileOffset(20)
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
		IAnalysisDataset dataset = new TestDatasetBuilder().cellCount(1).baseHeight(40).baseWidth(40).profiled().build();

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
		IAnalysisDataset dataset = new TestDatasetBuilder().cellCount(1).baseHeight(40).baseWidth(40).profiled().build();

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
