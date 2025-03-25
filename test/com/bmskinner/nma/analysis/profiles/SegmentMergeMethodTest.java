package com.bmskinner.nma.analysis.profiles;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import java.util.Arrays;
import java.util.List;
import java.util.UUID;
import java.util.logging.Handler;
import java.util.logging.Level;
import java.util.logging.Logger;

import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;
import org.junit.runners.Parameterized.Parameter;
import org.junit.runners.Parameterized.Parameters;

import com.bmskinner.nma.TestDatasetBuilder;
import com.bmskinner.nma.analysis.classification.NucleusClusteringMethod;
import com.bmskinner.nma.components.cells.ICell;
import com.bmskinner.nma.components.cells.Nucleus;
import com.bmskinner.nma.components.datasets.DatasetValidator;
import com.bmskinner.nma.components.datasets.DefaultAnalysisDataset;
import com.bmskinner.nma.components.datasets.IAnalysisDataset;
import com.bmskinner.nma.components.datasets.VirtualDataset;
import com.bmskinner.nma.components.measure.Measurement;
import com.bmskinner.nma.components.options.HashOptions;
import com.bmskinner.nma.components.options.OptionsFactory;
import com.bmskinner.nma.components.profiles.IProfileSegment;
import com.bmskinner.nma.components.profiles.ISegmentedProfile;
import com.bmskinner.nma.components.profiles.ProfileType;
import com.bmskinner.nma.components.rules.OrientationMark;
import com.bmskinner.nma.io.SampleDatasetReader;
import com.bmskinner.nma.logging.ConsoleFormatter;
import com.bmskinner.nma.logging.ConsoleHandler;
import com.bmskinner.nma.logging.Loggable;
import com.bmskinner.nma.stats.Stats;

/**
 * Test methods for segment merging
 * 
 * @author Ben Skinner
 *
 */
@RunWith(Parameterized.class)
public class SegmentMergeMethodTest {

	private static final Logger LOGGER = Logger.getLogger(Loggable.PROJECT_LOGGER);
	private IAnalysisDataset dataset;
	private DatasetValidator dv = new DatasetValidator();

	static {
		for (Handler h : LOGGER.getHandlers())
			LOGGER.removeHandler(h);
		Handler h = new ConsoleHandler(new ConsoleFormatter());
		LOGGER.setLevel(Level.FINE);
		h.setLevel(Level.FINE);
		LOGGER.addHandler(h);
	}

	@Rule
	public final ExpectedException exception = ExpectedException.none();

	@Parameter(0)
	public Class<? extends IAnalysisDataset> source;

	@Parameters
	public static Iterable<Class<? extends IAnalysisDataset>> arguments() {
		return Arrays.asList(DefaultAnalysisDataset.class,
				VirtualDataset.class);
	}

	@Before
	public void setUp() throws Exception {
		dataset = createInstance(source);
	}

	/**
	 * Create an instance of the class under test
	 * 
	 * @param source the class to create
	 * @return
	 * @throws Exception
	 */
	public static IAnalysisDataset createInstance(Class<? extends IAnalysisDataset> source)
			throws Exception {
		IAnalysisDataset d = SampleDatasetReader.openTestMouseClusterDataset();

		if (source == VirtualDataset.class) {
			VirtualDataset v = new VirtualDataset(d, TestDatasetBuilder.TEST_DATASET_NAME,
					TestDatasetBuilder.TEST_DATASET_UUID);
			v.addAll(d.getCollection().getCells());
			v.getProfileCollection().calculateProfiles();
			d.getCollection().getProfileManager().copySegmentsAndLandmarksTo(v);
			for (ICell c : d.getCollection().getCells()) {
				v.add(c);
			}

			// Ensure this child dataset has child datasets
			HashOptions o = OptionsFactory.makeDefaultClusteringOptions()
					.withValue(Measurement.AREA.toString(), true)
					.withValue(HashOptions.CLUSTER_MANUAL_CLUSTER_NUMBER_KEY, 2)
					.build();

			new NucleusClusteringMethod(v, o).call();

			d = v;
		}

		return d;
	}

	/**
	 * Test that the merging process works when segments are merged in a profile and
	 * that profile is assigned back to a nucleus
	 * 
	 * @throws Exception
	 */
	@Test
	public void testMergingSegmentsInNucleus() throws Exception {

		for (Nucleus n : dataset.getCollection().getNuclei()) {

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
			assertTrue("Profile should have merged segment", profile.hasSegment(newId));
			assertTrue("Segment should have merge sources",
					profile.getSegment(newId).hasMergeSources());
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
		if (dataset.getCollection().isVirtual())
			return;

		// Get the median profile with segments
		ISegmentedProfile profile = dataset.getCollection().getProfileCollection()
				.getSegmentedProfile(
						ProfileType.ANGLE,
						OrientationMark.REFERENCE, Stats.MEDIAN);

		assertTrue(profile.getSegmentCount() > 1);

		for (Nucleus n : dataset.getCollection().getNuclei())
			assertFalse(n.isLocked());

		IProfileSegment seg1 = profile.getSegments().get(1);
		IProfileSegment seg2 = profile.getSegments().get(2);

		UUID newId = UUID.randomUUID();
		UUID segId1 = seg1.getID();
		UUID segId2 = seg2.getID();

		// Confirm the collection is valid before merging
		boolean b = dv.validate(dataset);
		assertTrue("Dataset should be valid before merging: " + dv.getSummary(), b);

		// Merge the segments
		new SegmentMergeMethod(dataset, segId1, segId2, newId).call();

		dv.validate(dataset.getCollection());
		assertTrue("Dataset is not valid after merging: " + dv.getSummary(),
				dv.validate(dataset.getCollection()));

		List<UUID> newIds = dataset.getCollection().getProfileCollection().getSegmentIDs();

		// Test if the profile segments merged correctly
		assertEquals(profile.getSegmentCount() - 1, newIds.size());
		assertTrue(newIds.contains(newId));
		assertFalse(newIds.contains(segId1));
		assertFalse(newIds.contains(segId2));

		IProfileSegment mergedSegment = dataset.getCollection().getProfileCollection()
				.getSegmentedProfile(ProfileType.ANGLE, OrientationMark.REFERENCE, Stats.MEDIAN)
				.getSegment(newId);
		assertTrue(mergedSegment.hasMergeSources());
		assertTrue(mergedSegment.hasMergeSource(segId1));
		assertTrue(mergedSegment.hasMergeSource(segId2));

		// If this is a virtual collection, merging is not possible
		if (dataset.getCollection().isReal()) {
			for (Nucleus n : dataset.getCollection().getNuclei()) {
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

//	@Test
//	public void testTestSegmentsMergeable() throws Exception {
//
//		ISegmentedProfile profile = dataset.getCollection().getProfileCollection()
//				.getSegmentedProfile(
//						ProfileType.ANGLE,
//						OrientationMark.REFERENCE, Stats.MEDIAN);
//
//		List<UUID> segIds = profile.getSegmentIDs();
//		for (int i = 0; i < segIds.size(); i++) {
//			UUID segId1 = segIds.get(i);
//			IProfileSegment seg1 = profile.getSegment(segId1);
//			for (int j = 0; j < segIds.size(); j++) {
//				UUID segId2 = segIds.get(j);
//				IProfileSegment seg2 = profile.getSegment(segId2);
//				if (i == j) {
//					exception.expect(ProfileException.class);
//					new SegmentMergeMethod(dataset, segId1, segId2).call();
//				} else {
//					if (i == j - 1)
//						new SegmentMergeMethod(dataset, segId1, segId2).call();
//					else {
//						exception.expect(ProfileException.class);
//						new SegmentMergeMethod(dataset, segId1, segId2).call();
//					}
//				}
//			}
//		}
//	}

}
