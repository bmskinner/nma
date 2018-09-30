package com.bmskinner.nuclear_morphology.api;

import org.junit.runner.RunWith;
import org.junit.runners.Suite;
import org.junit.runners.Suite.SuiteClasses;

@RunWith(Suite.class)
@SuiteClasses({ 
	BasicAnalysisPipelineTest.class, 
	SavedOptionsAnalysisPipelineTest.class })
public class ApiTestSuite {

}
