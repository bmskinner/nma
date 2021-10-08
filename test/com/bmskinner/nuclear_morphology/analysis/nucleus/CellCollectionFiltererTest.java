package com.bmskinner.nuclear_morphology.analysis.nucleus;

import static org.junit.Assert.*;

import org.junit.Before;
import org.junit.Test;

import com.bmskinner.nuclear_morphology.ComponentTester;
import com.bmskinner.nuclear_morphology.TestDatasetBuilder;
import com.bmskinner.nuclear_morphology.components.datasets.IAnalysisDataset;
import com.bmskinner.nuclear_morphology.components.datasets.ICellCollection;
import com.bmskinner.nuclear_morphology.components.rules.RuleSetCollection;

public class CellCollectionFiltererTest extends ComponentTester {
	
	private IAnalysisDataset d1;

	@Before
    public void loadDataset() throws Exception {    	
    	d1 = new TestDatasetBuilder(RNG_SEED).cellCount(N_CELLS)
				.ofType(RuleSetCollection.roundRuleSetCollection())
				.withMaxSizeVariation(10)
				.randomOffsetProfiles(true)
				.numberOfClusters(N_CHILD_DATASETS)
				.segmented().build();
    }
	
	@Test
	public void testAnd() throws Exception {
		IAnalysisDataset d2 = d1.copy();
		
		ICellCollection result = CellCollectionFilterer.and(d1.getCollection(), d2.getCollection());
		
		assertEquals("Adding duplicate collection should include all cells", d1.getCollection().size(), result.size());
	}

}
