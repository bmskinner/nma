package com.bmskinner.nuclear_morphology.io.xml;

import static org.junit.Assert.assertEquals;

import java.io.File;
import java.io.IOException;

import org.junit.Test;

import com.bmskinner.nuclear_morphology.TestDatasetBuilder;
import com.bmskinner.nuclear_morphology.TestResources;
import com.bmskinner.nuclear_morphology.components.cells.ComponentCreationException;
import com.bmskinner.nuclear_morphology.components.datasets.IAnalysisDataset;
import com.bmskinner.nuclear_morphology.components.options.HashOptions;
import com.bmskinner.nuclear_morphology.components.options.IAnalysisOptions;
import com.bmskinner.nuclear_morphology.components.options.OptionsFactory;
import com.bmskinner.nuclear_morphology.components.rules.RuleSetCollection;
import com.bmskinner.nuclear_morphology.io.XMLReader;
import com.bmskinner.nuclear_morphology.io.XMLWriter;
import com.bmskinner.nuclear_morphology.io.XMLReader.XMLReadingException;

public class XMLReaderTest {

	@Test
	public void testReadRuleSets() throws IOException, XMLReadingException, ComponentCreationException {
		RuleSetCollection r = RuleSetCollection.mouseSpermRuleSetCollection();
		File f = new File(TestResources.UNIT_TEST_FOLDER, "Ruleset.xml");
		
		XMLWriter.writeXML(r.toXmlElement(), f);
		
		RuleSetCollection test = XMLReader.readRulesetCollection(f);
		assertEquals(r, test);
	}
	
	@Test
	public void testReadOptions() throws IOException, XMLReadingException {
		HashOptions o = OptionsFactory.makeDefaultClusteringOptions().build();
		File f = new File(TestResources.UNIT_TEST_FOLDER, "Options.xml");
		
		XMLWriter.writeXML(o.toXmlElement(), f);
		
		HashOptions test = XMLReader.readOptions(f);
		assertEquals(o, test);
	}
	
	@Test
	public void testReadAnalysisOptions() throws IOException, XMLReadingException, ComponentCreationException {
		IAnalysisOptions o = OptionsFactory.makeDefaultRodentAnalysisOptions(TestResources.UNIT_TEST_FOLDER);
		File f = new File(TestResources.UNIT_TEST_FOLDER, "AnalysisOptions.xml");
		
		XMLWriter.writeXML(o.toXmlElement(), f);
		
		IAnalysisOptions test = XMLReader.readAnalysisOptions(f);
		assertEquals(o, test);
	}
	
	@Test
	public void testReadAnalysisDataset() throws Exception {
		IAnalysisDataset d = new TestDatasetBuilder(123).cellCount(20)
				.ofType(RuleSetCollection.roundRuleSetCollection())
				.withMaxSizeVariation(10)
				.randomOffsetProfiles(true)
				.numberOfClusters(2)
				.segmented().build();
		File f = new File(TestResources.UNIT_TEST_FOLDER, "AnalysisDataset.nmd");
		
		XMLWriter.writeXML(d.toXmlElement(), f);
		
		IAnalysisDataset test = XMLReader.readDataset(f);
		assertEquals(d, test);
	}
	
	@Test
	public void testReadAnalysisDatasetWithSignals() throws Exception {
		IAnalysisDataset d = new TestDatasetBuilder(123).cellCount(20)
				.ofType(RuleSetCollection.roundRuleSetCollection())
				.withMaxSizeVariation(10)
				.randomOffsetProfiles(true)
				.addSignalsInChannel(0)
				.addSignalsInChannel(1)
				.numberOfClusters(2)
				.segmented().build();
		File f = new File(TestResources.UNIT_TEST_FOLDER, "AnalysisDatasetSignals.nmd");
		
		XMLWriter.writeXML(d.toXmlElement(), f);
		
		IAnalysisDataset test = XMLReader.readDataset(f);
		assertEquals(d, test);
	}
}
