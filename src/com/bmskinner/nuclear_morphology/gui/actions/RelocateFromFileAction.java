package com.bmskinner.nuclear_morphology.gui.actions;

import java.io.File;
import java.util.concurrent.CountDownLatch;

import javax.swing.JFileChooser;
import javax.swing.filechooser.FileNameExtensionFilter;

import com.bmskinner.nuclear_morphology.analysis.DefaultAnalysisWorker;
import com.bmskinner.nuclear_morphology.analysis.IAnalysisMethod;
import com.bmskinner.nuclear_morphology.analysis.nucleus.CellRelocationMethod;
import com.bmskinner.nuclear_morphology.components.IAnalysisDataset;
import com.bmskinner.nuclear_morphology.components.options.IAnalysisOptions;
import com.bmskinner.nuclear_morphology.gui.InterfaceEvent.InterfaceMethod;
import com.bmskinner.nuclear_morphology.gui.MainWindow;
import com.bmskinner.nuclear_morphology.gui.ThreadManager;
import com.bmskinner.nuclear_morphology.io.Importer;

public class RelocateFromFileAction extends ProgressableAction {

	public RelocateFromFileAction(IAnalysisDataset dataset, MainWindow mw, CountDownLatch latch) {
		super(dataset, "Relocating cells", mw);
		this.setLatch(latch);
		setProgressBarIndeterminate();
		
		
		
		
	}
	
	@Override
	public void run(){
		/*
		 * Get the file to search
		 */

		File file = selectFile();
		if(file!=null){
			
			/*
			 * Make the worker
			 */
			IAnalysisMethod m = new CellRelocationMethod(dataset, file);
			worker = new DefaultAnalysisWorker(m);
			
//			worker = new CellRelocator(dataset, file);
			worker.addPropertyChangeListener(this);
			
			this.setProgressMessage("Locating cells...");
			log("Locating cells...");
			ThreadManager.getInstance().submit(worker);
		} else {
			fine( "Cancelled");
			cancel();
		}
	}
	
	@Override
	public void finished(){
		fine("Firing refresh of populations");
		fireInterfaceEvent(InterfaceMethod.REFRESH_POPULATIONS);
		this.countdownLatch();
		super.finished();		
	}
	
	/**
	 * Get the file to be loaded
	 * @return
	 */
	private File selectFile(){

		FileNameExtensionFilter filter = new FileNameExtensionFilter("Remapping file", Importer.LOC_FILE_EXTENSION);
		File defaultDir = dataset.getAnalysisOptions().getDetectionOptions(IAnalysisOptions.NUCLEUS).getFolder();
//		File defaultDir = new File("J:\\Protocols\\Scripts and macros\\");
		JFileChooser fc = new JFileChooser("Select a file...");
		if(defaultDir.exists()){
			fc = new JFileChooser(defaultDir);
		} else {
			fc = new JFileChooser( (File) null);
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
