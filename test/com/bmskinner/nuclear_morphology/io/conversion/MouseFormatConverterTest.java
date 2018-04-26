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

package com.bmskinner.nuclear_morphology.io.conversion;

import static org.junit.Assert.*;

import java.io.File;

import org.junit.Before;
import org.junit.Test;

import com.bmskinner.nuclear_morphology.components.IAnalysisDataset;
import com.bmskinner.nuclear_morphology.components.generic.Version;
import com.bmskinner.nuclear_morphology.io.SampleDatasetReader;

/**
 * Test conversion and opening of old dataset versions
 * @author bms41
 * @since 1.13.8
 *
 */
public class MouseFormatConverterTest extends OldFormatConverterTest {
    
    private static final String MOUSE_FILE = "Mouse.bak";
    
    @Override
    @Test
    public void test_1_13_0_ConvertsToCurrent() throws Exception {
        File f = new File(SampleDatasetReader.SAMPLE_DATASET_PATH+DIR_1_13_0, MOUSE_FILE);
        IAnalysisDataset d = testConvertsToCurrent(f);
        assertTrue(d.getVersion().equals(Version.currentVersion()));
    }

    @Override
    @Test
    public void test_1_13_1_ConvertsToCurrent() throws Exception {
        File f = new File(SampleDatasetReader.SAMPLE_DATASET_PATH+DIR_1_13_1, MOUSE_FILE);
        IAnalysisDataset d = testConvertsToCurrent(f);
        assertTrue(d.getVersion().equals(Version.currentVersion()));
    }

    @Test
    public void test_1_13_2_ConvertsToCurrent() throws Exception {

        File f = new File(SampleDatasetReader.SAMPLE_DATASET_PATH+DIR_1_13_2, MOUSE_FILE);
        IAnalysisDataset d = testConvertsToCurrent(f);
        assertTrue(d.getVersion().equals(Version.currentVersion()));
    }

    @Test
    public void test_1_13_3_ConvertsToCurrent() throws Exception {

        File f = new File(SampleDatasetReader.SAMPLE_DATASET_PATH+DIR_1_13_3, MOUSE_FILE);
        IAnalysisDataset d = testConvertsToCurrent(f);
        assertTrue(d.getVersion().equals(Version.v_1_13_3));
    }

    @Override
    @Test
    public void test_1_13_4_ConvertsToCurrent() throws Exception {
        File f = new File(SampleDatasetReader.SAMPLE_DATASET_PATH+DIR_1_13_4, MOUSE_FILE);
        IAnalysisDataset d = testConvertsToCurrent(f);
        assertTrue(d.getVersion().equals(Version.v_1_13_4));
    }
    
//    @Test
//    public void test_1_13_4_Merge_ConvertsToCurrent() throws Exception {
//        File f = new File(SampleDatasetReader.SAMPLE_DATASET_PATH+DIR_1_13_4, "Mouse_merge.bak");
//        IAnalysisDataset d = testConvertsToCurrent(f);
//        assertTrue(d.getVersion().equals(Version.v_1_13_4));
//    }

    @Override
    @Test
    public void test_1_13_5_ConvertsToCurrent() throws Exception {
        File f = new File(SampleDatasetReader.SAMPLE_DATASET_PATH+DIR_1_13_5, MOUSE_FILE);
        IAnalysisDataset d = testConvertsToCurrent(f);
        assertTrue(d.getVersion().equals(Version.v_1_13_5));
    }

    @Override
    @Test
    public void test_1_13_6_ConvertsToCurrent() throws Exception {
        File f = new File(SampleDatasetReader.SAMPLE_DATASET_PATH+DIR_1_13_6, MOUSE_FILE);
        IAnalysisDataset d = testConvertsToCurrent(f);
        assertTrue(d.getVersion().equals(Version.v_1_13_6));
    }

    @Override
    @Test
    public void test_1_13_7_ConvertsToCurrent() throws Exception {
        File f = new File(SampleDatasetReader.SAMPLE_DATASET_PATH+DIR_1_13_7, MOUSE_FILE);
        IAnalysisDataset d = testConvertsToCurrent(f);
        assertTrue(d.getVersion().equals(Version.v_1_13_7));
    }
    
    //TODO reenable when signal folder can be input. Currently opens UI
//  @Test
//  public void test_1_13_2_withSignalsConvertsToCurrent() throws Exception {
//
//      File f = new File(SampleDatasetReader.SAMPLE_DATASET_PATH+DIR_1_13_2, "Testing_signals_A.bak");
//      IAnalysisDataset d = testConvertsToCurrent(f);
//      assertTrue(d.getVersion().equals(Version.currentVersion()));
//  }


}
