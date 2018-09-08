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

import org.eclipse.jdt.annotation.NonNull;
import org.eclipse.jdt.annotation.Nullable;

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
    
    public static final String SAMPLE_DATASET_PATH = "test/samples/datasets/";
    
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
     * @param f the file to open
     * @return the dataset
     * @throws Exception
     */
    public static IAnalysisDataset openDataset(@NonNull File f) throws Exception {
        return openDataset(f, null);
    }
    
    /**
     * Open the dataset in the given file. Also provide a map of signal images.
     * @param f the file to open
     * @param signalMap a map of signal ids to folders. Can be null,
     * @return the dataset
     * @throws Exception
     */
    public static IAnalysisDataset openDataset(@NonNull File f, @Nullable Map<UUID, File> signalMap) throws Exception {        
        if(!f.exists())
            throw new Exception("File does not exist: "+f.getAbsolutePath()); 
        IAnalysisMethod m = signalMap==null ? new DatasetImportMethod(f) : new DatasetImportMethod(f, signalMap);
        IAnalysisResult r = m.call();
        return r.getFirstDataset();
    }
    
    

}
