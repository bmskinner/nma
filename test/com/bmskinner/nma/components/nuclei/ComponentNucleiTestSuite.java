package com.bmskinner.nma.components.nuclei;

import org.junit.runner.RunWith;
import org.junit.runners.Suite;
import org.junit.runners.Suite.SuiteClasses;

/**
 * Runs all test classes in the components.nuclei package
 * @author Ben Skinner
 * @since 1.14.0
 *
 */
@RunWith(Suite.class)
@SuiteClasses({ 
	DefaultNucleusTest.class, 
	DefaultRodentSpermNucleusTest.class, 
	NucleusTest.class })
public class ComponentNucleiTestSuite {

}
