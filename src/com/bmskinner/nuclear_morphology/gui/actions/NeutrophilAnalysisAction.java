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


package com.bmskinner.nuclear_morphology.gui.actions;

import java.io.File;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.ExecutionException;

import javax.swing.JFileChooser;

import org.eclipse.jdt.annotation.NonNull;

import com.bmskinner.nuclear_morphology.analysis.DefaultAnalysisWorker;
import com.bmskinner.nuclear_morphology.analysis.IAnalysisMethod;
import com.bmskinner.nuclear_morphology.analysis.IAnalysisResult;
import com.bmskinner.nuclear_morphology.analysis.nucleus.NeutrophilDetectionMethod;
import com.bmskinner.nuclear_morphology.components.IAnalysisDataset;
import com.bmskinner.nuclear_morphology.components.options.IAnalysisOptions;
import com.bmskinner.nuclear_morphology.components.options.IDetectionOptions;
import com.bmskinner.nuclear_morphology.core.EventHandler;
import com.bmskinner.nuclear_morphology.core.GlobalOptions;
import com.bmskinner.nuclear_morphology.core.ThreadManager;
import com.bmskinner.nuclear_morphology.gui.ProgressBarAcceptor;
import com.bmskinner.nuclear_morphology.gui.dialogs.prober.NeutrophilImageProber;
import com.bmskinner.nuclear_morphology.gui.events.DatasetEvent;
import com.bmskinner.nuclear_morphology.gui.main.MainWindow;
import com.bmskinner.nuclear_morphology.io.Io.Importer;

public class NeutrophilAnalysisAction extends VoidResultAction {

    private IAnalysisOptions options;
    private Date                    startTime;
    private String                  outputFolderName;

    private File folder = null;

    public static final int NEW_ANALYSIS = 0;
    private static final String PROGRESS_BAR_LABEL = "Neutrophil detection";

    /**
     * Create a new analysis. The folder of images to analyse will be requested
     * by a dialog.
     * 
     * @param mw the main window to which a progress bar will be attached
     */
    public NeutrophilAnalysisAction(@NonNull ProgressBarAcceptor acceptor, @NonNull EventHandler eh) {
        this(acceptor, eh, null);
    }

    /**
     * Create a new analysis, specifying the initial directory of images
     * 
     * @param mw the main window to which a progress bar will be attached
     * @param folder the folder of images to analyse
     */
    public NeutrophilAnalysisAction(@NonNull ProgressBarAcceptor acceptor, @NonNull EventHandler eh, final File folder) {
        super(PROGRESS_BAR_LABEL, acceptor, eh);
        this.folder = folder;
    }

    @Override
    public void run() {

        this.setProgressBarIndeterminate();

        if (folder == null) {
            if (!getImageDirectory()) {
                this.cancel();
                return;
            }
        }

        NeutrophilImageProber analysisSetup = new NeutrophilImageProber(folder);

        if (analysisSetup.isOk()) {

            options = analysisSetup.getOptions();
            
            Optional<IDetectionOptions> op = options.getDetectionOptions(IAnalysisOptions.NUCLEUS);
            if(!op.isPresent()){
            	cancel();
            	return;
            }

            File directory = op.get().getFolder();

            if (directory == null) {
                this.cancel();
                return;
            }

            log("Directory: " + directory.getName());

            this.startTime = Calendar.getInstance().getTime();
            this.outputFolderName = new SimpleDateFormat("yyyy-MM-dd_HH-mm-ss").format(this.startTime);

            // craete the analysis folder early. Did not before in case folder
            // had no images
            File analysisFolder = new File(directory, outputFolderName);
            if (!analysisFolder.exists()) {
                analysisFolder.mkdir();
            }
            //
            File logFile = new File(analysisFolder, directory.getName() + Importer.LOG_FILE_EXTENSION);

            IAnalysisMethod m = new NeutrophilDetectionMethod(this.outputFolderName, logFile, options);

            worker = new DefaultAnalysisWorker(m);
            worker.addPropertyChangeListener(this);
            ThreadManager.getInstance().submit(worker);
            finest("Worker is executing");
            analysisSetup.dispose();

        } else {
            analysisSetup.dispose();
            fine("Analysis cancelled");
            this.cancel();
        }
    }

    @Override
    public void finished() {
        // log("Method finished");
        List<IAnalysisDataset> datasets;

        try {
            IAnalysisResult r = worker.get();
            datasets = r.getDatasets();

            if (datasets == null || datasets.size() == 0) {
                log("No datasets returned");
            } else {
                // log("Fire profiling");
                getDatasetEventHandler().fireDatasetEvent(DatasetEvent.PROFILING_ACTION, datasets);

            }

        } catch (InterruptedException e) {
            warn("Interruption to swing worker");
            stack("Interruption to swing worker", e);
        } catch (ExecutionException e) {
            warn("Execution error in swing worker");
            stack("Execution error in swing worker", e);
        }

        super.finished();
    }

    private boolean getImageDirectory() {
        File defaultDir = GlobalOptions.getInstance().getDefaultDir();
        JFileChooser fc = new JFileChooser(defaultDir); // if null, will be home
                                                        // dir

        fc.setFileSelectionMode(JFileChooser.DIRECTORIES_ONLY);

        int returnVal = fc.showOpenDialog(fc);
        if (returnVal != 0) {
            return false; // user cancelled
        }

        File file = fc.getSelectedFile();

        if (!file.isDirectory()) {
            return false;
        }
        fine("Selected directory: " + file.getAbsolutePath());
        folder = file;

        return true;
    }
}
