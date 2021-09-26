package com.bmskinner.nuclear_morphology.io.xml;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import java.io.File;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import org.junit.Ignore;
import org.junit.Test;

import com.bmskinner.nuclear_morphology.ComponentTester;
import com.bmskinner.nuclear_morphology.TestResources;
import com.bmskinner.nuclear_morphology.analysis.DatasetValidator;
import com.bmskinner.nuclear_morphology.components.cells.ICell;
import com.bmskinner.nuclear_morphology.components.datasets.IAnalysisDataset;
import com.bmskinner.nuclear_morphology.components.nuclei.Nucleus;
import com.bmskinner.nuclear_morphology.io.SampleDatasetReader;

/**
 * Test that XML files can be read and deserialised correctly
 * @author bms41
 * @since 1.14.0
 *
 */
@Ignore // the XML format is not yet finalised
public class DatasetXMLReaderTest extends ComponentTester {

	private void testXMLRead(File f) throws Exception {
		LOGGER.fine("Opening serialised template dataset");
		IAnalysisDataset d = SampleDatasetReader.openDataset(f.getAbsoluteFile());
		
		// Create the XML file from the serialised file
		DatasetXMLCreator dxc = new DatasetXMLCreator(d);
		File xmlFile = new File(makeXmlFileName(d.getSavePath().getAbsolutePath()));
		XMLWriter.writeXML(dxc.create(), xmlFile);

		// Read the XML file back in and check it's the same
		LOGGER.fine("Opening XML dataset");
		IAnalysisDataset read =  SampleDatasetReader.openXMLDataset(xmlFile);
		
		assertEquals(d.getName(), read.getName());
		assertEquals(d.getId(), read.getId());
		assertEquals(d.getDatasetColour(), read.getDatasetColour());
		assertEquals("Cell count", d.getCollection().size(), read.getCollection().size());
		
		for(UUID cellId : d.getCollection().getCellIDs()) {
			ICell wroteCell = d.getCollection().getCell(cellId);
			ICell readCell  = read.getCollection().getCell(cellId);
			
			Nucleus wroteNucleus = wroteCell.getPrimaryNucleus();
			Nucleus readNucleus = readCell.getPrimaryNucleus();
			List<String> skip = new ArrayList<>();
			skip.add("profileMap");
			skip.add("signalCollection");
//			testDuplicatesByField(wroteNucleus, readNucleus, skip); //TODO
		}
		
		for(IAnalysisDataset child : d.getAllChildDatasets()) {
			UUID childId = child.getId();
			assertTrue(read.hasDirectChild(childId));
			assertEquals("Child cell count", child.getCollection().size(), read.getChildDataset(childId).getCollection().size());
			for(ICell c : child.getCollection())
				assertTrue(read.getChildDataset(childId).getCollection().contains(c));
				
		}
		
		DatasetValidator dv = new DatasetValidator();
		boolean isValid = dv.validate(read);
		assertTrue(isValid);
		
//		TODO: Functionality is not fully enabled
//		assertEquals(d.getCollection(), read.getCollection());
//		
//		assertEquals(d, read);
	}
	
	/**
	 * Make the xml version of test dataset file names
	 * @param fileName the nae to convert e.g. TestResources.MOUSE_TEST_DATASET
	 * @return the file name of the xml nmd
	 */
	private static String makeXmlFileName(String fileName) {
		return fileName.replace(".nmd", ".xml.nmd");
	}
	
	@Test 
	public void testXMLReadForMouse() throws Exception {
		File f = new File(TestResources.MOUSE_TEST_DATASET);
		testXMLRead(f);
	}
	
	@Test 
	public void testXMLReadForMouseWithClusters() throws Exception {
		File f = new File(TestResources.MOUSE_CLUSTERS_DATASET);
		testXMLRead(f);
	}
	
	@Test 
	public void testXMLReadForMouseWithSignals() throws Exception {
		File f = new File(TestResources.MOUSE_SIGNALS_DATASET);
		testXMLRead(f);
	}
	
	@Test 
	public void testXMLReadForPig() throws Exception {
		File f = new File(TestResources.PIG_TEST_DATASET);
		testXMLRead(f);
	}
	
	@Test 
	public void testXMLReadForPigWithClusters() throws Exception {
		File f = new File(TestResources.PIG_CLUSTERS_DATASET);
		testXMLRead(f);
	}
	
	@Test 
	public void testXMLReadForPigWithSignals() throws Exception {
		File f = new File(TestResources.PIG_SIGNALS_DATASET);
		testXMLRead(f);
	}
	
	@Test 
	public void testXMLReadForRound() throws Exception {
		File f = new File(TestResources.ROUND_TEST_DATASET);
		testXMLRead(f);
	}
	
	@Test 
	public void testXMLReadForRoundWithClusters() throws Exception {
		File f = new File(TestResources.ROUND_CLUSTERS_DATASET);
		testXMLRead(f);
	}
	
	@Test 
	public void testXMLReadForRoundWithSignals() throws Exception {
		File f = new File(TestResources.ROUND_SIGNALS_DATASET);
		testXMLRead(f);
	}
}
