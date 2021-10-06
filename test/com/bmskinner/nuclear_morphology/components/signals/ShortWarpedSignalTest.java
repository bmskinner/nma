package com.bmskinner.nuclear_morphology.components.signals;

import static org.junit.Assert.assertEquals;

import java.io.PrintWriter;
import java.util.UUID;

import org.jdom2.Element;
import org.jdom2.output.Format;
import org.jdom2.output.XMLOutputter;
import org.junit.Before;
import org.junit.Test;

import com.bmskinner.nuclear_morphology.ComponentTester;
import com.bmskinner.nuclear_morphology.components.TestComponentFactory;
import com.bmskinner.nuclear_morphology.components.nuclei.Nucleus;

import ij.process.ByteProcessor;

public class ShortWarpedSignalTest  extends ComponentTester {
	
	private IWarpedSignal signal;	
	@Before
	public void setUp() throws Exception {
		Nucleus n = TestComponentFactory.rectangularNucleus(50, 50, 100, 100, 0, 0);
		signal = new ShortWarpedSignal(UUID.randomUUID());
		
		ByteProcessor ip = new ByteProcessor(50, 50);
		ip.set(128);
		
		signal.addWarpedImage(n, UUID.randomUUID(), "test", false, 70, false, false, ip);
	}

	@Test
	public void testXmlSerializes() throws Exception {

		Element e = signal.toXmlElement();		
		XMLOutputter xmlOutput = new XMLOutputter();
		xmlOutput.setFormat(Format.getPrettyFormat());
		xmlOutput.output(e, new PrintWriter( System.out ));
		
		IWarpedSignal test = new ShortWarpedSignal(e);
		
		WarpedSignalKey k = signal.getWarpedSignalKeys().stream().findFirst().get();

		
		testDuplicatesByField(signal, test);
		assertEquals(signal, test);
	}
}
