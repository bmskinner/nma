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

/**
 * This class tests the dataset merging functionality
 * @author bms41
 * @since 1.13.8
 *
 */
public class DatasetMergeTest {
    
    /**
     * This test checks that merging of two sample datasets is possible, and that the number
     * of cells in the merged dataset is the sum of the input datasets. 
     * @throws Exception 
     */
    @Test
    public void testDatasetMergeIncludesAllCells() throws Exception {

        int cells = 0;

        IAnalysisDataset d1 = SampleDatasetReader.openDataset(TestResources.MULTIPLE_SOURCE_1_DATASET);
        IAnalysisDataset d2 = SampleDatasetReader.openDataset(TestResources.MULTIPLE_SOURCE_2_DATASET);

        List<IAnalysisDataset> toMerge = new ArrayList<>();
        toMerge.add(d1);
        toMerge.add(d2);

        cells += d1.getCollection().getNucleusCount();
        cells += d2.getCollection().getNucleusCount();

        File saveFile = new File(TestResources.MULTIPLE_BASE_FOLDER, "Merge_test.nmd");
        IAnalysisMethod m = new DatasetMergeMethod(toMerge, saveFile);  

        IAnalysisResult r = m.call();
        IAnalysisDataset d = r.getFirstDataset();
        assertNotNull("Dataset should be returned from merge method", d);

        assertEquals(d.getCollection().getNucleusCount(), cells);

        for(ICell c : d1.getCollection().getCells()){
            if(!d.getCollection().contains(c))
                fail("Missing dataset 1 cell "+c.toString());
        }

        for(ICell c : d2.getCollection().getCells()){
            if(!d.getCollection().contains(c))
                fail("Missing dataset 2 cell "+c.toString());
        }
    }

}
