package com.bmskinner.nuclear_morphology.components.signals;

import static org.junit.Assert.assertEquals;

import java.io.IOException;
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

/**
 * Tests for the default nuclear signal class
 * @author bms41
 * @since 1.14.0
 *
 */
public class DefaultNuclearSignalTest extends ComponentTester {

	private static final int N_CELLS = 1;

	private INuclearSignal signal;	
	@Before
	public void setUp() throws Exception {
		IAnalysisDataset d = new TestDatasetBuilder(RNG_SEED).cellCount(N_CELLS)
				.ofType(RuleSetCollection.roundRuleSetCollection())
				.withMaxSizeVariation(10)
				.randomOffsetProfiles(true)
				.addSignalsInChannel(0)
				.segmented().build();
		
		signal = d.getCollection().streamCells().findFirst().get().getPrimaryNucleus()
				.getSignalCollection().getSignals(TestDatasetBuilder.RED_SIGNAL_GROUP).get(0);
	}
	
	@Test
	public void testDuplicate() throws Exception {
		INuclearSignal dup = signal.duplicate();
		// Don't test the original, because test components override
		// image methods, changing class signatures
		testDuplicatesByField(dup.duplicate(), dup);
	}
	
	
	@Test
	public void testXmlSerialiseas() throws IOException {

		Element e = signal.toXmlElement();
		INuclearSignal test = new DefaultNuclearSignal(e);
		
		XMLOutputter xmlOutput = new XMLOutputter();
		xmlOutput.setFormat(Format.getPrettyFormat());
		xmlOutput.output(e, new PrintWriter( System.out ));
		assertEquals(signal, test);
	}
	
}
