package com.bmskinner.nuclear_morphology.components.options;

import static org.junit.Assert.assertEquals;

import java.io.IOException;

import org.jdom2.Element;
import org.jdom2.output.Format;
import org.jdom2.output.XMLOutputter;
import org.junit.Test;

public class DefaultOptionsTest {

	@Test
	public void testXmlSerializes() throws IOException {

		HashOptions o = OptionsFactory.makePreprocessingOptions().build();
		o.setSubOptions("test", OptionsFactory.makeShellAnalysisOptions().build());
		
		Element e = o.toXmlElement();
		
		XMLOutputter xmlOutput = new XMLOutputter();
		xmlOutput.setFormat(Format.getPrettyFormat());		
		HashOptions test = new DefaultOptions(e);
		
		assertEquals(o, test);
	}

}
