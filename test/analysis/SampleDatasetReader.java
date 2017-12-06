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

package analysis;

import java.io.File;

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
    
    /**
     * Open the dataset in the given file.
     * @param f
     * @return
     * @throws Exception
     */
    public IAnalysisDataset openDataset(File f) throws Exception{
        
        if(f==null){
            throw new Exception("Null file argument");
        }
        
        if(!f.exists()){
            throw new Exception("File does not exist: "+f.getAbsolutePath());
        }
        
        IAnalysisMethod m = new DatasetImportMethod(f);

        System.out.println("Importing "+f.toString());
        IAnalysisResult r = m.call();

        IAnalysisDataset d = r.getFirstDataset();
        return d;
    }
    
    

}
