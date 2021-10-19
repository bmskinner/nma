package com.bmskinner.nuclear_morphology.components.datasets;

import static org.junit.Assert.*;

import java.io.PrintWriter;
import java.util.UUID;

import org.jdom2.Element;
import org.jdom2.output.Format;
import org.jdom2.output.XMLOutputter;
import org.junit.Before;
import org.junit.Test;

import com.bmskinner.nuclear_morphology.ComponentTester;
import com.bmskinner.nuclear_morphology.TestDatasetBuilder;
import com.bmskinner.nuclear_morphology.components.rules.RuleSetCollection;

public class VirtualDatasetTest extends ComponentTester {
	
	private IAnalysisDataset parent;
	private VirtualDataset d;

	@Before
    public void loadDataset() throws Exception {    	
		parent = new TestDatasetBuilder(RNG_SEED).cellCount(N_CELLS)
				.ofType(RuleSetCollection.roundRuleSetCollection())
				.withMaxSizeVariation(10)
				.randomOffsetProfiles(true)
				.numberOfClusters(N_CHILD_DATASETS)
				.segmented().build();
		
		d = new VirtualDataset(parent, "test", UUID.randomUUID(), parent.getCollection());
    }
	
	@Test
	public void testConstructFromDataset() throws Exception {
		IAnalysisDataset d1 = new TestDatasetBuilder(RNG_SEED).cellCount(N_CELLS)
				.ofType(RuleSetCollection.roundRuleSetCollection())
				.withMaxSizeVariation(10)
				.randomOffsetProfiles(true)
				.numberOfClusters(N_CHILD_DATASETS)
				.segmented().build();	
		assertEquals(parent.getCollection().getProfileCollection(), d.getProfileCollection());
	}
	
	@Test
    public void testDuplicate() throws Exception {
    	IAnalysisDataset dup = d.copy();
    	testDuplicatesByField(d, dup);
    }
	
	@Test
	public void testXmlSerializes() throws Exception {

		Element e = d.toXmlElement();		
		XMLOutputter xmlOutput = new XMLOutputter();
		xmlOutput.setFormat(Format.getPrettyFormat());
		xmlOutput.output(e, new PrintWriter( System.out ));
		
		// files are not absolute on test dataset creation
		d.setSavePath(d.getSavePath().getAbsoluteFile());
		
		IAnalysisDataset test = new VirtualDataset(e);
		xmlOutput.output(test.toXmlElement(), new PrintWriter( System.out ));
		testDuplicatesByField(d, test);
		assertEquals(d, test);
	}
}
