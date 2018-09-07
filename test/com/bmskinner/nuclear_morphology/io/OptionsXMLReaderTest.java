package com.bmskinner.nuclear_morphology.io;

import static org.junit.Assert.*;

import java.io.File;

import org.jdom2.Document;
import org.junit.Before;
import org.junit.Test;

import com.bmskinner.nuclear_morphology.api.BasicAnalysisPipelineTest;
import com.bmskinner.nuclear_morphology.components.IAnalysisDataset;
import com.bmskinner.nuclear_morphology.components.options.IAnalysisOptions;

/**
 * Tests for the options reader
 * @author ben
 * @since 1.14.0
 *
 */
public class OptionsXMLReaderTest {

	@Before
	public void setUp() throws Exception {
	}

	@Test
	public void testImportedOptionsMatchesExported() throws Exception {
		File xmlFile = new File("C:/Users/ben/Documents/test.xml");
		IAnalysisDataset dataset = BasicAnalysisPipelineTest.runSignalDetectionInRoundDataset();
		IAnalysisOptions exp = dataset.getAnalysisOptions().get();
		Document doc = OptionsXMLWriter.createDocument(dataset);
		OptionsXMLWriter.writeXML(doc, xmlFile);
		
		
		OptionsXMLReader r = new OptionsXMLReader(xmlFile);
		IAnalysisOptions o = r.read();
		assertEquals(exp, o);
	}

}
