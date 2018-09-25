package com.bmskinner.nuclear_morphology.components.nuclei;

import org.junit.Before;
import org.junit.Test;

import com.bmskinner.nuclear_morphology.components.ComponentTest;
import com.bmskinner.nuclear_morphology.components.TestComponentFactory;

/**
 * Tests for the default nucleus class
 * @author bms41
 * @since 1.14.0
 *
 */
public class DefaultNucleusTest extends ComponentTest {

	private Nucleus nucleus;	
	@Before
	public void setUp() throws Exception {
		nucleus = TestComponentFactory.rectangularNucleus(100, 100, 20, 20, 0, 20);
	}

	@Test
	public void testDuplicate() throws Exception {
		Nucleus dup = nucleus.duplicate();
		testDuplicatesByField(dup.duplicate(), dup);
	}
}
