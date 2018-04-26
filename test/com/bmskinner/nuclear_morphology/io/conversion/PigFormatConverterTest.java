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
public class PigFormatConverterTest extends OldFormatConverterTest {

    
    @Override
    @Test
    public void test_1_13_0_ConvertsToCurrent() throws Exception {
        fail("Not yet implemented");
    }
    
    
    @Test
    @Override
    public void test_1_13_1_ConvertsToCurrent() throws Exception {

        File f = new File(SampleDatasetReader.SAMPLE_DATASET_PATH+DIR_1_13_1, "Test 4 - Pig.bak");
        IAnalysisDataset d = testConvertsToCurrent(f);
        assertTrue(d.getVersion().equals(Version.currentVersion()));
    }

    @Test
    public void test_1_13_2_ConvertsToCurrent() throws Exception {

        File f = new File(SampleDatasetReader.SAMPLE_DATASET_PATH+DIR_1_13_2, "Test 13 - Pig.bak");
        IAnalysisDataset d = testConvertsToCurrent(f);
        assertTrue(d.getVersion().equals(Version.currentVersion()));
    }
    
    @Override
    @Test
    public void test_1_13_3_ConvertsToCurrent() throws Exception {
        fail("Not yet implemented");
    }
    
    @Override
    @Test
    public void test_1_13_4_ConvertsToCurrent() throws Exception {
        fail("Not yet implemented");
    }

    @Override
    @Test
    public void test_1_13_5_ConvertsToCurrent() throws Exception {
        fail("Not yet implemented");
    }

    @Override
    @Test
    public void test_1_13_6_ConvertsToCurrent() throws Exception {
        fail("Not yet implemented");
    }

    @Override
    @Test
    public void test_1_13_7_ConvertsToCurrent() throws Exception {
        fail("Not yet implemented");
    }

}
