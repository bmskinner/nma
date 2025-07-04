package com.bmskinner.nma.components.signals;

import org.junit.runner.RunWith;
import org.junit.runners.Suite;
import org.junit.runners.Suite.SuiteClasses;

@RunWith(Suite.class)
@SuiteClasses({ DefaultNuclearSignalTest.class, 
	DefaultShellResultTest.class, 
	DefaultSignalCollectionTest.class,
	DefaultSignalGroupTest.class, 
	DefaultWarpedSignalTest.class,
	ShellCountTest.class,
	ShellKeyTest.class})
public class ComponentSignalsTestSuite {

}
