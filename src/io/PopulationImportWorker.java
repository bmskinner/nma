package io;

import java.io.File;
import java.util.ArrayList;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;

import analysis.AnalysisDataset;
import analysis.AnalysisWorker;
import utility.Constants;

public class PopulationImportWorker extends AnalysisWorker {
	
	private File file;
	private AnalysisDataset dataset;
	
	public PopulationImportWorker(Logger programLogger, File f){
		super(null, programLogger);
		this.file = f;
		this.setProgressTotal(1);
	}
	
	public AnalysisDataset getLoadedDataset(){
		return this.dataset;
	}
	
	@Override
	protected Boolean doInBackground() {
		
		try {
			dataset = PopulationImporter.readDataset(file, programLogger);
			
			programLogger.log(Level.FINE, "Read dataset");
			if(checkVersion( dataset.getVersion() )){

				programLogger.log(Level.FINE, "Version check OK");
				dataset.setRoot(true);

				
				// update the log file to the same folder as the dataset
				File logFile = new File(file.getParent()
						+File.separator
						+file.getName().replace(Constants.SAVE_FILE_EXTENSION, Constants.LOG_FILE_EXTENSION));
				
				dataset.getCollection().setDebugFile(logFile);
				programLogger.log(Level.FINE, "Updated log file location");
				
//				publish(1);
				return true;
				
			} else {
				programLogger.log(Level.SEVERE, "Unable to open dataset version: "+ dataset.getVersion());
				return false;
			}
			
			
		} catch(Exception e){
			logError("Unable to open file", e);
			return false;
		}
	}
	
	/**
	 * Check a version string to see if the program will be able to open a 
	 * dataset. The major version must be the same, while the revision of the
	 * dataset must be equal to or greater than the program revision. Bugfixing
	 * versions are not checked for.
	 * @param version
	 * @return a pass or fail
	 */
	public boolean checkVersion(String version){
		boolean ok = true;
		
		if(version==null){ // allow for debugging, but warn
			programLogger.log(Level.WARNING, "No version info found: functions may not work as expected");
			return true;
		}
		
		String[] parts = version.split("\\.");
		
		// major version MUST be the same
		if(Integer.valueOf(parts[0])!=Constants.VERSION_MAJOR){
			ok = false;
		}
		// dataset revision should be equal or greater to program
		if(Integer.valueOf(parts[1])<Constants.VERSION_REVISION){
			programLogger.log(Level.WARNING, "Dataset was created with an older version of the program");
			programLogger.log(Level.WARNING, "Some functionality may not work as expected");
		}
		return ok;
	}
	
	

}
