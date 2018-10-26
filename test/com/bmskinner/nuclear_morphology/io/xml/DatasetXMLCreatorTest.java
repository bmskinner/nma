package com.bmskinner.nuclear_morphology.io.xml;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import java.io.File;

import org.jdom2.Document;
import org.jdom2.output.Format;
import org.jdom2.output.XMLOutputter;
import org.junit.Test;

import com.bmskinner.nuclear_morphology.TestDatasetBuilder;
import com.bmskinner.nuclear_morphology.TestResources;
import com.bmskinner.nuclear_morphology.components.IAnalysisDataset;
import com.bmskinner.nuclear_morphology.io.SampleDatasetReader;

/**
 * Test XML files are created and exported correctly 
 * @author bms41
 * @since 1.14.0
 *
 */
public class DatasetXMLCreatorTest {

	@Test 
	public void testXMLCreated() throws Exception {
		File f = new File(TestResources.ROUND_CLUSTERS_DATASET);
		IAnalysisDataset d = SampleDatasetReader.openDataset(f);
		
		DatasetXMLCreator dxc = new DatasetXMLCreator(d);
		
		Document doc = dxc.create();
		XMLOutputter xmlOutput = new XMLOutputter();
		xmlOutput.setFormat(Format.getPrettyFormat());
		
		File outputFile = new File(d.getSavePath().getParentFile(), d.getName()+".xml.nmd");
		
		if(outputFile.exists())
			outputFile.delete();
		
		assertFalse(outputFile.exists());
		XMLWriter.writeXML(doc, outputFile);
		assertTrue(outputFile.exists());
	}

}
