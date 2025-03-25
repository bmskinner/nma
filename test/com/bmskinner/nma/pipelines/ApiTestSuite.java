package com.bmskinner.nma.pipelines;

import org.junit.runner.RunWith;
import org.junit.runners.Suite;
import org.junit.runners.Suite.SuiteClasses;

/**
 * Runs all test classes in the api package
 * @author Ben Skinner
 * @since 1.14.0
 *
 */
@RunWith(Suite.class)
@SuiteClasses({ 
	BasicAnalysisPipelineTest.class, 
	SavedOptionsAnalysisPipelineTest.class })
public class ApiTestSuite {

}
