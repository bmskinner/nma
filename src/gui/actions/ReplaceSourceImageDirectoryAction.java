package gui.actions;

import java.io.File;
import java.util.logging.Level;

import analysis.AnalysisDataset;
import gui.MainWindow;
import ij.io.DirectoryChooser;
import io.PopulationImporter;

public class ReplaceSourceImageDirectoryAction extends ProgressableAction {

	public ReplaceSourceImageDirectoryAction(AnalysisDataset dataset, MainWindow mw) {
		super(dataset, "Replacing images", mw);
		this.cooldown();

		try{

			if(!dataset.hasMergeSources()){

				DirectoryChooser localOpenDialog = new DirectoryChooser("Select new directory of images...");
				String folderName = localOpenDialog.getDirectory();

				if(folderName!=null) { 


					File newFolder = new File(folderName);

					programLogger.log(Level.INFO, "Updating folder to "+folderName );

					PopulationImporter.updateSourceImageDirectory(newFolder, dataset);
					finished();

				} else {
					programLogger.log(Level.INFO, "Update cancelled");
					cancel();
				}
			}else {
				programLogger.log(Level.WARNING, "Dataset is a merge; cancelling");
				cancel();
			}

		} catch(Exception e){
			programLogger.log(Level.SEVERE, "Error in folder update: "+e.getMessage(), e);
		}


	}
	
	@Override
	public void finished(){
		// Do not use super.finished(), or it will trigger another save action
		log(Level.FINE, "Folder update complete");
		cancel();		
		this.removeInterfaceEventListener(mw);
		this.removeDatasetEventListener(mw);		
	}
}
