package com.bmskinner.nuclear_morphology.io;

import static org.junit.Assert.*;

import java.io.File;
import java.util.Map;
import java.util.UUID;

import org.jdom2.Document;
import org.junit.Before;
import org.junit.Test;

import com.bmskinner.nuclear_morphology.api.BasicAnalysisPipelineTest;
import com.bmskinner.nuclear_morphology.components.IAnalysisDataset;
import com.bmskinner.nuclear_morphology.components.nuclear.ISignalGroup;
import com.bmskinner.nuclear_morphology.components.nuclear.SignalGroup;
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
		IAnalysisOptions o = r.readAnalysisOptions();
		assertEquals(exp, o);
	}
	
	@Test
	public void testSignalNamesAreReadCorrectly() throws Exception {
		File xmlFile = new File("C:/Users/ben/Documents/test.xml");
		OptionsXMLReader r = new OptionsXMLReader(xmlFile);
		Map<UUID, String> signalNames = r.readSignalGroupNames();
		
		UUID redId     =  UUID.fromString("00000000-0000-0000-0000-100000000001");
        String redName = "Test red";
        
        UUID greenId =  UUID.fromString("00000000-0000-0000-0000-100000000002");
        String greenName = "Test green";
		
        assertEquals(redName, signalNames.get(redId));
        assertEquals(greenName, signalNames.get(greenId));
	}

}
