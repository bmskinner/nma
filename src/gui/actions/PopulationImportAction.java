package gui.actions;

import java.io.File;
import java.util.ArrayList;
import java.util.List;
import java.util.logging.Level;

import javax.swing.JFileChooser;
import javax.swing.filechooser.FileNameExtensionFilter;

import analysis.AnalysisDataset;
import gui.DatasetEvent.DatasetMethod;
import gui.MainWindow;
import io.PopulationImportWorker;


public class PopulationImportAction extends ProgressableAction {

	/**
	 * Refold the given selected dataset
	 */
	
	public PopulationImportAction(MainWindow mw) {
		super("Opening file", mw);
		cooldown();
		
		File file = selectFile();
		if(file!=null){
			worker = new PopulationImportWorker(programLogger, file);
			worker.addPropertyChangeListener(this);
			
			this.setProgressMessage("Opening file...");
			programLogger.log(Level.FINE, "Opening dataset...");
			worker.execute();
		} else {
			programLogger.log(Level.FINE, "Open cancelled");
			cancel();
		}
		
	}
	
	/**
	 * Get the file to be loaded
	 * @return
	 */
	public File selectFile(){

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
		return file;
	}
		

	@Override
	public void finished(){
		
		AnalysisDataset dataset = ((PopulationImportWorker) worker).getLoadedDataset();
		programLogger.log(Level.FINE, "Opened dataset");

		List<AnalysisDataset> list = new ArrayList<AnalysisDataset>(0);
		list.add(dataset);
		programLogger.log(Level.FINE, "Firing add signal");
		fireDatasetEvent(DatasetMethod.ADD_DATASET, list);
		super.finished();		
	}

}
