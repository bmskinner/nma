/*******************************************************************************
 *  	Copyright (C) 2016 Ben Skinner
 *   
 *     This file is part of Nuclear Morphology Analysis.
 *
 *     Nuclear Morphology Analysis is free software: you can redistribute it and/or modify
 *     it under the terms of the GNU General Public License as published by
 *     the Free Software Foundation, either version 3 of the License, or
 *     (at your option) any later version.
 *
 *     Nuclear Morphology Analysis is distributed in the hope that it will be useful,
 *     but WITHOUT ANY WARRANTY; without even the implied warranty of
 *     MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 *     GNU General Public License for more details.
 *
 *     You should have received a copy of the GNU General Public License
 *     along with Nuclear Morphology Analysis. If not, see <http://www.gnu.org/licenses/>.
 *******************************************************************************/
package gui.actions;

import java.io.File;

import analysis.IAnalysisDataset;
import gui.MainWindow;
import ij.io.DirectoryChooser;

public class ReplaceSourceImageDirectoryAction extends ProgressableAction {

	public ReplaceSourceImageDirectoryAction(IAnalysisDataset dataset, MainWindow mw) {
		super(dataset, "Replacing images", mw);
		this.cooldown();

		try{

			if(!dataset.hasMergeSources()){

				DirectoryChooser localOpenDialog = new DirectoryChooser("Select new directory of images...");
				String folderName = localOpenDialog.getDirectory();

				if(folderName!=null) { 


					File newFolder = new File(folderName);

					log("Updating folder to "+folderName );
					
					dataset.updateSourceImageDirectory(newFolder);

					finished();

				} else {
					log("Update cancelled");
					cancel();
				}
			}else {
				warn("Dataset is a merge; cancelling");
				cancel();
			}

		} catch(Exception e){
			logError("Error in folder update: "+e.getMessage(), e);
		}


	}
	
	@Override
	public void finished(){
		// Do not use super.finished(), or it will trigger another save action
		fine("Folder update complete");
		cancel();		
		this.removeInterfaceEventListener(mw);
		this.removeDatasetEventListener(mw);		
	}
}
