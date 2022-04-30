package com.bmskinner.nma.io.xml;

import static org.junit.Assert.assertEquals;

import java.io.File;
import java.io.IOException;

import org.jdom2.Document;
import org.jdom2.Element;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;

import com.bmskinner.nma.TestDatasetBuilder;
import com.bmskinner.nma.components.datasets.IAnalysisDataset;
import com.bmskinner.nma.components.rules.RuleSetCollection;
import com.bmskinner.nma.io.XMLReader;
import com.bmskinner.nma.io.XMLWriter;

public class XMLWriterTest {

	@Rule
	public ExpectedException exception = ExpectedException.none();
	
	@Before
	public void setUp() throws Exception {
	}
	
	@Test
	public void testExceptionThrownOnNonExistantFile() throws IOException {
		Element rootElement = new Element(XMLWriterTest.class.getSimpleName());
		Document doc = new Document(rootElement);
		exception.expect(IllegalArgumentException.class);
		XMLWriter.writeXML(doc, new File("moose"));
	}
	
	@Test
	public void testDatasetCanBeWrittenAndRead() throws Exception {
		IAnalysisDataset d = new TestDatasetBuilder(123).cellCount(10)
				.ofType(RuleSetCollection.roundRuleSetCollection())
				.withMaxSizeVariation(10)
				.randomOffsetProfiles(true)
				.numberOfClusters(2)
				.segmented().build();

		exception.expect(IllegalArgumentException.class);
		
		File f =  new File("moose");
		XMLWriter.writeXML(d.toXmlElement(), f);
		
		IAnalysisDataset test = XMLReader.readDataset(f);
		
		assertEquals(d, test);
	}


}
