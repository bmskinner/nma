package com.bmskinner.nuclear_morphology.analysis;

import org.junit.runner.RunWith;
import org.junit.runners.Suite;
import org.junit.runners.Suite.SuiteClasses;

import com.bmskinner.nuclear_morphology.analysis.profiles.AnalysisProfilesTestSuite;

@RunWith(Suite.class)
@SuiteClasses({ 
	ImageFiltererTest.class, 
	MergeSourceExtracterTest.class, 
	ProfileAggregateTest.class,
	RuleSetTester.class,
	AnalysisProfilesTestSuite.class})
public class AnalysisTestSuite {

}
