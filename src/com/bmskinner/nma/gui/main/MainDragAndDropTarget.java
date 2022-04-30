/*******************************************************************************
 * Copyright (C) 2018 Ben Skinner
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
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 ******************************************************************************/
package com.bmskinner.nma.gui.main;

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
import java.util.logging.Logger;

import com.bmskinner.nma.gui.events.UserActionEvent;
import com.bmskinner.nma.gui.events.revamp.UserActionController;
import com.bmskinner.nma.io.Io.Importer;
import com.bmskinner.nma.logging.Loggable;

@SuppressWarnings("serial")
public class MainDragAndDropTarget extends DropTarget {

	private static final Logger LOGGER = Logger.getLogger(MainDragAndDropTarget.class.getName());

	public MainDragAndDropTarget() {
		super();
	}

	@Override
	public synchronized void drop(DropTargetDropEvent dtde) {

		try {
			dtde.acceptDrop(DnDConstants.ACTION_COPY_OR_MOVE);
			Transferable t = dtde.getTransferable();

			List<File> fileList = new ArrayList<>();

			// Check that what was provided is a list
			if (t.getTransferData(DataFlavor.javaFileListFlavor) instanceof List<?>) {

				// Check that what is in the list is files
				List<?> tempList = (List<?>) t.getTransferData(DataFlavor.javaFileListFlavor);
				for (Object o : tempList) {
					if (o instanceof File file)
						fileList.add(file);
				}

				// Open the files - we process *.nmd, *.bak, *.wrk,and *.xml files

				for (File f : fileList) {
					LOGGER.fine("Checking dropped file");
					if (f.getName().endsWith(Importer.SAVE_FILE_EXTENSION)
							|| f.getName().endsWith(Importer.BACKUP_FILE_EXTENSION))
						UserActionController.getInstance().userActionEventReceived(
								new UserActionEvent(this, UserActionEvent.IMPORT_DATASET_PREFIX + f.getAbsolutePath()));

					if (f.getName().endsWith(Importer.WRK_FILE_EXTENSION))
						UserActionController.getInstance().userActionEventReceived(new UserActionEvent(this,
								UserActionEvent.IMPORT_WORKSPACE_PREFIX + f.getAbsolutePath()));

					if (f.getName().endsWith(Importer.XML_FILE_EXTENSION))
						UserActionController.getInstance().userActionEventReceived(new UserActionEvent(this,
								UserActionEvent.IMPORT_WORKFLOW_PREFIX + f.getAbsolutePath()));

					if (f.isDirectory())
						UserActionController.getInstance().userActionEventReceived(
								new UserActionEvent(this, UserActionEvent.NEW_ANALYSIS_PREFIX + f.getAbsolutePath()));
				}
			}

		} catch (UnsupportedFlavorException e) {
			LOGGER.log(Loggable.STACK, "Error in DnD", e);
		} catch (IOException e) {
			LOGGER.log(Loggable.STACK, "IO error in DnD", e);
		}
	}
}
