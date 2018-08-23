/*******************************************************************************
 * Copyright (C) 2017 Ben Skinner
 * 
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 * 
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 * 
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.\
 *******************************************************************************/


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

import com.bmskinner.nuclear_morphology.components.workspaces.IWorkspace;
import com.bmskinner.nuclear_morphology.core.DatasetListManager;
import com.bmskinner.nuclear_morphology.core.EventHandler;
import com.bmskinner.nuclear_morphology.gui.actions.NewAnalysisAction;
import com.bmskinner.nuclear_morphology.gui.actions.PopulationImportAction;
import com.bmskinner.nuclear_morphology.gui.events.SignalChangeEvent;
import com.bmskinner.nuclear_morphology.gui.events.SignalChangeEventHandler;
import com.bmskinner.nuclear_morphology.io.Io.Importer;
import com.bmskinner.nuclear_morphology.io.WorkspaceImporter;
import com.bmskinner.nuclear_morphology.logging.Loggable;

@SuppressWarnings("serial")
public class MainDragAndDropTarget extends DropTarget implements Loggable {

	SignalChangeEventHandler sh = new SignalChangeEventHandler(this);

    public MainDragAndDropTarget(EventHandler eh) {
        super();
        sh.addListener(eh);
    }

    @Override
    public synchronized void drop(DropTargetDropEvent dtde) {

        try {
            dtde.acceptDrop(DnDConstants.ACTION_COPY_OR_MOVE);
            Transferable t = dtde.getTransferable();

            List<File> fileList = new ArrayList<File>();

            // Check that what was provided is a list
            if (t.getTransferData(DataFlavor.javaFileListFlavor) instanceof List<?>) {

                // Check that what is in the list is files
                List<?> tempList = (List<?>) t.getTransferData(DataFlavor.javaFileListFlavor);
                for (Object o : tempList) {

                    if (o instanceof File) {
                        fileList.add((File) o);
                    }
                }

                // Open the files - we process *.nmd, *.bak and *.wrk files
                
                for (File f : fileList) {
                    fine("Checking dropped file");
                    if (f.getName().endsWith(Importer.SAVE_FILE_EXTENSION) 
                            || f.getName().endsWith(Importer.BACKUP_FILE_EXTENSION)) {
                        sh.fireSignalChangeEvent(SignalChangeEvent.IMPORT_DATASET_PREFIX + f.getAbsolutePath());

                    }
                    
                    if (f.getName().endsWith(Importer.WRK_FILE_EXTENSION)) {
                    	sh.fireSignalChangeEvent(SignalChangeEvent.IMPORT_WORKSPACE_PREFIX+f.getAbsolutePath());

                    }

                    if (f.isDirectory()) {
                    	sh.fireSignalChangeEvent(SignalChangeEvent.NEW_ANALYSIS_PREFIX+f.getAbsolutePath());

                    }

                }
            }

        } catch (UnsupportedFlavorException e) {
            error("Error in DnD", e);
        } catch (IOException e) {
            error("IO error in DnD", e);
        }

    }

}
