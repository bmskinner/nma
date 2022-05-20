package com.bmskinner.nma.components.cells;

import static org.junit.Assert.assertEquals;

import org.jdom2.Element;
import org.junit.Test;

import com.bmskinner.nma.components.cells.DefaultNucleus;
import com.bmskinner.nma.components.cells.Nucleus;
import com.bmskinner.nma.io.SampleDatasetReader;

public class DefaultCellularComponentTest {

	@Test
	public void testXmlSerializes() throws Exception {
		
		Nucleus n = SampleDatasetReader.openTestMouseDataset()
				.getCollection().stream()
				.findFirst().orElseThrow(Exception::new)
				.getPrimaryNucleus();
				
		Element e = n.toXmlElement();
		Nucleus test = new DefaultNucleus(e);

		assertEquals(n, test);
	}
}
