package com.bmskinner.nma.analysis.classification;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import java.util.logging.Handler;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.stream.Collectors;

import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;

import com.bmskinner.nma.analysis.AnalysisMethodException;
import com.bmskinner.nma.analysis.IAnalysisResult;
import com.bmskinner.nma.components.datasets.DatasetValidator;
import com.bmskinner.nma.components.datasets.IAnalysisDataset;
import com.bmskinner.nma.components.datasets.IClusterGroup;
import com.bmskinner.nma.components.measure.Measurement;
import com.bmskinner.nma.components.options.DefaultOptions;
import com.bmskinner.nma.components.options.HashOptions;
import com.bmskinner.nma.components.options.OptionsFactory;
import com.bmskinner.nma.components.profiles.ProfileType;
import com.bmskinner.nma.io.SampleDatasetReader;
import com.bmskinner.nma.logging.ConsoleFormatter;
import com.bmskinner.nma.logging.ConsoleHandler;


/**
 * Tests for the nucleus clustering
 * 
 * @author Ben Skinner
 *
 */
public class NucleusClusteringMethodTest {

	static final Logger LOGGER = Logger.getLogger(Logger.GLOBAL_LOGGER_NAME);

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

	private static final int TWO_CLUSTERS = 2;

	private IAnalysisDataset dataset;

	@Before
	public void setUp() throws Exception {
		dataset = SampleDatasetReader.openTestMouseDataset();
	}

	@Test
	public void testConstructorFailsOnNullOptions() throws Exception {

		exception.expect(AnalysisMethodException.class);
		new NucleusClusteringMethod(dataset, new DefaultOptions()).call();
	}

	/**
	 * Test that clustering works on all profile types. Each type is tested in turn
	 * 
	 * @throws Exception
	 */
	@Test
	public void testCanClusterOnProfiles() throws Exception {
		for (ProfileType type : ProfileType.displayValues()) {
			testCanClusterOnProfile(type, ClusteringMethod.HIERARCHICAL);
			testCanClusterOnProfile(type, ClusteringMethod.EM);
		}
	}

	/**
	 * Test if clustering works for the given profile type.
	 * 
	 * @param type
	 * @throws Exception
	 */
	private void testCanClusterOnProfile(ProfileType type, ClusteringMethod method)
			throws Exception {
		setUp();
		HashOptions o = OptionsFactory.makeDefaultClusteringOptions()
				.withValue(HashOptions.CLUSTER_METHOD_KEY, method.name())
				.withValue(ProfileType.ANGLE.toString(), false)
				.withValue(type.toString(), true)
				.withValue(HashOptions.CLUSTER_MANUAL_CLUSTER_NUMBER_KEY, TWO_CLUSTERS)
				.build();

		new NucleusClusteringMethod(dataset, o).call();
		assertNotNull(dataset.getCollection());

		assertTrue(type.toString() + " has clusters:", dataset.hasClusters());
		assertEquals(type.toString() + " has single cluster group:", 1,
				dataset.getClusterGroups().size());

		IClusterGroup group = dataset.getClusterGroups().stream().findFirst().get();
		assertTrue(type.toString() + " should have at least one cluster in group:",
				group.getUUIDs().size() >= 1);
	}

	@Test
	public void testCanClusterOnIndividualStatistics() throws Exception {
		for (Measurement stat : Measurement.getRoundNucleusStats()) {
			setUp();
			LOGGER.fine("Clustering on " + stat);
			testCanClusterOnStatistic(stat, ClusteringMethod.HIERARCHICAL);
			testCanClusterOnStatistic(stat, ClusteringMethod.EM);
		}
	}

	private void testCanClusterOnStatistic(Measurement stat, ClusteringMethod method)
			throws Exception {
		setUp();
		HashOptions o = OptionsFactory.makeDefaultClusteringOptions()
				.withValue(HashOptions.CLUSTER_METHOD_KEY, method.name())
				.withValue(ProfileType.ANGLE.toString(), false)
				.withValue(stat.toString(), true)
				.withValue(HashOptions.CLUSTER_MANUAL_CLUSTER_NUMBER_KEY, TWO_CLUSTERS)
				.build();

		new NucleusClusteringMethod(dataset, o).call();
		assertNotNull(dataset.getCollection());
		assertTrue(stat.toString() + " has clusters:", dataset.hasClusters());
		assertEquals(stat.toString() + " has single cluster group:", 1,
				dataset.getClusterGroups().size());

		IClusterGroup group = dataset.getClusterGroups().stream().findFirst().get();

		assertTrue(stat.toString() + " should have at least one cluster in group:",
				group.getUUIDs().size() >= 1);

	}

	@Test
	public void testCanClusterChildDataset() throws Exception {
		testClusteringChildDataset(ClusteringMethod.HIERARCHICAL);
		testClusteringChildDataset(ClusteringMethod.EM);
	}

	private void testClusteringChildDataset(ClusteringMethod method) throws Exception {
		setUp();
		HashOptions o = OptionsFactory.makeDefaultClusteringOptions()
				.withValue(HashOptions.CLUSTER_METHOD_KEY, method.name())
				.withValue(ProfileType.ANGLE.toString(), false)
				.withValue(Measurement.AREA.toString(), true)
				.withValue(HashOptions.CLUSTER_MANUAL_CLUSTER_NUMBER_KEY, TWO_CLUSTERS)
				.build();

		IAnalysisResult r = new NucleusClusteringMethod(dataset, o).call();

		// First of the two clusters
		IAnalysisDataset child = r.getFirstDataset();
		if (child.getCollection().size() < 2) {
			LOGGER.fine("Cannot cluster a child with less than 2 cells");
			child = r.getDatasets().get(1);
		}

		// Now cluster again
		IAnalysisResult r2 = new NucleusClusteringMethod(child, o).call();
		assertTrue("Child should have at least one cluster in group",
				r2.getDatasets().size() >= 1);

		assertTrue(child.getChildCount() >= 1);

		DatasetValidator dv = new DatasetValidator();

		if (!dv.validate(child))
			fail(dv.getErrors().stream().collect(Collectors.joining(", ")));
	}

}
