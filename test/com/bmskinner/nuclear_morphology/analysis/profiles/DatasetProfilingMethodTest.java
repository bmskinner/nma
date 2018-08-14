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
import com.bmskinner.nuclear_morphology.components.TestDatasetFactory;
import com.bmskinner.nuclear_morphology.components.generic.IProfile;
import com.bmskinner.nuclear_morphology.components.generic.ISegmentedProfile;
import com.bmskinner.nuclear_morphology.components.generic.ProfileType;
import com.bmskinner.nuclear_morphology.components.generic.Tag;
import com.bmskinner.nuclear_morphology.components.generic.UnavailableBorderTagException;
import com.bmskinner.nuclear_morphology.components.generic.UnavailableProfileTypeException;
import com.bmskinner.nuclear_morphology.components.nuclear.IBorderPoint;
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
	
	private void testProfiling(IAnalysisDataset dataset) throws Exception {
		DatasetProfilingMethod m = new DatasetProfilingMethod(dataset);
		m.call();

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
		IAnalysisDataset dataset = TestDatasetFactory.squareDataset(1);
		testProfiling(dataset);
	}
	
	@Test
	public void testMultiCellIdenticalRectangularDataset() throws Exception {
		int nCells = 10;
		IAnalysisDataset dataset = TestDatasetFactory.identicalRectangularDataset(nCells);
		testProfiling(dataset);
	}
	
	@Test
	public void testMultiCellIdenticalRectangularDatasetIsNotAffectedByFixedBorderOffset() throws Exception {
		IAnalysisDataset dataset = TestDatasetFactory.variableRectangularDataset(10, 0, TestDatasetFactory.DEFAULT_BASE_WIDTH, 
				TestDatasetFactory.DEFAULT_BASE_HEIGHT, TestDatasetFactory.DEFAULT_X_BASE, TestDatasetFactory.DEFAULT_Y_BASE, 
				TestDatasetFactory.DEFAULT_ROTATION, false, TestDatasetFactory.DEFAULT_BORDER_OFFSET);
		testProfiling(dataset);
	}
	
	@Test
	public void testMultiCellVariableRectangularDataset() throws Exception {

		IAnalysisDataset dataset = TestDatasetFactory.variableRectangularDataset(10, 20);
		testProfiling(dataset);
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
				IAnalysisDataset dataset = TestDatasetFactory.variableRectangularDataset(10, 0, 40, 50, xBase, yBase, 0, false, 20);
				
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
				
				IAnalysisMethod m = new DatasetProfilingMethod(dataset);
				m.call();
				
				
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

			IAnalysisDataset dataset = TestDatasetFactory.variableRectangularDataset(10, 0, 40, 50, xBase, yBase, angle, false, 20);

			IAnalysisMethod m = new DatasetProfilingMethod(dataset);
			m.call();
			
//			for(ICell cell : dataset.getCollection().getCells()) {
//				System.out.println("Cell\n");
//				for(IBorderPoint b : cell.getNucleus().getBorderList()) {
//					System.out.println(b.toString());
//				}
//			}

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
