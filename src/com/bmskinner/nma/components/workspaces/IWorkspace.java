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
import java.util.Set;
import java.util.UUID;

import org.eclipse.jdt.annotation.NonNull;
import org.eclipse.jdt.annotation.Nullable;

import com.bmskinner.nma.components.datasets.IAnalysisDataset;
import com.bmskinner.nma.io.XmlSerializable;

/**
 * A workspace is a collection of nmd files that can be reopened together. This
 * interface mey be extended depending on how useful workspaces turn out to be.
 * 
 * @author ben
 * @since 1.13.3
 *
 */
public interface IWorkspace extends XmlSerializable {

	static final String XML_WORKSPACE = "workspace";
	static final String WORKSPACE_NAME = "name";
	static final String DATASETS_ELEMENT = "datasets";
	static final String DATASET_PATH = "path";
//	static final String BIOSAMPLES_ELEMENT = "biosamples";
//	static final String BIOSAMPLES_NAME_KEY = "name";
//	static final String BIOSAMPLES_DATASET_KEY = "dataset";

	/**
	 * Get the UUID of the workspace
	 * 
	 * @return
	 */
	UUID getId();

	/**
	 * Set the display name of the workspace
	 * 
	 * @param s
	 */
	void setName(@NonNull final String s);

	/**
	 * Get the display name of the workspace
	 * 
	 * @return
	 */
	@NonNull
	String getName();

	/**
	 * Add the given dataset to the workspace
	 * 
	 * @param d the dataset to add
	 */
	void add(@NonNull final IAnalysisDataset d);

	/**
	 * Add the given file to the workspace
	 * 
	 * @param f the file
	 */
	void add(@NonNull final File f);

	/**
	 * Remove the given dataset from the workspace
	 * 
	 * @param d the dataset to remove
	 */
	void remove(@NonNull final IAnalysisDataset d);

	/**
	 * Remove the given file from the workspace
	 * 
	 * @param f the file to remove
	 */
	void remove(@NonNull final File f);

	/**
	 * Test if the given dataset is in the workspace
	 * 
	 * @param d
	 * @return true if the dataset is in the workspace
	 */
	boolean has(@NonNull final IAnalysisDataset d);

	/**
	 * Save the workspace
	 */
	void save();

	/**
	 * Get the files in the workspace
	 * 
	 * @return
	 */
	@NonNull
	Set<File> getFiles();

	/**
	 * Set the save path of the workspace
	 * 
	 * @param f
	 */
	void setSaveFile(@NonNull final File f);

	/**
	 * Get the save file of the workspace
	 * 
	 * @return
	 */
	@Nullable
	File getSaveFile();

}
