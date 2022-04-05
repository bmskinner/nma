package com.bmskinner.nuclear_morphology.components.nuclei;

import static org.junit.Assert.assertEquals;

import java.io.PrintWriter;

import org.jdom2.Element;
import org.jdom2.output.Format;
import org.jdom2.output.XMLOutputter;
import org.junit.Before;
import org.junit.Test;

import com.bmskinner.nuclear_morphology.ComponentTester;
import com.bmskinner.nuclear_morphology.TestDatasetBuilder;
import com.bmskinner.nuclear_morphology.analysis.nucleus.ConsensusAveragingMethod;
import com.bmskinner.nuclear_morphology.components.datasets.IAnalysisDataset;
import com.bmskinner.nuclear_morphology.components.rules.RuleSetCollection;

public class DefaultConsensusNucleusTest {

	private Consensus nucleus;	

	@Before
	public void setUp() throws Exception {
		IAnalysisDataset d = new TestDatasetBuilder(ComponentTester.RNG_SEED)
				.cellCount(ComponentTester.N_CELLS)
				.ofType(RuleSetCollection.roundRuleSetCollection())
				.withMaxSizeVariation(1)
				.randomOffsetProfiles(true)
				.numberOfClusters(ComponentTester.N_CHILD_DATASETS)
				.segmented().build();
		
		new ConsensusAveragingMethod(d).call();
		nucleus = d.getCollection().getRawConsensus();
	}

	/**
	 * Test that the consensus can be duplicated exactly
	 * @throws Exception
	 */
	@Test
	public void testDuplicate() throws Exception {
		Consensus dup = nucleus.duplicate();
		ComponentTester.testDuplicatesByField("Consensus", nucleus, dup);
	}
	

	/**
	 * Check that duplicating a component multiple times does
	 * not introduce errors
	 * @throws Exception
	 */
	@Test
	public void testDuplicateIsStableOverRepeatedIterations() throws Exception {
		Consensus dup = nucleus.duplicate();
		for(int i=0; i<20; i++) {
			Consensus dup2 = dup.duplicate();
			ComponentTester.testDuplicatesByField("Consensus", dup, dup2);
			dup = dup2;
		}		
	}
	
	@Test
	public void testXmlSerializes() throws Exception {
		
		Element e = nucleus.toXmlElement();
		
		XMLOutputter xmlOutput = new XMLOutputter();
		xmlOutput.setFormat(Format.getPrettyFormat());

		Nucleus test = new DefaultConsensusNucleus(e);
		
		assertEquals(nucleus, test);
	}

}
