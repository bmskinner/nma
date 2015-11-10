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