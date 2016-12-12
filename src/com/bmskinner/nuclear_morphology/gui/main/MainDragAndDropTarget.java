package com.bmskinner.nuclear_morphology.gui.main;

import java.awt.datatransfer.DataFlavor;
import java.awt.datatransfer.Transferable;
import java.awt.datatransfer.UnsupportedFlavorException;
import java.awt.dnd.DnDConstants;
import java.awt.dnd.DropTarget;
import java.awt.dnd.DropTargetDropEvent;
import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import com.bmskinner.nuclear_morphology.analysis.IWorkspace;
import com.bmskinner.nuclear_morphology.gui.MainWindow;
import com.bmskinner.nuclear_morphology.gui.actions.NewAnalysisAction;
import com.bmskinner.nuclear_morphology.gui.actions.PopulationImportAction;
import com.bmskinner.nuclear_morphology.io.WorkspaceImporter;
import com.bmskinner.nuclear_morphology.logging.Loggable;
import com.bmskinner.nuclear_morphology.utility.Constants;

@SuppressWarnings("serial")
public class MainDragAndDropTarget extends DropTarget implements Loggable {
	
	private MainWindow mw;
	public MainDragAndDropTarget(MainWindow target){
		super();
		mw = target;
	}
	
	@Override
    public synchronized void drop(DropTargetDropEvent dtde) {
		
		try {
			dtde.acceptDrop(DnDConstants.ACTION_COPY_OR_MOVE);
			Transferable t = dtde.getTransferable();
			
			List<File> fileList = new ArrayList<File>();
			
			// Check that what was provided is a list
			if(t.getTransferData(DataFlavor.javaFileListFlavor) instanceof List<?>){
				
				// Check that what is in the list is files
				List<?> tempList = (List<?>) t.getTransferData(DataFlavor.javaFileListFlavor);
				for(Object o : tempList){
					
					if(o instanceof File){
						fileList.add( (File) o);
					}
				}
				
				// Open the files - we process only *.nmd and *.wrk files

				for(File f : fileList){
					if(f.getName().endsWith(Constants.SAVE_FILE_EXTENSION)){
						fine("File is nmd");
						receiveDatasetFile(f);
					} 
					
					if(f.getName().endsWith(Constants.WRK_FILE_EXTENSION)){	
						fine("File is wrk");
						receiveWorkspaceFile(f);
					} 
					
					if(f.isDirectory()){	
						receiveFolder(f);
					}

				}
			}
			
		} catch (UnsupportedFlavorException e) {
			error("Error in DnD", e);
		} catch (IOException e) {
			error("IO error in DnD", e);
		}
       
    }
	
	private void receiveFolder(final File f){
		// Pass to new analysis
		Runnable task = new NewAnalysisAction(mw, f);
		task.run();
	}
	
	private void receiveWorkspaceFile(final File f){
		finer("Opening workgroup file "+f.getAbsolutePath());
		
		IWorkspace w = new WorkspaceImporter(f).importWorkspace();
		
		mw.addWorkspace(w);
		
		for(File dataFile : w.getFiles()){
			receiveDatasetFile(dataFile);
		}

	}
	
	private void receiveDatasetFile(final File f){
		fine("Opening file "+f.getAbsolutePath());

		Runnable task = new PopulationImportAction(mw, f);
		task.run();
	}
}
