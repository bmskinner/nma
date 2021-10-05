package com.bmskinner.nuclear_morphology.components.nuclei;

import static org.junit.Assert.assertEquals;

import java.io.PrintWriter;

import org.jdom2.Element;
import org.jdom2.output.Format;
import org.jdom2.output.XMLOutputter;
import org.junit.Before;
import org.junit.Test;

import com.bmskinner.nuclear_morphology.ComponentTester;
import com.bmskinner.nuclear_morphology.components.TestComponentFactory;

/**
 * Tests for the default nucleus class
 * @author bms41
 * @since 1.14.0
 *
 */
public class DefaultNucleusTest extends ComponentTester {

	private Nucleus nucleus;	
	@Override
	@Before
	public void setUp() throws Exception {
		nucleus = TestComponentFactory.rectangularNucleus(100, 100, 20, 20, 0, 20);
	}

	@Test
	public void testDuplicate() throws Exception {
		Nucleus dup = nucleus.duplicate();
		testDuplicatesByField(dup.duplicate(), dup);
	}
	
	@Test
	public void testXmlSerializes() throws Exception {
		
		Element e = nucleus.toXmlElement();
		
		XMLOutputter xmlOutput = new XMLOutputter();
		xmlOutput.setFormat(Format.getPrettyFormat());
		xmlOutput.output(e, new PrintWriter( System.out ));

		Nucleus test = new DefaultNucleus(e);
		
		assertEquals(nucleus, test);
	}
	
}
