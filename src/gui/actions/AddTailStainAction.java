package gui.actions;

import gui.MainWindow;
import gui.TailDetectionSettingsWindow;
import ij.io.DirectoryChooser;

import java.io.File;
import java.util.logging.Level;

import analysis.AnalysisDataset;
import analysis.tail.TubulinTailDetector;

public class AddTailStainAction extends ProgressableAction {

	public AddTailStainAction(AnalysisDataset dataset, MainWindow mw) {
		super(dataset, "Tail detection", "Error in tail detection", mw);
		try{
			
			TailDetectionSettingsWindow analysisSetup = new TailDetectionSettingsWindow(dataset.getAnalysisOptions(), programLogger);
			
			final int channel = analysisSetup.getChannel();
			
			DirectoryChooser openDialog = new DirectoryChooser("Select directory of tubulin images...");
			String folderName = openDialog.getDirectory();

			if(folderName==null){
				this.cancel();
				return; // user cancelled
			}

			final File folder =  new File(folderName);

			if(!folder.isDirectory() ){
				this.cancel();
				return;
			}
			if(!folder.exists()){
				this.cancel();
				return; // check folder is ok
			}

			worker = new TubulinTailDetector(dataset, folder, channel);
			worker.addPropertyChangeListener(this);
			this.setProgressMessage("Tail detection:"+dataset.getName());
			worker.execute();
		} catch(Exception e){
			this.cancel();
			programLogger.log(Level.SEVERE, "Error in tail analysis", e);

		}
	}
}