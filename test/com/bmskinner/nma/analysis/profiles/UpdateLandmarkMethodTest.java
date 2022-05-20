package com.bmskinner.nma.analysis.profiles;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import java.util.Arrays;
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
import com.bmskinner.nma.components.generic.IPoint;
import com.bmskinner.nma.components.measure.Measurement;
import com.bmskinner.nma.components.options.HashOptions;
import com.bmskinner.nma.components.options.OptionsFactory;
import com.bmskinner.nma.components.profiles.IProfile;
import com.bmskinner.nma.components.profiles.ProfileType;
import com.bmskinner.nma.components.rules.OrientationMark;
import com.bmskinner.nma.io.SampleDatasetReader;
import com.bmskinner.nma.logging.ConsoleFormatter;
import com.bmskinner.nma.logging.ConsoleHandler;
import com.bmskinner.nma.logging.Loggable;
import com.bmskinner.nma.stats.Stats;

/**
 * Test methods for landmark updates
 * 
 * @author ben
 *
 */
@RunWith(Parameterized.class)
public class UpdateLandmarkMethodTest {

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
				v.addCell(c);
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
	 * The RP should only be updated when called directly on root datasets. Ensure
	 * this is the case by finding the coordinates of the RP, then checking that
	 * these coordinates change in root datasets, but not when invoked on virtual
	 * datasets.
	 * 
	 * @throws Exception
	 */
	@Test
	public void testRPUpdatedOnce() throws Exception {

		Nucleus firstNucleus = dataset.getCollection().stream().findFirst().get()
				.getPrimaryNucleus();

		IPoint rp = firstNucleus.getBorderPoint(OrientationMark.REFERENCE);
		IPoint op = firstNucleus.getBorderPoint(OrientationMark.Y);

		IProfile median = dataset.getCollection().getProfileCollection()
				.getProfile(ProfileType.ANGLE, OrientationMark.REFERENCE, Stats.MEDIAN);
		IProfile opMedian = dataset.getCollection().getProfileCollection()
				.getProfile(ProfileType.ANGLE, OrientationMark.Y, Stats.MEDIAN);

		new UpdateLandmarkMethod(dataset, dataset.getAnalysisOptions().get().getRuleSetCollection()
				.getLandmark(OrientationMark.REFERENCE).get(), 100).call();

		IPoint rpPostUpdate = firstNucleus.getBorderPoint(OrientationMark.REFERENCE);
		IPoint opPostUpdate = firstNucleus.getBorderPoint(OrientationMark.Y);

		IProfile medianPostUpdate = dataset.getCollection().getProfileCollection()
				.getProfile(ProfileType.ANGLE, OrientationMark.REFERENCE, Stats.MEDIAN);

		IProfile opMedianPostUpdate = dataset.getCollection().getProfileCollection()
				.getProfile(ProfileType.ANGLE, OrientationMark.Y, Stats.MEDIAN);

		if (dataset.isRoot()) {
			assertFalse("RP should move in root dataset", rp.equals(rpPostUpdate));
			assertFalse("Median should change in root dataset", median.equals(medianPostUpdate));
		} else {
			assertTrue("RP should not move in virtual dataset", rp.equals(rpPostUpdate));
			assertTrue("Median should not change in virtual dataset",
					median.equals(medianPostUpdate));
		}

		// Other landmarks should not be moved; should remain fixed in the border
		assertTrue("OP should not move in any dataset", op.equals(opPostUpdate));

		// Position of the OP in the median profile should be updated relative to the RP
		// so that the effective OP position is unchanged
		int diff = opMedian.findBestFitOffset(opMedianPostUpdate);
		assertEquals("OP should not move relatively within profiles", 0, diff);

	}
}
