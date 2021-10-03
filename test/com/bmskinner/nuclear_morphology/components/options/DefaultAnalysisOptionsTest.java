package com.bmskinner.nuclear_morphology.components.options;

import static org.junit.Assert.assertEquals;

import java.io.File;
import java.io.IOException;
import java.io.PrintWriter;

import org.jdom2.Element;
import org.jdom2.output.Format;
import org.jdom2.output.XMLOutputter;
import org.junit.Test;

public class DefaultAnalysisOptionsTest {

	@Test
	public void testXmlSerializes() throws IOException {

		IAnalysisOptions o = OptionsFactory.makeDefaultRodentAnalysisOptions(new File("file"));		
		Element e = o.toXmlElement();
		
		XMLOutputter xmlOutput = new XMLOutputter();
		xmlOutput.setFormat(Format.getPrettyFormat());
		xmlOutput.output(e, new PrintWriter( System.out ));
		
		IAnalysisOptions test = new DefaultAnalysisOptions(e);
		
		assertEquals(o, test);
	}

}
