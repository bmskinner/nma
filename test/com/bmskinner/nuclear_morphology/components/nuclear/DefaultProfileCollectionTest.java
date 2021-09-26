package com.bmskinner.nuclear_morphology.components.nuclear;

import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;

import com.bmskinner.nuclear_morphology.ComponentTester;
import com.bmskinner.nuclear_morphology.TestDatasetBuilder;
import com.bmskinner.nuclear_morphology.components.datasets.IAnalysisDataset;
import com.bmskinner.nuclear_morphology.components.nuclei.NucleusType;
import com.bmskinner.nuclear_morphology.components.profiles.IProfileCollection;

public class DefaultProfileCollectionTest extends ComponentTester {

	@Rule
	public final ExpectedException exception = ExpectedException.none();
	
	private IProfileCollection profiles;

	@Before
	public void setUp() throws Exception {
		IAnalysisDataset d = new TestDatasetBuilder(RNG_SEED).cellCount(N_CELLS)
				.ofType(NucleusType.ROUND)
				.withMaxSizeVariation(10)
				.randomOffsetProfiles(true)
				.segmented().build();
		profiles = d.getCollection().getProfileCollection();
	}

	@Test
	public void testDuplicate() throws Exception {
		IProfileCollection dup = profiles.duplicate();
		testDuplicatesByField(dup.duplicate(), dup);
	}

}
