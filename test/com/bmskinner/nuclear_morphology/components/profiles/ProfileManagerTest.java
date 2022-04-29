package com.bmskinner.nuclear_morphology.components.profiles;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import java.io.File;
import java.util.Arrays;
import java.util.List;
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

import com.bmskinner.nuclear_morphology.TestDatasetBuilder;
import com.bmskinner.nuclear_morphology.TestResources;
import com.bmskinner.nuclear_morphology.analysis.nucleus.NucleusDetectionMethod;
import com.bmskinner.nuclear_morphology.components.MissingLandmarkException;
import com.bmskinner.nuclear_morphology.components.cells.ICell;
import com.bmskinner.nuclear_morphology.components.cells.Nucleus;
import com.bmskinner.nuclear_morphology.components.datasets.DatasetValidator;
import com.bmskinner.nuclear_morphology.components.datasets.DefaultCellCollection;
import com.bmskinner.nuclear_morphology.components.datasets.IAnalysisDataset;
import com.bmskinner.nuclear_morphology.components.datasets.ICellCollection;
import com.bmskinner.nuclear_morphology.components.datasets.VirtualDataset;
import com.bmskinner.nuclear_morphology.components.options.IAnalysisOptions;
import com.bmskinner.nuclear_morphology.components.options.MissingOptionException;
import com.bmskinner.nuclear_morphology.components.options.OptionsFactory;
import com.bmskinner.nuclear_morphology.components.rules.OrientationMark;
import com.bmskinner.nuclear_morphology.components.rules.RuleSetCollection;
import com.bmskinner.nuclear_morphology.logging.ConsoleFormatter;
import com.bmskinner.nuclear_morphology.logging.ConsoleHandler;
import com.bmskinner.nuclear_morphology.logging.Loggable;
import com.bmskinner.nuclear_morphology.stats.Stats;

/**
 * Tests for population level profile manipulation consistency
 * 
 * @author ben
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
				v.addCell(c);
			}
			return v;
		}

		throw new Exception("Unable to create instance of " + source);
	}

	@Parameters
	public static Iterable<Class<? extends ICellCollection>> arguments() {
		return Arrays.asList(DefaultCellCollection.class, VirtualDataset.class);
	}

	@Test
	public void testUpdateLandmarkToMedianBestFit() throws Exception {
//		fail();

		// Need a cell collection and a median profile

		File testFolder = TestResources.MOUSE_INPUT_FOLDER.getAbsoluteFile();
		IAnalysisOptions op = OptionsFactory.makeDefaultRodentAnalysisOptions(testFolder);

		IAnalysisDataset d = new NucleusDetectionMethod(
				TestResources.UNIT_TEST_FOLDER.getAbsoluteFile(), op).call()
						.getFirstDataset();
		d.getCollection().getProfileCollection().calculateProfiles();

		// Create a median from the current reference points in the nuclei
		IProfile median = d.getCollection().getProfileCollection().getProfile(ProfileType.ANGLE,
				OrientationMark.REFERENCE, Stats.MEDIAN);

		double diff = 0;
		for (Nucleus n : d.getCollection().getNuclei()) {
			diff += n.getProfile(ProfileType.ANGLE).absoluteSquareDifference(median);
		}

		// Run the fit. Each nucleus should now have the best possible fit
		// to the median profile
		Landmark rp = d.getCollection().getRuleSetCollection()
				.getLandmark(OrientationMark.REFERENCE).orElseThrow(MissingOptionException::new);
		d.getCollection().getProfileManager().updateLandmarkToMedianBestFit(rp, ProfileType.ANGLE,
				median);

		// Update profile collection
		collection.getProfileCollection().calculateProfiles();

		IProfile newMedian = d.getCollection().getProfileCollection().getProfile(ProfileType.ANGLE,
				OrientationMark.REFERENCE, Stats.MEDIAN);

		double postDiff = 0;
		for (Nucleus n : d.getCollection().getNuclei()) {
			postDiff += n.getProfile(ProfileType.ANGLE).absoluteSquareDifference(newMedian);
		}
		LOGGER.fine("Diff: " + diff + "; post diff " + postDiff);
		assertTrue(postDiff <= diff);
	}

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
	public void testTestSegmentsMergeable() throws Exception {

		ISegmentedProfile profile = collection.getProfileCollection().getSegmentedProfile(
				ProfileType.ANGLE,
				OrientationMark.REFERENCE, Stats.MEDIAN);

		List<UUID> segIds = profile.getSegmentIDs();
		for (int i = 0; i < segIds.size(); i++) {
			UUID segId1 = segIds.get(i);
			IProfileSegment seg1 = profile.getSegment(segId1);
			for (int j = 0; j < segIds.size(); j++) {
				UUID segId2 = segIds.get(j);
				IProfileSegment seg2 = profile.getSegment(segId2);
				if (i == j)
					assertFalse(
							"Merging " + seg1.toString() + " and " + seg2.toString()
									+ " should fail",
							manager.testSegmentsMergeable(seg1, seg2));
				else {
					if (i == j - 1)
						assertTrue(
								"Merging " + seg1.toString() + " and " + seg2.toString()
										+ " should succeed",
								manager.testSegmentsMergeable(seg1, seg2));
					else
						assertFalse(
								"Merging " + seg1.toString() + " and " + seg2.toString()
										+ " should fail",
								manager.testSegmentsMergeable(seg1, seg2));
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

	/**
	 * Test that the merging process works when segments are merged in a profile and
	 * that profile is assigned back to a nucleus
	 * 
	 * @throws Exception
	 */
	@Test
	public void testMergingSegmentsInNucleus() throws Exception {

		for (Nucleus n : collection.getNuclei()) {

			// Get the profile
			ISegmentedProfile profile = n.getProfile(ProfileType.ANGLE, OrientationMark.REFERENCE);
			List<UUID> segs = profile.getSegmentIDs();

			// Choose the segments to merge
			UUID newId = UUID.randomUUID();
			UUID segId1 = segs.get(0);
			UUID segId2 = segs.get(1);
			;

			// Merge the segments and assign to the nucleus
			profile.mergeSegments(segId1, segId2, newId);
			assertTrue(profile.hasSegment(newId));
			assertTrue(profile.getSegment(newId).hasMergeSources());
			n.setSegments(profile.getSegments());

			// Get the profile back out from the nucleus
			ISegmentedProfile newProfile = n.getProfile(ProfileType.ANGLE,
					OrientationMark.REFERENCE);

			assertEquals("Profiles should match", profile, newProfile);

			assertTrue(newProfile.hasSegment(newId));
			assertFalse(newProfile.hasSegment(segId1));
			assertFalse(newProfile.hasSegment(segId2));

			assertTrue(newProfile.getSegment(newId).hasMergeSources());
			assertTrue("Merged segment should have merge source 1",
					newProfile.getSegment(newId).hasMergeSource(segId1));
			assertTrue("Merged segment should have merge source 2",
					newProfile.getSegment(newId).hasMergeSource(segId2));
		}
	}

	@Test
	public void testMergeSegments() throws Exception {

		// Virtual collections can be passed into the profile manager
		// because we need to merge within their profile collections,
		// but the cells they contain are only merged when invoking from
		// a root dataset. Don't bother testing, it would only be inconsistent.
		if (collection.isVirtual())
			return;

		// Get the median profile with segments
		ISegmentedProfile profile = collection.getProfileCollection().getSegmentedProfile(
				ProfileType.ANGLE,
				OrientationMark.REFERENCE, Stats.MEDIAN);

		assertTrue(profile.getSegmentCount() > 1);

		for (Nucleus n : collection.getNuclei())
			assertFalse(n.isLocked());

		IProfileSegment seg1 = profile.getSegments().get(1);
		IProfileSegment seg2 = profile.getSegments().get(2);

		UUID newId = UUID.randomUUID();
		UUID segId1 = seg1.getID();
		UUID segId2 = seg2.getID();

		// Confirm the collection is valid before merging
		DatasetValidator dv = new DatasetValidator();
		dv.validate(collection);
		assertTrue("Dataset is not valid before merging: " + dv.getSummary(),
				dv.validate(collection));

		assertTrue("Segments should be mergeable", manager.testSegmentsMergeable(seg1, seg2));

		// Merge the segments
		manager.mergeSegments(segId1, segId2, newId);

		dv.validate(collection);
		assertTrue("Dataset is not valid after merging: " + dv.getSummary(),
				dv.validate(collection));

		List<UUID> newIds = collection.getProfileCollection().getSegmentIDs();

		// Test if the profile segments merged correctly
		assertEquals(profile.getSegmentCount() - 1, newIds.size());
		assertTrue(newIds.contains(newId));
		assertFalse(newIds.contains(segId1));
		assertFalse(newIds.contains(segId2));

		IProfileSegment mergedSegment = collection.getProfileCollection()
				.getSegmentedProfile(ProfileType.ANGLE, OrientationMark.REFERENCE, Stats.MEDIAN)
				.getSegment(newId);
		assertTrue(mergedSegment.hasMergeSources());
		assertTrue(mergedSegment.hasMergeSource(segId1));
		assertTrue(mergedSegment.hasMergeSource(segId2));

		// If this is a virtual collection, merging is not possible
		if (collection.isReal()) {
			for (Nucleus n : collection.getNuclei()) {
				ISegmentedProfile nucleusProfile = n.getProfile(ProfileType.ANGLE,
						OrientationMark.REFERENCE);
				List<UUID> nucleusIds = nucleusProfile.getSegmentIDs();
				assertEquals(newIds.size(), nucleusIds.size());
				assertTrue(newIds.contains(newId));
				assertFalse(newIds.contains(segId1));
				assertFalse(newIds.contains(segId2));
				IProfileSegment mergedSeg = nucleusProfile.getSegment(newId);
				assertTrue("Merged segment should have merge sources", mergedSeg.hasMergeSources());
				assertTrue("Merged segment should have merge source 1",
						mergedSeg.hasMergeSource(segId1));
				assertTrue("Merged segment should have merge source 2",
						mergedSeg.hasMergeSource(segId2));
			}
		}
	}

	@Test
	public void testUnmergeSegments() throws Exception {

		// Virtual collections can be passed into the profile manager
		// because we need to merge within their profile collections,
		// but the cells they contain are only merged when invoking from
		// a root dataset. Don't bother testing, it would only be inconsistent.
		if (collection.isVirtual())
			return;

		ISegmentedProfile profile = collection.getProfileCollection().getSegmentedProfile(
				ProfileType.ANGLE,
				OrientationMark.REFERENCE, Stats.MEDIAN);
		List<UUID> segIds = profile.getSegmentIDs();
		UUID newId = UUID.randomUUID();
		UUID segId1 = profile.getSegments().get(1).getID();
		UUID segId2 = profile.getSegments().get(2).getID();

		DatasetValidator dv = new DatasetValidator();
		boolean b = dv.validate(collection);

		assertTrue(source.getSimpleName() + " should validate: " + dv.getErrors(), b);

		manager.mergeSegments(segId1, segId2, newId);

		b = dv.validate(collection);

		assertTrue(source.getSimpleName() + " should validate: " + dv.getErrors(), b);
		manager.unmergeSegments(newId); // only testable for real collection here, because merging
										// is a noop

		List<UUID> newIds = collection.getProfileCollection().getSegmentIDs();
		assertEquals(segIds.size(), newIds.size());
		assertFalse(newIds.contains(newId));
		assertTrue(newIds.contains(segId1));
		assertTrue(newIds.contains(segId2));

		assertTrue(source.getSimpleName(), dv.validate(collection));

		for (Nucleus n : collection.getNuclei()) {
			ISegmentedProfile nucleusProfile = n.getProfile(ProfileType.ANGLE,
					OrientationMark.REFERENCE);
			List<UUID> nucleusIds = nucleusProfile.getSegmentIDs();
			assertEquals(segIds.size(), nucleusIds.size());
			assertFalse(newIds.contains(newId));
			assertTrue(newIds.contains(segId1));
			assertTrue(newIds.contains(segId2));
		}

	}
}
