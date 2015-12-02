package gui.actions;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

import io.PopulationExporter;
import gui.MainWindow;
import gui.DatasetEvent.DatasetMethod;
import gui.InterfaceEvent.InterfaceMethod;
import analysis.AnalysisDataset;

public class SaveDatasetAction extends ProgressableAction {

	/**
	 * Default constructor to save the current dataset
	 * @param dataset
	 * @param barMessage
	 * @param errorMessage
	 * @param mw
	 */
	public SaveDatasetAction(AnalysisDataset dataset, String barMessage,
			String errorMessage, MainWindow mw) {
		super(dataset, barMessage, errorMessage, mw);
		
		worker = new PopulationExporter(dataset, programLogger);
		worker.addPropertyChangeListener(this);
		worker.execute();	
	}
	
	/**
	 * Constructor allowing a specific save path
	 * @param dataset
	 * @param saveFile the file to save the dataset to
	 * @param barMessage
	 * @param errorMessage
	 * @param mw
	 */
	public SaveDatasetAction(AnalysisDataset dataset, File saveFile, String barMessage,
			String errorMessage, MainWindow mw) {
		super(dataset, barMessage, errorMessage, mw);
		
		worker = new PopulationExporter(dataset, saveFile, programLogger);
		worker.addPropertyChangeListener(this);
		worker.execute();	
	}	
	
	@Override
	public void finished(){
		// Do not use super.finished(), or it will trigger another save action
		this.removeInterfaceEventListener(mw);
		this.removeDatasetEventListener(mw);
		cancel();		
	}

}
