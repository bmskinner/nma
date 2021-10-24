package com.bmskinner.nuclear_morphology.components;

import org.junit.runner.RunWith;
import org.junit.runners.Suite;
import org.junit.runners.Suite.SuiteClasses;

import com.bmskinner.nuclear_morphology.components.cells.ComponentCellsTestSuite;
import com.bmskinner.nuclear_morphology.components.datasets.ComponentDatasetsTestSuite;
import com.bmskinner.nuclear_morphology.components.generic.ComponentGenericTestSuite;
import com.bmskinner.nuclear_morphology.components.measure.ComponentMeasureTestSuite;
import com.bmskinner.nuclear_morphology.components.nuclei.ComponentNucleiTestSuite;
import com.bmskinner.nuclear_morphology.components.options.ComponentOptionsTestSuite;
import com.bmskinner.nuclear_morphology.components.profiles.ComponentProfilesTestSuite;
import com.bmskinner.nuclear_morphology.components.signals.ComponentSignalsTestSuite;

/**
 * Runs all test classes in the components package
 * @author ben
 * @since 1.14.0
 *
 */
@RunWith(Suite.class)
@SuiteClasses({ 
	ComponentCellsTestSuite.class,
	ComponentDatasetsTestSuite.class,
	ComponentGenericTestSuite.class,
	ComponentMeasureTestSuite.class,
	ComponentNucleiTestSuite.class,
	ComponentOptionsTestSuite.class,
	ComponentProfilesTestSuite.class,
//	ComponentRulesTestSuite.class,
	ComponentSignalsTestSuite.class,
	ImageableTest.class,
	RotatableTest.class,
	TaggableTest.class, 
	TestComponentFactory.class })
public class ComponentTestSuite {

}
