package com.bmskinner.nma.components.signals;

import static org.junit.Assert.assertEquals;

import org.jdom2.Element;
import org.jdom2.output.Format;
import org.jdom2.output.XMLOutputter;
import org.junit.Test;

import com.bmskinner.nma.ComponentTester;
import com.bmskinner.nma.TestDatasetBuilder;
import com.bmskinner.nma.analysis.signals.shells.ShellAnalysisMethod;
import com.bmskinner.nma.components.datasets.IAnalysisDataset;
import com.bmskinner.nma.components.options.OptionsFactory;
import com.bmskinner.nma.components.rules.RuleSetCollection;
import com.bmskinner.nma.components.signals.DefaultSignalGroup;
import com.bmskinner.nma.components.signals.ISignalGroup;

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
			
			ISignalGroup test = new DefaultSignalGroup(e);
			ComponentTester.testDuplicatesByField("Signal group", s, test);
			assertEquals(s, test);
		}
	}

}
