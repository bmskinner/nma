package com.bmskinner.nuclear_morphology.io.xml;

import org.junit.runner.RunWith;
import org.junit.runners.Suite;
import org.junit.runners.Suite.SuiteClasses;

@RunWith(Suite.class)
@SuiteClasses({ 
	DatasetXMLCreatorTest.class, 
	DatasetXMLReaderTest.class, 
	OptionsXMLReaderTest.class,
	RuleSetCollectionXMLCreatorTest.class,
	RuleSetCollectionXMLReaderTest.class,
	XMLWriterTest.class })
public class IoXmlTestSuite {

}
