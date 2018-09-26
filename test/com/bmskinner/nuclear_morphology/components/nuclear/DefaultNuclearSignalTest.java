package com.bmskinner.nuclear_morphology.components.nuclear;

import org.junit.Before;
import org.junit.Test;

import com.bmskinner.nuclear_morphology.ComponentTester;
import com.bmskinner.nuclear_morphology.components.IAnalysisDataset;
import com.bmskinner.nuclear_morphology.components.TestDatasetBuilder;

/**
 * Tests for the default nuclear signal class
 * @author bms41
 * @since 1.14.0
 *
 */
public class DefaultNuclearSignalTest extends ComponentTester {

	private static final int N_CELLS = 1;

	private INuclearSignal signal;	
	@Before
	public void setUp() throws Exception {
		IAnalysisDataset d = new TestDatasetBuilder(RNG_SEED).cellCount(N_CELLS)
				.ofType(NucleusType.ROUND)
				.withMaxSizeVariation(10)
				.randomOffsetProfiles(true)
				.addSignalsInChannel(0)
				.segmented().build();
		
		signal = d.getCollection().streamCells().findFirst().get().getNucleus()
				.getSignalCollection().getSignals(TestDatasetBuilder.RED_SIGNAL_GROUP).get(0);
	}
	
	@Test
	public void testDuplicate() throws Exception {
		INuclearSignal dup = signal.duplicate();
		// Don't test the original, because test components override
		// image methods, changing class signatures
		testDuplicatesByField(dup.duplicate(), dup);
	}
}
