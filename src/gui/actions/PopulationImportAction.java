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
package gui.actions;

import java.io.File;
import java.util.ArrayList;
import java.util.List;
import java.util.logging.Level;

import javax.swing.JFileChooser;
import javax.swing.filechooser.FileNameExtensionFilter;

import analysis.AnalysisDataset;
import gui.DatasetEvent.DatasetMethod;
import gui.InterfaceEvent.InterfaceMethod;
import gui.MainWindow;
import gui.ThreadManager;
import io.PopulationImportWorker;

/**
 * Call an open dialog to choose a saved .nbd dataset. The opened dataset
 * will be added to the bottom of the dataset list.
 */
public class PopulationImportAction extends ProgressableAction {

	/**
	 * Refold the given selected dataset
	 */
	
	public PopulationImportAction(MainWindow mw) {
		super("Opening file", mw);
		cooldown();
		
		File file = selectFile();
		processFile(file);		
	}
	
	public PopulationImportAction(MainWindow mw, File file) {
		super("Opening file", mw);
		cooldown();
		processFile(file);
		
	}
	
	private void processFile(File file){
		if(file!=null){
			worker = new PopulationImportWorker(file);
			worker.addPropertyChangeListener(this);
			
			this.setProgressMessage("Opening file...");
			log(Level.FINE, "Opening dataset...");
			ThreadManager.getInstance().submit(worker);
		} else {
			log(Level.FINE, "Open cancelled");
			cancel();
		}
	}
	
	/**
	 * Get the file to be loaded
	 * @return
	 */
	private File selectFile(){

		FileNameExtensionFilter filter = new FileNameExtensionFilter("Nuclear morphology datasets", "nmd");
		
		File defaultDir = new File("J:\\Protocols\\Scripts and macros\\");
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
		log(Level.FINE, "Selected file: "+file.getAbsolutePath());
		return file;
	}
		

	@Override
	public void finished(){
//		setProgressBarVisible(false);
		AnalysisDataset dataset = ((PopulationImportWorker) worker).getLoadedDataset();
		log(Level.FINE, "Opened dataset");

		List<AnalysisDataset> list = new ArrayList<AnalysisDataset>(0);
		list.add(dataset);
		log(Level.FINE, "Firing add signal");
		fireDatasetEvent(DatasetMethod.ADD_DATASET, list);
		
		/*
		 * Code after this point is never reached - check thread 16
		 */
//		
//		fireDatasetEvent(DatasetMethod.RECALCULATE_CACHE, list);
//		fireDatasetEvent(DatasetMethod.SELECT_DATASETS, list);
//		this.cancel();
		super.finished();		
	}

}
