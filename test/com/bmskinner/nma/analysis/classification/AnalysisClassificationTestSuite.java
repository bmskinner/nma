package com.bmskinner.nma.analysis.classification;

import org.junit.runner.RunWith;
import org.junit.runners.Suite;
import org.junit.runners.Suite.SuiteClasses;

/**
 * Runs all test classes in the analysis.classification package
 * @author Ben Skinner
 * @since 1.16.0
 *
 */
@RunWith(Suite.class)
@SuiteClasses({ NucleusClusteringMethodTest.class,
	PrincipalComponentAnalysisTest.class,
	TsneMethodTest.class})
public class AnalysisClassificationTestSuite {

}
