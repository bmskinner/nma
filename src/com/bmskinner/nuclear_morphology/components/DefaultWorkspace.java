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
import java.util.LinkedHashSet;
import java.util.Set;

import com.bmskinner.nuclear_morphology.io.Io.Importer;

/**
 * This is a grouping of open AnalysisDatasets, which can act as a shortcut to
 * opening a lot of nmd files in one go.
 * 
 * @author ben
 * @since 1.13.3
 *
 */
public class DefaultWorkspace implements IWorkspace {

    private Set<File> datasets = new LinkedHashSet<File>();
    private Set<BioSample> samples = new LinkedHashSet<BioSample>();

    private File saveFile = null;
    private String name;

    public DefaultWorkspace(final File f) {
        this.saveFile = f;
        this.name = f.getName().replace(Importer.WRK_FILE_EXTENSION, "");
    }
    
    public void setName(String s){
        this.name = s;
    }
    
    public String getName() {
    	return name;
    }

    @Override
    public void add(final IAnalysisDataset d) {
        if (d.isRoot()) {
            datasets.add(d.getSavePath());
        }

        // TODO: warn or get root
    }

    @Override
    public void add(final File f) {
        datasets.add(f);
    }

    @Override
    public void remove(final IAnalysisDataset d) {
        datasets.remove(d.getSavePath());
    }

    @Override
    public void remove(File f) {
        datasets.remove(f);
    }

    @Override
    public Set<File> getFiles() {
        return datasets;
    }

    @Override
    public void setSaveFile(File f) {
        saveFile = f;

    }

    @Override
    public File getSaveFile() {
        return saveFile;
    }

    @Override
    public void save() {
        // TODO Auto-generated method stub

    }

    @Override
    public void addBioSample(String name) {
        for(BioSample s : samples){
            if(s.getName().equals(name)){
                return;
            }
        }
        samples.add(new DefaultBioSample(name)); 
    }

    @Override
    public BioSample getBioSample(File f) {
        return samples.stream().filter( s-> s.hasDataset(f)).findFirst().orElse(null);
    }
    
    @Override
    public BioSample getBioSample(String name) {
        return samples.stream().filter( s-> s.getName().equals(name)).findFirst().orElse(null);
    }

    @Override
    public Set<BioSample> getBioSamples() {
        return samples;
    }
    
    @Override
    public String toString(){
        return name;
    }

}
