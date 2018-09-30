package com.bmskinner.nuclear_morphology.components;

import org.junit.runner.RunWith;
import org.junit.runners.Suite;
import org.junit.runners.Suite.SuiteClasses;

import com.bmskinner.nuclear_morphology.components.generic.ComponentGenericTestSuite;
import com.bmskinner.nuclear_morphology.components.nuclear.ComponentNuclearTestSuite;
import com.bmskinner.nuclear_morphology.components.nuclei.ComponentNucleiTestSuite;

@RunWith(Suite.class)
@SuiteClasses({ 
	ComponentGenericTestSuite.class,
	ComponentNuclearTestSuite.class,
	ComponentNucleiTestSuite.class,
	DefaultAnalysisDatasetTest.class, 
	DefaultCellCollectionTest.class, 
	DefaultCellTest.class,
	ICellCollectionTest.class, 
	TaggableTest.class, 
	TestComponentFactory.class })
public class ComponentTestSuite {

}
