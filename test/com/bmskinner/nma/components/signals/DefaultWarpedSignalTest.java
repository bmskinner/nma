package com.bmskinner.nma.components.signals;

import static org.junit.Assert.assertEquals;

import java.awt.Color;
import java.util.UUID;

import org.jdom2.Element;
import org.junit.Before;
import org.junit.Test;

import com.bmskinner.nma.ComponentTester;
import com.bmskinner.nma.components.TestComponentFactory;
import com.bmskinner.nma.components.cells.Nucleus;
import com.bmskinner.nma.components.rules.RuleSetCollection;
import com.bmskinner.nma.components.signals.DefaultWarpedSignal;
import com.bmskinner.nma.components.signals.IWarpedSignal;

import ij.process.ByteProcessor;

public class DefaultWarpedSignalTest extends ComponentTester {

	private IWarpedSignal signal;

	@Before
	public void setUp() throws Exception {
		Nucleus n = TestComponentFactory.rectangularNucleus(50, 50, 100, 100, 0, 0,
				RuleSetCollection.roundRuleSetCollection());

		ByteProcessor ip = new ByteProcessor(50, 50);
		ip.set(128);
		signal = new DefaultWarpedSignal(n, "test", "test", "test", UUID.randomUUID(), false, 70, false, false,
				IWarpedSignal.toArray(ip), ip.getWidth(), Color.RED, 255);
	}

	@Test
	public void testXmlSerializes() throws Exception {

		Element e = signal.toXmlElement();
		IWarpedSignal test = new DefaultWarpedSignal(e);

		testDuplicatesByField("Warped signal", signal, test);
		assertEquals(signal, test);
	}
}
