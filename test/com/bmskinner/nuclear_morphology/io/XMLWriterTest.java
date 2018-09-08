package com.bmskinner.nuclear_morphology.io;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import java.io.File;
import java.io.IOException;
import java.util.UUID;

import org.jdom2.Document;
import org.jdom2.Element;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;

public class XMLWriterTest {

	@Rule
	public ExpectedException exception = ExpectedException.none();
	
	@Before
	public void setUp() throws Exception {
	}
	
	@Test
	public void testExceptionThrownOnNonExistantDocument() throws IOException {
		exception.expect(IllegalArgumentException.class);
		XMLWriter.writeXML(null, new File(""));
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
		assertTrue(XMLWriter.isUUID(id.toString()));
		assertFalse(XMLWriter.isUUID(null));
		assertFalse(XMLWriter.isUUID("This is not a UUID"));
		assertFalse(XMLWriter.isUUID("00001111-2222-333-44444-555566667777")); // too few in 3
	}

}
