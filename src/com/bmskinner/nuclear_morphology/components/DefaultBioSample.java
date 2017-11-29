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

package com.bmskinner.nuclear_morphology.components;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

import com.bmskinner.nuclear_morphology.components.IWorkspace.BioSample;

/**
 * The default implementation of BioSamples.
 * @author bms41
 * @since 1.13.8
 *
 */
public class DefaultBioSample implements BioSample {
    
    private String name;
    private List<File> datasets = new ArrayList<>();
    
    public DefaultBioSample(String name){
        if(name==null){
            throw new IllegalArgumentException("Name cannot be null");
        }
        this.name = name;
    }
    
    @Override
    public String getName() {
        return name;
    }

    @Override
    public List<File> getDatasets() {
        return datasets;

    }

    @Override
    public void addDataset(File dataset) {
        if(dataset==null){
            return;
        }
        datasets.add(dataset);

    }

    @Override
    public void removeDataset(File dataset) {
        if(dataset==null){
            return;
        }
       datasets.remove(dataset);
        
    }

    @Override
    public boolean hasDataset(File dataset) {
        return datasets.contains(dataset);
    }

}
