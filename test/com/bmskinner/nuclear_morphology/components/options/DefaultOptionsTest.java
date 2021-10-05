package com.bmskinner.nuclear_morphology.components.options;

import static org.junit.Assert.assertEquals;

import java.io.IOException;
import java.io.PrintWriter;

import org.jdom2.Element;
import org.jdom2.output.Format;
import org.jdom2.output.XMLOutputter;
import org.junit.Test;

public class DefaultOptionsTest {

	@Test
	public void testXmlSerializes() throws IOException {

		HashOptions o = OptionsFactory.makePreprocessingOptions();
		o.setSubOptions("test", OptionsFactory.makeShellAnalysisOptions());
		
		Element e = o.toXmlElement();
		
		XMLOutputter xmlOutput = new XMLOutputter();
		xmlOutput.setFormat(Format.getPrettyFormat());
		xmlOutput.output(e, new PrintWriter( System.out ));
		
		HashOptions test = new DefaultOptions(e);
		
		assertEquals(o, test);
	}

}
