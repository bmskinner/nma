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
import java.util.concurrent.ExecutionException;
import java.util.logging.Logger;

import javax.swing.JFileChooser;
import javax.swing.filechooser.FileNameExtensionFilter;

import org.eclipse.jdt.annotation.NonNull;
import org.eclipse.jdt.annotation.Nullable;

import com.bmskinner.nuclear_morphology.analysis.DefaultAnalysisWorker;
import com.bmskinner.nuclear_morphology.analysis.IAnalysisMethod;
import com.bmskinner.nuclear_morphology.analysis.IAnalysisResult;
import com.bmskinner.nuclear_morphology.analysis.IAnalysisWorker;
import com.bmskinner.nuclear_morphology.components.IAnalysisDataset;
import com.bmskinner.nuclear_morphology.components.generic.Version.UnsupportedVersionException;
import com.bmskinner.nuclear_morphology.core.EventHandler;
import com.bmskinner.nuclear_morphology.core.GlobalOptions;
import com.bmskinner.nuclear_morphology.core.ThreadManager;
import com.bmskinner.nuclear_morphology.gui.ProgressBarAcceptor;
import com.bmskinner.nuclear_morphology.gui.events.DatasetEvent;
import com.bmskinner.nuclear_morphology.io.DatasetImportMethod;
import com.bmskinner.nuclear_morphology.io.Io.Importer;
import com.bmskinner.nuclear_morphology.logging.Loggable;

/**
 * Call an open dialog to choose a saved .nbd dataset. The opened dataset will
 * be added to the bottom of the dataset list.
 */
public class ImportDatasetAction extends VoidResultAction {
	
	private static final Logger LOGGER = Logger.getLogger(Loggable.ROOT_LOGGER);

    private final File          file;
    private static final String PROGRESS_BAR_LABEL = "Opening file...";
    private static final String DEFAULT_FILE_TYPE  = "Nuclear morphology datasets";

    /**
     * Create an import action for the given main window. This will create a
     * dialog asking for the file to open.
     * 
     * @param mw the main window to which a progress bar will be attached
     */
    public ImportDatasetAction(@NonNull final ProgressBarAcceptor acceptor, @NonNull final EventHandler eh) {
        this(acceptor, eh, null);
    }

    /**
     * Create an import action for the given main window. Specify the file to be
     * opened.
     * 
     * @param mw the main window to which a progress bar will be attached
     * @param file the dataset file to open
     */
    public ImportDatasetAction(@NonNull final ProgressBarAcceptor acceptor, @NonNull final EventHandler eh, @Nullable File file) {
        super(PROGRESS_BAR_LABEL, acceptor, eh);
        this.file = file==null ? selectFile() : file;
    }

    @Override
    public void run() {
        setProgressBarIndeterminate();

        if (file != null) {

            try{
                IAnalysisMethod m = new DatasetImportMethod(file);
                worker = new DefaultAnalysisWorker(m, file.length()){
                    @Override
                    public void done() {

                        try {

                            if (this.get() != null) {
                                firePropertyChange(FINISHED_MSG, getProgress(), IAnalysisWorker.FINISHED);

                            } else {
                                firePropertyChange(ERROR_MSG, getProgress(), IAnalysisWorker.ERROR);
                            }
                        } catch (StackOverflowError e) {
                            LOGGER.warning("Stack overflow detected");
                            LOGGER.log(Loggable.STACK, "Stack overflow in worker", e);
                            firePropertyChange("Error", getProgress(), IAnalysisWorker.ERROR);
                        } catch (InterruptedException e) {
                            LOGGER.warning("Interruption to swing worker: "+e.getMessage());
                            LOGGER.log(Loggable.STACK, "Interruption to swing worker", e);
                            firePropertyChange("Error", getProgress(), IAnalysisWorker.ERROR);
                        } catch (ExecutionException e) {
                            
                            if(e.getCause() instanceof UnsupportedVersionException){
                                firePropertyChange(FINISHED_MSG, getProgress(), IAnalysisWorker.FINISHED);
                                return;
                            }

                            LOGGER.warning("Execution error in swing worker: "+e.getMessage());
                            LOGGER.log(Loggable.STACK, "Execution error in swing worker", e);
                            Throwable cause = e.getCause();
                            LOGGER.warning("Causing error: "+cause.getMessage());
                            LOGGER.log(Loggable.STACK, "Causing error: ", cause);
                            firePropertyChange("Error", getProgress(), IAnalysisWorker.ERROR);
                        }

                    }
                };
                
                worker.addPropertyChangeListener(this);
                
            } catch(IllegalArgumentException e){
                LOGGER.warning("Unable to import file");
                LOGGER.log(Loggable.STACK, e.getMessage(), e);
                cancel();
            }

            setProgressMessage(PROGRESS_BAR_LABEL);

            ThreadManager.getInstance().submit(worker);
        } else {
            LOGGER.fine("Open cancelled");
            cancel();
        }
    }

    /**
     * Get the file to be loaded
     * 
     * @return
     */
    private File selectFile() {

        FileNameExtensionFilter filter = new FileNameExtensionFilter(DEFAULT_FILE_TYPE,
                Importer.SAVE_FILE_EXTENSION_NODOT);

        File defaultDir = GlobalOptions.getInstance().getDefaultDir();// new
                                                                      // File("J:\\Protocols\\Scripts
                                                                      // and
                                                                      // macros\\");
        JFileChooser fc = new JFileChooser("Select a saved dataset...");
        if (defaultDir.exists()) {
            fc = new JFileChooser(defaultDir);
        }
        fc.setFileFilter(filter);

        int returnVal = fc.showOpenDialog(fc);
        if (returnVal != 0) {
            return null;
        }
        File file = fc.getSelectedFile();

        if (file.isDirectory()) {
            return null;
        }
        LOGGER.fine("Selected file: " + file.getAbsolutePath());
        return file;
    }

    @Override
    public void finished() {
        setProgressBarVisible(false);
        IAnalysisDataset dataset;
        try {

            IAnalysisResult r = worker.get();

            dataset = r.getFirstDataset();

            // Save newly converted datasets
            if (r.getBoolean(DatasetImportMethod.WAS_CONVERTED_BOOL)) {
                getDatasetEventHandler().fireDatasetEvent(DatasetEvent.SAVE, dataset);
            }

        } catch (InterruptedException e) {
            LOGGER.warning("Unable to open file '" + file.getAbsolutePath() + "': " + e.getMessage());
            LOGGER.log(Loggable.STACK, "Unable to open '" + file.getAbsolutePath() + "': ", e);
            return;
        } catch (ExecutionException e) {
            LOGGER.warning("Unable to open '" + file.getAbsolutePath() + "': " + e.getMessage());
            LOGGER.log(Loggable.STACK, "Unable to open '" + file.getAbsolutePath() + "': ", e);
            return;
        }
        LOGGER.fine("Opened dataset");

        getDatasetEventHandler().fireDatasetEvent(DatasetEvent.ADD_DATASET, dataset);
        super.finished();
    }

}
