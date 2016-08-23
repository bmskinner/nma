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
import components.CellCollection;
import gui.MainWindow;
import gui.dialogs.DatasetArithmeticSetupDialog;
import gui.dialogs.DatasetArithmeticSetupDialog.DatasetArithmeticOperation;
import analysis.AnalysisDataset;

public class DatasetArithmeticAction extends ProgressableAction {

	public DatasetArithmeticAction(List<AnalysisDataset> list, MainWindow mw) {
		super("Dataset arithmetic", mw);
		this.cooldown();
		try {
			fine("Performing arithmetic...");

			/*
			 * Make a dialog with a dropdown for dataset 1, operator, then  dropdown for dataset 2
			 */

			DatasetArithmeticSetupDialog dialog = new DatasetArithmeticSetupDialog(list, mw);

			if(dialog.isReadyToRun()){

				AnalysisDataset one = dialog.getDatasetOne();
				AnalysisDataset two = dialog.getDatasetTwo();
				DatasetArithmeticOperation operation = dialog.getOperation();

				
				log("Performing "+operation+" on datasets");
				// prepare a new collection

				CellCollection newCollection = null; 

				switch(operation){
					case AND: // present in both
						newCollection = one.getCollection().and(two.getCollection());
						break;
					case NOT: // present in one, not present in two
						newCollection = one.getCollection().not(two.getCollection());
						break;
					case OR: // present in either (merge)
						
						List<AnalysisDataset> toMerge = new ArrayList<AnalysisDataset>();
						toMerge.add(one);
						toMerge.add(two);
						new MergeCollectionAction(toMerge, mw);
						break;
					case XOR: // present in either but not both
						newCollection = one.getCollection().xor(two.getCollection());
						break;
					default:
						break;
					
				}


				if(newCollection !=null && newCollection.getNucleusCount()>0){
					log("Found "+newCollection.getNucleusCount()+" cells");
					log("Running morphology analysis...");
					AnalysisDataset newDataset = new AnalysisDataset(newCollection);
					newDataset.setRoot(true);
					int flag = MainWindow.ADD_POPULATION;
					flag |= MainWindow.SAVE_DATASET;
					flag |= MainWindow.ASSIGN_SEGMENTS;
					new RunProfilingAction(newDataset, flag, mw);
										
				} else {
					log("No populations returned");
				}
			} else {
				fine("User cancelled operation");
			}



		} catch (Exception e1) {
			error("Error in dataset arithmetic", e1);
		} finally {
			cancel();
		}

	} 
}
