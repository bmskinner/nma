package com.bmskinner.nuclear_morphology.gui.actions;

import java.io.File;

import org.eclipse.jdt.annotation.NonNull;

import com.bmskinner.nuclear_morphology.components.IAnalysisDataset;
import com.bmskinner.nuclear_morphology.core.EventHandler;
import com.bmskinner.nuclear_morphology.core.ThreadManager;
import com.bmskinner.nuclear_morphology.gui.ProgressBarAcceptor;
import com.bmskinner.nuclear_morphology.gui.components.FileSelector;
import com.bmskinner.nuclear_morphology.io.OptionsXMLWriter;

/**
 * Export the options stored in a dataset
 * @author ben
 * @since 1.14.0
 *
 */
public class ExportOptionsAction extends SingleDatasetResultAction {
	
	private static final String PROGRESS_LBL = "Exporting options";
	
	public ExportOptionsAction(IAnalysisDataset dataset, @NonNull final ProgressBarAcceptor acceptor, @NonNull final EventHandler eh) {
        super(dataset, PROGRESS_LBL, acceptor, eh);
    }
	
	 @Override
     public void run() {
		 setProgressBarIndeterminate();
         File file = FileSelector.chooseOptionsExportFile(dataset);

         if (file == null) {
             cancel();
             return;
         }

         Runnable r = () ->{
        	 OptionsXMLWriter m = new OptionsXMLWriter();
        	 m.write(dataset, file);
        	 cancel();
         };
         ThreadManager.getInstance().submit(r);
     }

}
