package com.bmskinner.nuclear_morphology.components.profiles;

import org.junit.runner.RunWith;
import org.junit.runners.Suite;
import org.junit.runners.Suite.SuiteClasses;

@RunWith(Suite.class)
@SuiteClasses({ 
	DefaultBorderSegmentTest.class, 
	DefaultProfileCollectionTest.class, 
	DefaultProfileTest.class,
	IProfileSegmentTest.class, 
	IProfileTester.class, 
	ISegmentedProfileTest.class, 
	ProfileManagerTest.class,
	DefaultSegmentedProfileTest.class })
public class ComponentProfilesTestSuite {

}
