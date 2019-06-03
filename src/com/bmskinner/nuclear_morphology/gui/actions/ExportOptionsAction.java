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
import java.util.logging.Logger;

import org.eclipse.jdt.annotation.NonNull;

import com.bmskinner.nuclear_morphology.components.IAnalysisDataset;
import com.bmskinner.nuclear_morphology.core.EventHandler;
import com.bmskinner.nuclear_morphology.core.InputSupplier.RequestCancelledException;
import com.bmskinner.nuclear_morphology.core.ThreadManager;
import com.bmskinner.nuclear_morphology.gui.ProgressBarAcceptor;
import com.bmskinner.nuclear_morphology.gui.components.FileSelector;
import com.bmskinner.nuclear_morphology.io.Io;
import com.bmskinner.nuclear_morphology.io.xml.OptionsXMLWriter;

/**
 * Export the options stored in a dataset
 * @author ben
 * @since 1.14.0
 *
 */
public class ExportOptionsAction extends MultiDatasetResultAction {
	
	private static final Logger LOGGER = Logger.getLogger(Logger.GLOBAL_LOGGER_NAME);
	
	private static final String PROGRESS_LBL = "Exporting options";
	
	public ExportOptionsAction(@NonNull List<IAnalysisDataset> datasets, @NonNull final ProgressBarAcceptor acceptor, @NonNull final EventHandler eh) {
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
	        	 OptionsXMLWriter m = new OptionsXMLWriter();
	        	 m.write(datasets.get(0), file);
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
						OptionsXMLWriter m = new OptionsXMLWriter();
						m.write(d, f);
					 LOGGER.info(String.format("Exported %s options to %s", d.getName(), f.getAbsolutePath()));
					}
					 cancel();
		         };
		         ThreadManager.getInstance().submit(r);
			} catch (RequestCancelledException e) {
				cancel();
	             return;
			}
			 
			 
		 }
		 
         
     }

}
