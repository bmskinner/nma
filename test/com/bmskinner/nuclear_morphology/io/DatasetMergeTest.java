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

package com.bmskinner.nuclear_morphology.io;

import static org.junit.Assert.*;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

import javax.swing.table.TableModel;

import org.junit.Test;

import com.bmskinner.nuclear_morphology.analysis.DatasetMergeMethod;
import com.bmskinner.nuclear_morphology.analysis.IAnalysisMethod;
import com.bmskinner.nuclear_morphology.analysis.IAnalysisResult;
import com.bmskinner.nuclear_morphology.charting.datasets.AnalysisDatasetTableCreator;
import com.bmskinner.nuclear_morphology.charting.options.DefaultTableOptions.TableType;
import com.bmskinner.nuclear_morphology.charting.options.TableOptions;
import com.bmskinner.nuclear_morphology.charting.options.TableOptionsBuilder;
import com.bmskinner.nuclear_morphology.components.IAnalysisDataset;
import com.bmskinner.nuclear_morphology.components.ICell;
import com.bmskinner.nuclear_morphology.components.nuclei.Nucleus;

/**
 * This class tests the dataset merging functionality
 * @author bms41
 * @since 1.13.8
 *
 */
public class DatasetMergeTest extends SampleDatasetReader {
    
    public static final String TEST_PATH_1 = SAMPLE_DATASET_PATH + "1.13.7/Mouse.bak";
    public static final String TEST_PATH_2 = SAMPLE_DATASET_PATH + "1.13.8/Mouse.bak";

    /**
     * This test checks that merging of two sample datasets is possible, and that the number
     * of cells in the merged dataset is the sum of the input datasets. 
     * @throws Exception 
     */
    @Test
    public void testDatasetMergeIncludesAllCells() throws Exception {
        
        File f1 = new File(TEST_PATH_1);
        File f2 = new File(TEST_PATH_2);
        
        int cells = 0;

        IAnalysisDataset d1 = openDataset(f1);
        IAnalysisDataset d2 = openDataset(f2);

        List<IAnalysisDataset> toMerge = new ArrayList<>();
        toMerge.add(d1);
        toMerge.add(d2);

        cells += d1.getCollection().getNucleusCount();
        cells += d2.getCollection().getNucleusCount();

        IAnalysisMethod m = new DatasetMergeMethod(toMerge, new File(SAMPLE_DATASET_PATH+"Merge_test.nmd"));  

        System.out.println("Merging "+d1.toString()+" and "+d2.toString());
        IAnalysisResult r = m.call();
        IAnalysisDataset d = r.getFirstDataset();

        assertEquals(d.getCollection().getNucleusCount(), cells);

        for(ICell c : d1.getCollection().getCells()){
            if(!d.getCollection().contains(c))
                fail("Missing dataset 1 cell "+c.toString());
        }

        for(ICell c : d2.getCollection().getCells()){
            if(!d.getCollection().contains(c))
                fail("Missing dataset 2 cell "+c.toString());
        }

        // Now get the analysis parameters table model that will be displayed 

        //            System.out.println(d1.getAnalysisOptions().toString());
        //            System.out.println(d2.getAnalysisOptions().toString());
        //            
        //            List<IAnalysisDataset> l = new ArrayList<>();
        //            l.add(d);
        //            
        //            TableOptions op = new TableOptionsBuilder()
        //                    .setDatasets(l)
        //                    .setType(TableType.ANALYSIS_PARAMETERS)
        //                    .build();
        //            
        //            AnalysisDatasetTableCreator c = new AnalysisDatasetTableCreator(op);

        //            TableModel tm = c.createAnalysisTable();

        //            for(int row=0; row<tm.getRowCount(); row++){
        //                for(int col=0; col<tm.getColumnCount(); col++){
        //                    System.out.println(tm.getValueAt(row, col));
        //                }
        //                System.out.println("\n");
        //            }

    }

}
