package com.bmskinner.nuclear_morphology.io;

import java.io.File;
import java.io.IOException;

import org.jdom2.Document;
import org.junit.Test;

import com.bmskinner.nuclear_morphology.api.BasicAnalysisPipelineTest;
import com.bmskinner.nuclear_morphology.components.IAnalysisDataset;
import com.bmskinner.nuclear_morphology.components.options.IAnalysisOptions;
import com.bmskinner.nuclear_morphology.components.options.IDetectionOptions;
import com.bmskinner.nuclear_morphology.components.options.OptionsFactory;

/**
 * Tests for the XML writer
 * @author ben
 * @since 1.14.0
 *
 */
public class OptionsXMLWriterTest {

	@Test
	public void testNucleusOnlyDataset() throws Exception {
		IAnalysisDataset dataset = SampleDatasetReader.openTestRodentDataset();
		Document doc = OptionsXMLWriter.createDocument(dataset);
		OptionsXMLWriter.writeXML(doc, new File("C:/Users/ben/Documents/test.xml"));
	}
	
	@Test
	public void testDatasetWithSignals() throws Exception {
		IAnalysisDataset dataset = BasicAnalysisPipelineTest.runSignalDetectionInRoundDataset();
		Document doc = OptionsXMLWriter.createDocument(dataset);
		OptionsXMLWriter.writeXML(doc, new File("C:/Users/ben/Documents/test.xml"));
	}
	
	@Test
	public void testDetectionOptions() throws IOException {
		File folder = new File("C:/Users/ben/Documents/test.xml");
		IDetectionOptions op = OptionsFactory.makeNucleusDetectionOptions(folder);
		Document doc = OptionsXMLWriter.createDocument(op);
		OptionsXMLWriter.writeXML(doc, new File("C:/Users/ben/Documents/test.xml"));
	}
	
	@Test
	public void testAnalysisOptions() throws IOException {
		File folder = new File("C:/Users/ben/Documents/test.xml");
		IAnalysisOptions op = OptionsFactory.makeDefaultRodentAnalysisOptions(folder);
		Document doc = OptionsXMLWriter.createDocument(op);
		OptionsXMLWriter.writeXML(doc, new File("C:/Users/ben/Documents/test.xml"));
	}

}
