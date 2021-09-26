package com.bmskinner.nuclear_morphology.gui.actions;

import java.io.File;
import java.util.List;
import java.util.logging.Logger;

import org.eclipse.jdt.annotation.NonNull;

import com.bmskinner.nuclear_morphology.components.datasets.IAnalysisDataset;
import com.bmskinner.nuclear_morphology.core.EventHandler;
import com.bmskinner.nuclear_morphology.core.InputSupplier.RequestCancelledException;
import com.bmskinner.nuclear_morphology.core.ThreadManager;
import com.bmskinner.nuclear_morphology.gui.ProgressBarAcceptor;
import com.bmskinner.nuclear_morphology.gui.components.FileSelector;
import com.bmskinner.nuclear_morphology.io.Io;
import com.bmskinner.nuclear_morphology.io.xml.RulesetCollectionXMLWriter;

/**
 * Action to export ruleset collections as XML
 * @author ben
 * @since 1.18.3
 *
 */
public class ExportRuleSetsAction extends MultiDatasetResultAction {
	
	private static final Logger LOGGER = Logger.getLogger(ExportOptionsAction.class.getName());
	
	private static final String PROGRESS_LBL = "Exporting options";
	
	public ExportRuleSetsAction(@NonNull List<IAnalysisDataset> datasets, @NonNull final ProgressBarAcceptor acceptor, @NonNull final EventHandler eh) {
        super(datasets, PROGRESS_LBL, acceptor, eh);
    }
		
	 @Override
     public void run() {
		 setProgressBarIndeterminate();
		 
		 if(datasets.size()==1) {
			 File file = FileSelector.chooseOptionsExportFile(datasets.get(0));

	         if (file == null) {
	             cancel();
	             return;
	         }

	         Runnable r = () ->{
	        	 RulesetCollectionXMLWriter m = new RulesetCollectionXMLWriter();
	        	 m.write(datasets.get(0).getCollection().getRuleSetCollection(), file);
	        	 cancel();
	         };
	         ThreadManager.getInstance().submit(r);
		 } else {
			 
			 // More than one dataset, choose folder only
			 try {
				File folder = eh.getInputSupplier().requestFolder(IAnalysisDataset.commonPathOfFiles(datasets));
				Runnable r = () ->{
					
					for(IAnalysisDataset d : datasets) {
						File f = new File(folder, d.getName()+Io.XML_FILE_EXTENSION);
						RulesetCollectionXMLWriter m = new RulesetCollectionXMLWriter();
						m.write(d.getCollection().getRuleSetCollection(), f);
					 LOGGER.info(String.format("Exported %s rulesets to %s", d.getName(), f.getAbsolutePath()));
					}
					 cancel();
		         };
		         ThreadManager.getInstance().submit(r);
			} catch (RequestCancelledException e) {
				cancel();
			}			 
		 }
     }
}
