package com.bmskinner.nuclear_morphology.analysis.image;

import org.junit.runner.RunWith;
import org.junit.runners.Suite;
import org.junit.runners.Suite.SuiteClasses;

/**
 * Runs all test classes in the analysis.image package
 * @author ben
 * @since 1.16.0
 *
 */
@RunWith(Suite.class)
@SuiteClasses({MultiScaleStructuralSimilarityIndexTest.class,
	GLCMTest.class})
public class AnalysisImageTestSuite {
	
}