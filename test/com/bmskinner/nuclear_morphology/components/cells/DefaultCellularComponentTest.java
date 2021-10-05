package com.bmskinner.nuclear_morphology.components.cells;

import java.io.PrintWriter;

import org.jdom2.Element;
import org.jdom2.output.Format;
import org.jdom2.output.XMLOutputter;
import org.junit.Test;

import com.bmskinner.nuclear_morphology.components.datasets.IAnalysisDataset;
import com.bmskinner.nuclear_morphology.io.SampleDatasetReader;

public class DefaultCellularComponentTest {

	@Test
	public void testXmlSerializes() throws Exception {

		IAnalysisDataset d = SampleDatasetReader.openTestRodentDataset();
		
		ICell c = d.getCollection().stream().findFirst().orElseThrow(Exception::new);
		
		CellularComponent comp = c.getPrimaryNucleus();
		
		Element e = comp.toXmlElement();
		
		XMLOutputter xmlOutput = new XMLOutputter();
		xmlOutput.setFormat(Format.getPrettyFormat());
		xmlOutput.output(e, new PrintWriter( System.out ));

		
//		assertEquals(comp, test);
	}


}
