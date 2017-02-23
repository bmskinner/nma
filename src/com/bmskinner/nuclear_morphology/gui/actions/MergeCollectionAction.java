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
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ExecutionException;

import com.bmskinner.nuclear_morphology.analysis.DatasetMergeMethod;
import com.bmskinner.nuclear_morphology.analysis.DefaultAnalysisWorker;
import com.bmskinner.nuclear_morphology.analysis.IAnalysisMethod;
import com.bmskinner.nuclear_morphology.analysis.IAnalysisResult;
import com.bmskinner.nuclear_morphology.components.IAnalysisDataset;
import com.bmskinner.nuclear_morphology.gui.MainWindow;
import com.bmskinner.nuclear_morphology.gui.ThreadManager;
import com.bmskinner.nuclear_morphology.gui.dialogs.DatasetMergingDialog;
import com.bmskinner.nuclear_morphology.io.Importer;

import ij.io.SaveDialog;

/**
 * Carry out a merge of datasets
 * @author ben
 *
 */
public class MergeCollectionAction extends ProgressableAction {
	
	private final List<IAnalysisDataset> datasets;

	public MergeCollectionAction(final List<IAnalysisDataset> datasets, MainWindow mw) {
		super("Merging", mw);
		this.datasets = datasets;

	}
	
	@Override
	public void run(){

		if( ! checkDatasetsMergable(datasets)){
			this.cancel();
			
		} else {

			SaveDialog saveDialog = new SaveDialog("Save merged dataset as...", "Merge_of_datasets", Importer.SAVE_FILE_EXTENSION);

			String fileName   = saveDialog.getFileName();
			String folderName = saveDialog.getDirectory();

			if(fileName!=null && folderName!=null){
				File saveFile = new File(folderName+File.separator+fileName);

				// Check for signals in >1 dataset
				int signals=0;
				for(IAnalysisDataset d : datasets){
					if(d.getCollection().getSignalManager().hasSignals()){
						signals++;
					}
				}

				IAnalysisMethod m;
				
				if(signals>1){

					DatasetMergingDialog dialog = new DatasetMergingDialog(datasets);

					Map<UUID, Set<UUID>> pairs = dialog.getPairedSignalGroups();



					if(pairs.keySet().size()!=0){
						finest("Found paired signal groups");
						// User decided to merge signals
						m = new DatasetMergeMethod(datasets, saveFile, pairs);
						
//						worker = new DatasetMerger(datasets, saveFile, pairs);
					} else {
						finest("No paired signal groups");
						m = new DatasetMergeMethod(datasets, saveFile);
//						worker = new DatasetMerger(datasets, saveFile);
					}
				} else {
					finest("No signal groups to merge");
					// no signals to merge
//					worker = new DatasetMerger(datasets, saveFile);
					m = new DatasetMergeMethod(datasets, saveFile);
				}

				worker = new DefaultAnalysisWorker(m, 100);
				worker.addPropertyChangeListener(this);
				ThreadManager.getInstance().submit(worker);
			} else {
				this.cancel();
			}
		}
	}
	
	
	/**
	 * Check datasets are valid to be merged
	 * @param datasets
	 * @return
	 */
	private boolean checkDatasetsMergable(List<IAnalysisDataset> datasets){
		
		if(datasets.size()==2){
			
			if(datasets.get(0).hasChild(datasets.get(1)) ||
				datasets.get(1).hasChild(datasets.get(0))
					
					){
				warn("No. Merging parent and child is silly.");
				return false;
			}
			
		}
		return true;
		
	}

	@Override
	public void finished(){

		IAnalysisResult r;
		try {
			r = worker.get();
		} catch (InterruptedException | ExecutionException e) {
			warn("Error merging datasets");
			stack("Error merging datasets", e);
			this.cancel();
			return;
		}
		List<IAnalysisDataset> datasets = r.getDatasets();
		
		if(datasets==null){
			this.cancel();
			return;
		}

		if(datasets.size()==0){
			this.cancel();
			return;
		}

		int flag = ProgressableAction.ADD_POPULATION;
		flag |= ProgressableAction.ASSIGN_SEGMENTS;
		flag |= ProgressableAction.SAVE_DATASET;
		RunProfilingAction pr = new RunProfilingAction(datasets, flag, mw);
		ThreadManager.getInstance().execute(pr);

		this.cancel();
	}
}