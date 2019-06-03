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
import java.util.logging.Logger;

import org.eclipse.jdt.annotation.NonNull;

import com.bmskinner.nuclear_morphology.components.IAnalysisDataset;
import com.bmskinner.nuclear_morphology.core.EventHandler;
import com.bmskinner.nuclear_morphology.core.InputSupplier.RequestCancelledException;
import com.bmskinner.nuclear_morphology.gui.ProgressBarAcceptor;
import com.bmskinner.nuclear_morphology.gui.events.InterfaceEvent.InterfaceMethod;

public class ReplaceSourceImageDirectoryAction extends SingleDatasetResultAction {
	
	private static final Logger LOGGER = Logger.getLogger(Logger.GLOBAL_LOGGER_NAME);
	
	private static final String PROGRESS_BAR_LABEL = "Replacing images";

    public ReplaceSourceImageDirectoryAction(@NonNull final IAnalysisDataset dataset, @NonNull final ProgressBarAcceptor acceptor, @NonNull final EventHandler eh) {
        super(dataset, PROGRESS_BAR_LABEL, acceptor, eh);
        this.setProgressBarIndeterminate();
    }

    @Override
    public void run() {

    	if (!dataset.hasMergeSources()) {

    		try {
    			File folder = eh.getInputSupplier().requestFolder("Select new directory of images...");
    		 LOGGER.info("Updating folder to " + folder.getAbsolutePath());

    			dataset.updateSourceImageDirectory(folder);

    			finished();

    		} catch (RequestCancelledException e) {
    			cancel();
    		}
    	} else {
    		LOGGER.warning("Dataset is a merge; cancelling");
    		cancel();
    	}

    }

    @Override
    public void finished() {
        // Do not use super.finished(), or it will trigger another save action
        cancel();
        getInterfaceEventHandler().fireInterfaceEvent(InterfaceMethod.RECACHE_CHARTS);
        getInterfaceEventHandler().removeListener(eh);
        getDatasetEventHandler().removeListener(eh);
    }
}
