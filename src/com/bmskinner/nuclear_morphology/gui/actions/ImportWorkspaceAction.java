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
import java.io.IOException;
import java.util.logging.Logger;

import javax.swing.JFileChooser;
import javax.swing.filechooser.FileNameExtensionFilter;

import org.eclipse.jdt.annotation.NonNull;
import org.eclipse.jdt.annotation.Nullable;
import org.jdom2.JDOMException;

import com.bmskinner.nuclear_morphology.components.workspaces.IWorkspace;
import com.bmskinner.nuclear_morphology.core.DatasetListManager;
import com.bmskinner.nuclear_morphology.core.EventHandler;
import com.bmskinner.nuclear_morphology.core.GlobalOptions;
import com.bmskinner.nuclear_morphology.gui.ProgressBarAcceptor;
import com.bmskinner.nuclear_morphology.io.Io.Importer;
import com.bmskinner.nuclear_morphology.io.WorkspaceImporter;

public class ImportWorkspaceAction extends VoidResultAction {
	
	private static final Logger LOGGER = Logger.getLogger(ImportWorkspaceAction.class.getName());

    private final File          file;
    private static final @NonNull String PROGRESS_BAR_LABEL = "Opening workspace...";
    private static final String DEFAULT_FILE_TYPE  = "Nuclear morphology workspace";
    
    /**
     * Create an import action for the given main window. This will create a
     * dialog asking for the file to open.
     * 
     * @param mw the main window to which a progress bar will be attached
     */
    public ImportWorkspaceAction(@NonNull final ProgressBarAcceptor acceptor, @NonNull final EventHandler eh) {
        this(acceptor, eh, null);
    }

    /**
     * Create an import action for the given main window. Specify the file to be
     * opened.
     * 
     * @param mw the main window to which a progress bar will be attached
     * @param file the workspace file to open
     */
    public ImportWorkspaceAction(@NonNull final ProgressBarAcceptor acceptor, @NonNull final EventHandler eh, @Nullable File file) {
        super(PROGRESS_BAR_LABEL, acceptor, eh);
        this.file = file;
    }

    @Override
    public void run() {
        setProgressBarIndeterminate();
        File f = file==null ? selectFile() : file;
        if (f != null) {
        	
        	IWorkspace w;
			try {
				w = WorkspaceImporter.importWorkspace(f);
				DatasetListManager.getInstance().addWorkspace(w);

	    		for (File dataFile : w.getFiles()) {
	    			new ImportDatasetAction(progressAcceptors.get(0), eh, dataFile).run();
	    		}

	            setProgressMessage(PROGRESS_BAR_LABEL);
			} catch (JDOMException | IOException e) {
				LOGGER.warning("Unable to read workspace file:"+e.getMessage());
				cancel();
			}
        }
        cancel();
    }

    /**
     * Get the file to be loaded
     * 
     * @return
     */
    private File selectFile() {

        FileNameExtensionFilter filter = new FileNameExtensionFilter(DEFAULT_FILE_TYPE,
                Importer.WRK_FILE_EXTENSION_NODOT);

        File defaultDir = GlobalOptions.getInstance().getDefaultDir();
        JFileChooser fc = new JFileChooser("Select a workspace file...");
        if (defaultDir.exists()) {
            fc = new JFileChooser(defaultDir);
        }
        fc.setFileFilter(filter);

        int returnVal = fc.showOpenDialog(fc);
        if (returnVal != 0)
            return null;
        File file = fc.getSelectedFile();

        if (file.isDirectory())
            return null;
        return file;
    }

}
