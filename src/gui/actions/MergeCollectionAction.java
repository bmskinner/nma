/*******************************************************************************
 *  	Copyright (C) 2015 Ben Skinner
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

import java.io.File;
import java.util.List;

import analysis.AnalysisDataset;
import analysis.nucleus.DatasetMerger;
import analysis.nucleus.MorphologyAnalysis;

public class MergeCollectionAction extends ProgressableAction {

	public MergeCollectionAction(List<AnalysisDataset> datasets, File saveFile, MainWindow mw) {
		super(null, "Merging", "Error merging", mw);

		worker = new DatasetMerger(datasets, DatasetMerger.DATASET_MERGE, saveFile, programLogger);
		worker.addPropertyChangeListener(this);
		worker.execute();	
	}

	@Override
	public void finished(){

		List<AnalysisDataset> datasets = ((DatasetMerger) worker).getResults();

		if(datasets.size()==0 || datasets==null){
			this.cancel();
		} else {

			int flag = MainWindow.ADD_POPULATION;
			flag |= MainWindow.SAVE_DATASET;
			new MorphologyAnalysisAction(datasets, MorphologyAnalysis.MODE_NEW, flag, mw);
			this.cancel();
		}
	}
}