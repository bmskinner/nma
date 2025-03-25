package com.bmskinner.nma.analysis.classification;

import static org.junit.Assert.assertTrue;

import java.util.UUID;
import java.util.logging.Logger;
import java.util.logging.Level;

import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;

import com.bmskinner.nma.ComponentTester;
import com.bmskinner.nma.TestDatasetBuilder;
import com.bmskinner.nma.components.datasets.IAnalysisDataset;
import com.bmskinner.nma.components.measure.Measurement;
import com.bmskinner.nma.components.options.DefaultOptions;
import com.bmskinner.nma.components.options.HashOptions;
import com.bmskinner.nma.components.profiles.ProfileType;
import com.bmskinner.nma.components.rules.RuleSetCollection;

/**
 * Tests for tSNE
 * 
 * @author Ben Skinner
 * @since 1.16.0
 *
 */
public class TsneMethodTest extends ComponentTester {

	@Rule
	public final ExpectedException exception = ExpectedException.none();

	private static final Logger LOGGER = Logger.getLogger(TsneMethodTest.class.getName());

	private IAnalysisDataset dataset;

	@Before
	public void setUp() throws Exception {
		dataset = new TestDatasetBuilder(RNG_SEED).cellCount(50)
				.ofType(RuleSetCollection.roundRuleSetCollection())
				.withMaxSizeVariation(10)
				.randomOffsetProfiles(true)
				.numberOfClusters(N_CHILD_DATASETS)
				.segmented().build();
	}

	@Test
	public void testAllNucleiGetTsneValues() throws Exception {

		// Check tSNE stats are empty
		boolean nonePresent = dataset.getCollection().getNuclei()
				.stream()
				.noneMatch(m -> m.hasMeasurement(Measurement.TSNE_1)
						|| m.hasMeasurement(Measurement.TSNE_2));
		assertTrue(nonePresent);

		// Run the tSNE on angle profiles
		HashOptions options = new DefaultOptions();
		options.setBoolean(ProfileType.ANGLE.toString(), true);

		options.setInt(TsneMethod.MAX_ITERATIONS_KEY, 1000);
		options.setDouble(TsneMethod.PERPLEXITY_KEY, 10);

		UUID clusterID = UUID.randomUUID();
		options.setUUID(HashOptions.CLUSTER_GROUP_ID_KEY, clusterID);

		TsneMethod tSNE = new TsneMethod(dataset, options);
		tSNE.call();

		// Test that tSNE stats have been set
		boolean allPresent = dataset.getCollection().getNuclei()
				.stream()
				.allMatch((m -> m.hasMeasurement(Measurement.makeTSNE(1,
						clusterID))
						&& m.hasMeasurement(Measurement.makeTSNE(2,
								clusterID))));
		assertTrue(allPresent);

	}
}
