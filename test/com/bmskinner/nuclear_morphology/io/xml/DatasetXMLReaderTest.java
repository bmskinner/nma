package com.bmskinner.nuclear_morphology.io.xml;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import java.io.File;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import org.jdom2.Document;
import org.jdom2.output.Format;
import org.jdom2.output.XMLOutputter;
import org.junit.Before;
import org.junit.Test;

import com.bmskinner.nuclear_morphology.ComponentTester;
import com.bmskinner.nuclear_morphology.TestResources;
import com.bmskinner.nuclear_morphology.components.IAnalysisDataset;
import com.bmskinner.nuclear_morphology.components.ICell;
import com.bmskinner.nuclear_morphology.components.nuclei.Nucleus;
import com.bmskinner.nuclear_morphology.io.SampleDatasetReader;

public class DatasetXMLReaderTest extends ComponentTester {

	@Before
	public void setUp() throws Exception {
	}

	@Test
	public void test() throws Exception {
		File f = new File(TestResources.ROUND_CLUSTERS_DATASET).getAbsoluteFile();
		IAnalysisDataset d = SampleDatasetReader.openDataset(f);
		
		DatasetXMLCreator dxc = new DatasetXMLCreator(d);
		File xmlFile = new File(d.getSavePath().getParentFile(), d.getName()+".xml.nmd");
		XMLWriter.writeXML(dxc.create(), xmlFile);

		DatasetXMLReader dxr = new DatasetXMLReader(xmlFile);
		IAnalysisDataset read = dxr.read();
//		Document doc = dxr.readDocument();
//		XMLOutputter xmlOutput = new XMLOutputter();
		
		assertEquals(d.getName(), read.getName());
		assertEquals(d.getId(), read.getId());
		assertEquals(d.getDatasetColour(), read.getDatasetColour());
		
		for(UUID cellId : d.getCollection().getCellIDs()) {
			ICell wroteCell = d.getCollection().getCell(cellId);
			ICell readCell  = read.getCollection().getCell(cellId);
			
			Nucleus wroteNucleus = wroteCell.getNucleus();
			Nucleus readNucleus = readCell.getNucleus();
			List<String> skip = new ArrayList<>();
			skip.add("profileMap");
			testDuplicatesByField(wroteNucleus, readNucleus, skip);
		}
		
		assertEquals(d.getCollection(), read.getCollection());
		
		assertEquals(d, read);
	}

}
