package com.bmskinner.nuclear_morphology.components.profiles;

import org.junit.runner.RunWith;
import org.junit.runners.Suite;
import org.junit.runners.Suite.SuiteClasses;

@RunWith(Suite.class)
@SuiteClasses({ 
	DefaultBorderSegmentTest.class, 
	DefaultProfileCollectionTest.class, 
	FloatProfileTest.class,
	IProfileSegmentTest.class, 
	IProfileTester.class, 
	ISegmentedProfileTester.class, 
	ProfileManagerTest.class,
	SegmentedFloatProfileTest.class })
public class ComponentProfilesTestSuite {

}
