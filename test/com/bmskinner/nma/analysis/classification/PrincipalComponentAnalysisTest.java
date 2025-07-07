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
import com.bmskinner.nma.components.options.HashOptions;
import com.bmskinner.nma.components.options.OptionsBuilder;
import com.bmskinner.nma.components.profiles.ProfileType;
import com.bmskinner.nma.components.rules.RuleSetCollection;
import com.bmskinner.nma.utility.StreamUtils;

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

		final UUID clusterId = UUID.randomUUID();
		final Measurement mm = Measurement.makePrincipalComponent(clusterId);

		// Check PC measurements are not present
		final boolean anyPresent = dataset.getCollection().getNuclei().stream()
				.anyMatch(m -> m.hasMeasurement(mm));
		assertFalse(anyPresent);

		// Run the PCA on angle profiles
		final HashOptions options =  new  OptionsBuilder()
				.withValue(HashOptions.CLUSTER_GROUP_ID_KEY, clusterId)
				.withValue(ProfileType.ANGLE.toString(), true)
				.withValue(PrincipalComponentAnalysis.PROPORTION_VARIANCE_KEY, 0.95)
				.build();

		new PrincipalComponentAnalysis(dataset, options).call();

		// check number of PCSs
		final Nucleus n = dataset.getCollection().getNuclei().stream().findFirst().get();
		final int nPcs = n.getArrayMeasurement(Measurement.makePrincipalComponent(clusterId)).size();

		// Test that PCs have been set in all nuclei
		final boolean allPresent = dataset.getCollection().getNuclei().stream()
				.allMatch(m -> StreamUtils
						.uncheckCall(() -> m.hasMeasurement(mm) && m.getArrayMeasurement(mm).size() == nPcs));
		assertTrue(allPresent);
	}

}
