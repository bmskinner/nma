package gui.actions;

import java.io.File;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.logging.Level;

import ij.io.SaveDialog;
import io.PopulationExporter;
import gui.MainWindow;
import gui.DatasetEvent.DatasetMethod;
import gui.InterfaceEvent.InterfaceMethod;
import analysis.AnalysisDataset;

public class SaveDatasetAction extends ProgressableAction {
	
	/**
	 * Constructor to save the current dataset. This gives programmatic
	 * access to saving to non-default locations, without needing to use
	 * the save dialog
	 * @param dataset the dataset to save
	 * @param saveFile the location to save to
	 * @param mw the main window, to access program logger
	 * @param doneSignal a latch to hold threads until the save is complete
	 */
	public SaveDatasetAction(AnalysisDataset dataset, File saveFile, MainWindow mw, CountDownLatch doneSignal) {
		super(dataset, "Saving dataset", "Error saving dataset", mw);
		setLatch(doneSignal);
		log(Level.FINEST, "Save dataset action created by explicit file location");
		
		worker = new PopulationExporter(dataset, saveFile, programLogger);
		worker.addPropertyChangeListener(this);
		worker.execute();	
	}	
	
	/**
	 * Default constructor to save the current dataset
	 * @param dataset the dataset to save
	 * @param mw the main window, to access program logger
	 * @param doneSignal a latch to hold threads until the save is complete
	 * @param chooseSaveLocation save to the default dataset save file, or choose another location
	 */
	public SaveDatasetAction(AnalysisDataset dataset, MainWindow mw, CountDownLatch doneSignal, boolean chooseSaveLocation) {
		super(dataset, "Saving dataset", "Error saving dataset", mw);
		setLatch(doneSignal);
		log(Level.FINEST, "Save dataset action created by default or manual file location");
		
		File saveFile = null;
		if(chooseSaveLocation){
			SaveDialog saveDialog = new SaveDialog("Save as...", dataset.getName(), ".nmd");
			
			String fileName = saveDialog.getFileName();
			String folderName = saveDialog.getDirectory();
			if(!fileName.isEmpty() && !folderName.isEmpty()){
				saveFile = new File(folderName+File.separator+fileName);
			} else {
				cancel();
				this.finished();
			}

		} else {
			saveFile = dataset.getSavePath();
		}
		log(Level.INFO, "Saving as "+saveFile.getAbsolutePath()+"...");
		worker = new PopulationExporter(dataset, saveFile, programLogger);
		worker.addPropertyChangeListener(this);
		worker.execute();	
		log(Level.FINE, "Save dataset action created");
		
	}
	
	
	
	@Override
	public void finished(){
		// Do not use super.finished(), or it will trigger another save action
		log(Level.FINE, "Save complete");
		cancel();		
		this.removeInterfaceEventListener(mw);
		this.removeDatasetEventListener(mw);
		log(Level.FINE, "Removing latch");
		this.countdownLatch();
		
	}

}
