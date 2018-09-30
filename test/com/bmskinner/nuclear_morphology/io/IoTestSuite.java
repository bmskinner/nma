package com.bmskinner.nuclear_morphology.io;

import org.junit.runner.RunWith;
import org.junit.runners.Suite;
import org.junit.runners.Suite.SuiteClasses;

import com.bmskinner.nuclear_morphology.io.conversion.IoConversionTestSuite;

@RunWith(Suite.class)
@SuiteClasses({ 
	IoConversionTestSuite.class,
	DatasetMergeTest.class, 
	OptionsXMLReaderTest.class,
	WorkspaceExporterTest.class,
	WorkspaceImporterTest.class, 
	XMLWriterTest.class })
public class IoTestSuite {

}
