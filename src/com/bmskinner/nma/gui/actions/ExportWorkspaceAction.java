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
package com.bmskinner.nma.gui.actions;

import java.io.File;
import java.util.ArrayList;
import java.util.List;
import java.util.logging.Logger;

import org.eclipse.jdt.annotation.NonNull;

import com.bmskinner.nma.components.workspaces.IWorkspace;
import com.bmskinner.nma.core.InputSupplier.RequestCancelledException;
import com.bmskinner.nma.gui.ProgressBarAcceptor;
import com.bmskinner.nma.io.Io;
import com.bmskinner.nma.io.WorkspaceExporter;
import com.bmskinner.nma.utility.FileUtils;

/**
 * Action to export workspaces
 * 
 * @author bms41
 * @since 1.13.4
 *
 */
public class ExportWorkspaceAction extends VoidResultAction {

	private static final Logger LOGGER = Logger.getLogger(ExportWorkspaceAction.class.getName());

	private static final String PROGRESS_LBL = "Saving workspace";

	private final List<IWorkspace> workspaces = new ArrayList<>();

	public ExportWorkspaceAction(@NonNull final IWorkspace workspace,
			@NonNull final ProgressBarAcceptor acceptor) {
		super(PROGRESS_LBL, acceptor);
		workspaces.add(workspace);
	}

	public ExportWorkspaceAction(@NonNull final List<IWorkspace> list,
			@NonNull final ProgressBarAcceptor acceptor) {
		super(PROGRESS_LBL, acceptor);
		workspaces.addAll(list);
	}

	@Override
	public void run() {
		for (IWorkspace w : workspaces) {

			if (w.getSaveFile() == null) {
				try {
					File defaultFolder = w.getFiles().size() > 0
							? FileUtils.commonPathOfFiles(w.getFiles())
							: null;
					File f = is.requestFileSave(defaultFolder, w.getName(),
							Io.WRK_FILE_EXTENSION_NODOT);
					w.setSaveFile(f);
				} catch (RequestCancelledException e) {
					continue;
				}
			}
			WorkspaceExporter.exportWorkspace(w);
			LOGGER.fine("Saved workspace '" + w.getName() + "'");
		}
		this.cancel();
	}
}
