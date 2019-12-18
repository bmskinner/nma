package com.bmskinner.nuclear_morphology.io;

import org.junit.runner.RunWith;
import org.junit.runners.Suite;
import org.junit.runners.Suite.SuiteClasses;

import com.bmskinner.nuclear_morphology.io.conversion.IoConversionTestSuite;
import com.bmskinner.nuclear_morphology.io.xml.IoXmlTestSuite;

/**
 * Runs all test classes in the io package
 * @author ben
 * @since 1.14.0
 *
 */
@RunWith(Suite.class)
@SuiteClasses({ 
	IoConversionTestSuite.class,
	IoXmlTestSuite.class,
	DatasetMergeTest.class, 
	WorkspaceExporterTest.class,
	WorkspaceImporterTest.class,
	UpdateCheckerTest.class
})
public class IoTestSuite {

}
