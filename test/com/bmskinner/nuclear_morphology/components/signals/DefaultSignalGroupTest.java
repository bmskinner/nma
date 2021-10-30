package com.bmskinner.nuclear_morphology.components.signals;

import static org.junit.Assert.assertEquals;

import java.io.PrintWriter;

import org.jdom2.Element;
import org.jdom2.output.Format;
import org.jdom2.output.XMLOutputter;
import org.junit.Test;

import com.bmskinner.nuclear_morphology.ComponentTester;
import com.bmskinner.nuclear_morphology.TestDatasetBuilder;
import com.bmskinner.nuclear_morphology.analysis.signals.shells.ShellAnalysisMethod;
import com.bmskinner.nuclear_morphology.components.datasets.IAnalysisDataset;
import com.bmskinner.nuclear_morphology.components.options.OptionsFactory;
import com.bmskinner.nuclear_morphology.components.rules.RuleSetCollection;

public class DefaultSignalGroupTest {
	
	@Test
	public void testXmlSerializes() throws Exception {
		
		IAnalysisDataset d = new TestDatasetBuilder(ComponentTester.RNG_SEED)
				.cellCount(ComponentTester.N_CELLS)
				.ofType(RuleSetCollection.roundRuleSetCollection())
				.withMaxSizeVariation(10)
				.randomOffsetProfiles(true)
				.addSignalsInChannel(0)
				.addSignalsInChannel(1)
				.segmented().build();
		
		new ShellAnalysisMethod(d, OptionsFactory.makeShellAnalysisOptions().build()).call();
		
		for(ISignalGroup s : d.getCollection().getSignalGroups()) {
			Element e = s.toXmlElement();		
			XMLOutputter xmlOutput = new XMLOutputter();
			xmlOutput.setFormat(Format.getPrettyFormat());
			xmlOutput.output(e, new PrintWriter( System.out ));
			
			ISignalGroup test = new DefaultSignalGroup(e);
			ComponentTester.testDuplicatesByField(s, test);
			assertEquals(s, test);
		}
	}

}
