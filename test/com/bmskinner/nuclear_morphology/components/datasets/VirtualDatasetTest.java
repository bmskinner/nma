package com.bmskinner.nuclear_morphology.components.datasets;

import static org.junit.Assert.*;

import java.util.UUID;

import org.junit.Before;
import org.junit.Test;

import com.bmskinner.nuclear_morphology.ComponentTester;
import com.bmskinner.nuclear_morphology.TestDatasetBuilder;
import com.bmskinner.nuclear_morphology.components.rules.RuleSetCollection;

public class VirtualDatasetTest extends ComponentTester {
	
	private VirtualDataset d;
	private static final UUID CHILD_ID_1 = UUID.fromString("00000000-0000-0000-0000-000000000001");
    private static final UUID CHILD_ID_2 = UUID.fromString("00000000-0000-0000-0000-000000000002");
    private static final UUID CHILD_ID_NULL = UUID.fromString("00000000-0000-0000-0000-000000000000");
    

	@Before
    public void loadDataset() throws Exception {    	
		IAnalysisDataset d1 = new TestDatasetBuilder(RNG_SEED).cellCount(N_CELLS)
				.ofType(RuleSetCollection.roundRuleSetCollection())
				.withMaxSizeVariation(10)
				.randomOffsetProfiles(true)
				.numberOfClusters(N_CHILD_DATASETS)
				.segmented().build();
		
		d = new VirtualDataset(d1, "test", UUID.randomUUID(), d1.getCollection());
    }
	
	@Test
	public void testConstructFromDataset() throws Exception {
		IAnalysisDataset d1 = new TestDatasetBuilder(RNG_SEED).cellCount(N_CELLS)
				.ofType(RuleSetCollection.roundRuleSetCollection())
				.withMaxSizeVariation(10)
				.randomOffsetProfiles(true)
				.numberOfClusters(N_CHILD_DATASETS)
				.segmented().build();
		
		VirtualDataset v = new VirtualDataset(d1, "test", UUID.randomUUID(), d1.getCollection());		
		assertEquals(d1.getCollection().getProfileCollection(), v.getProfileCollection());
	}
	
	@Test
    public void testDuplicate() throws Exception {
    	IAnalysisDataset dup = d.copy();
    	testDuplicatesByField(d, dup);
    }
}
