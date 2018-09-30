package com.bmskinner.nuclear_morphology.analysis.profiles;

import org.junit.runner.RunWith;
import org.junit.runners.Suite;
import org.junit.runners.Suite.SuiteClasses;

@RunWith(Suite.class)
@SuiteClasses({ 
	DatasetProfilingMethodTest.class, 
	DatasetSegmentationMethodTest.class, 
	IterativeSegmentFitterTest.class,
	ProfileIndexFinderTest.class, 
	ProfileOffsetterTest.class, 
	ProfileSegmenterTest.class,
	RepresentativeMedianFinderTest.class, 
	SegmentFitterTest.class })
public class AnalysisProfilesTestSuite {

}
