package com.bmskinner.nuclear_morphology.components.nuclear;

import static org.junit.Assert.*;

import java.lang.reflect.Field;

import org.junit.Before;
import org.junit.Test;

import com.bmskinner.nuclear_morphology.components.ComponentTest;
import com.bmskinner.nuclear_morphology.components.IAnalysisDataset;
import com.bmskinner.nuclear_morphology.components.TestComponentFactory;
import com.bmskinner.nuclear_morphology.components.TestDatasetBuilder;
import com.bmskinner.nuclear_morphology.components.nuclei.Nucleus;

/**
 * Tests for the default signal collection class
 * @author bms41
 * @since 1.14.0
 *
 */
public class DefaultSignalCollectionTest extends ComponentTest {
	

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
		
		collection = d.getCollection().streamCells().findFirst().get().getNucleus().getSignalCollection();
	}

	@Test
	public void testDuplicate() throws Exception {
		ISignalCollection dup = collection.duplicate();
		testDuplicatesByField(collection, dup);
	}

}
