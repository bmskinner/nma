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
package com.bmskinner.nuclear_morphology.components.workspaces;

import java.io.File;
import java.util.LinkedHashSet;
import java.util.Set;
import java.util.UUID;

import org.eclipse.jdt.annotation.NonNull;

import com.bmskinner.nuclear_morphology.components.datasets.IAnalysisDataset;
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

    private Set<File> datasets = new LinkedHashSet<>();
    private Set<BioSample> samples = new LinkedHashSet<>();

    private File saveFile = null;
    private String name;
    private UUID id = UUID.randomUUID();

    public DefaultWorkspace(@NonNull final File f) {
        this.saveFile = f;
        this.name = f.getName().replace(Importer.WRK_FILE_EXTENSION, "");
    }
    
    public DefaultWorkspace(@NonNull final String name) {
        this.name = name;
    }
    
    public DefaultWorkspace(@NonNull final File f, @NonNull final String name) {
    	this(f);
        this.name = name;
    }
    
    @Override
	public UUID getId() {
    	return id;
    }
    
    @Override
	public void setName(@NonNull String s){
        this.name = s;
    }
    
    @Override
	public @NonNull String getName() {
    	return name;
    }

    @Override
    public void add(final @NonNull IAnalysisDataset d) {
        if (d.isRoot())
            datasets.add(d.getSavePath());

        // TODO: warn or get root
    }

    @Override
    public void add(final @NonNull File f) {
        datasets.add(f);
    }

    @Override
    public void remove(final @NonNull IAnalysisDataset d) {
        datasets.remove(d.getSavePath());
        samples.stream().forEach(b->b.removeDataset(d));
    }

    @Override
    public void remove(@NonNull File f) {
        datasets.remove(f);
        samples.stream().forEach(b->b.removeDataset(f));
    }
    
    @Override
    public boolean has(final @NonNull IAnalysisDataset d) {
        return datasets.contains(d.getSavePath());
    }

    @Override
    public @NonNull Set<File> getFiles() {
        return datasets;
    }

    @Override
    public void setSaveFile(@NonNull File f) {
        saveFile = f;

    }

    @Override
    public @NonNull File getSaveFile() {
        return saveFile;
    }

    @Override
    public void save() {
        // TODO Auto-generated method stub

    }

    @Override
    public void addBioSample(@NonNull String name) {
        for(BioSample s : samples){
            if(s.getName().equals(name))
                return;
        }
        samples.add(new DefaultBioSample(name)); 
    }

    @Override
    public BioSample getBioSample(@NonNull IAnalysisDataset d) {
        return samples.stream().filter( s-> s.hasDataset(d.getSavePath())).findFirst().orElse(null);
    }
    
    @Override
    public BioSample getBioSample(@NonNull String name) {
        return samples.stream().filter( s-> s.getName().equals(name)).findFirst().orElse(null);
    }

    @Override
    public @NonNull Set<BioSample> getBioSamples() {
        return samples;
    }
    
    @Override
    public String toString(){
        return name;
    }

	@Override
	public int hashCode() {
		final int prime = 31;
		int result = 1;
		result = prime * result + ((datasets == null) ? 0 : datasets.hashCode());
		result = prime * result + ((id == null) ? 0 : id.hashCode());
		result = prime * result + ((name == null) ? 0 : name.hashCode());
		result = prime * result + ((samples == null) ? 0 : samples.hashCode());
		result = prime * result + ((saveFile == null) ? 0 : saveFile.hashCode());
		return result;
	}

	@Override
	public boolean equals(Object obj) {
		if (this == obj)
			return true;
		if (obj == null)
			return false;
		if (getClass() != obj.getClass())
			return false;
		DefaultWorkspace other = (DefaultWorkspace) obj;
		if (datasets == null) {
			if (other.datasets != null)
				return false;
		} else if (!datasets.equals(other.datasets))
			return false;
		if (id == null) {
			if (other.id != null)
				return false;
		} else if (!id.equals(other.id))
			return false;
		if (name == null) {
			if (other.name != null)
				return false;
		} else if (!name.equals(other.name))
			return false;
		if (samples == null) {
			if (other.samples != null)
				return false;
		} else if (!samples.equals(other.samples))
			return false;
		if (saveFile == null) {
			if (other.saveFile != null)
				return false;
		} else if (!saveFile.equals(other.saveFile))
			return false;
		return true;
	}
    
    

}
