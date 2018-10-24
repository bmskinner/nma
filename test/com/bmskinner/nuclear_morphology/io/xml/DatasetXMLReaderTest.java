package com.bmskinner.nuclear_morphology.io.xml;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import java.io.File;

import org.jdom2.Document;
import org.jdom2.output.Format;
import org.jdom2.output.XMLOutputter;
import org.junit.Before;
import org.junit.Test;

import com.bmskinner.nuclear_morphology.TestResources;
import com.bmskinner.nuclear_morphology.components.IAnalysisDataset;
import com.bmskinner.nuclear_morphology.io.SampleDatasetReader;

public class DatasetXMLReaderTest {

	@Before
	public void setUp() throws Exception {
	}

	@Test
	public void test() throws Exception {
		File f = new File(TestResources.ROUND_CLUSTERS_DATASET);
		IAnalysisDataset d = SampleDatasetReader.openDataset(f);
		
		File xmlFile = new File(d.getSavePath().getParentFile(), d.getName()+".serial.xml");
		DatasetXMLReader dxr = new DatasetXMLReader(xmlFile);
		IAnalysisDataset read = dxr.read();
		Document doc = dxr.readDocument();
		XMLOutputter xmlOutput = new XMLOutputter();
//		xmlOutput.setFormat(Format.getPrettyFormat());
//		xmlOutput.output(doc, System.out); 
		
		assertEquals(d.getName(), read.getName());
		assertEquals(d.getDatasetColour(), read.getDatasetColour());
		assertEquals(d.getCollection(), read.getCollection());
		
		assertEquals(d, read);
	}

}
