package com.bmskinner.nma.components.options;

import static org.junit.Assert.assertEquals;

import java.io.File;
import java.io.IOException;

import org.jdom2.Element;
import org.jdom2.output.Format;
import org.jdom2.output.XMLOutputter;
import org.junit.Test;

import com.bmskinner.nma.components.cells.ComponentCreationException;
import com.bmskinner.nma.components.options.DefaultAnalysisOptions;
import com.bmskinner.nma.components.options.IAnalysisOptions;
import com.bmskinner.nma.components.options.OptionsFactory;

public class DefaultAnalysisOptionsTest {

	@Test
	public void testXmlSerializes() throws IOException, ComponentCreationException {

		IAnalysisOptions o = OptionsFactory.makeDefaultRodentAnalysisOptions(new File("file"));		
		Element e = o.toXmlElement();
		
		XMLOutputter xmlOutput = new XMLOutputter();
		xmlOutput.setFormat(Format.getPrettyFormat());
		
		IAnalysisOptions test = new DefaultAnalysisOptions(e);
		
		assertEquals(o, test);
	}

}
