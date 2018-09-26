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

import com.bmskinner.nuclear_morphology.analysis.DatasetValidator;
import com.bmskinner.nuclear_morphology.components.IAnalysisDataset;
import com.bmskinner.nuclear_morphology.components.generic.Version;
import com.bmskinner.nuclear_morphology.io.SampleDatasetReader;

/**
 * Tests that old datasets can be opened
 * @author bms41
 * @since 1.13.8
 *
 */
public abstract class OldFormatConverterTest {
    
	public static final String DIR_1_13_0 = "1.13.0/";
	public static final String DIR_1_13_1 = "1.13.1/";
	public static final String DIR_1_13_2 = "1.13.2/";
	public static final String DIR_1_13_3 = "1.13.3/";
	public static final String DIR_1_13_4 = "1.13.4/";
	public static final String DIR_1_13_5 = "1.13.5/";
	public static final String DIR_1_13_6 = "1.13.6/";
	public static final String DIR_1_13_7 = "1.13.7/";
	public static final String DIR_1_13_8 = "1.13.8/";
    
    /**
     * Try to open the dataset in the given file and test if it is valid after any conversions
     * @param f the file to open
     * @return the opened dataset
     * @throws Exception
     */
    protected IAnalysisDataset testConvertsToCurrent(File f) throws Exception {
    	if(!f.exists())
    		fail("Test file does not exist: "+f.getAbsolutePath());
        IAnalysisDataset d = SampleDatasetReader.openDataset(f);
        DatasetValidator v = new DatasetValidator();
        assertTrue(v.validate(d));
        return d;
    }
    
    @Test
    public abstract void test_1_13_0_ConvertsToCurrent() throws Exception;
    
    @Test
    public abstract void test_1_13_1_ConvertsToCurrent() throws Exception;
    
    @Test
    public abstract void test_1_13_2_ConvertsToCurrent() throws Exception;
    
    @Test
    public abstract void test_1_13_3_ConvertsToCurrent() throws Exception;
    
    @Test
    public abstract void test_1_13_4_ConvertsToCurrent() throws Exception;
    
    @Test
    public abstract void test_1_13_5_ConvertsToCurrent() throws Exception;
    
    @Test
    public abstract void test_1_13_6_ConvertsToCurrent() throws Exception;
    
    @Test
    public abstract void test_1_13_7_ConvertsToCurrent() throws Exception;
    
    @Test
    public abstract void test_1_13_8_ConvertsToCurrent() throws Exception;

}
