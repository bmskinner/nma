package com.bmskinner.nma.analysis.nucleus;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import java.util.List;

import org.junit.Before;
import org.junit.Test;

import com.bmskinner.nma.ComponentTester;
import com.bmskinner.nma.TestDatasetBuilder;
import com.bmskinner.nma.components.datasets.IAnalysisDataset;
import com.bmskinner.nma.components.datasets.ICellCollection;
import com.bmskinner.nma.components.options.DefaultOptions;
import com.bmskinner.nma.components.rules.RuleSetCollection;
import com.bmskinner.nma.io.SampleDatasetReader;

/**
 * Test that cells are correctly filtered
 * 
 * @author bs19022
 *
 */
public class CellCollectionFiltererTest extends ComponentTester {

	private IAnalysisDataset d1;

	@Before
	public void loadDataset() throws Exception {
		d1 = new TestDatasetBuilder(RNG_SEED).cellCount(N_CELLS)
				.ofType(RuleSetCollection.roundRuleSetCollection())
				.withMaxSizeVariation(10)
				.randomOffsetProfiles(true)
				.numberOfClusters(N_CHILD_DATASETS)
				.segmented().build();
	}

	@Test
	public void testAnd() throws Exception {
		IAnalysisDataset d2 = d1.copy();

		ICellCollection result = CellCollectionFilterer
				.and(List.of(d1.getCollection(), d2.getCollection()));

		assertEquals("Adding duplicate collection should include all cells",
				d1.getCollection().size(), result.size());
	}

	@Test
	public void testPoorEdgeDetectorFilterRemovesCells() throws Exception {
		IAnalysisDataset d = SampleDatasetReader.openTestMouseDataset();
		ICellCollection result = CellCollectionFilterer.filter(d.getCollection(),
				new PoorEdgeDetectionProfilePredicate(
						d.getAnalysisOptions().get().getRuleSetCollection().getOtherOptions()));

		assertTrue("Poor edge filter should remove cells",
				result.size() < d.getCollection().size());

	}

	/**
	 * Older versions of the rulesets will not have the edge filter values. In these
	 * cases, the filter must exit cleanly, either a noop or passing all cells.
	 * 
	 * @throws Exception
	 */
	@Test
	public void testPoorEdgeDetectorFilterExitsCleanlyWhenNoRulesetValuesSet() throws Exception {
		IAnalysisDataset d = SampleDatasetReader.openTestMouseDataset();
		ICellCollection result = CellCollectionFilterer.filter(d.getCollection(),
				new PoorEdgeDetectionProfilePredicate(new DefaultOptions()));
	}

}
