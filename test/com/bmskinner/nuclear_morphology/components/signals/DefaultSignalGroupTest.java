package com.bmskinner.nuclear_morphology.components.signals;

import static org.junit.Assert.assertEquals;

import java.io.PrintWriter;

import org.jdom2.Element;
import org.jdom2.output.Format;
import org.jdom2.output.XMLOutputter;
import org.junit.Before;
import org.junit.Test;

import com.bmskinner.nuclear_morphology.ComponentTester;
import com.bmskinner.nuclear_morphology.TestDatasetBuilder;
import com.bmskinner.nuclear_morphology.components.datasets.IAnalysisDataset;
import com.bmskinner.nuclear_morphology.components.rules.RuleSetCollection;

public class DefaultSignalGroupTest extends ComponentTester {
	
	private ISignalGroup collection;	
	@Before
	public void setUp() throws Exception {
		IAnalysisDataset d = new TestDatasetBuilder(RNG_SEED).cellCount(N_CELLS)
				.ofType(RuleSetCollection.roundRuleSetCollection())
				.withMaxSizeVariation(10)
				.randomOffsetProfiles(true)
				.addSignalsInChannel(0)
				.addSignalsInChannel(1)
				.segmented().build();
		
		collection = d.getCollection().getSignalGroup(TestDatasetBuilder.RED_SIGNAL_GROUP).get();
	}

	@Test
	public void testXmlSerializes() throws Exception {

		Element e = collection.toXmlElement();		
		XMLOutputter xmlOutput = new XMLOutputter();
		xmlOutput.setFormat(Format.getPrettyFormat());
		xmlOutput.output(e, new PrintWriter( System.out ));
		
		ISignalGroup test = new DefaultSignalGroup(e);
		testDuplicatesByField(collection, test);
		assertEquals(collection, test);
	}

}
