package com.bmskinner.nuclear_morphology.analysis;

import org.junit.runner.RunWith;
import org.junit.runners.Suite;
import org.junit.runners.Suite.SuiteClasses;

import com.bmskinner.nuclear_morphology.analysis.classification.AnalysisClassificationTestSuite;
import com.bmskinner.nuclear_morphology.analysis.image.AnalysisImageTestSuite;
import com.bmskinner.nuclear_morphology.analysis.mesh.AnalysisMeshTestSuite;
import com.bmskinner.nuclear_morphology.analysis.nucleus.AnalysisNucleusTestSuite;
import com.bmskinner.nuclear_morphology.analysis.profiles.AnalysisProfilesTestSuite;
import com.bmskinner.nuclear_morphology.analysis.signals.AnalysisSignalsTestSuite;

/**
 * Runs all test classes in the analysis package
 * @author ben
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
	DatasetMergeMethodTest.class,
	MergeSourceExtracterTest.class, 
	NucleusDetectionTest.class,
	ProfileAggregateTest.class,
	RuleSetTester.class})
public class AnalysisTestSuite {

}
