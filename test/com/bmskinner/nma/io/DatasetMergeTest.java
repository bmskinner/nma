/*******************************************************************************
 *      Copyright (C) 2016 Ben Skinner
 *   
 *     This file is part of Nuclear Morphology Analysis.
 *
 *     Nuclear Morphology Analysis is free software: you can redistribute it and/or modify
 *     it under the terms of the GNU General Public License as published by
 *     the Free Software Foundation, either version 3 of the License, or
 *     (at your option) any later version.
 *
 *     Nuclear Morphology Analysis is distributed in the hope that it will be useful,
 *     but WITHOUT ANY WARRANTY; without even the implied warranty of
 *     MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 *     GNU General Public License for more details.
 *
 *     You should have received a copy of the GNU General Public License
 *     along with Nuclear Morphology Analysis. If not, see <http://www.gnu.org/licenses/>.
 *******************************************************************************/

package com.bmskinner.nma.io;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.fail;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

import org.junit.Test;

import com.bmskinner.nma.TestResources;
import com.bmskinner.nma.analysis.DatasetMergeMethod;
import com.bmskinner.nma.analysis.IAnalysisMethod;
import com.bmskinner.nma.analysis.IAnalysisResult;
import com.bmskinner.nma.components.cells.ICell;
import com.bmskinner.nma.components.datasets.IAnalysisDataset;
import com.bmskinner.nma.gui.dialogs.DatasetArithmeticSetupDialog.BooleanOperation;

/**
 * This class tests the dataset merging functionality
 * 
 * @author Ben Skinner
 * @since 1.13.8
 *
 */
public class DatasetMergeTest {

	/**
	 * This test checks that merging of two sample datasets is possible, and that
	 * the number of cells in the merged dataset is the sum of the input datasets.
	 * 
	 * @throws Exception
	 */
	@Test
	public void testDatasetMergeIncludesAllCells() throws Exception {

		int cells = 0;

		IAnalysisDataset d1 = SampleDatasetReader
				.openDataset(TestResources.MULTIPLE_SOURCE_1_DATASET);
		IAnalysisDataset d2 = SampleDatasetReader
				.openDataset(TestResources.MULTIPLE_SOURCE_2_DATASET);

		List<IAnalysisDataset> toMerge = new ArrayList<>();
		toMerge.add(d1);
		toMerge.add(d2);

		cells += d1.getCollection().getNucleusCount();
		cells += d2.getCollection().getNucleusCount();

		File saveFile = new File(TestResources.MULTIPLE_BASE_FOLDER, "Merge_test.nmd");
		IAnalysisMethod m = new DatasetMergeMethod(toMerge, BooleanOperation.OR, saveFile);

		IAnalysisResult r = m.call();
		IAnalysisDataset mergedDataset = r.getFirstDataset();
		assertNotNull("Dataset should be returned from merge method", mergedDataset);

		assertEquals("Dataset merge should have the same cell count as input datasets",
				mergedDataset.getCollection().getNucleusCount(), cells);

		// Check all the cells in input dataset one were copied
		// Do not check all fields; that is a test for the CellCollectionFilterer
		// Since segments must be accounted for, we don't worry about equality testing
		// here, just if the same cell ids are present
		for (ICell originalCell : d1.getCollection().getCells()) {

			// Is there a cell with the same ID?
			if (!mergedDataset.getCollection().getCellIDs().contains(originalCell.getId())) {
				fail("Cell from input dataset 1 is not present in merged dataset: "
						+ originalCell.toString());
			}

		}

		// Now do the same for the cells in input dataset 2
		for (ICell originalCell : d2.getCollection().getCells()) {
			// Is there a cell with the same ID?
			if (!mergedDataset.getCollection().getCellIDs().contains(originalCell.getId())) {
				fail("Cell from input dataset 2 is not present in merged dataset: "
						+ originalCell.toString());
			}
		}

	}

}
