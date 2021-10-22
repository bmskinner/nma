package com.bmskinner.nuclear_morphology.analysis.signals;

import org.junit.runner.RunWith;
import org.junit.runners.Suite;
import org.junit.runners.Suite.SuiteClasses;

import com.bmskinner.nuclear_morphology.analysis.signals.shells.AnalysisSignalsShellsTestSuite;
@RunWith(Suite.class)
@SuiteClasses({ 
	AnalysisSignalsShellsTestSuite.class, 
	PairedSignalGroupsTest.class,
	SignalDetectionMethodTest.class,
	SignalDetectorTest.class
	})
public class AnalysisSignalsTestSuite {

}
