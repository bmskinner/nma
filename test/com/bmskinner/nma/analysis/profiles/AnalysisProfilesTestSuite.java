package com.bmskinner.nma.analysis.profiles;

import org.junit.runner.RunWith;
import org.junit.runners.Suite;
import org.junit.runners.Suite.SuiteClasses;

/**
 * Runs all test classes in the analysis.profiles package
 * 
 * @author ben
 * @since 1.14.0
 *
 */
@RunWith(Suite.class)
@SuiteClasses({ DatasetProfilingMethodTest.class,
		DatasetSegmentationMethodTest.class,
		IterativeSegmentFitterTest.class,
		ProfileCreatorTest.class,
		ProfileSegmenterTest.class,
		RepresentativeMedianFinderTest.class,
		SegmentFitterTest.class,
		SegmentMergeMethodTest.class,
		SegmentUnmergeMethodTest.class,
		SegmentSplitMethodTest.class,
		UpdateLandmarkMethodTest.class,
		UpdateSegmentIndexMethodTest.class
})
public class AnalysisProfilesTestSuite {

}
