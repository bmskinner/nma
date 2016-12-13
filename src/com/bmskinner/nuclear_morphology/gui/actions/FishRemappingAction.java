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
package com.bmskinner.nuclear_morphology.gui.actions;

import java.util.ArrayList;
import java.util.List;
import java.util.logging.Level;

import com.bmskinner.nuclear_morphology.components.IAnalysisDataset;
import com.bmskinner.nuclear_morphology.components.ICellCollection;
import com.bmskinner.nuclear_morphology.gui.MainWindow;
import com.bmskinner.nuclear_morphology.gui.dialogs.FishRemappingDialog;

/**
 * Compare morphology images with post-FISH images, and select nuclei into new
 * sub-populations
 */
public class FishRemappingAction extends ProgressableAction {
	
	final List<IAnalysisDataset> datasets;

	public FishRemappingAction(final List<IAnalysisDataset> datasets, final MainWindow mw) {
		super("Remapping", mw);
		this.datasets = datasets;
		
	}
	
	@Override
	public void run(){
		try{

			if(datasets.size()==1){
				
				final IAnalysisDataset dataset = datasets.get(0);
				
				if(dataset.hasMergeSources()){
					log(Level.INFO, "Cannot remap merged datasets");
					cancel();
					return;
				}
				
				FishRemappingDialog fishMapper = new FishRemappingDialog(mw, dataset);

				if(fishMapper.getOK()){
					
					log(Level.INFO, "Fetching collections...");
					List<ICellCollection> subs = fishMapper.getSubCollections();
					
					if(!subs.isEmpty()){

						final List<IAnalysisDataset> newList = new ArrayList<IAnalysisDataset>();
						for(ICellCollection sub : subs){

							if(sub.hasCells()){
								
								log(sub.getName()+": "+sub.size()+" cells");

								dataset.addChildCollection(sub);

								final IAnalysisDataset subDataset = dataset.getChildDataset(sub.getID());
								newList.add(subDataset);
							}
						}
						log(Level.INFO, "Reapplying morphology...");



						new RunSegmentationAction(newList, dataset, MainWindow.ADD_POPULATION, mw);
						finished();
					}else {
						log(Level.INFO, "No collections returned");
						cancel();
					}

				} else {
					log(Level.INFO, "Remapping cancelled");
					cancel();
				}
				
			} else {
				log(Level.INFO, "Multiple datasets selected, cancelling");
				cancel();
			}

		} catch(Exception e){
			error("Error in FISH remapping: "+e.getMessage(), e);
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
