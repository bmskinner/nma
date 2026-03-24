package com.bmskinner.nma.components.datasets;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

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
		final IAnalysisDataset d1 = new TestDatasetBuilder(RNG_SEED).cellCount(N_CELLS)
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
	public void testContainsAll() throws Exception {
		// Check full subset is correct
		assertTrue(d.containsAll(parent.getCollection()));

		// Check partial subset forward and reverse
		final IAnalysisDataset parentCopy = parent.copy();

		for (final IAnalysisDataset child : parent.getAllChildDatasets()) {
			assertFalse(child.getCollection().containsAll(parentCopy.getCollection()));
			assertTrue(parentCopy.getCollection().containsAll(child.getCollection()));
		}
	}

	@Test
    public void testDuplicate() throws Exception {
    	final IAnalysisDataset dup = d.copy();
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
		
		final Element e = parent.toXmlElement();		
		final XMLOutputter xmlOutput = new XMLOutputter();
		xmlOutput.setFormat(Format.getPrettyFormat());
		
		// files are not absolute on test dataset creation
		d.setSavePath(d.getSavePath().getAbsoluteFile());
		
		final IAnalysisDataset test = DatasetCreator.createRoot(e);

		testDuplicatesByField(d.getName(), parent, test);
		assertEquals(parent, test);
	}
}
