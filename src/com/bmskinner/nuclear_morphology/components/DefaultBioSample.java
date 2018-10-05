/*******************************************************************************
 * Copyright (C) 2018 Ben Skinner
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
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 ******************************************************************************/
package com.bmskinner.nuclear_morphology.components;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

import com.bmskinner.nuclear_morphology.components.workspaces.IWorkspace.BioSample;

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
        if(dataset==null)
            return;
        datasets.add(dataset);
    }

    @Override
    public void addDataset(IAnalysisDataset dataset) {
    	addDataset(dataset.getSavePath());
    }

    @Override
    public void removeDataset(File dataset) {
        if(dataset==null)
            return;
       datasets.remove(dataset);
        
    }
    
    @Override
    public void removeDataset(IAnalysisDataset dataset) {
        if(dataset==null){
            return;
        }
       datasets.remove(dataset.getSavePath());
    }

    @Override
    public boolean hasDataset(File dataset) {
        return datasets.contains(dataset);
    }
    
    @Override
    public boolean hasDataset(IAnalysisDataset dataset) {
        return datasets.contains(dataset.getSavePath());
    }

}
