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
import org.junit.Test;
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
import com.bmskinner.nma.components.profiles.ISegmentedProfile;
import com.bmskinner.nma.components.profiles.ProfileType;
import com.bmskinner.nma.components.rules.OrientationMark;
import com.bmskinner.nma.io.SampleDatasetReader;
import com.bmskinner.nma.logging.ConsoleFormatter;
import com.bmskinner.nma.logging.ConsoleHandler;
import com.bmskinner.nma.logging.Loggable;
import com.bmskinner.nma.stats.Stats;

/**
 * Test methods for segment unmerging
 * 
 * @author ben
 *
 */
@RunWith(Parameterized.class)
public class SegmentUnmergeMethodTest {

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

	@Test
	public void testUnmergeSegments() throws Exception {

		// Virtual collections can be passed into the profile manager
		// because we need to merge within their profile collections,
		// but the cells they contain are only merged when invoking from
		// a root dataset. Don't bother testing, it would only be inconsistent.
		if (!dataset.isRoot())
			return;

		ISegmentedProfile profile = dataset.getCollection().getProfileCollection()
				.getSegmentedProfile(
						ProfileType.ANGLE,
						OrientationMark.REFERENCE, Stats.MEDIAN);
		List<UUID> segIds = profile.getSegmentIDs();
		UUID newId = UUID.randomUUID();
		UUID segId1 = profile.getSegments().get(1).getID();
		UUID segId2 = profile.getSegments().get(2).getID();

		boolean b = dv.validate(dataset);

		assertTrue(source.getSimpleName() + " should validate: " + dv.getErrors(), b);

		new SegmentMergeMethod(dataset, segId1, segId2, newId).call();

		// only testable for real collection here, because merging
		// is a noop
		new SegmentUnmergeMethod(dataset, newId).call();

		List<UUID> newIds = dataset.getCollection().getProfileCollection().getSegmentIDs();
		assertEquals(segIds.size(), newIds.size());
		assertFalse(newIds.contains(newId));
		assertTrue(newIds.contains(segId1));
		assertTrue(newIds.contains(segId2));

		assertTrue(source.getSimpleName(), dv.validate(dataset.getCollection()));

		for (Nucleus n : dataset.getCollection().getNuclei()) {
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
