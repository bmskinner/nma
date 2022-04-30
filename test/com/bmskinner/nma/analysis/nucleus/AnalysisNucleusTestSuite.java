package com.bmskinner.nma.analysis.nucleus;

import org.junit.runner.RunWith;
import org.junit.runners.Suite;
import org.junit.runners.Suite.SuiteClasses;

@RunWith(Suite.class)
@SuiteClasses({ CellCollectionFiltererTest.class, 
	ConsensusAveragingMethodTest.class,
	NucleusDetectionMethodTest.class
	})
public class AnalysisNucleusTestSuite {

}
