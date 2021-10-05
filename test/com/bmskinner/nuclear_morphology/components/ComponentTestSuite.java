package com.bmskinner.nuclear_morphology.components;

import org.junit.runner.RunWith;
import org.junit.runners.Suite;
import org.junit.runners.Suite.SuiteClasses;

import com.bmskinner.nuclear_morphology.components.generic.ComponentGenericTestSuite;
import com.bmskinner.nuclear_morphology.components.nuclei.ComponentNucleiTestSuite;
import com.bmskinner.nuclear_morphology.components.rules.ComponentRulesTestSuite;

/**
 * Runs all test classes in the components package
 * @author ben
 * @since 1.14.0
 *
 */
@RunWith(Suite.class)
@SuiteClasses({ 
	ComponentGenericTestSuite.class,
	ComponentNucleiTestSuite.class,
	ComponentRulesTestSuite.class,
	ImageableTest.class,
	TaggableTest.class, 
	RotatableTest.class,
	TestComponentFactory.class })
public class ComponentTestSuite {

}
