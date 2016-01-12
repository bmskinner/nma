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
			programLogger.log(Level.FINE, "Performing arithmetic...");

			/*
			 * Make a dialog with a dropdown for dataset 1, operator, then  dropdown for dataset 2
			 */

			DatasetArithmeticSetupDialog dialog = new DatasetArithmeticSetupDialog(selected, list, mw);

			if(dialog.isReadyToRun()){

				AnalysisDataset one = dialog.getDatasetOne();
				AnalysisDataset two = dialog.getDatasetTwo();
				DatasetArithmeticOperation operation = dialog.getOperation();

				
				programLogger.log(Level.INFO,"Performing "+operation+" on datasets");
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
					programLogger.log(Level.INFO,"Found "+newCollection.getNucleusCount()+" cells");
					programLogger.log(Level.INFO,"Applying morphology...");
					AnalysisDataset newDataset = new AnalysisDataset(newCollection);
					newDataset.setRoot(true);
					int flag = MainWindow.ADD_POPULATION;
					flag |= MainWindow.SAVE_DATASET;
					flag |= MainWindow.ASSIGN_SEGMENTS;
					new RunProfilingAction(newDataset, flag, mw);
										
				} else {
					programLogger.log(Level.INFO,"No populations returned");
				}
			} else {
				programLogger.log(Level.FINE,"User cancelled operation");
			}



		} catch (Exception e1) {
			programLogger.log(Level.SEVERE,"Error in dataset arithmetic", e1);
		} finally {
			cancel();
		}

	} 
	
	/**
	 * Return a collection of cells present in both datasets
	 * @param one
	 * @param two
	 * @return
	 */
	private CellCollection datasetsAND(AnalysisDataset one, AnalysisDataset two){

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
	 */
	private CellCollection datasetsNOT(AnalysisDataset one, AnalysisDataset two){

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
	 */
	private CellCollection datasetsXOR(AnalysisDataset one, AnalysisDataset two){

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
