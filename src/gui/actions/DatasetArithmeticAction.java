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
import java.util.logging.Level;

import javax.swing.JOptionPane;

import components.Cell;
import components.CellCollection;
import gui.MainWindow;
import gui.dialogs.DatasetArithmeticSetupDialog;
import gui.dialogs.DatasetArithmeticSetupDialog.DatasetArithmeticOperation;
import analysis.AnalysisDataset;
import analysis.nucleus.DatasetSegmenter.MorphologyAnalysisMode;

public class DatasetArithmeticAction extends ProgressableAction {

	public DatasetArithmeticAction(AnalysisDataset selected, List<AnalysisDataset> list, MainWindow mw) {
		super("Dataset arithmetic", mw);
		this.cooldown();
		try {
			log(Level.FINE, "Performing arithmetic...");

			/*
			 * Make a dialog with a dropdown for dataset 1, operator, then  dropdown for dataset 2
			 */

			DatasetArithmeticSetupDialog dialog = new DatasetArithmeticSetupDialog(selected, list, mw);

			if(dialog.isReadyToRun()){

				AnalysisDataset one = dialog.getDatasetOne();
				AnalysisDataset two = dialog.getDatasetTwo();
				DatasetArithmeticOperation operation = dialog.getOperation();

				
				log(Level.INFO,"Performing "+operation+" on datasets");
				// prepare a new collection

				CellCollection newCollection = null; 

				switch(operation){
					case AND: // present in both
						newCollection = datasetsAND(one, two);
						break;
					case NOT: // present in one, not present in two
						newCollection = datasetsNOT(one, two);
						break;
					case OR: // present in either (merge)
						
						List<AnalysisDataset> toMerge = new ArrayList<AnalysisDataset>();
						toMerge.add(one);
						toMerge.add(two);
						new MergeCollectionAction(toMerge, mw);
						break;
					case XOR: // present in either but not both
						newCollection = datasetsXOR(one, two);
						break;
					default:
						break;
					
				}


				if(newCollection !=null && newCollection.getNucleusCount()>0){
					log(Level.INFO,"Found "+newCollection.getNucleusCount()+" cells");
					log(Level.INFO,"Applying morphology...");
					AnalysisDataset newDataset = new AnalysisDataset(newCollection);
					newDataset.setRoot(true);
					int flag = MainWindow.ADD_POPULATION;
					flag |= MainWindow.SAVE_DATASET;
					flag |= MainWindow.ASSIGN_SEGMENTS;
					new RunProfilingAction(newDataset, flag, mw);
										
				} else {
					log(Level.INFO,"No populations returned");
				}
			} else {
				log(Level.FINE,"User cancelled operation");
			}



		} catch (Exception e1) {
			logError("Error in dataset arithmetic", e1);
		} finally {
			cancel();
		}

	} 
	
	/**
	 * Return a collection of cells present in both datasets
	 * @param one
	 * @param two
	 * @return
	 * @throws Exception 
	 */
	private CellCollection datasetsAND(AnalysisDataset one, AnalysisDataset two) throws Exception{

		CellCollection newCollection = new CellCollection(one, "AND operation");
		
		for(Cell c : one.getCollection().getCells()){

			if(two.getCollection().contains(c)){
				newCollection.addCell(new Cell(c));
			}
		}

		return newCollection;
	}
	
	/**
	 * Return a collection of cells present in one, not two
	 * @param one
	 * @param two
	 * @return
	 * @throws Exception 
	 */
	private CellCollection datasetsNOT(AnalysisDataset one, AnalysisDataset two) throws Exception{

		CellCollection newCollection = new CellCollection(one, "NOT operation");
		
		for(Cell c : one.getCollection().getCells()){

			if(!two.getCollection().contains(c)){
				newCollection.addCell(new Cell(c));
			}
		}

		return newCollection;
	}
	
	/**
	 * Return a collection of cells present in one, or two, but not both
	 * @param one
	 * @param two
	 * @return
	 * @throws Exception 
	 */
	private CellCollection datasetsXOR(AnalysisDataset one, AnalysisDataset two) throws Exception{

		CellCollection newCollection = new CellCollection(one, "XOR operation");
		
		for(Cell c : one.getCollection().getCells()){

			if(!two.getCollection().contains(c)){
				newCollection.addCell(new Cell(c));
			}
		}
		
		for(Cell c : two.getCollection().getCells()){

			if(!one.getCollection().contains(c)){
				newCollection.addCell(new Cell(c));
			}
		}

		return newCollection;
	}

}
