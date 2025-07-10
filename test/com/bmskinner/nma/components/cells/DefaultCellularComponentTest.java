package com.bmskinner.nma.components.cells;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import java.util.Arrays;

import org.jdom2.Element;
import org.junit.Test;

import com.bmskinner.nma.ComponentTester;
import com.bmskinner.nma.components.XMLNames;
import com.bmskinner.nma.io.SampleDatasetReader;
import com.bmskinner.nma.samples.dummy.DummyRodentSpermNucleus;

public class DefaultCellularComponentTest {

	@Test
	public void testXmlSerializes() throws Exception {
		
		final Nucleus n = SampleDatasetReader.openTestMouseDataset()
				.getCollection().stream()
				.findFirst().orElseThrow(Exception::new)
				.getPrimaryNucleus();
				
		final Element e = n.toXmlElement();
		final Nucleus test = new DefaultNucleus(e);

		assertEquals(n, test);
	}

	/**
	 * In 2.3.0 we want to replace the int points in object outlines with the full
	 * border points. This may change how some values are calculated.
	 */
	@Test
	public void testBorderPointSerializationMatchesIntPointSerialization() throws Exception {
		// Make a marahalled version of the nucleus with and without the new serialized
		// border list
		final DummyRodentSpermNucleus template220 = new DummyRodentSpermNucleus();
		final DummyRodentSpermNucleus template230 = new DummyRodentSpermNucleus();

		final Element e220 = template220.toXmlElement();
		final Element e230 = template230.toXmlElement();

		// With no direct border, we will be forced to create it from the int points
		e220.removeChildren(XMLNames.XML_XBORDER);
		e220.removeChildren(XMLNames.XML_YBORDER);
		
		
		final Nucleus n220 = new DefaultNucleus(e220);
		final Nucleus n230 = new DefaultNucleus(e230);

		assertTrue("Int x coordinates should be the same", Arrays.equals(n220.xpoints(), n230.xpoints()));
		assertTrue("Int y coordinates should be the same", Arrays.equals(n220.ypoints(), n230.ypoints()));

		System.out.println("2.2.0\n" + n220.toString());
		System.out.println("2.3.0\n" + n230.toString());

		// Both have the same int values
		// The border list is constructed from these, then serialised in 2.3.0

		assertEquals("Original base coordinates should be the same", n220.getOriginalBase(), n230.getOriginalBase());
		assertEquals("Base coordinates should be the same", n220.getBase(), n230.getBase());

		System.out.println(n220.getOriginalBorderList());
		System.out.println(n230.getOriginalBorderList());

		ComponentTester.testDuplicatesByField("Nuclei should be equal", n220.xpoints(),
				n230.xpoints());
		// Note that this check may require adding VM options to the run configuration:
		// --add-opens java.base/java.util=ALL-UNNAMED
		ComponentTester.testDuplicatesByField("Nuclei should be equal", n220.getOriginalBorderList(),
				n230.getOriginalBorderList());
		
		ComponentTester.testDuplicatesByField("Nuclei should be equal", n220, n230);
	}
}
