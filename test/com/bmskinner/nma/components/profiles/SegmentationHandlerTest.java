package com.bmskinner.nma.components.profiles;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

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
import com.bmskinner.nma.analysis.classification.NucleusClusteringMethod;
import com.bmskinner.nma.components.cells.ICell;
import com.bmskinner.nma.components.datasets.DatasetValidator;
import com.bmskinner.nma.components.datasets.DefaultAnalysisDataset;
import com.bmskinner.nma.components.datasets.IAnalysisDataset;
import com.bmskinner.nma.components.datasets.VirtualDataset;
import com.bmskinner.nma.components.measure.Measurement;
import com.bmskinner.nma.components.options.HashOptions;
import com.bmskinner.nma.components.options.OptionsFactory;
import com.bmskinner.nma.components.rules.OrientationMark;
import com.bmskinner.nma.components.rules.RuleSetCollection;
import com.bmskinner.nma.io.SampleDatasetReader;
import com.bmskinner.nma.logging.ConsoleFormatter;
import com.bmskinner.nma.logging.ConsoleHandler;
import com.bmskinner.nma.logging.Loggable;
import com.bmskinner.nma.stats.Stats;

@RunWith(Parameterized.class)
public class SegmentationHandlerTest {
	private static final Logger LOGGER = Logger.getLogger(Loggable.PROJECT_LOGGER);
	private SegmentationHandler sh;
	private IAnalysisDataset dataset;
	private DatasetValidator dv = new DatasetValidator();

	private static final long RNG_SEED = 42;

	static {
		for (Handler h : LOGGER.getHandlers())
			LOGGER.removeHandler(h);
		Handler h = new ConsoleHandler(new ConsoleFormatter());
		LOGGER.setLevel(Level.FINE);
		h.setLevel(Level.FINE);
		LOGGER.addHandler(h);
	}

	@Parameter(0)
	public Class<? extends IAnalysisDataset> source;

	@Parameters
	public static Iterable<Class<? extends IAnalysisDataset>> arguments() {
		return Arrays.asList(DefaultAnalysisDataset.class, VirtualDataset.class);
	}

	@Before
	public void setUp() throws Exception {
		dataset = createInstance(source);
		sh = new SegmentationHandler(dataset);
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
		IAnalysisDataset d = new TestDatasetBuilder(RNG_SEED)
				.cellCount(10)
				.ofType(RuleSetCollection.roundRuleSetCollection())
				.randomOffsetProfiles(true)
				.segmented().build();

		if (source == DefaultAnalysisDataset.class) {
			return d;
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

		// Ensure the dataset has child datasets
		HashOptions o = OptionsFactory.makeDefaultClusteringOptions()
				.withValue(Measurement.AREA.toString(), true)
				.withValue(HashOptions.CLUSTER_MANUAL_CLUSTER_NUMBER_KEY, 2)
				.build();

		new NucleusClusteringMethod(d, o).call();

		throw new Exception("Unable to create instance of " + source);
	}

	/**
	 * The reference point should not be moved by segment start updates - it has to
	 * be via changing the RP position, and the segments then follow.
	 * 
	 * @throws Exception
	 */
	@Test
	public void testUpdateSegmentStartIndexActionHasNoEffectAtRP() throws Exception {

		IProfileSegment rpSeg = dataset.getCollection().getProfileCollection()
				.getSegmentContaining(OrientationMark.REFERENCE);

		int oldIndex = rpSeg.getStartIndex();

		// Should not complete -this is at the RP
		sh.updateSegmentStartIndexAction(rpSeg.getID(), oldIndex + 10);

		IProfileSegment rpSegNew = dataset.getCollection().getProfileCollection()
				.getSegmentContaining(OrientationMark.REFERENCE);

		assertEquals("RP should not update", oldIndex, rpSegNew.getStartIndex());
		assertTrue("Dataset should validate", dv.validate(dataset));
	}

	/**
	 * Check that all segment starts not at the RP can be updated
	 * 
	 * @throws Exception
	 */
	@Test
	public void testUpdateSegmentStartIndexActionHasEffectAtNonRP() throws Exception {

		IProfileSegment rpSeg = dataset.getCollection().getProfileCollection()
				.getSegmentContaining(OrientationMark.REFERENCE);

		for (IProfileSegment seg : dataset.getCollection().getProfileCollection()
				.getSegments(OrientationMark.REFERENCE)) {
			if (seg.getID().equals(rpSeg.getID()))
				continue;

			int oldIndex = seg.getStartIndex();

			// Should not complete -this is at the RP
			sh.updateSegmentStartIndexAction(seg.getID(), oldIndex + 10);

			IProfileSegment segNew = dataset.getCollection().getProfileCollection()
					.getSegmentedProfile(ProfileType.ANGLE, OrientationMark.REFERENCE, Stats.MEDIAN)
					.getSegment(seg.getID());

			assertEquals("Segment index should update", oldIndex + 10, segNew.getStartIndex());
			assertTrue("Dataset should validate", dv.validate(dataset));
		}

	}

	/**
	 * Check that if a merged segment has a start index update, the merge source
	 * segments are also updated as needed
	 * 
	 * @throws Exception
	 */
	@Test
	public void testUpdateSegmentStartIndexCorrectlyHandlesMergedSegments() throws Exception {
		ISegmentedProfile profile = dataset.getCollection().getProfileCollection()
				.getSegmentedProfile(ProfileType.ANGLE, OrientationMark.REFERENCE, Stats.MEDIAN);

		IProfileSegment seg0 = profile.getSegmentContaining(profile.size() / 2);
		IProfileSegment seg1 = seg0.nextSegment();

		sh.mergeSegments(seg0.getID(), seg1.getID());

		if (dataset.isRoot()) {
			assertEquals("Segment should be merged", profile.getSegmentCount() - 1,
					dataset.getCollection().getProfileManager().getSegmentCount());

			// Get the id of the newly added segment
			UUID newSegId = dataset.getCollection().getProfileCollection().getSegmentIDs().stream()
					.filter(
							id -> !profile.getSegmentIDs().contains(id))
					.findFirst().orElseThrow(Exception::new);

			ISegmentedProfile newProfile = dataset.getCollection().getProfileCollection()
					.getSegmentedProfile(ProfileType.ANGLE, OrientationMark.REFERENCE,
							Stats.MEDIAN);
			IProfileSegment newSeg = newProfile.getSegment(newSegId);
			assertTrue(newSeg.hasMergeSources());

			sh.updateSegmentStartIndexAction(newSegId, newSeg.getStartIndex() + 10);
			if (!dv.validate(dataset))
				fail("Dataset should validate: " + dv.getSummary() + " " + dv.getErrors());
		}

	}

	/**
	 * Check that if a merged segment has a start index update, the merge source
	 * segments are cleared
	 * 
	 * @throws Exception
	 */
	@Test
	public void testUpdateSegmentStartIndexCorrectlyHandlesMergedSegmentInRealDataset()
			throws Exception {
		IAnalysisDataset d = SampleDatasetReader.openTestRodenClusterDataset();

		ISegmentedProfile profile = d.getCollection().getProfileCollection()
				.getSegmentedProfile(ProfileType.ANGLE, OrientationMark.REFERENCE, Stats.MEDIAN);

		SegmentationHandler s = new SegmentationHandler(d);

		// Merge two segments that are not at the RP
		IProfileSegment s0 = d.getCollection().getProfileCollection()
				.getSegmentContaining(OrientationMark.REFERENCE);
		IProfileSegment s1 = s0.nextSegment();
		IProfileSegment s2 = s1.nextSegment();

		s.mergeSegments(s1.getID(), s2.getID());

		// Get the id of the newly added segment
		UUID newSegId = d.getCollection().getProfileCollection().getSegmentIDs().stream()
				.filter(
						id -> !profile.getSegmentIDs().contains(id))
				.findFirst().orElseThrow(Exception::new);

		ISegmentedProfile newProfile = d.getCollection().getProfileCollection()
				.getSegmentedProfile(ProfileType.ANGLE, OrientationMark.REFERENCE,
						Stats.MEDIAN);
		IProfileSegment newSeg = newProfile.getSegment(newSegId);
		assertTrue(newSeg.hasMergeSources());

		s.updateSegmentStartIndexAction(newSegId, newSeg.getStartIndex() + 20);
		if (!dv.validate(dataset))
			fail("Dataset should validate: " + dv.getSummary() + " " + dv.getErrors());

		// check if the merge sources were cleared properly
		newProfile = d.getCollection().getProfileCollection()
				.getSegmentedProfile(ProfileType.ANGLE, OrientationMark.REFERENCE,
						Stats.MEDIAN);
		newSeg = newProfile.getSegment(newSegId);
		assertFalse(newSeg.hasMergeSources());

	}

}
