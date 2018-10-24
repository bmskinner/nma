package com.bmskinner.nuclear_morphology.io.xml;

import java.io.File;

import org.jdom2.Document;
import org.jdom2.output.Format;
import org.jdom2.output.XMLOutputter;
import org.junit.Test;

import com.bmskinner.nuclear_morphology.TestDatasetBuilder;
import com.bmskinner.nuclear_morphology.TestResources;
import com.bmskinner.nuclear_morphology.components.IAnalysisDataset;
import com.bmskinner.nuclear_morphology.io.SampleDatasetReader;

public class DatasetXMLCreatorTest {
	
	
	
	@Test 
	public void testXMLCreated() throws Exception {
		File f = new File(TestResources.ROUND_CLUSTERS_DATASET);
		IAnalysisDataset d = SampleDatasetReader.openDataset(f);
		
		DatasetXMLCreator dxc = new DatasetXMLCreator(d);
		
		Document doc = dxc.create();
		XMLOutputter xmlOutput = new XMLOutputter();
		xmlOutput.setFormat(Format.getPrettyFormat());
		xmlOutput.output(doc, System.out); 
		
		File outputFile = new File(d.getSavePath().getParentFile(), d.getName()+".serial.xml");
		XMLWriter.writeXML(doc, outputFile);
	}

}
