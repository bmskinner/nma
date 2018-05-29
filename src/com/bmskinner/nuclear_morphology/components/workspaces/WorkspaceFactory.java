package com.bmskinner.nuclear_morphology.components.workspaces;

import java.io.File;

import org.eclipse.jdt.annotation.NonNull;

import com.bmskinner.nuclear_morphology.components.IAnalysisDataset;
import com.bmskinner.nuclear_morphology.io.Io.Exporter;

public class WorkspaceFactory {
	
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
		File f = new File(dataset.getSavePath().getParent(), dataset.getName()+Exporter.WRK_FILE_EXTENSION);
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
		File f = new File(dataset.getSavePath().getParent(), dataset.getName()+Exporter.WRK_FILE_EXTENSION);
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
