package com.bmskinner.nuclear_morphology.components.nuclear;

import static org.junit.Assert.*;

import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import org.junit.Before;
import org.junit.Test;

import com.bmskinner.nuclear_morphology.components.ComponentTest;
import com.bmskinner.nuclear_morphology.components.IAnalysisDataset;
import com.bmskinner.nuclear_morphology.components.TestDatasetBuilder;

/**
 * Tests for the default nuclear signal class
 * @author bms41
 * @since 1.14.0
 *
 */
public class DefaultNuclearSignalTest extends ComponentTest {

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
		testDuplicatesByField(signal, dup);
	}
}
