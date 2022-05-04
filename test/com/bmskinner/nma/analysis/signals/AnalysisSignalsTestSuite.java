package com.bmskinner.nma.analysis.signals;

import org.junit.runner.RunWith;
import org.junit.runners.Suite;
import org.junit.runners.Suite.SuiteClasses;

import com.bmskinner.nma.analysis.signals.shells.AnalysisSignalsShellsTestSuite;

@RunWith(Suite.class)
@SuiteClasses({
		AnalysisSignalsShellsTestSuite.class,
		SignalDetectionMethodTest.class,
		SignalDetectorTest.class
})
public class AnalysisSignalsTestSuite {

}
