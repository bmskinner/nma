package com.bmskinner.nuclear_morphology.components.nuclei;

import org.junit.runner.RunWith;
import org.junit.runners.Suite;
import org.junit.runners.Suite.SuiteClasses;

/**
 * Runs all test classes in the components.nuclei package
 * @author ben
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
