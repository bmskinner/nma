package com.bmskinner.nuclear_morphology;

import org.junit.runner.RunWith;
import org.junit.runners.Suite;
import org.junit.runners.Suite.SuiteClasses;

import com.bmskinner.nuclear_morphology.api.BasicAnalysisPipelineTest;
import com.bmskinner.nuclear_morphology.api.SavedOptionsAnalysisPipelineTest;

/**
 * This suite runs the test file creators, then runs the
 * tests that depend on them
 * @author ben
 * @since 1.14.0
 *
 */
@RunWith(Suite.class)
@SuiteClasses({ TestDatasetCreator.class,
	BasicAnalysisPipelineTest.class,
	SavedOptionsAnalysisPipelineTest.class
	})
public class CreateAllTestFiles {

}
