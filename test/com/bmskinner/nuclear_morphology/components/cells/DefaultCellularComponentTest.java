package com.bmskinner.nuclear_morphology.components.cells;

import static org.junit.Assert.assertEquals;

import org.jdom2.Element;
import org.junit.Test;

import com.bmskinner.nuclear_morphology.io.SampleDatasetReader;

public class DefaultCellularComponentTest {

	@Test
	public void testXmlSerializes() throws Exception {
		
		Nucleus n = SampleDatasetReader.openTestRodentDataset()
				.getCollection().stream()
				.findFirst().orElseThrow(Exception::new)
				.getPrimaryNucleus();
				
		Element e = n.toXmlElement();
		Nucleus test = new DefaultNucleus(e);

		assertEquals(n, test);
	}
}
