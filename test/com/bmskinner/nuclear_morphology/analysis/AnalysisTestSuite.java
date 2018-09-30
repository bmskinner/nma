package com.bmskinner.nuclear_morphology.analysis;

import org.junit.runner.RunWith;
import org.junit.runners.Suite;
import org.junit.runners.Suite.SuiteClasses;

import com.bmskinner.nuclear_morphology.analysis.profiles.AnalysisProfilesTestSuite;

/**
 * Runs all test classes in the analysis package
 * @author ben
 * @since 1.14.0
 *
 */
@RunWith(Suite.class)
@SuiteClasses({ 
	ImageFiltererTest.class, 
	MergeSourceExtracterTest.class, 
	ProfileAggregateTest.class,
	RuleSetTester.class,
	AnalysisProfilesTestSuite.class})
public class AnalysisTestSuite {

}
