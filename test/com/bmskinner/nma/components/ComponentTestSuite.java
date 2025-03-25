package com.bmskinner.nma.components;

import org.junit.runner.RunWith;
import org.junit.runners.Suite;
import org.junit.runners.Suite.SuiteClasses;

import com.bmskinner.nma.components.cells.ComponentCellsTestSuite;
import com.bmskinner.nma.components.datasets.ComponentDatasetsTestSuite;
import com.bmskinner.nma.components.generic.ComponentGenericTestSuite;
import com.bmskinner.nma.components.measure.ComponentMeasureTestSuite;
import com.bmskinner.nma.components.nuclei.ComponentNucleiTestSuite;
import com.bmskinner.nma.components.options.ComponentOptionsTestSuite;
import com.bmskinner.nma.components.profiles.ComponentProfilesTestSuite;
import com.bmskinner.nma.components.signals.ComponentSignalsTestSuite;

/**
 * Runs all test classes in the components package
 * @author Ben Skinner
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
