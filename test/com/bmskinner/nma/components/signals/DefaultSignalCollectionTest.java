package com.bmskinner.nma.components.signals;

import static org.junit.Assert.assertEquals;

import java.io.IOException;

import org.jdom2.Element;
import org.jdom2.output.Format;
import org.jdom2.output.XMLOutputter;
import org.junit.Before;
import org.junit.Test;

import com.bmskinner.nma.ComponentTester;
import com.bmskinner.nma.TestDatasetBuilder;
import com.bmskinner.nma.components.datasets.IAnalysisDataset;
import com.bmskinner.nma.components.rules.RuleSetCollection;

/**
 * Tests for the default signal collection class
 * @author bms41
 * @since 1.14.0
 *
 */
public class DefaultSignalCollectionTest extends ComponentTester {
	

	private static final int N_CELLS = 1;

	private ISignalCollection collection;	
	@Before
	public void setUp() throws Exception {
		IAnalysisDataset d = new TestDatasetBuilder(RNG_SEED).cellCount(N_CELLS)
				.ofType(RuleSetCollection.roundRuleSetCollection())
				.withMaxSizeVariation(10)
				.randomOffsetProfiles(true)
				.addSignalsInChannel(0)
				.addSignalsInChannel(1)
				.segmented().build();
		
		collection = d.getCollection().streamCells().findFirst().get().getPrimaryNucleus().getSignalCollection();
	}

	@Test
	public void testDuplicate() throws Exception {
		ISignalCollection dup = collection.duplicate();
		testDuplicatesByField("Signal collection", dup.duplicate(), dup);
	}
	
	@Test
	public void testXmlSerializes() throws IOException {

		Element e = collection.toXmlElement();		
		XMLOutputter xmlOutput = new XMLOutputter();
		xmlOutput.setFormat(Format.getPrettyFormat());

		
		ISignalCollection test = new DefaultSignalCollection(e);
		assertEquals(collection, test);
	}

}
