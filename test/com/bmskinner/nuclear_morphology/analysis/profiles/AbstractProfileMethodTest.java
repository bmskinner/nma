package com.bmskinner.nuclear_morphology.analysis.profiles;

import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import org.eclipse.jdt.annotation.NonNull;

import com.bmskinner.nuclear_morphology.ComponentTester;
import com.bmskinner.nuclear_morphology.analysis.DatasetValidator;
import com.bmskinner.nuclear_morphology.charting.ChartFactoryTest;
import com.bmskinner.nuclear_morphology.charting.OutlineTestChartFactory;
import com.bmskinner.nuclear_morphology.components.UnavailableBorderTagException;
import com.bmskinner.nuclear_morphology.components.cells.ICell;
import com.bmskinner.nuclear_morphology.components.datasets.IAnalysisDataset;
import com.bmskinner.nuclear_morphology.components.nuclei.Nucleus;
import com.bmskinner.nuclear_morphology.components.profiles.IProfile;
import com.bmskinner.nuclear_morphology.components.profiles.ISegmentedProfile;
import com.bmskinner.nuclear_morphology.components.profiles.ProfileType;
import com.bmskinner.nuclear_morphology.components.profiles.Tag;
import com.bmskinner.nuclear_morphology.components.profiles.UnavailableProfileTypeException;
import com.bmskinner.nuclear_morphology.components.profiles.UnsegmentedProfileException;
import com.bmskinner.nuclear_morphology.stats.Stats;

/**
 * Base class for testing profiling methods
 * @author bms41
 * @since 1.14.0
 *
 */
public class AbstractProfileMethodTest extends ComponentTester {
			
	/**
	 * Test that profiles within identical cells dataset are consistent between 
	 * the median and the cell
	 * @param dataset
	 * @throws Exception
	 */
	protected void testProfilingIsConsistentBetweenMedianAndCells(@NonNull IAnalysisDataset dataset) throws Exception {

		// Check the collection
		IProfile median = dataset.getCollection()
				.getProfileCollection()
				.getProfile(ProfileType.ANGLE, Tag.REFERENCE_POINT, Stats.MEDIAN);
						
		for(ICell cell : dataset.getCollection().getCells()) {
			ISegmentedProfile cellProfile = cell.getPrimaryNucleus().getProfile(ProfileType.ANGLE, Tag.REFERENCE_POINT);
			if(!equals(median.toFloatArray(), cellProfile.toFloatArray(), 0.0001f))
				fail("Failed for dataset with "+dataset.getCollection().getNucleusCount()+" nuclei");			
		}
	}
	
	/**
	 * Test that every cell in the dataset has the same profile
	 * @param dataset
	 * @throws UnavailableBorderTagException
	 * @throws UnavailableProfileTypeException
	 * @throws ProfileException
	 */
	protected void testProfilesAreIdenticalForAllCells(@NonNull IAnalysisDataset dataset) throws UnavailableBorderTagException, UnavailableProfileTypeException, ProfileException {

		IProfile globalMedian = null;
		Nucleus globalCell = null;
		// Confirm all cells are identical
		for(ICell cell : dataset.getCollection()) {
			if(globalCell==null)
				globalCell = cell.getPrimaryNucleus();			
			Nucleus n = cell.getPrimaryNucleus();
			assertTrue(equals(globalCell.getProfile(ProfileType.ANGLE, Tag.REFERENCE_POINT).toFloatArray(), n.getProfile(ProfileType.ANGLE, Tag.REFERENCE_POINT).toFloatArray(), 0.001f));
		}
	}
	
	/**
	 * Test that the segmentation is consistent with a dataset
	 * @param d
	 * @throws UnavailableBorderTagException
	 * @throws UnavailableProfileTypeException
	 * @throws ProfileException
	 * @throws UnsegmentedProfileException
	 * @throws InterruptedException
	 */
	protected void testSegmentationIsConsistent(@NonNull IAnalysisDataset d) throws UnavailableBorderTagException, UnavailableProfileTypeException, ProfileException, UnsegmentedProfileException, InterruptedException {

		DatasetValidator v = new DatasetValidator();
		boolean ok = v.validate(d);
		
		if(!ok) {
			ChartFactoryTest.showProfiles(v.getErrorCells(), d);
			OutlineTestChartFactory.generateOutlineChartsForAllCells(d, "An error was found in segmentation of "+d.getName());
		}
				
		assertTrue("Dataset should be valid", ok);

		ISegmentedProfile median = d.getCollection()
				.getProfileCollection().getSegmentedProfile(ProfileType.ANGLE, Tag.REFERENCE_POINT, Stats.MEDIAN);
		
		assertTrue(median.hasSegments() && median.getSegmentCount()>1);	
	}
	
	

}
