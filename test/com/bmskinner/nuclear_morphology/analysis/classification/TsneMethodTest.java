package com.bmskinner.nuclear_morphology.analysis.classification;

import static org.junit.Assert.assertTrue;

import java.util.logging.Logger;

import org.junit.Before;
import org.junit.Test;

import com.bmskinner.nuclear_morphology.ComponentTester;
import com.bmskinner.nuclear_morphology.TestDatasetBuilder;
import com.bmskinner.nuclear_morphology.components.datasets.IAnalysisDataset;
import com.bmskinner.nuclear_morphology.components.measure.Measurement;
import com.bmskinner.nuclear_morphology.components.options.DefaultOptions;
import com.bmskinner.nuclear_morphology.components.options.HashOptions;
import com.bmskinner.nuclear_morphology.components.profiles.ProfileType;
import com.bmskinner.nuclear_morphology.components.rules.RuleSetCollection;

/**
 * Tests for tSNE
 * @author bms41
 * @since 1.16.0
 *
 */
public class TsneMethodTest extends ComponentTester {

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
		boolean isPresent = dataset.getCollection().getNuclei()
				.stream()
				.noneMatch(m->m.hasStatistic(Measurement.TSNE_1)||m.hasStatistic(Measurement.TSNE_2));
		assertTrue(isPresent);
		
		
		// Run the tSNE on angle profiles
		HashOptions options = new DefaultOptions();
		options.setBoolean(ProfileType.ANGLE.toString(), true);
		
		options.setInt(TsneMethod.MAX_ITERATIONS_KEY, 1000);
		options.setDouble(TsneMethod.PERPLEXITY_KEY, 10);
		
		TsneMethod tSNE = new TsneMethod(dataset, options);
		tSNE.call();
			
		
		// Test that tSNE stats have been set
		isPresent = dataset.getCollection().getNuclei()
				.stream()
				.allMatch(m->m.hasStatistic(Measurement.TSNE_1)&&m.hasStatistic(Measurement.TSNE_2));
		assertTrue(isPresent);
		
	}
}
