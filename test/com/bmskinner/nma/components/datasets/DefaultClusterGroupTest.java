package com.bmskinner.nma.components.datasets;

import static org.junit.Assert.assertEquals;

import java.util.UUID;

import org.junit.Before;
import org.junit.Test;

import com.bmskinner.nma.ComponentTester;
import com.bmskinner.nma.TestDatasetBuilder;
import com.bmskinner.nma.components.options.OptionsFactory;
import com.bmskinner.nma.components.rules.RuleSetCollection;

public class DefaultClusterGroupTest extends ComponentTester {

	private DefaultClusterGroup g;

	@Before
	public void createGroup() throws Exception {
		g = new DefaultClusterGroup("test", OptionsFactory.makeDefaultClusteringOptions().build(),
				"tree", UUID.randomUUID());

		IAnalysisDataset d1 = new TestDatasetBuilder(RNG_SEED).cellCount(N_CELLS)
				.ofType(RuleSetCollection.roundRuleSetCollection())
				.withMaxSizeVariation(1)
				.randomOffsetProfiles(true)
				.segmented().build();

		IAnalysisDataset d2 = new TestDatasetBuilder(RNG_SEED).cellCount(N_CELLS)
				.ofType(RuleSetCollection.roundRuleSetCollection())
				.withMaxSizeVariation(1)
				.randomOffsetProfiles(true)
				.segmented().build();

		g.addDataset(d1);
		g.addDataset(d2);
	}

	@Test
	public void testDuplicate() {
		IClusterGroup c = g.duplicate();
		assertEquals(g, c);
	}

}
