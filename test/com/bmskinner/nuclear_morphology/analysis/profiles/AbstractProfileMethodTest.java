package com.bmskinner.nuclear_morphology.analysis.profiles;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import java.util.logging.Handler;
import java.util.logging.Level;
import java.util.logging.Logger;

import org.eclipse.jdt.annotation.NonNull;
import org.junit.Before;

import com.bmskinner.nuclear_morphology.ComponentTester;
import com.bmskinner.nuclear_morphology.FloatArrayTester;
import com.bmskinner.nuclear_morphology.analysis.DatasetValidator;
import com.bmskinner.nuclear_morphology.charting.ChartFactoryTest;
import com.bmskinner.nuclear_morphology.charting.OutlineChartFactoryTest;
import com.bmskinner.nuclear_morphology.components.IAnalysisDataset;
import com.bmskinner.nuclear_morphology.components.ICell;
import com.bmskinner.nuclear_morphology.components.generic.IProfile;
import com.bmskinner.nuclear_morphology.components.generic.ISegmentedProfile;
import com.bmskinner.nuclear_morphology.components.generic.ProfileType;
import com.bmskinner.nuclear_morphology.components.generic.Tag;
import com.bmskinner.nuclear_morphology.components.generic.UnavailableBorderTagException;
import com.bmskinner.nuclear_morphology.components.generic.UnavailableProfileTypeException;
import com.bmskinner.nuclear_morphology.components.generic.UnsegmentedProfileException;
import com.bmskinner.nuclear_morphology.components.nuclei.Nucleus;
import com.bmskinner.nuclear_morphology.logging.ConsoleHandler;
import com.bmskinner.nuclear_morphology.logging.LogPanelFormatter;
import com.bmskinner.nuclear_morphology.logging.Loggable;
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
			ISegmentedProfile cellProfile = cell.getNucleus().getProfile(ProfileType.ANGLE, Tag.REFERENCE_POINT);
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
				globalCell = cell.getNucleus();			
			Nucleus n = cell.getNucleus();
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
		for(String s : v.getErrors()){
			System.out.println(s);
		}
		
		if(!ok) {
			ChartFactoryTest.showProfiles(v.getErrorCells(), d);
			OutlineChartFactoryTest.generateOutlineChartsForAllCells(d, "An error was found in segmentation of "+d.getName());
		}
				
		assertTrue(ok);

		ISegmentedProfile median = d.getCollection()
				.getProfileCollection().getSegmentedProfile(ProfileType.ANGLE, Tag.REFERENCE_POINT, Stats.MEDIAN);
		
		assertTrue(median.hasSegments() && median.getSegmentCount()>1);	
	}
	
	

}
