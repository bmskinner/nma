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
package com.bmskinner.nuclear_morphology.gui.actions;

import java.io.File;
import java.util.ArrayList;
import java.util.List;
import java.util.logging.Logger;

import org.eclipse.jdt.annotation.NonNull;

import com.bmskinner.nuclear_morphology.components.workspaces.IWorkspace;
import com.bmskinner.nuclear_morphology.core.EventHandler;
import com.bmskinner.nuclear_morphology.core.InputSupplier.RequestCancelledException;
import com.bmskinner.nuclear_morphology.gui.ProgressBarAcceptor;
import com.bmskinner.nuclear_morphology.io.Io;
import com.bmskinner.nuclear_morphology.io.WorkspaceExporter;
import com.bmskinner.nuclear_morphology.utility.FileUtils;

/**
 * Action to export workspaces
 * @author bms41
 * @since 1.13.4
 *
 */
public class ExportWorkspaceAction extends VoidResultAction {
	
	private static final Logger LOGGER = Logger.getLogger(Logger.GLOBAL_LOGGER_NAME);

    private static final String PROGRESS_LBL = "Saving workspace";

    private final List<IWorkspace> workspaces = new ArrayList<>();

    public ExportWorkspaceAction(@NonNull final IWorkspace workspace, @NonNull final ProgressBarAcceptor acceptor, @NonNull final EventHandler eh) {
        super(PROGRESS_LBL, acceptor, eh);
        workspaces.add(workspace);
    }
    
    public ExportWorkspaceAction(@NonNull final List<IWorkspace> list, @NonNull final ProgressBarAcceptor acceptor, @NonNull final EventHandler eh) {
        super(PROGRESS_LBL, acceptor, eh);
        workspaces.addAll(list);
    }

    @Override
    public void run() {
    	WorkspaceExporter exp = WorkspaceExporter.createExporter();
    	for(IWorkspace w : workspaces) {
    	 LOGGER.info("Saving workspace "+w.getName()+"...");
            if(w.getSaveFile()==null) {
                try {
                	File defaultFolder = w.getFiles().size()>0 ? FileUtils.commonPathOfFiles(w.getFiles()) : null;
                    File f = eh.getInputSupplier().requestFileSave(defaultFolder, w.getName(), Io.WRK_FILE_EXTENSION_NODOT);
                    w.setSaveFile(f);
                } catch(RequestCancelledException e) {
                    continue;
                }
            }
            exp.exportWorkspace(w);
    	}
        this.cancel();
    }
}
