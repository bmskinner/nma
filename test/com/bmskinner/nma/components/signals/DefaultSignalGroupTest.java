package com.bmskinner.nma.components.signals;

import static org.junit.Assert.assertEquals;

import org.jdom2.Element;
import org.junit.Test;

import com.bmskinner.nma.ComponentTester;
import com.bmskinner.nma.TestResources;
import com.bmskinner.nma.analysis.signals.shells.ShellAnalysisMethod;
import com.bmskinner.nma.components.datasets.IAnalysisDataset;
import com.bmskinner.nma.components.options.OptionsFactory;
import com.bmskinner.nma.io.SampleDatasetReader;

public class DefaultSignalGroupTest {

	@Test
	public void testXmlSerializes() throws Exception {

		IAnalysisDataset d = SampleDatasetReader.openDataset(TestResources.ROUND_SIGNALS_DATASET);

		new ShellAnalysisMethod(d, OptionsFactory.makeShellAnalysisOptions().build()).call();

		for (ISignalGroup s : d.getCollection().getSignalGroups()) {
			Element e = s.toXmlElement();

			ISignalGroup test = new DefaultSignalGroup(e);
			ComponentTester.testDuplicatesByField("Signal group", s, test);
			assertEquals(s, test);
		}
	}

}
