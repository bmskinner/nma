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
import com.bmskinner.nuclear_morphology.components.options.IClusteringOptions;
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

	private IAnalysisDataset merged;
	
	@Override
	@Before
	public void setUp() throws Exception {
		super.setUp();
		merged = SampleDatasetReader.openTestRodentDataset();
		List<IClusterGroup> groups = merged.getClusterGroups();
		merged.deleteClusterGroups();
	}
	
	@Test
	public void testCanClusterOnProfiles() throws Exception {
		for(ProfileType type : ProfileType.displayValues()) {
			setUp();
			testCanClusterOnProfile(type);
		}
	}

	private void testCanClusterOnProfile(ProfileType type) throws Exception {
		IClusteringOptions o = OptionsFactory.makeClusteringOptions();
		o.setIncludeProfileType(ProfileType.ANGLE, false);
		o.setIncludeProfileType(type, true);
		o.setClusterNumber(TWO_CLUSTERS);
		new NucleusClusteringMethod(merged, o).call();
		assertNotNull(merged.getCollection());
		assertTrue(type.toString()+" has clusters:",merged.hasClusters());
		assertEquals(type.toString()+" has single cluster group:",1, merged.getClusterGroups().size());
		
		IClusterGroup group = merged.getClusterGroups().stream().findFirst().get();		
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
		IClusteringOptions o = OptionsFactory.makeClusteringOptions();
		o.setIncludeProfileType(ProfileType.ANGLE, false);
		o.setIncludeStatistic(stat, true);
		o.setClusterNumber(TWO_CLUSTERS);
		new NucleusClusteringMethod(merged, o).call();
		assertNotNull(merged.getCollection());
		assertTrue(stat.toString()+" has clusters:",merged.hasClusters());
		assertEquals(stat.toString()+" has single cluster group:",1, merged.getClusterGroups().size());
		
		IClusterGroup group = merged.getClusterGroups().stream().findFirst().get();		
		assertEquals(stat.toString()+" should have two clusters in group:",TWO_CLUSTERS, group.getUUIDs().size());
	}
}
