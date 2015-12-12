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
				CellCollection collection = one.getCollection();

				CellCollection newCollection = new CellCollection(one, "operation");

				switch(operation){
					case AND: // present in both
						break;
					case NOT: // present in one, not present in two
						break;
					case OR: // present in either (merge)
						
						List<AnalysisDataset> toMerge = new ArrayList<AnalysisDataset>();
						toMerge.add(one);
						toMerge.add(two);
						new MergeCollectionAction(toMerge, mw);
						break;
					case XOR: // present in either but not both
						break;
					default:
						break;
					
				}


				if(newCollection.getNucleusCount()>0){

					programLogger.log(Level.INFO,"Reapplying morphology...");

					int flag = 0;
					AnalysisDataset newDataset = new AnalysisDataset(newCollection);
					//        					new MorphologyAnalysisAction(newDataset, MainWindow., flag, mw);
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

}
