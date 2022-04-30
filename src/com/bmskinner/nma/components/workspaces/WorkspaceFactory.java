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
package com.bmskinner.nma.components.workspaces;

import java.io.File;

import org.eclipse.jdt.annotation.NonNull;

import com.bmskinner.nma.components.datasets.IAnalysisDataset;
import com.bmskinner.nma.io.Io;

/**
 * Utility class for creating workspaces
 * @author Ben Skinner
 *
 */
public class WorkspaceFactory {
	
	private WorkspaceFactory() {
		// private constructor does nothing
	}
	
	/**
	 * Create an empty workspace with the given name. The save file location
	 * will remain null.
	 * @param name the name of the workspace
	 * @return
	 */
	public static IWorkspace createWorkspace(@NonNull String name) {
		return new DefaultWorkspace(name);
	}
	
	/**
	 * Create an empty workspace with the given save file location. The workspace name
	 * will be the same as the filename
	 * @param file
	 * @return
	 */
	public static IWorkspace createWorkspace(@NonNull File file) {
		return new DefaultWorkspace(file);
	}
	
	/**
	 * Create a workspace containing the given dataset. The save file location
	 * will be the dataset save folder, and the name will be the same as the dataset
	 * @param dataset the dataset to add to the workspace
	 * @return
	 */
	public static IWorkspace createWorkspace(@NonNull IAnalysisDataset dataset) {
		File f = new File(dataset.getSavePath().getParent(), dataset.getName()+Io.WRK_FILE_EXTENSION);
		IWorkspace w = new DefaultWorkspace(f, dataset.getName());
		w.add(dataset);
		return w;
	}
	
	/**
	 * Create a workspace containing the given dataset. The save file location
	 * will be the dataset save folder, and the name will be the same as the dataset
	 * @param dataset the dataset to add to the workspace
	 * @param name the name of the workspace
	 * @return
	 */
	public static IWorkspace createWorkspace(@NonNull IAnalysisDataset dataset, @NonNull String name) {
		File f = new File(dataset.getSavePath().getParent(), dataset.getName()+Io.WRK_FILE_EXTENSION);
		IWorkspace w = new DefaultWorkspace(f, name);
		w.add(dataset);
		return w;
	}
	
	/**
	 * Create a workspace containing the given dataset. The save file location
	 * will be the dataset save folder, and the name will be the same as the dataset
	 * @param dataset the dataset to add to the workspace
	 * @param name the name of the workspace
	 * @return
	 */
	public static IWorkspace createWorkspace(@NonNull IAnalysisDataset dataset, @NonNull String name, @NonNull File file) {
		IWorkspace w = new DefaultWorkspace(file, name);
		w.add(dataset);
		return w;
	}

}
