package com.bmskinner.nuclear_morphology.analysis.classification;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import java.util.List;
import java.util.logging.Logger;

import org.junit.Before;
import org.junit.Test;

import com.bmskinner.nuclear_morphology.ComponentTester;
import com.bmskinner.nuclear_morphology.components.datasets.IAnalysisDataset;
import com.bmskinner.nuclear_morphology.components.datasets.IClusterGroup;
import com.bmskinner.nuclear_morphology.components.measure.Measurement;
import com.bmskinner.nuclear_morphology.components.options.HashOptions;
import com.bmskinner.nuclear_morphology.components.options.OptionsFactory;
import com.bmskinner.nuclear_morphology.components.profiles.ProfileType;
import com.bmskinner.nuclear_morphology.io.SampleDatasetReader;

/**
 * Tests for the nucleus clustering
 * @author ben
 *
 */
public class NucleusClusteringMethodTest extends ComponentTester {
	
	private static final Logger LOGGER = Logger.getLogger(Logger.GLOBAL_LOGGER_NAME);
	
	private static final int TWO_CLUSTERS = 2;

	private IAnalysisDataset dataset;
	
	@Override
	@Before
	public void setUp() throws Exception {
		super.setUp();
		dataset = SampleDatasetReader.openTestRodentDataset();
	}
	
	/**
	 * Test that clustering works on all profile types.
	 * Each type is tested in turn
	 * @throws Exception
	 */
	@Test
	public void testCanClusterOnProfiles() throws Exception {
		for(ProfileType type : ProfileType.displayValues()) {
			setUp();
			testCanClusterOnProfile(type);
		}
	}

	/**
	 * Test if clustering works for the given profile type.
	 * @param type
	 * @throws Exception
	 */
	private void testCanClusterOnProfile(ProfileType type) throws Exception {
		HashOptions o = OptionsFactory.makeDefaultClusteringOptions();
		o.setBoolean(ProfileType.ANGLE.toString(), false);
		o.setBoolean(type.toString(), true);
		o.setInt(HashOptions.CLUSTER_MANUAL_CLUSTER_NUMBER_KEY, TWO_CLUSTERS);

		new NucleusClusteringMethod(dataset, o).call();
		assertNotNull(dataset.getCollection());
		assertTrue(type.toString()+" has clusters:",dataset.hasClusters());
		assertEquals(type.toString()+" has single cluster group:",1, dataset.getClusterGroups().size());
		
		IClusterGroup group = dataset.getClusterGroups().stream().findFirst().get();		
		assertEquals(type.toString()+" should have two clusters in group:",TWO_CLUSTERS, group.getUUIDs().size());
	}
	
	@Test
	public void testCanClusterOnIndividualStatistics() throws Exception {
		for(Measurement stat : Measurement.getRoundNucleusStats()) {
			setUp();
			testCanClusterOnStatistic(stat);
		}
	}
	
	private void testCanClusterOnStatistic(Measurement stat) throws Exception {
		HashOptions o = OptionsFactory.makeDefaultClusteringOptions();
		o.setBoolean(ProfileType.ANGLE.toString(), false);
		o.setBoolean(stat.toString(), true);
		o.setInt(HashOptions.CLUSTER_MANUAL_CLUSTER_NUMBER_KEY, TWO_CLUSTERS);

		new NucleusClusteringMethod(dataset, o).call();
		assertNotNull(dataset.getCollection());
		assertTrue(stat.toString()+" has clusters:",dataset.hasClusters());
		assertEquals(stat.toString()+" has single cluster group:",1, dataset.getClusterGroups().size());
		
		IClusterGroup group = dataset.getClusterGroups().stream().findFirst().get();		
		assertEquals(stat.toString()+" should have two clusters in group:",TWO_CLUSTERS, group.getUUIDs().size());
	}
}
