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
import java.util.List;
import java.util.concurrent.ExecutionException;
import java.util.logging.Logger;

import org.eclipse.jdt.annotation.NonNull;
import org.eclipse.jdt.annotation.Nullable;

import com.bmskinner.nuclear_morphology.analysis.DefaultAnalysisWorker;
import com.bmskinner.nuclear_morphology.analysis.IAnalysisMethod;
import com.bmskinner.nuclear_morphology.analysis.IAnalysisResult;
import com.bmskinner.nuclear_morphology.api.AnalysisPipeline.AnalysisPipelineException;
import com.bmskinner.nuclear_morphology.api.SavedOptionsAnalysisPipeline;
import com.bmskinner.nuclear_morphology.components.IAnalysisDataset;
import com.bmskinner.nuclear_morphology.core.EventHandler;
import com.bmskinner.nuclear_morphology.core.InputSupplier.RequestCancelledException;
import com.bmskinner.nuclear_morphology.core.ThreadManager;
import com.bmskinner.nuclear_morphology.gui.ProgressBarAcceptor;
import com.bmskinner.nuclear_morphology.gui.events.DatasetEvent;
import com.bmskinner.nuclear_morphology.io.Io.Importer;
import com.bmskinner.nuclear_morphology.logging.Loggable;

public class ImportWorkflowAction  extends VoidResultAction {
	
	private static final Logger LOGGER = Logger.getLogger(ImportWorkflowAction.class.getName());

    private File file;
    private static final @NonNull String PROGRESS_BAR_LABEL = "Running workflow...";
    private static final String DEFAULT_FILE_TYPE  = "Nuclear morphology workflow";
    
    /**
     * Create an import action for the given main window. This will create a
     * dialog asking for the file to open.
     * 
     * @param mw the main window to which a progress bar will be attached
     */
    public ImportWorkflowAction(@NonNull final ProgressBarAcceptor acceptor, @NonNull final EventHandler eh) {
        this(acceptor, eh, null);
    }

    /**
     * Create an import action for the given main window. Specify the file to be
     * opened.
     * 
     * @param mw the main window to which a progress bar will be attached
     * @param file the workspace file to open
     */
    public ImportWorkflowAction(@NonNull final ProgressBarAcceptor acceptor, 
    		@NonNull final EventHandler eh, @Nullable File file) {
        super(PROGRESS_BAR_LABEL, acceptor, eh);
        this.file = file;
    }

    @Override
    public void run() {
    	setProgressMessage(PROGRESS_BAR_LABEL);
    	setProgressBarIndeterminate();
   
    		try {
    			if(file==null)
    				file = eh.getInputSupplier().requestFile("Choose analysis options", null, Importer.XML_FILE_EXTENSION_NODOT, "Analysis options file");

    			File folder = eh.getInputSupplier().requestFolder("Choose image folder", file.getParentFile());    
    			
    			IAnalysisMethod m = new SavedOptionsAnalysisPipeline(folder, file);
                
                worker = new DefaultAnalysisWorker(m);
                worker.addPropertyChangeListener(this);
                ThreadManager.getInstance().submit(worker);

    		} catch (RequestCancelledException | AnalysisPipelineException e) {
    			LOGGER.warning("Cancelled workflow; "+e.getMessage());
    			cancel();
    		}
    }
        
    @Override
    public void finished() {

        List<IAnalysisDataset> datasets;

        try {
            IAnalysisResult r = worker.get();
            datasets = r.getDatasets();

            if (datasets == null || datasets.isEmpty()) {
                LOGGER.info("No datasets returned");
            } else {
                getDatasetEventHandler().fireDatasetEvent(DatasetEvent.ADD_DATASET, datasets);
            }
        } catch (InterruptedException e) {
            LOGGER.warning("Interruption to swing worker");
            LOGGER.log(Loggable.STACK, "Interruption to swing worker", e);
        } catch (ExecutionException e) {
            LOGGER.warning("Execution error in swing worker");
            LOGGER.log(Loggable.STACK, "Execution error in swing worker", e);
        }

        super.finished();
    }


}
