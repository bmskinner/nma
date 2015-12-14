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
import analysis.nucleus.MorphologyAnalysis;

public class DatasetArithmeticAction extends ProgressableAction {

	public DatasetArithmeticAction(AnalysisDataset selected, List<AnalysisDataset> list, MainWindow mw) {
		super(null, "Dataset arithmetic", "Error in dataset arithmetic", mw);

		try {
			programLogger.log(Level.INFO, "Performing arithmetic...");

			/*
			 * Make a dialog with a dropdown for dataset 1, operator, then  dropdown for dataset 2
			 */

			DatasetArithmeticSetupDialog dialog = new DatasetArithmeticSetupDialog(selected, list, mw);

			if(dialog.isReadyToRun()){

				AnalysisDataset one = dialog.getDatasetOne();
				AnalysisDataset two = dialog.getDatasetTwo();
				DatasetArithmeticOperation operation = dialog.getOperation();

				// prepare a new collection

				CellCollection newCollection = null; // = new CellCollection(one, "operation");

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

					programLogger.log(Level.INFO,"Reapplying morphology...");
					AnalysisDataset newDataset = new AnalysisDataset(newCollection);
					int flag = MainWindow.ADD_POPULATION;
					flag |= MainWindow.SAVE_DATASET;
					new MorphologyAnalysisAction(newDataset, MorphologyAnalysis.MODE_NEW, flag, mw);
//					this.cancel();
										
				} else {
					programLogger.log(Level.INFO,"No populations returned");
				}
			} else {
				programLogger.log(Level.FINE,"User cancelled operation");
			}



		} catch (Exception e1) {
			programLogger.log(Level.SEVERE,"Error splitting collection", e1);
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

		CellCollection newCollection = new CellCollection(one, "and");
		
		for(Cell c : one.getCollection().getCells()){

			if(two.getCollection().getCells().contains(c)){
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

		CellCollection newCollection = new CellCollection(one, "and");
		
		for(Cell c : one.getCollection().getCells()){

			if(!two.getCollection().getCells().contains(c)){
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

		CellCollection newCollection = new CellCollection(one, "and");
		
		for(Cell c : one.getCollection().getCells()){

			if(!two.getCollection().getCells().contains(c)){
				newCollection.addCell(new Cell(c));
			}
		}
		
		for(Cell c : two.getCollection().getCells()){

			if(!one.getCollection().getCells().contains(c)){
				newCollection.addCell(new Cell(c));
			}
		}

		return newCollection;
	}

}
