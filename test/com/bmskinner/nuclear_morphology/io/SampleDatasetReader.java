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

import java.io.File;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

import com.bmskinner.nuclear_morphology.analysis.IAnalysisMethod;
import com.bmskinner.nuclear_morphology.analysis.IAnalysisResult;
import com.bmskinner.nuclear_morphology.components.IAnalysisDataset;
import com.bmskinner.nuclear_morphology.io.DatasetImportMethod;

/**
 * Provides a simple access point to open datasets for testing classes
 * @author bms41
 * @since 1.13.8
 *
 */
public class SampleDatasetReader {
    
    public static final String SAMPLE_DATASET_PATH = "test/com/bmskinner/nuclear_morphology/samples/datasets/";
    
    public static final String RODENT_TEST_DATASET = "Testing_1_13_8.nmd";
    public static final String PIG_TEST_DATASET    = "Testing_pig_1_13_8.nmd";
    public static final String ROUND_TEST_DATASET    = "Testing_round_1_13_8.nmd";
    public static final String MOUSE_SIGNALS_DATASET  = "Testing_signals.nmd";
    
    
    /**
     * Open the default rodent testing dataset
     * @return
     * @throws Exception
     */
    public static final IAnalysisDataset openTestRodentDataset() throws Exception {
        File f = new File(SAMPLE_DATASET_PATH+RODENT_TEST_DATASET);
        return openDataset(f);
    }
    
    /**
     * Open the default pig testing dataset
     * @return
     * @throws Exception
     */
    public static final IAnalysisDataset openTestPigDataset() throws Exception {
        File f = new File(SAMPLE_DATASET_PATH+PIG_TEST_DATASET);
        return openDataset(f);
    }
    
    /**
     * Open the default round testing dataset
     * @return
     * @throws Exception
     */
    public static final IAnalysisDataset openTestRoundDataset() throws Exception {
        File f = new File(SAMPLE_DATASET_PATH+ROUND_TEST_DATASET);
        return openDataset(f);
    }
    
    /**
     * Open the default mouse signals testing dataset
     * @return
     * @throws Exception
     */
    public static final IAnalysisDataset openTestMouseSignalsDataset() throws Exception {
        File f = new File(SAMPLE_DATASET_PATH+MOUSE_SIGNALS_DATASET);
        return openDataset(f);
    }
        
    /**
     * Open the dataset in the given file.
     * @param f
     * @return
     * @throws Exception
     */
    public static IAnalysisDataset openDataset(File f) throws Exception {
        
        if(f==null)
            throw new Exception("Null file argument");
        
        if(!f.exists())
            throw new Exception("File does not exist: "+f.getAbsolutePath());
        
        IAnalysisMethod m = new DatasetImportMethod(f);

        System.out.println("Importing "+f.toString());
        IAnalysisResult r = m.call();

        IAnalysisDataset d = r.getFirstDataset();
        return d;
    }
    
    /**
     * Open the dataset in the given file. Also provide a map of signal images.
     * @param f
     * @param signalMap
     * @return
     * @throws Exception
     */
    public static IAnalysisDataset openDataset(File f, Map<UUID, File> signalMap) throws Exception {
        if(f==null)
            throw new Exception("Null file argument");
        
        if(!f.exists())
            throw new Exception("File does not exist: "+f.getAbsolutePath());
        
        IAnalysisMethod m = new DatasetImportMethod(f, signalMap);

        System.out.println("Importing "+f.toString());
        IAnalysisResult r = m.call();

        IAnalysisDataset d = r.getFirstDataset();
        return d;
    }
    
    

}
