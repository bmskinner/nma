/*******************************************************************************
 *  	Copyright (C) 2016 Ben Skinner
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
package com.bmskinner.nuclear_morphology.gui.actions;

import java.io.File;
import java.util.concurrent.ExecutionException;

import javax.swing.JFileChooser;
import javax.swing.filechooser.FileNameExtensionFilter;

import com.bmskinner.nuclear_morphology.analysis.DefaultAnalysisWorker;
import com.bmskinner.nuclear_morphology.analysis.IAnalysisMethod;
import com.bmskinner.nuclear_morphology.analysis.IAnalysisResult;
import com.bmskinner.nuclear_morphology.components.IAnalysisDataset;
import com.bmskinner.nuclear_morphology.gui.DatasetEvent;
import com.bmskinner.nuclear_morphology.gui.GlobalOptions;
import com.bmskinner.nuclear_morphology.gui.MainWindow;
import com.bmskinner.nuclear_morphology.gui.ThreadManager;
import com.bmskinner.nuclear_morphology.io.DatasetImportMethod;
import com.bmskinner.nuclear_morphology.io.Importer;

/**
 * Call an open dialog to choose a saved .nbd dataset. The opened dataset
 * will be added to the bottom of the dataset list.
 */
public class PopulationImportAction extends VoidResultAction {

	private final File file;
	private static final String PROGRESS_BAR_LABEL = "Opening file...";
	private static final String DEFAULT_FILE_TYPE  = "Nuclear morphology datasets";
	
	
	/**
	 * Create an import action for the given main window.
	 * This will create a dialog asking for the file to open.
	 * @param mw the main window to which a progress bar will be attached
	 */
	public PopulationImportAction(MainWindow mw) {
		super(PROGRESS_BAR_LABEL, mw);		
		file = selectFile();	
	}
	
	/**
	 * Create an import action for the given main window.
	 * Specify the file to be opened.
	 * @param mw the main window to which a progress bar will be attached
	 * @param file the dataset file to open 
	 */
	public PopulationImportAction(MainWindow mw, File file) {
		super(PROGRESS_BAR_LABEL, mw);
		this.file = file;
	}
	
	@Override
	public void run(){
		setProgressBarIndeterminate();		
		fine("Running dataset open action");
		if(file!=null){
			
			IAnalysisMethod m = new DatasetImportMethod(file);
			worker = new DefaultAnalysisWorker(m);

			worker.addPropertyChangeListener(this);
			
			setProgressMessage(PROGRESS_BAR_LABEL);

			ThreadManager.getInstance().submit(worker);
		} else {
			fine("Open cancelled");
			cancel();
		}
	}

	
	/**
	 * Get the file to be loaded
	 * @return
	 */
	private File selectFile(){

		FileNameExtensionFilter filter = new FileNameExtensionFilter(DEFAULT_FILE_TYPE, Importer.SAVE_FILE_EXTENSION_NODOT);
		
		File defaultDir = GlobalOptions.getInstance().getDefaultDir();//new File("J:\\Protocols\\Scripts and macros\\");
		JFileChooser fc = new JFileChooser("Select a saved dataset...");
		if(defaultDir.exists()){
			fc = new JFileChooser(defaultDir);
		}
		fc.setFileFilter(filter);

		int returnVal = fc.showOpenDialog(fc);
		if (returnVal != 0)	{
			return null;
		}
		File file = fc.getSelectedFile();

		if(file.isDirectory()){
			return null;
		}
		fine("Selected file: "+file.getAbsolutePath());
		return file;
	}
		

	@Override
	public void finished(){
		setProgressBarVisible(false);
		IAnalysisDataset dataset;
		try {
			
			IAnalysisResult r = worker.get();
			
			dataset = r.getFirstDataset();
			
			// Save newly converted datasets
			if(r.getBoolean(DatasetImportMethod.WAS_CONVERTED_BOOL)){
				fireDatasetEvent(DatasetEvent.SAVE, dataset);
			}

		} catch (InterruptedException e) {
			warn("Unable to open file '"+file.getAbsolutePath()+"': "+e.getMessage());
			stack("Unable to open '"+file.getAbsolutePath()+"': ", e);
			return;
		} catch (ExecutionException e) {
			warn("Unable to open '"+file.getAbsolutePath()+"': "+e.getMessage());
			stack("Unable to open '"+file.getAbsolutePath()+"': ", e);
			return;
		}
		fine("Opened dataset");

		fireDatasetEvent(DatasetEvent.ADD_DATASET, dataset);
		
		fine("Finishing action");

		super.finished();		
	}

}
