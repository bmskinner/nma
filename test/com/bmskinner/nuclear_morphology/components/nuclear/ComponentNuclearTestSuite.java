package com.bmskinner.nuclear_morphology.components.nuclear;

import org.junit.runner.RunWith;
import org.junit.runners.Suite;
import org.junit.runners.Suite.SuiteClasses;

@RunWith(Suite.class)
@SuiteClasses({ 
	DefaultNuclearSignalTest.class, 
	DefaultProfileCollectionTest.class, 
	DefaultSignalCollectionTest.class,
	KeyedShellResultTest.class })
public class ComponentNuclearTestSuite {

}
