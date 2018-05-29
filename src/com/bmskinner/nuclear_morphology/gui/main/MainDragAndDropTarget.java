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
import com.bmskinner.nuclear_morphology.gui.MainWindow;
import com.bmskinner.nuclear_morphology.gui.actions.NewAnalysisAction;
import com.bmskinner.nuclear_morphology.gui.actions.PopulationImportAction;
import com.bmskinner.nuclear_morphology.io.Io.Importer;
import com.bmskinner.nuclear_morphology.io.WorkspaceImporter;
import com.bmskinner.nuclear_morphology.logging.Loggable;
import com.bmskinner.nuclear_morphology.main.DatasetListManager;

@SuppressWarnings("serial")
public class MainDragAndDropTarget extends DropTarget implements Loggable {

    private MainWindow mw;

    public MainDragAndDropTarget(MainWindow target) {
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
                    if (f.getName().endsWith(Importer.SAVE_FILE_EXTENSION) 
                            || f.getName().endsWith(Importer.BACKUP_FILE_EXTENSION))
                        receiveDatasetFile(f);

                    if (f.getName().endsWith(Importer.WRK_FILE_EXTENSION))
                        receiveWorkspaceFile(f);

                    if (f.isDirectory())
                        receiveFolder(f);

                }
            }

        } catch (UnsupportedFlavorException e) {
            error("Error in DnD", e);
        } catch (IOException e) {
            error("IO error in DnD", e);
        }

    }

    private void receiveFolder(final File f) {
        // Pass to new analysis
        Runnable task = new NewAnalysisAction(mw, f);
        task.run();
    }

    private void receiveWorkspaceFile(final File f) {
        finer("Opening workgroup file " + f.getAbsolutePath());

        IWorkspace w = WorkspaceImporter.createImporter(f).importWorkspace();
        DatasetListManager.getInstance().addWorkspace(w);

        for (File dataFile : w.getFiles()) {
            receiveDatasetFile(dataFile);
        }

    }

    private void receiveDatasetFile(final File f) {
        fine("Opening file " + f.getAbsolutePath());

        Runnable task = new PopulationImportAction(mw, f);
        task.run();
    }
}
