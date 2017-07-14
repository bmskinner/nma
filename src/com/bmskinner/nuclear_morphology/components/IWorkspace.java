/*******************************************************************************
 * Copyright (C) 2017 Ben Skinner
 * 
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 * 
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 * 
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.\
 *******************************************************************************/


package com.bmskinner.nuclear_morphology.components;

import java.io.File;
import java.util.List;
import java.util.Set;

/**
 * A workspace is a collection of nmd files that can be reopened together. This
 * interface mey be extended depending on how useful workspaces turn out to be.
 * 
 * @author ben
 * @since 1.13.3
 *
 */
public interface IWorkspace {

    void setName(String s);
    
    /**
     * Add the given datasest to the workspace
     * @param d
     */
    void add(IAnalysisDataset d);

    /**
     * Add the given file to the workspace
     * @param d
     */
    void add(File f);

    
    void remove(IAnalysisDataset d);

    void remove(File f);

    void save();

    Set<File> getFiles();

    void setSaveFile(File f);

    File getSaveFile();
        
    /**
     * This describes the information available for 
     * biological samples. Individual datasets can belong
     * to the same biological sample. For example, different
     * animals of a wild-type.
     * @author bms41
     * @since 1.13.8
     *
     */
    public interface BioSample {
        
        /**
         * Get the name of the sample
         */
        String getName();
        
        /**
         * Get the datasets in the sample
         */
        List<File> getDatasets();
        
        /**
         * Add a dataset to the sample
         * @param dataset
         */
       void addDataset(File dataset);
        
        /**
         * Remove the dataset from the sample
         * @param dataset
         */
        void removeDataset(File dataset);
        
        /**
         * Test if the given dataset is in the sample
         * @param dataset
         * @return
         */
        boolean hasDataset(File dataset);
        
    }
       
    Set<BioSample> getBioSamples();
    
    /**
     * Add a new sample to the workspace
     * @param name
     */
    void addBioSample(String name);
    
    /**
     * Get the biosample associated with the given dataset
     * @param dataset
     * @return
     */
    BioSample getBioSample(File dataset);
    
    /**
     * Get the biosample with the given name
     * @param dataset
     * @return
     */
    BioSample getBioSample(String name);


}
