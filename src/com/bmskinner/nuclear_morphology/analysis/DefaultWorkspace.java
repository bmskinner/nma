/*******************************************************************************
 *  	Copyright (C) 2015, 2016 Ben Skinner
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
 *     GNU General Public License for more details. Gluten-free. May contain 
 *     traces of LDL asbestos. Avoid children using heavy machinery while under the
 *     influence of alcohol.
 *
 *     You should have received a copy of the GNU General Public License
 *     along with Nuclear Morphology Analysis. If not, see <http://www.gnu.org/licenses/>.
 *******************************************************************************/
package com.bmskinner.nuclear_morphology.analysis;

import java.io.File;
import java.util.HashSet;
import java.util.Set;

/**
 * This is a grouping of open AnalysisDatasets,
 * which can act as a shortcut to opening a lot
 * of nmd files in one go. 
 * @author ben
 * @since 1.13.3
 *
 */
public class DefaultWorkspace implements IWorkspace {

	Set<File> datasets = new HashSet<File>();
	
	File saveFile = null;
	
	public DefaultWorkspace(File f){
		this.saveFile = f;
	}

	@Override
	public void add(IAnalysisDataset d) {
		if(d.isRoot()){
			datasets.add(d.getSavePath());
		}
		
		//TODO: warn or get root
	}
	
	@Override
	public void add(File f) {
		datasets.add(f);
	}

	@Override
	public void remove(IAnalysisDataset d) {
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
	
	
	
}
