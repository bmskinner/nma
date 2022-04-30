package com.bmskinner.nma.api;

import org.junit.runner.RunWith;
import org.junit.runners.Suite;
import org.junit.runners.Suite.SuiteClasses;

/**
 * Runs all test classes in the api package
 * @author ben
 * @since 1.14.0
 *
 */
@RunWith(Suite.class)
@SuiteClasses({ 
	BasicAnalysisPipelineTest.class, 
	SavedOptionsAnalysisPipelineTest.class })
public class ApiTestSuite {

}
