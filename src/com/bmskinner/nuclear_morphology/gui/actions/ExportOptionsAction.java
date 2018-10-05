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
