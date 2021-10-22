package com.bmskinner.nuclear_morphology.analysis.classification;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import java.util.List;
import java.util.logging.Handler;
import java.util.logging.Level;
import java.util.logging.Logger;

import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;

import com.bmskinner.nuclear_morphology.ComponentTester;
import com.bmskinner.nuclear_morphology.analysis.AnalysisMethodException;
import com.bmskinner.nuclear_morphology.components.datasets.IAnalysisDataset;
import com.bmskinner.nuclear_morphology.components.datasets.IClusterGroup;
import com.bmskinner.nuclear_morphology.components.measure.Measurement;
import com.bmskinner.nuclear_morphology.components.options.DefaultOptions;
import com.bmskinner.nuclear_morphology.components.options.HashOptions;
import com.bmskinner.nuclear_morphology.components.options.OptionsFactory;
import com.bmskinner.nuclear_morphology.components.profiles.ProfileType;
import com.bmskinner.nuclear_morphology.io.SampleDatasetReader;
import com.bmskinner.nuclear_morphology.logging.ConsoleFormatter;
import com.bmskinner.nuclear_morphology.logging.ConsoleHandler;
import com.bmskinner.nuclear_morphology.logging.Loggable;

/**
 * Tests for the nucleus clustering
 * @author ben
 *
 */
public class NucleusClusteringMethodTest extends ComponentTester {
		
	static final Logger LOGGER = Logger.getLogger(Loggable.PROJECT_LOGGER);
	
	static {
		for(Handler h : LOGGER.getHandlers())
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
	
	@Test
	public void testConstructorFailsOnNullOptions() throws Exception {
		
		exception.expect(AnalysisMethodException.class);
		new NucleusClusteringMethod(dataset, new DefaultOptions()).call();
	}

	/**
	 * Test if clustering works for the given profile type.
	 * @param type
	 * @throws Exception
	 */
	private void testCanClusterOnProfile(ProfileType type) throws Exception {
		HashOptions o = OptionsFactory.makeDefaultClusteringOptions()
				.withValue(ProfileType.ANGLE.toString(), false)
				.withValue(type.toString(), true)
				.withValue(HashOptions.CLUSTER_MANUAL_CLUSTER_NUMBER_KEY, TWO_CLUSTERS)
				.build();

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
		HashOptions o = OptionsFactory.makeDefaultClusteringOptions()
				.withValue(ProfileType.ANGLE.toString(), false)
				.withValue(stat.toString(), true)
				.withValue(HashOptions.CLUSTER_MANUAL_CLUSTER_NUMBER_KEY, TWO_CLUSTERS)
				.build();

		new NucleusClusteringMethod(dataset, o).call();
		assertNotNull(dataset.getCollection());
		assertTrue(stat.toString()+" has clusters:",dataset.hasClusters());
		assertEquals(stat.toString()+" has single cluster group:",1, dataset.getClusterGroups().size());
		
		IClusterGroup group = dataset.getClusterGroups().stream().findFirst().get();		
		assertEquals(stat.toString()+" should have two clusters in group:",TWO_CLUSTERS, group.getUUIDs().size());
	}
}
