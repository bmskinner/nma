package com.bmskinner.nuclear_morphology.io.xml;

import static org.junit.Assert.assertEquals;

import java.io.File;
import java.util.Map;
import java.util.UUID;

import org.jdom2.Document;
import org.junit.Before;
import org.junit.Test;

import com.bmskinner.nuclear_morphology.TestDatasetBuilder;
import com.bmskinner.nuclear_morphology.components.IAnalysisDataset;
import com.bmskinner.nuclear_morphology.components.options.IAnalysisOptions;

/**
 * Tests for the options reader. Also implicitly tests the options writer
 * @author ben
 * @since 1.14.0
 *
 */
public class OptionsXMLReaderTest {
	
	private IAnalysisDataset dataset;
	private static final File XML_FILE = new File("test/samples/test.xml");

	@Before
	public void setUp() throws Exception {

		dataset = new TestDatasetBuilder(1234).cellCount(10)
				.withMaxSizeVariation(20)
				.addSignalsInChannel(0)
				.addSignalsInChannel(1)
				.numberOfClusters(2)
				.segmented().build();
		Document doc = new OptionsXMLCreator(dataset).create();
		OptionsXMLWriter.writeXML(doc, XML_FILE);
	}

	@Test
	public void testImportedOptionsMatchesExported() throws Exception {


		OptionsXMLReader r = new OptionsXMLReader(XML_FILE);
		IAnalysisOptions o = r.readAnalysisOptions();
		assertEquals(dataset.getAnalysisOptions().get(), o);
	}
	
	@Test
	public void testSignalNamesAreReadCorrectly() throws Exception {

		OptionsXMLReader r = new OptionsXMLReader(XML_FILE);
		Map<UUID, String> signalNames = r.readSignalGroupNames();
				
        assertEquals(TestDatasetBuilder.RED_SIGNAL_GROUP_NAME, signalNames.get(TestDatasetBuilder.RED_SIGNAL_GROUP));
        assertEquals(TestDatasetBuilder.GREEN_SIGNAL_GROUP_NAME, signalNames.get(TestDatasetBuilder.GREEN_SIGNAL_GROUP));
	}

}
