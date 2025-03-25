package com.bmskinner.nma.analysis.classification;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import java.util.UUID;
import java.util.logging.Logger;

import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;

import com.bmskinner.nma.ComponentTester;
import com.bmskinner.nma.TestDatasetBuilder;
import com.bmskinner.nma.components.cells.Nucleus;
import com.bmskinner.nma.components.datasets.IAnalysisDataset;
import com.bmskinner.nma.components.measure.Measurement;
import com.bmskinner.nma.components.options.DefaultOptions;
import com.bmskinner.nma.components.options.HashOptions;
import com.bmskinner.nma.components.profiles.ProfileType;
import com.bmskinner.nma.components.rules.RuleSetCollection;

/**
 * Tests for principal component analysis
 * 
 * @author Ben Skinner
 * @since 1.16.0
 *
 */
public class PrincipalComponentAnalysisTest extends ComponentTester {

	@Rule
	public final ExpectedException exception = ExpectedException.none();

	private static final Logger LOGGER = Logger
			.getLogger(PrincipalComponentAnalysisTest.class.getName());

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
	public void testAllNucleiGetPrincipalComponents() throws Exception {

		UUID clusterId = UUID.randomUUID();
		// Check first 10 PC stats are empty
		for (int i = 0; i < 10; i++) {
			final int j = i;
			boolean anyPresent = dataset.getCollection().getNuclei().stream()
					.anyMatch(m -> m.hasMeasurement(Measurement
							.makePrincipalComponent(j + 1,
									clusterId)));
			assertFalse(anyPresent);
		}

		// Run the PCA on angle profiles
		HashOptions options = new DefaultOptions();
		options.setUUID(HashOptions.CLUSTER_GROUP_ID_KEY, clusterId);
		options.setBoolean(ProfileType.ANGLE.toString(), true);
		options.setDouble(PrincipalComponentAnalysis.PROPORTION_VARIANCE_KEY, 0.95);

		PrincipalComponentAnalysis pca = new PrincipalComponentAnalysis(dataset, options);
		pca.call();

		Nucleus n = dataset.getCollection().getNuclei().stream().findFirst().get();
		int nPcs = (int) n.getMeasurement(Measurement.makePrincipalComponentNumber(clusterId));

		// Test that PCs have been set
		for (int i = 0; i < nPcs; i++) {
			final int j = i;
			boolean allPresent = dataset.getCollection().getNuclei().stream()
					.allMatch(m -> m.hasMeasurement(Measurement
							.makePrincipalComponent(j + 1,
									clusterId)));
			assertTrue(allPresent);
		}
	}

}
