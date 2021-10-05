package com.bmskinner.nuclear_morphology.components.generic;

import org.junit.runner.RunWith;
import org.junit.runners.Suite;
import org.junit.runners.Suite.SuiteClasses;

/**
 * Runs all test classes in the components.generic package
 * @author ben
 * @since 1.14.0
 *
 */
@RunWith(Suite.class)
@SuiteClasses({ 
	FloatPointTest.class,
	VersionTest.class })
public class ComponentGenericTestSuite {

}
