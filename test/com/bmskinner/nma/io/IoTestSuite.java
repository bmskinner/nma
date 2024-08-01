package com.bmskinner.nma.io;

import org.junit.runner.RunWith;
import org.junit.runners.Suite;
import org.junit.runners.Suite.SuiteClasses;

import com.bmskinner.nma.io.conversion.IoConversionTestSuite;
import com.bmskinner.nma.io.xml.IoXmlTestSuite;

/**
 * Runs all test classes in the io package
 * 
 * @author ben
 * @since 1.14.0
 *
 */
@RunWith(Suite.class)
@SuiteClasses({
		IoConversionTestSuite.class,
		ImageImporterTest.class,
		DatasetOutlinesExporterTest.class,
		IoXmlTestSuite.class,
		DatasetMergeTest.class,
		DatasetStatsExporterTest.class,
		UpdateCheckerTest.class
})
public class IoTestSuite {

}
