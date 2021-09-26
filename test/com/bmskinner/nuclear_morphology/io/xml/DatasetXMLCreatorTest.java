package com.bmskinner.nuclear_morphology.io.xml;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import java.io.File;
import java.io.IOException;

import org.jdom2.Document;
import org.jdom2.output.Format;
import org.jdom2.output.XMLOutputter;
import org.junit.Test;

import com.bmskinner.nuclear_morphology.TestResources;
import com.bmskinner.nuclear_morphology.components.datasets.IAnalysisDataset;
import com.bmskinner.nuclear_morphology.io.SampleDatasetReader;

/**
 * Test XML files are created and exported correctly 
 * @author bms41
 * @since 1.14.0
 *
 */
public class DatasetXMLCreatorTest {
	
	private void testXMLCreated(IAnalysisDataset d) throws IOException {
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
	
	@Test 
	public void testXMLCreatedForMouse() throws Exception {
		File f = new File(TestResources.MOUSE_TEST_DATASET);
		IAnalysisDataset d = SampleDatasetReader.openDataset(f);
		testXMLCreated(d);
	}

	@Test 
	public void testXMLCreatedForMouseWithClusters() throws Exception {
		File f = new File(TestResources.MOUSE_CLUSTERS_DATASET);
		IAnalysisDataset d = SampleDatasetReader.openDataset(f);
		testXMLCreated(d);
	}
	
	@Test 
	public void testXMLCreatedForMouseWithSignals() throws Exception {
		File f = new File(TestResources.MOUSE_SIGNALS_DATASET);
		IAnalysisDataset d = SampleDatasetReader.openDataset(f);
		testXMLCreated(d);
	}
	
	@Test 
	public void testXMLCreatedForPig() throws Exception {
		File f = new File(TestResources.PIG_TEST_DATASET);
		IAnalysisDataset d = SampleDatasetReader.openDataset(f);
		testXMLCreated(d);
	}

	@Test 
	public void testXMLCreatedForPigWithClusters() throws Exception {
		File f = new File(TestResources.PIG_CLUSTERS_DATASET);
		IAnalysisDataset d = SampleDatasetReader.openDataset(f);
		testXMLCreated(d);
	}
	
	@Test 
	public void testXMLCreatedForPigWithSignals() throws Exception {
		File f = new File(TestResources.PIG_SIGNALS_DATASET);
		IAnalysisDataset d = SampleDatasetReader.openDataset(f);
		testXMLCreated(d);
	}
	
	@Test 
	public void testXMLCreatedForRound() throws Exception {
		File f = new File(TestResources.ROUND_TEST_DATASET);
		IAnalysisDataset d = SampleDatasetReader.openDataset(f);
		testXMLCreated(d);
	}

	@Test 
	public void testXMLCreatedForRoundWithClusters() throws Exception {
		File f = new File(TestResources.ROUND_CLUSTERS_DATASET);
		IAnalysisDataset d = SampleDatasetReader.openDataset(f);
		testXMLCreated(d);
	}
	
	@Test 
	public void testXMLCreatedForRoundWithSignals() throws Exception {
		File f = new File(TestResources.ROUND_SIGNALS_DATASET);
		IAnalysisDataset d = SampleDatasetReader.openDataset(f);
		testXMLCreated(d);
	}
	
	

}
