package com.bmskinner.nuclear_morphology.components.cells;

import static org.junit.Assert.assertEquals;

import java.util.Arrays;

import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;
import org.junit.runners.Parameterized.Parameter;
import org.junit.runners.Parameterized.Parameters;

import com.bmskinner.nuclear_morphology.ComponentTester;
import com.bmskinner.nuclear_morphology.TestDatasetBuilder;
import com.bmskinner.nuclear_morphology.components.datasets.IAnalysisDataset;
import com.bmskinner.nuclear_morphology.components.nuclei.DefaultNucleus;
import com.bmskinner.nuclear_morphology.components.rules.RuleSetCollection;

/**
 * Test for classes implementing the CellularComponent interface
 * @author bms41
 * @since 1.15.0
 *
 */
@RunWith(Parameterized.class)
public class CellularComponentTest  extends ComponentTester {
	private static final int N_CELLS = 1;
	
	private CellularComponent component;
	
	@Rule
    public final ExpectedException exception = ExpectedException.none();

	@Parameter(0)
	public Class<? extends CellularComponent> source;
	
	@Override
	@Before
	public void setUp() throws Exception {
		super.setUp();
		component = createInstance(source);
		
	}
	
	@Parameters
	public static Iterable<Class<? extends CellularComponent>> arguments() {
		return Arrays.asList(DefaultNucleus.class);
	}

	/**
	 * Create an instance of the class under test
	 * @param source the class to create
	 * @return
	 * @throws Exception 
	 */
	public static CellularComponent createInstance(Class<? extends CellularComponent> source) throws Exception {
		IAnalysisDataset d = new TestDatasetBuilder(RNG_SEED).cellCount(N_CELLS)
				.ofType(RuleSetCollection.roundRuleSetCollection())
				.randomOffsetProfiles(true)
				.addSignalsInChannel(0)
				.segmented().build();
		if(source==DefaultNucleus.class)
			return d.getCollection().stream().findFirst().get().getPrimaryNucleus();
		throw new Exception("Unable to create instance of "+source);
	}
	
	@Test
	public void testDuplicate() throws Exception {
		CellularComponent dup = component.duplicate();
		assertEquals(component, dup);
//		testDuplicatesByField(component, dup); //TODO: not yet implemented for signal collections
	}


}
