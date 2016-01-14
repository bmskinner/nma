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

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.logging.Level;

import analysis.AnalysisDataset;
import components.CellCollection;
import gui.MainWindow;
import gui.dialogs.FishRemappingDialog;

/**
 * Compare morphology images with post-FISH images, and select nuclei into new
 * sub-populations
 */
public class FishRemappingAction extends ProgressableAction {

	public FishRemappingAction(final List<AnalysisDataset> datasets, final MainWindow mw) {
		super("Remapping", mw);

		try{

			if(datasets.size()==1){
				
				final AnalysisDataset dataset = datasets.get(0);
				
				FishRemappingDialog fishMapper = new FishRemappingDialog(mw, dataset, programLogger);

				if(fishMapper.getOK()){
					
					programLogger.log(Level.INFO, "Fetching collections...");
					List<CellCollection> subs = fishMapper.getSubCollections();
					
					if(!subs.isEmpty()){

						final List<AnalysisDataset> newList = new ArrayList<AnalysisDataset>();
						for(CellCollection sub : subs){

							if(sub.hasCells()){

								dataset.addChildCollection(sub);

								final AnalysisDataset subDataset = dataset.getChildDataset(sub.getID());
								newList.add(subDataset);
							}
						}
						programLogger.log(Level.INFO, "Reapplying morphology...");



						new RunSegmentationAction(newList, dataset, MainWindow.ADD_POPULATION, mw);
						finished();
					}else {
						programLogger.log(Level.INFO, "No collections returned");
						cancel();
					}

				} else {
					programLogger.log(Level.INFO, "Remapping cancelled");
					cancel();
				}
				
			} else {
				programLogger.log(Level.INFO, "Multiple datasets selected, cancelling");
				cancel();
			}

		} catch(Exception e){
			programLogger.log(Level.SEVERE, "Error in FISH remapping: "+e.getMessage(), e);
		}
		

	}
	
	@Override
	public void finished(){
		// Do not use super.finished(), or it will trigger another save action
		log(Level.FINE, "FISH mapping complete");
		cancel();		
		this.removeInterfaceEventListener(mw);
		this.removeDatasetEventListener(mw);		
	}
}
