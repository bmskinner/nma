package gui.actions;

import java.io.File;
import java.util.concurrent.CountDownLatch;
import java.util.logging.Level;

import javax.swing.JFileChooser;
import javax.swing.filechooser.FileNameExtensionFilter;

import utility.Constants;
import analysis.AnalysisDataset;
import analysis.nucleus.CellRelocator;
import gui.MainWindow;
import gui.ThreadManager;
import gui.InterfaceEvent.InterfaceMethod;

public class RelocateFromFileAction extends ProgressableAction {

	public RelocateFromFileAction(AnalysisDataset dataset, MainWindow mw, CountDownLatch latch) {
		super(dataset, "Relocating cells", mw);
		this.setLatch(latch);
		cooldown();
		
		/*
		 * Get the file to search
		 */

		File file = selectFile();
		if(file!=null){
			
			/*
			 * Make the worker
			 */
			
			worker = new CellRelocator(dataset, file);
			worker.addPropertyChangeListener(this);
			
			this.setProgressMessage("Locating cells...");
			log(Level.INFO, "Locating cells...");
			ThreadManager.getInstance().submit(worker);
		} else {
			log(Level.FINE, "Cancelled");
			cancel();
		}
		
		
	}
	
	@Override
	public void finished(){
		log(Level.FINE, "Firing refresh of populations");
		fireInterfaceEvent(InterfaceMethod.REFRESH_POPULATIONS);
		this.countdownLatch();
		log(Level.INFO, "Cells added as new child dataset");
		super.finished();		
	}
	
	/**
	 * Get the file to be loaded
	 * @return
	 */
	private File selectFile(){

		FileNameExtensionFilter filter = new FileNameExtensionFilter("Remapping file", Constants.LOC_FILE_EXTENSION);
		
		File defaultDir = new File("J:\\Protocols\\Scripts and macros\\");
		JFileChooser fc = new JFileChooser("Select a file...");
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
	
	

}
