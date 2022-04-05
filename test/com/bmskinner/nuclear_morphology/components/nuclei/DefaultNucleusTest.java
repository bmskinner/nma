package com.bmskinner.nuclear_morphology.components.nuclei;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import java.awt.Shape;
import java.awt.geom.Path2D;
import java.awt.geom.PathIterator;
import java.io.File;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import org.jdom2.Element;
import org.jdom2.output.Format;
import org.jdom2.output.XMLOutputter;
import org.junit.Before;
import org.junit.Test;

import com.bmskinner.nuclear_morphology.ComponentTester;
import com.bmskinner.nuclear_morphology.TestDatasetBuilder;
import com.bmskinner.nuclear_morphology.TestImageDatasetCreator;
import com.bmskinner.nuclear_morphology.TestResources;
import com.bmskinner.nuclear_morphology.analysis.ComponentMeasurer;
import com.bmskinner.nuclear_morphology.components.datasets.IAnalysisDataset;
import com.bmskinner.nuclear_morphology.components.measure.Measurement;
import com.bmskinner.nuclear_morphology.components.options.IAnalysisOptions;
import com.bmskinner.nuclear_morphology.components.options.OptionsFactory;
import com.bmskinner.nuclear_morphology.components.rules.RuleSetCollection;

/**
 * Tests for the default nucleus class
 * @author bms41
 * @since 1.14.0
 *
 */
public class DefaultNucleusTest {

	private Nucleus nucleus;	

	@Before
	public void setUp() throws Exception {
		IAnalysisDataset d = new TestDatasetBuilder(ComponentTester.RNG_SEED)
				.cellCount(ComponentTester.N_CELLS)
				.ofType(RuleSetCollection.roundRuleSetCollection())
				.withMaxSizeVariation(1)
				.randomOffsetProfiles(true)
				.numberOfClusters(ComponentTester.N_CHILD_DATASETS)
				.segmented().build();
		nucleus = d.getCollection().getCells().stream().findFirst().get().getPrimaryNucleus();
	}

	@Test
	public void testDuplicate() throws Exception {
		Nucleus dup = nucleus.duplicate();
		ComponentTester.testDuplicatesByField(nucleus.getNameAndNumber(), nucleus, dup);
	}
	
	/**
	 * Check that duplicating a component multiple times does
	 * not introduce errors
	 * @throws Exception
	 */
	@Test
	public void testDuplicateIsStableOverRepeatedIterations() throws Exception {
		Nucleus dup = nucleus.duplicate();
		for(int i=0; i<20; i++) {
			Nucleus dup2 = dup.duplicate();
			ComponentTester.testDuplicatesByField(dup.getNameAndNumber(), dup, dup2);
			dup = dup2;
		}		
	}
	
	@Test
	public void testMeasurementsAreStable() {
		Nucleus dup = nucleus.duplicate();	
		for(Measurement m : nucleus.getMeasurements()) {
			double a1 = ComponentMeasurer.calculate(m, nucleus);
			double a2 = ComponentMeasurer.calculate(m, dup);
			double m1 = nucleus.getMeasurement(m);
			double m2 = dup.getMeasurement(m);
			assertEquals(m+" should be stable in original", a1, m1, 0.0000000000001);
			assertEquals(m+" should be stable in duplicate", a2, m2, 0.0000000000001);
			assertEquals(m+" should not change", a1, a2, 0.0000000000001);
			assertEquals(m+" should not change", m1, m2, 0.0000000000001);	
		}
	}
	
	/**
	 * Test serialisation works for test nuclei
	 * @throws Exception
	 */
	@Test
	public void testXmlSerializes() throws Exception {
		
		Element e = nucleus.toXmlElement();
		Nucleus test = new DefaultNucleus(e);
		ComponentTester.testDuplicatesByField("Nucleus", nucleus, test);
	}
	
	/**
	 * Test serialisation works for real nuclei
	 * @throws Exception
	 */
	@Test
	public void testXmlSerializesForRealNuclei() throws Exception {
		File testFolder = TestResources.MOUSE_INPUT_FOLDER.getAbsoluteFile();
    	IAnalysisOptions op = OptionsFactory.makeDefaultRodentAnalysisOptions(testFolder);
    	IAnalysisDataset d = TestImageDatasetCreator.createTestDataset(TestResources.UNIT_TEST_FOLDER, op, false);
    	
    	for(Nucleus n : d.getCollection().getNuclei()) {
    		Element e = n.toXmlElement();
    		Nucleus test = new DefaultNucleus(e);
    		Element e2 = test.toXmlElement();
    		
//    		if(!e.toString().equals(e2.toString())) {
//    			XMLOutputter xmlOutput = new XMLOutputter();
//    			xmlOutput.setFormat(Format.getPrettyFormat());
//    			xmlOutput.output(e, System.out);
//    			xmlOutput.output(e2, System.out);
//    		}
    		assertEquals(e.toString(), e2.toString());
    		
    		ComponentTester.testDuplicatesByField(n.getNameAndNumber(), n, test);
    	}
    	
    	Nucleus n = d.getCollection().getRawConsensus();
    	Element e = n.toXmlElement();
		Nucleus test = new DefaultConsensusNucleus(e);
		assertEquals(e.toString(), test.toXmlElement().toString());
		ComponentTester.testDuplicatesByField("Consensus", n, test);
	}
	
	@Test
	public void testToShapeIsConsistentAfterUnmarshalling() throws Exception {
		Element e = nucleus.toXmlElement();
		Nucleus dup = new DefaultNucleus(e);
		
		// Confirm both nuclei have the same outlines
		assertEquals(nucleus.getBorderList(), dup.getBorderList());
		
		Path2D.Double s = (Path2D.Double)nucleus.toShape();
		Path2D.Double t = (Path2D.Double)nucleus.toShape();
		
		// Test shapes generated from same nucleus	
		assertTrue(ComponentTester.shapesEqual(s, t));
		
		// Test shapes generated from duplicated nucleus
		Path2D.Double s2 = (Path2D.Double)dup.toShape();
		assertTrue(ComponentTester.shapesEqual(s, s2));		
	}
	

	
}
