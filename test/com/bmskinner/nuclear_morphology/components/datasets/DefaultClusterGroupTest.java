package com.bmskinner.nuclear_morphology.components.datasets;

import static org.junit.Assert.*;

import org.junit.Before;
import org.junit.Test;

import com.bmskinner.nuclear_morphology.ComponentTester;
import com.bmskinner.nuclear_morphology.TestDatasetBuilder;
import com.bmskinner.nuclear_morphology.components.options.OptionsFactory;
import com.bmskinner.nuclear_morphology.components.rules.RuleSetCollection;

public class DefaultClusterGroupTest extends ComponentTester {
	
	private DefaultClusterGroup g;
	
	@Before
	public void createGroup() throws Exception {
		g = new DefaultClusterGroup("test", OptionsFactory.makeDefaultClusteringOptions(), "tree");
		
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
