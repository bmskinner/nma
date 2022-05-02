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

import com.bmskinner.nma.gui.events.FileImportEventListener.FileImportEvent;
import com.bmskinner.nma.gui.events.UserActionController;
import com.bmskinner.nma.gui.events.UserActionEvent;
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

				for (File f : fileList) {
					LOGGER.fine("Checking dropped file");
					UserActionController.getInstance()
							.fileImportRequested(new FileImportEvent(this, f, null, null));

					if (f.isDirectory())
						UserActionController.getInstance().userActionEventReceived(
								new UserActionEvent(this,
										UserActionEvent.NEW_ANALYSIS_PREFIX + f.getAbsolutePath()));
				}
			}

		} catch (UnsupportedFlavorException e) {
			LOGGER.log(Loggable.STACK, "Error in DnD", e);
		} catch (IOException e) {
			LOGGER.log(Loggable.STACK, "IO error in DnD", e);
		}
	}
}
