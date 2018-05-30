package com.bmskinner.nuclear_morphology.gui.actions;

import java.io.File;

import javax.swing.JFileChooser;
import javax.swing.filechooser.FileNameExtensionFilter;

import org.eclipse.jdt.annotation.NonNull;
import org.eclipse.jdt.annotation.Nullable;

import com.bmskinner.nuclear_morphology.components.workspaces.IWorkspace;
import com.bmskinner.nuclear_morphology.gui.MainWindow;
import com.bmskinner.nuclear_morphology.gui.ProgressBarAcceptor;
import com.bmskinner.nuclear_morphology.io.Io.Importer;
import com.bmskinner.nuclear_morphology.io.WorkspaceImporter;
import com.bmskinner.nuclear_morphology.main.DatasetListManager;
import com.bmskinner.nuclear_morphology.main.EventHandler;
import com.bmskinner.nuclear_morphology.main.GlobalOptions;

public class WorkspaceImportAction extends VoidResultAction {

    private final File          file;
    private static final String PROGRESS_BAR_LABEL = "Opening workspace...";
    private static final String DEFAULT_FILE_TYPE  = "Nuclear morphology workspace";
    
    /**
     * Create an import action for the given main window. This will create a
     * dialog asking for the file to open.
     * 
     * @param mw the main window to which a progress bar will be attached
     */
    public WorkspaceImportAction(@NonNull final ProgressBarAcceptor acceptor, @NonNull final EventHandler eh) {
        this(acceptor, eh, null);
    }

    /**
     * Create an import action for the given main window. Specify the file to be
     * opened.
     * 
     * @param mw the main window to which a progress bar will be attached
     * @param file the workspace file to open
     */
    public WorkspaceImportAction(@NonNull final ProgressBarAcceptor acceptor, @NonNull final EventHandler eh, @Nullable File file) {
        super(PROGRESS_BAR_LABEL, acceptor, eh);
        this.file = file;
    }

    @Override
    public void run() {
        setProgressBarIndeterminate();
        File f = file==null ? selectFile() : file;
        if (f != null) {
        	
        	IWorkspace w = WorkspaceImporter.createImporter(f).importWorkspace();
    		DatasetListManager.getInstance().addWorkspace(w);

    		for (File dataFile : w.getFiles()) {
    			new PopulationImportAction(progressAcceptors.get(0), eh, dataFile).run();
    		}

            setProgressMessage(PROGRESS_BAR_LABEL);
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
