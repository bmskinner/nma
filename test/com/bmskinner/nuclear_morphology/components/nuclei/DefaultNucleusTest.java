package com.bmskinner.nuclear_morphology.components.nuclei;

import static org.junit.Assert.*;

import java.lang.reflect.Field;
import java.util.Arrays;

import org.junit.Before;
import org.junit.Test;
import org.junit.runners.Parameterized.Parameter;
import org.junit.runners.Parameterized.Parameters;

import com.bmskinner.nuclear_morphology.components.ComponentTest;
import com.bmskinner.nuclear_morphology.components.TestComponentFactory;
import com.bmskinner.nuclear_morphology.components.ComponentFactory.ComponentCreationException;
import com.bmskinner.nuclear_morphology.components.stats.PlottableStatistic;

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
		testDuplicatesByField(nucleus, dup);
	}
}
