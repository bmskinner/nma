package com.bmskinner.nuclear_morphology.components.nuclear;

import org.junit.Before;
import org.junit.Test;

import com.bmskinner.nuclear_morphology.ComponentTester;
import com.bmskinner.nuclear_morphology.TestDatasetBuilder;
import com.bmskinner.nuclear_morphology.components.datasets.IAnalysisDataset;
import com.bmskinner.nuclear_morphology.components.nuclei.NucleusType;
import com.bmskinner.nuclear_morphology.components.signals.ISignalCollection;

/**
 * Tests for the default signal collection class
 * @author bms41
 * @since 1.14.0
 *
 */
public class DefaultSignalCollectionTest extends ComponentTester {
	

	private static final int N_CELLS = 1;

	private ISignalCollection collection;	
	@Before
	public void setUp() throws Exception {
		IAnalysisDataset d = new TestDatasetBuilder(RNG_SEED).cellCount(N_CELLS)
				.ofType(NucleusType.ROUND)
				.withMaxSizeVariation(10)
				.randomOffsetProfiles(true)
				.addSignalsInChannel(0)
				.addSignalsInChannel(1)
				.segmented().build();
		
		collection = d.getCollection().streamCells().findFirst().get().getPrimaryNucleus().getSignalCollection();
	}

	@Test
	public void testDuplicate() throws Exception {
		ISignalCollection dup = collection.duplicate();
		testDuplicatesByField(dup.duplicate(), dup);
	}

}
