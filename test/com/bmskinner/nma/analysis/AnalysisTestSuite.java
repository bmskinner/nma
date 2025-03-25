package com.bmskinner.nma.analysis;

import org.junit.runner.RunWith;
import org.junit.runners.Suite;
import org.junit.runners.Suite.SuiteClasses;

import com.bmskinner.nma.analysis.classification.AnalysisClassificationTestSuite;
import com.bmskinner.nma.analysis.image.AnalysisImageTestSuite;
import com.bmskinner.nma.analysis.mesh.AnalysisMeshTestSuite;
import com.bmskinner.nma.analysis.nucleus.AnalysisNucleusTestSuite;
import com.bmskinner.nma.analysis.profiles.AnalysisProfilesTestSuite;
import com.bmskinner.nma.analysis.signals.AnalysisSignalsTestSuite;

/**
 * Runs all test classes in the analysis package
 * @author Ben Skinner
 * @since 1.14.0
 *
 */
@RunWith(Suite.class)
@SuiteClasses({ 
	AnalysisClassificationTestSuite.class,
	AnalysisImageTestSuite.class,
	AnalysisMeshTestSuite.class,
	AnalysisNucleusTestSuite.class,
	AnalysisProfilesTestSuite.class,
	AnalysisSignalsTestSuite.class,
	ComponentMeasurerTest.class,
	DatasetMergeMethodTest.class,
	MergeSourceExtracterTest.class, 
	RuleSetTester.class})
public class AnalysisTestSuite {

}
