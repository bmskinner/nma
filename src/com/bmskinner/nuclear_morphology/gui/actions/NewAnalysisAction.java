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
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;
import java.util.Calendar;
import java.util.Date;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.ExecutionException;

import javax.swing.JFileChooser;
import javax.swing.filechooser.FileSystemView;

import org.eclipse.jdt.annotation.NonNull;

import com.bmskinner.nuclear_morphology.analysis.DefaultAnalysisWorker;
import com.bmskinner.nuclear_morphology.analysis.IAnalysisMethod;
import com.bmskinner.nuclear_morphology.analysis.IAnalysisResult;
import com.bmskinner.nuclear_morphology.analysis.nucleus.NucleusDetectionMethod;
import com.bmskinner.nuclear_morphology.components.IAnalysisDataset;
import com.bmskinner.nuclear_morphology.components.options.IAnalysisOptions;
import com.bmskinner.nuclear_morphology.components.options.IDetectionOptions;
import com.bmskinner.nuclear_morphology.components.options.OptionsFactory;
import com.bmskinner.nuclear_morphology.core.EventHandler;
import com.bmskinner.nuclear_morphology.core.GlobalOptions;
import com.bmskinner.nuclear_morphology.core.ThreadManager;
import com.bmskinner.nuclear_morphology.gui.ProgressBarAcceptor;
import com.bmskinner.nuclear_morphology.gui.dialogs.prober.NucleusImageProber;
import com.bmskinner.nuclear_morphology.gui.events.DatasetEvent;
import com.bmskinner.nuclear_morphology.gui.main.MainWindow;

import net.samuelcampos.usbdrivedetector.USBDeviceDetectorManager;
import net.samuelcampos.usbdrivedetector.USBStorageDevice;

/**
 * Run a new analysis
 */
public class NewAnalysisAction extends VoidResultAction {

    private IAnalysisOptions options;
    private IDetectionOptions nucleusOptions;

    private File folder = null;

    public static final int NEW_ANALYSIS = 0;

    private static final String PROGRESS_BAR_LABEL = "Nucleus detection";

    /**
     * Create a new analysis. The folder of images to analyse will be requested
     * by a dialog.
     * 
     * @param mw
     *            the main window to which a progress bar will be attached
     */
    public NewAnalysisAction(@NonNull ProgressBarAcceptor acceptor, @NonNull EventHandler eh) {
        this(acceptor, eh, null);
    }

    /**
     * Create a new analysis, specifying the initial directory of images
     * 
     * @param mw
     *            the main window to which a progress bar will be attached
     * @param folder
     *            the folder of images to analyse
     */
    public NewAnalysisAction(@NonNull ProgressBarAcceptor acceptor, @NonNull EventHandler eh, final File folder) {
        super(PROGRESS_BAR_LABEL, acceptor, eh);
        this.folder = folder;
        options = OptionsFactory.makeAnalysisOptions();
        nucleusOptions = OptionsFactory.makeNucleusDetectionOptions(folder);
        options.setDetectionOptions(IAnalysisOptions.NUCLEUS, nucleusOptions);
    }

    @Override
    public void run() {

        this.setProgressBarIndeterminate();

        if (folder == null) {
            if (!getImageDirectory()) {
                fine("Could not get image directory");
                cancel();
                return;
            }
        }
        
        // Files on USB drives are causing issues with path names on dataset opening.
        // Block for now.
//        USBDeviceDetectorManager usb = new USBDeviceDetectorManager();
//       
//        for(USBStorageDevice u : usb.getRemovableDevices()) {
//        	if(folder.getAbsolutePath().startsWith(u.getRootDirectory().getName())) {
//        		warn("Unable to comply. Folder is on a USB stick. Copy images to hard disk.");
//        		cancel();
//        		return;
//        	}
//        }

        fine("Creating for " + folder.getAbsolutePath());

        NucleusImageProber analysisSetup = new NucleusImageProber(folder, options);

        if (analysisSetup.isOk()) {

        	Optional<IDetectionOptions> op = options.getDetectionOptions(IAnalysisOptions.NUCLEUS);
            if(!op.isPresent()){
            	cancel();
            	return;
            }

            File directory = op.get().getFolder();
            if (directory == null) {
                cancel();
                return;
            }

            log("Directory: " + directory.getName());

            Instant inst = Instant.ofEpochMilli(options.getAnalysisTime());
			LocalDateTime anTime = LocalDateTime.ofInstant(inst, ZoneOffset.systemDefault());
			String outputFolderName = anTime.format(DateTimeFormatter.ofPattern("YYYY-MM-dd_HH-mm-ss"));

            File analysisFolder = new File(directory, outputFolderName);
            if (!analysisFolder.exists())
                analysisFolder.mkdir();

            IAnalysisMethod m = new NucleusDetectionMethod(outputFolderName, options);
            
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

            if (datasets == null || datasets.isEmpty()) {
                log("No datasets returned");
            } else {
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

        if (!file.isDirectory())
            return false;

        folder = file;

        nucleusOptions.setFolder(file);
        return true;
    }

}
