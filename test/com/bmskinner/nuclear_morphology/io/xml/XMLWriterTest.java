package com.bmskinner.nuclear_morphology.io.xml;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import java.io.File;
import java.io.IOException;
import java.util.UUID;

import org.jdom2.Document;
import org.jdom2.Element;
import org.jdom2.output.Format;
import org.jdom2.output.XMLOutputter;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;

import com.bmskinner.nuclear_morphology.TestDatasetBuilder;
import com.bmskinner.nuclear_morphology.components.IAnalysisDataset;
import com.bmskinner.nuclear_morphology.io.xml.XMLCreator;
import com.bmskinner.nuclear_morphology.io.xml.XMLWriter;

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
	public void testUUIDDetected() {
		UUID id = UUID.randomUUID();
		assertTrue(XMLCreator.isUUID(id.toString()));
		assertFalse(XMLCreator.isUUID(null));
		assertFalse(XMLCreator.isUUID("This is not a UUID"));
		assertFalse(XMLCreator.isUUID("00001111-2222-333-44444-555566667777")); // too few in 3
	}
	
//	@Test
//	public void testDatasetXmlCreated() throws Exception {
//		IAnalysisDataset d = new TestDatasetBuilder(1234).cellCount(4).segmented().build();
//		Document doc = new DatasetXMLCreator(d).create();
//		XMLOutputter xmlOutput = new XMLOutputter();
//		xmlOutput.setFormat(Format.getPrettyFormat());
//		xmlOutput.output(doc, System.out); 
//		fail("Not yet implemented");
//	}

}
