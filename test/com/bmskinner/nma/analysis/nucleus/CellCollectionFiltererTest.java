package com.bmskinner.nma.analysis.nucleus;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import java.util.List;

import org.junit.Before;
import org.junit.Test;

import com.bmskinner.nma.ComponentTester;
import com.bmskinner.nma.TestDatasetBuilder;
import com.bmskinner.nma.components.cells.ICell;
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
	public void testOr() throws Exception {

		IAnalysisDataset d2 = new TestDatasetBuilder(456).cellCount(N_CELLS)
				.ofType(RuleSetCollection.roundRuleSetCollection())
				.withMaxSizeVariation(10)
				.randomOffsetProfiles(true)
				.numberOfClusters(N_CHILD_DATASETS)
				.segmented().build();

		ICellCollection mergedCollection = CellCollectionFilterer
				.or(List.of(d1.getCollection(), d2.getCollection()));

		assertEquals("Adding duplicate collection should include all cells",
				d1.getCollection().size() + d2.getCollection().size(), mergedCollection.size());

		// Check all the cells in input dataset one were copied correctly
		for (ICell originalCell : d1.getCollection().getCells()) {

			// Do we have a complete match for all cells?
			if (!mergedCollection.contains(originalCell)) {

				// The cell does not match - is there a cell with the same ID?
				if (mergedCollection.getCellIDs().contains(originalCell.getId())) {
					ICell mergedCell = mergedCollection.getCell(originalCell.getId());

					// What fields are different?
					ComponentTester.testDuplicatesByField(
							"Cell in merged dataset cell should match the input dataset",
							originalCell,
							mergedCell);
				} else {

					fail("Cell from input dataset 1 is not present in merged dataset: "
							+ originalCell.toString());
				}
			}
		}
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
