package com.bmskinner.nma.components.datasets;

import static org.junit.Assert.assertEquals;

import java.util.UUID;

import org.jdom2.Element;
import org.jdom2.output.Format;
import org.jdom2.output.XMLOutputter;
import org.junit.Before;
import org.junit.Test;

import com.bmskinner.nma.ComponentTester;
import com.bmskinner.nma.TestDatasetBuilder;
import com.bmskinner.nma.components.rules.RuleSetCollection;

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
		
		// Note we test string equality here because the profile collection classes are internal to
		// each dataset
		assertEquals(parent.getCollection().getProfileCollection().toString(), d.getProfileCollection().toString());
	}
	
	@Test
    public void testDuplicate() throws Exception {
    	IAnalysisDataset dup = d.copy();
    	testDuplicatesByField(d.getName(), d, dup);
    }
	
	/**
	 * A virtual dataset can only be deserialised as 
	 * part of a root dataset; otherwise it will not 
	 * have a parent set. Serialize the parent too.
	 * @throws Exception
	 */
	@Test
	public void testXmlSerializes() throws Exception {
		
		Element e = parent.toXmlElement();		
		XMLOutputter xmlOutput = new XMLOutputter();
		xmlOutput.setFormat(Format.getPrettyFormat());
		
		// files are not absolute on test dataset creation
		d.setSavePath(d.getSavePath().getAbsoluteFile());
		
		IAnalysisDataset test = DatasetCreator.createRoot(e);

		testDuplicatesByField(d.getName(), parent, test);
		assertEquals(parent, test);
	}
}
