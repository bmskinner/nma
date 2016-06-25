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

import gui.MainWindow;
import gui.ThreadManager;
import gui.dialogs.DatasetMergingDialog;
import ij.io.SaveDialog;

import java.io.File;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

import utility.Constants;
import analysis.AnalysisDataset;
import analysis.nucleus.DatasetMerger;

public class MergeCollectionAction extends ProgressableAction {

	public MergeCollectionAction(List<AnalysisDataset> datasets, MainWindow mw) {
		super("Merging", mw);

		SaveDialog saveDialog = new SaveDialog("Save merged dataset as...", "Merge_of_datasets", Constants.SAVE_FILE_EXTENSION);

		String fileName   = saveDialog.getFileName();
		String folderName = saveDialog.getDirectory();
		
		if(fileName!=null && folderName!=null){
			File saveFile = new File(folderName+File.separator+fileName);
			
			// Check for signals in >1 dataset
			int signals=0;
			for(AnalysisDataset d : datasets){
				if(d.getCollection().getSignalManager().hasSignals()){
					signals++;
				}
			}
			
			if(signals>1){
			
				DatasetMergingDialog dialog = new DatasetMergingDialog(datasets);
				
				Map<UUID, Set<UUID>> pairs = dialog.getPairedSignalGroups();
				
				
				
				if(pairs.keySet().size()!=0){
					finest("Found paired signal groups");
					// User decided to merge signals
					worker = new DatasetMerger(datasets, saveFile, pairs);
				} else {
					finest("No paired signal groups");
					worker = new DatasetMerger(datasets, saveFile);
				}
			} else {
				finest("No signal groups to merge");
				// no signals to merge
				worker = new DatasetMerger(datasets, saveFile);
			}
			
			
			worker.addPropertyChangeListener(this);
			ThreadManager.getInstance().submit(worker);
		} else {
			this.cancel();
		}

	}

	@Override
	public void finished(){

		List<AnalysisDataset> datasets = ((DatasetMerger) worker).getResults();

		if(datasets.size()==0 || datasets==null){
			this.cancel();
		} else {

			int flag = MainWindow.ADD_POPULATION;
			flag |= MainWindow.ASSIGN_SEGMENTS;
			flag |= MainWindow.SAVE_DATASET;
			new RunProfilingAction(datasets, flag, mw);
			this.cancel();
		}
	}
}