package com.bmskinner.nma.analysis.profiles;

import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import org.eclipse.jdt.annotation.NonNull;

import com.bmskinner.nma.ComponentTester;
import com.bmskinner.nma.charting.ChartFactoryTest;
import com.bmskinner.nma.charting.OutlineTestChartFactory;
import com.bmskinner.nma.components.MissingDataException;
import com.bmskinner.nma.components.cells.ICell;
import com.bmskinner.nma.components.cells.Nucleus;
import com.bmskinner.nma.components.datasets.DatasetValidator;
import com.bmskinner.nma.components.datasets.IAnalysisDataset;
import com.bmskinner.nma.components.profiles.IProfile;
import com.bmskinner.nma.components.profiles.IProfileSegment.SegmentUpdateException;
import com.bmskinner.nma.components.profiles.ISegmentedProfile;
import com.bmskinner.nma.components.profiles.ProfileException;
import com.bmskinner.nma.components.profiles.ProfileType;
import com.bmskinner.nma.components.profiles.UnsegmentedProfileException;
import com.bmskinner.nma.components.rules.OrientationMark;
import com.bmskinner.nma.stats.Stats;

/**
 * Base class for testing profiling methods
 * 
 * @author bms41
 * @since 1.14.0
 *
 */
public class AbstractProfileMethodTest extends ComponentTester {

	/**
	 * Test that profiles within identical cells dataset are consistent between the
	 * median and the cell
	 * 
	 * @param dataset
	 * @throws Exception
	 */
	protected void testProfilingIsConsistentBetweenMedianAndCells(@NonNull IAnalysisDataset dataset)
			throws Exception {

		// Check the collection
		IProfile median = dataset.getCollection()
				.getProfileCollection()
				.getProfile(ProfileType.ANGLE, OrientationMark.REFERENCE, Stats.MEDIAN);

		for (ICell cell : dataset.getCollection().getCells()) {
			ISegmentedProfile cellProfile = cell.getPrimaryNucleus().getProfile(ProfileType.ANGLE,
					OrientationMark.REFERENCE);
			if (!equals(median.toFloatArray(), cellProfile.toFloatArray(), 0.0001f))
				fail("Failed for dataset with " + dataset.getCollection().getNucleusCount()
						+ " nuclei");
		}
	}

	/**
	 * Test that every cell in the dataset has the same profile
	 * 
	 * @param dataset
	 * @throws ProfileException
	 * @throws MissingDataException
	 * @throws SegmentUpdateException
	 */
	protected void testProfilesAreIdenticalForAllCells(@NonNull IAnalysisDataset dataset)
			throws ProfileException, SegmentUpdateException, MissingDataException {

		IProfile globalMedian = null;
		Nucleus globalCell = null;
		// Confirm all cells are identical
		for (ICell cell : dataset.getCollection()) {
			if (globalCell == null)
				globalCell = cell.getPrimaryNucleus();
			Nucleus n = cell.getPrimaryNucleus();
			assertTrue(equals(
					globalCell.getProfile(ProfileType.ANGLE, OrientationMark.REFERENCE)
							.toFloatArray(),
					n.getProfile(ProfileType.ANGLE, OrientationMark.REFERENCE).toFloatArray(),
					0.001f));
		}
	}

	/**
	 * Test that the segmentation is consistent with a dataset
	 * 
	 * @param d
	 * @throws ProfileException
	 * @throws UnsegmentedProfileException
	 * @throws InterruptedException
	 * @throws SegmentUpdateException
	 * @throws MissingDataException
	 */
	protected void testSegmentationIsConsistent(@NonNull IAnalysisDataset d)
			throws ProfileException,
			UnsegmentedProfileException, InterruptedException, MissingDataException,
			SegmentUpdateException {

		DatasetValidator v = new DatasetValidator();
		boolean ok = v.validate(d);

		if (!ok) {
			ChartFactoryTest.showProfiles(v.getErrorCells(), d);
			OutlineTestChartFactory.generateOutlineChartsForAllCells(d,
					"An error was found in segmentation of " + d.getName());
		}

		assertTrue("Dataset should be valid", ok);

		ISegmentedProfile median = d.getCollection()
				.getProfileCollection()
				.getSegmentedProfile(ProfileType.ANGLE, OrientationMark.REFERENCE, Stats.MEDIAN);

		assertTrue("Median should have segments", median.hasSegments());
//		assertTrue("Median should have more than one segment", median.getSegmentCount()>1);	
	}

}
