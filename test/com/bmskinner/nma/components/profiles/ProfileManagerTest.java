package com.bmskinner.nma.components.profiles;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import java.util.Arrays;
import java.util.UUID;
import java.util.logging.Handler;
import java.util.logging.Level;
import java.util.logging.Logger;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;
import org.junit.runners.Parameterized.Parameter;
import org.junit.runners.Parameterized.Parameters;

import com.bmskinner.nma.TestDatasetBuilder;
import com.bmskinner.nma.components.cells.ICell;
import com.bmskinner.nma.components.cells.Nucleus;
import com.bmskinner.nma.components.datasets.DatasetValidator;
import com.bmskinner.nma.components.datasets.DefaultCellCollection;
import com.bmskinner.nma.components.datasets.IAnalysisDataset;
import com.bmskinner.nma.components.datasets.ICellCollection;
import com.bmskinner.nma.components.datasets.VirtualDataset;
import com.bmskinner.nma.components.rules.OrientationMark;
import com.bmskinner.nma.components.rules.RuleSetCollection;
import com.bmskinner.nma.logging.ConsoleFormatter;
import com.bmskinner.nma.logging.ConsoleHandler;
import com.bmskinner.nma.logging.Loggable;
import com.bmskinner.nma.stats.Stats;

/**
 * Tests for population level profile manipulation consistency
 * 
 * @author Ben Skinner
 * @since 1.14.0
 *
 */
@RunWith(Parameterized.class)
public class ProfileManagerTest {

	private static final Logger LOGGER = Logger.getLogger(Loggable.PROJECT_LOGGER);
	private static final long RNG_SEED = 42;
	private ProfileManager manager;
	private ICellCollection collection;

	static {
		for (Handler h : LOGGER.getHandlers())
			LOGGER.removeHandler(h);
		Handler h = new ConsoleHandler(new ConsoleFormatter());
		LOGGER.setLevel(Level.FINE);
		h.setLevel(Level.FINE);
		LOGGER.addHandler(h);
	}

	@Parameter(0)
	public Class<? extends ICellCollection> source;

	@Before
	public void setUp() throws Exception {
		collection = createInstance(source);
		manager = collection.getProfileManager();
	}

	/**
	 * Create an instance of the class under test
	 * 
	 * @param source the class to create
	 * @return
	 * @throws Exception
	 */
	public static ICellCollection createInstance(Class<? extends ICellCollection> source)
			throws Exception {
		IAnalysisDataset d = new TestDatasetBuilder(RNG_SEED).cellCount(10)
				.ofType(RuleSetCollection.roundRuleSetCollection()).randomOffsetProfiles(true)
				.segmented().build();

		if (source == DefaultCellCollection.class) {
			return d.getCollection();
		}

		if (source == VirtualDataset.class) {
			VirtualDataset v = new VirtualDataset(d, TestDatasetBuilder.TEST_DATASET_NAME,
					TestDatasetBuilder.TEST_DATASET_UUID);
			v.addAll(d.getCollection().getCells());
			v.getProfileCollection().calculateProfiles();
			d.getCollection().getProfileManager().copySegmentsAndLandmarksTo(v);
			for (ICell c : d.getCollection().getCells()) {
				v.add(c);
			}
			return v;
		}

		throw new Exception("Unable to create instance of " + source);
	}

	@Parameters
	public static Iterable<Class<? extends ICellCollection>> arguments() {
		return Arrays.asList(DefaultCellCollection.class, VirtualDataset.class);
	}

//	@Test
//	public void testUpdateLandmarkToMedianBestFit() throws Exception {
////		fail();
//
//		// Need a cell collection and a median profile
//
//		File testFolder = TestResources.MOUSE_INPUT_FOLDER.getAbsoluteFile();
//		IAnalysisOptions op = OptionsFactory.makeDefaultRodentAnalysisOptions(testFolder);
//
//		IAnalysisDataset d = new NucleusDetectionMethod(
//				TestResources.MOUSE_OUTPUT_FOLDER.getAbsoluteFile(), op).call()
//						.getFirstDataset();
//		d.getCollection().getProfileCollection().calculateProfiles();
//
//		// Create a median from the current reference points in the nuclei
//		IProfile median = d.getCollection().getProfileCollection().getProfile(ProfileType.ANGLE,
//				OrientationMark.REFERENCE, Stats.MEDIAN);
//
//		double diff = 0;
//		for (Nucleus n : d.getCollection().getNuclei()) {
//			diff += n.getProfile(ProfileType.ANGLE).absoluteSquareDifference(median);
//		}
//
//		// Run the fit. Each nucleus should now have the best possible fit
//		// to the median profile
//		Landmark rp = d.getCollection().getRuleSetCollection()
//				.getLandmark(OrientationMark.REFERENCE).orElseThrow(MissingOptionException::new);
//		d.getCollection().getProfileManager().updateLandmarkToMedianBestFit(rp, ProfileType.ANGLE,
//				median);
//
//		// Update profile collection
//		collection.getProfileCollection().calculateProfiles();
//
//		IProfile newMedian = d.getCollection().getProfileCollection().getProfile(ProfileType.ANGLE,
//				OrientationMark.REFERENCE, Stats.MEDIAN);
//
//		double postDiff = 0;
//		for (Nucleus n : d.getCollection().getNuclei()) {
//			postDiff += n.getProfile(ProfileType.ANGLE).absoluteSquareDifference(newMedian);
//		}
//		LOGGER.fine("Diff: " + diff + "; post diff " + postDiff);
//		assertTrue(postDiff <= diff);
//	}

	/**
	 * Test that the sample cell collection is suitable for the tests in this class
	 * - e.g. does it have enough segments in the profile
	 * 
	 * @throws ProfileException
	 * @throws MissingProfileException
	 * @throws MissingLandmarkException
	 */
	@Test
	public void testSampleCollectionIsValid() throws Exception {
		assertTrue("Test collection should have multiple segments",
				collection.getProfileManager().getSegmentCount() > 1);
		for (Nucleus n : collection.getNuclei()) {
			ISegmentedProfile profile = n.getProfile(ProfileType.ANGLE, OrientationMark.REFERENCE);
			assertTrue("Test nucleus should have multiple segments", profile.getSegmentCount() > 1);
		}
	}

	/**
	 * Test that when a true segment lock is requested for all but one segment, the
	 * lock is applied.
	 */
	@Test
	public void testSetLockTrueOnSegment() throws Exception {
		ISegmentedProfile profile = collection.getProfileCollection().getSegmentedProfile(
				ProfileType.ANGLE,
				OrientationMark.REFERENCE, Stats.MEDIAN);
		UUID segId1 = profile.getSegments().get(1).getID();

		// Ensure that all segments are unlocked
		collection.getProfileManager().setLockOnAllNucleusSegments(false);
		for (ICell c : collection) {
			for (Nucleus n : c.getNuclei()) {
				ISegmentedProfile p = n.getProfile(ProfileType.ANGLE);
				for (IProfileSegment s : p.getSegments()) {
					assertFalse(s.isLocked());
				}
			}
		}

		// Lock all but one segment
		collection.getProfileManager().setLockOnSegment(segId1, true);
		for (ICell c : collection) {
			for (Nucleus n : c.getNuclei()) {
				ISegmentedProfile p = n.getProfile(ProfileType.ANGLE);
				for (IProfileSegment s : p.getSegments()) {
					assertEquals("Segment lock state error: " + segId1, s.getID().equals(segId1),
							s.isLocked());
				}
			}
		}

	}

	/**
	 * Test that when a false segment lock is requested for all but one segment, the
	 * lock is applied.
	 */
	@Test
	public void testSetLockFalseOnSegments() throws Exception {
		ISegmentedProfile profile = collection.getProfileCollection().getSegmentedProfile(
				ProfileType.ANGLE,
				OrientationMark.REFERENCE, Stats.MEDIAN);
		UUID segId1 = profile.getSegments().get(1).getID();

		// Ensure that all segments are locked
		collection.getProfileManager().setLockOnAllNucleusSegments(true);
		for (ICell c : collection) {
			for (Nucleus n : c.getNuclei()) {
				ISegmentedProfile p = n.getProfile(ProfileType.ANGLE);
				for (IProfileSegment s : p.getSegments()) {
					assertTrue(s.isLocked());
				}
			}
		}

		// Unlock all but one segment
		collection.getProfileManager().setLockOnSegment(segId1, false);
		for (ICell c : collection) {
			for (Nucleus n : c.getNuclei()) {
				ISegmentedProfile p = n.getProfile(ProfileType.ANGLE);
				for (IProfileSegment s : p.getSegments()) {
					assertEquals("Segment lock state error: " + segId1, s.getID().equals(segId1),
							!s.isLocked());
				}
			}
		}

	}

	/**
	 * Test that when a true segment lock is requested for all segments, the lock is
	 * applied.
	 */
	@Test
	public void testSetLockTrueOnAllNucleusSegments() throws Exception {
		collection.getProfileManager().setLockOnAllNucleusSegments(false);
		for (ICell c : collection) {
			for (Nucleus n : c.getNuclei()) {
				ISegmentedProfile p = n.getProfile(ProfileType.ANGLE);
				for (IProfileSegment s : p.getSegments()) {
					assertFalse("Segment should be unlocked", s.isLocked());
				}
			}
		}

		collection.getProfileManager().setLockOnAllNucleusSegments(true);

		for (ICell c : collection) {
			for (Nucleus n : c.getNuclei()) {
				ISegmentedProfile p = n.getProfile(ProfileType.ANGLE);
				for (IProfileSegment s : p.getSegments()) {
					assertTrue("Segment should be locked", s.isLocked());
				}
			}
		}
	}

	/**
	 * Test that when a false segment lock is requested for all segments, the lock
	 * is applied.
	 */
	@Test
	public void testSetLockFalseOnAllNucleusSegments() throws Exception {
		collection.getProfileManager().setLockOnAllNucleusSegments(true);
		for (ICell c : collection) {
			for (Nucleus n : c.getNuclei()) {
				ISegmentedProfile p = n.getProfile(ProfileType.ANGLE);
				for (IProfileSegment s : p.getSegments()) {
					assertTrue("Segment should be locked", s.isLocked());
				}
			}
		}

		collection.getProfileManager().setLockOnAllNucleusSegments(false);

		for (ICell c : collection) {
			for (Nucleus n : c.getNuclei()) {
				ISegmentedProfile p = n.getProfile(ProfileType.ANGLE);
				for (IProfileSegment s : p.getSegments()) {
					assertFalse("Segment should be unlocked", s.isLocked());
				}
			}
		}
	}

	@Test
	public void testCountNucleiNotMatchingMedianSegmentation() throws Exception {
		DatasetValidator dv = new DatasetValidator();
		assertTrue(dv.validate(collection));
		// Merge on one nucleus will take it out of sync
		Nucleus n = collection.streamCells().findFirst().get().getPrimaryNucleus();
		ISegmentedProfile profile = n.getProfile(ProfileType.ANGLE, OrientationMark.REFERENCE);
		UUID segId1 = profile.getSegments().get(1).getID();
		UUID segId2 = profile.getSegments().get(2).getID();
		profile.mergeSegments(segId1, segId2, UUID.randomUUID());
		n.setSegments(profile.getSegments());
		assertFalse(dv.validate(collection));
	}

}
