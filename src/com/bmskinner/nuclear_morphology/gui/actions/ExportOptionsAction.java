package com.bmskinner.nuclear_morphology.gui.actions;

import java.io.File;

import org.eclipse.jdt.annotation.NonNull;

import com.bmskinner.nuclear_morphology.components.IAnalysisDataset;
import com.bmskinner.nuclear_morphology.core.EventHandler;
import com.bmskinner.nuclear_morphology.gui.ProgressBarAcceptor;
import com.bmskinner.nuclear_morphology.gui.components.FileSelector;
import com.bmskinner.nuclear_morphology.io.OptionsXMLWriter;

public class ExportOptionsAction extends SingleDatasetResultAction {
	
	private static final String PROGRESS_LBL = "Exporting options";
	
	public ExportOptionsAction(IAnalysisDataset dataset, @NonNull final ProgressBarAcceptor acceptor, @NonNull final EventHandler eh) {
        super(dataset, PROGRESS_LBL, acceptor, eh);
    }
	
	 @Override
     public void run() {

         File file = FileSelector.chooseOptionsExportFile(dataset);

         if (file == null) {
             cancel();
             return;
         }

         OptionsXMLWriter m = new OptionsXMLWriter();
         m.write(dataset, file);
         cancel();
     }

}
