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
import javax.swing.filechooser.FileNameExtensionFilter;

import org.eclipse.jdt.annotation.NonNull;
import org.eclipse.jdt.annotation.Nullable;

import com.bmskinner.nuclear_morphology.analysis.DefaultAnalysisWorker;
import com.bmskinner.nuclear_morphology.analysis.IAnalysisMethod;
import com.bmskinner.nuclear_morphology.analysis.IAnalysisResult;
import com.bmskinner.nuclear_morphology.analysis.nucleus.NucleusDetectionMethod;
import com.bmskinner.nuclear_morphology.components.IAnalysisDataset;
import com.bmskinner.nuclear_morphology.components.options.IAnalysisOptions;
import com.bmskinner.nuclear_morphology.components.options.IDetectionOptions;
import com.bmskinner.nuclear_morphology.core.EventHandler;
import com.bmskinner.nuclear_morphology.core.GlobalOptions;
import com.bmskinner.nuclear_morphology.core.ThreadManager;
import com.bmskinner.nuclear_morphology.core.InputSupplier.RequestCancelledException;
import com.bmskinner.nuclear_morphology.gui.ProgressBarAcceptor;
import com.bmskinner.nuclear_morphology.gui.events.DatasetEvent;
import com.bmskinner.nuclear_morphology.io.Io.Importer;
import com.bmskinner.nuclear_morphology.io.OptionsXMLReader;

public class ImportWorkflowAction  extends VoidResultAction {

    private File file;
    private static final String PROGRESS_BAR_LABEL = "Opening workflow...";
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
    public ImportWorkflowAction(@NonNull final ProgressBarAcceptor acceptor, @NonNull final EventHandler eh, @Nullable File file) {
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

    			OptionsXMLReader r = new OptionsXMLReader(file);
    			IAnalysisOptions options = r.readAnalysisOptions();

    			File folder = eh.getInputSupplier().requestFolder("Choose image folder");    
    			
    			Optional<IDetectionOptions> nucleusOptions = options.getDetectionOptions(IAnalysisOptions.NUCLEUS);
    			if(!nucleusOptions.isPresent()) {
    				cancel();
    				return;
    			}
    			
    			nucleusOptions.get().setFolder(folder);
    			Instant inst = Instant.ofEpochMilli(options.getAnalysisTime());
    			LocalDateTime anTime = LocalDateTime.ofInstant(inst, ZoneOffset.systemDefault());
    			String outputFolderName = anTime.format(DateTimeFormatter.ofPattern("YYYY-MM-dd_HH-mm-ss"));

                File analysisFolder = new File(folder, outputFolderName);
                if (!analysisFolder.exists())
                    analysisFolder.mkdir();
                
    			IAnalysisMethod m = new NucleusDetectionMethod(outputFolderName, options);
                
                worker = new DefaultAnalysisWorker(m);
                worker.addPropertyChangeListener(this);
                ThreadManager.getInstance().submit(worker);

    		} catch (RequestCancelledException e) {
    			cancel();
    			return;
    		}
    }
    
    @Override
    public void finished() {

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


}
