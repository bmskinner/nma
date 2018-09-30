package com.bmskinner.nuclear_morphology.components.generic;

import org.junit.runner.RunWith;
import org.junit.runners.Suite;
import org.junit.runners.Suite.SuiteClasses;

@RunWith(Suite.class)
@SuiteClasses({ 
	BorderSegmentTreeTest.class, 
	DefaultBorderSegmentTest.class, 
	DefaultProfileTest.class,
	DefaultSegmentedProfileTest.class, 
	DoubleEquationTest.class, 
	FloatEquationTest.class, 
	FloatPointTest.class,
	FloatProfileTest.class, 
	IBorderSegmentTester.class, 
	IProfileTester.class, 
	ISegmentedProfileTester.class,
	ProfileManagerTest.class, 
	SegmentedFloatProfileTest.class, 
	VersionTest.class })
public class ComponentGenericTestSuite {

}
