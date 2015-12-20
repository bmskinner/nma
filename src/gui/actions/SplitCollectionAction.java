package gui.actions;

import java.util.logging.Level;

import javax.swing.JOptionPane;

import components.Cell;
import components.CellCollection;

import gui.MainWindow;
import analysis.AnalysisDataset;

public class SplitCollectionAction extends ProgressableAction {

	public SplitCollectionAction(AnalysisDataset dataset, MainWindow mw) {
		super(dataset, "Splitting collection", mw);

        	try {

        		if(dataset.hasChildren()){
        			programLogger.log(Level.INFO, "Splitting collection...");

        			AnalysisDataset[] names =  dataset.getAllChildDatasets().toArray(new AnalysisDataset[0]);

        			AnalysisDataset negative = (AnalysisDataset) JOptionPane.showInputDialog(null,
        					"Give me nuclei that are NOT present within the following population", "Split population",
        					JOptionPane.PLAIN_MESSAGE, null,
        					names, names[0]);
        			
        			if(negative!=null){

        				// prepare a new collection
        				CellCollection collection = dataset.getCollection();

        				CellCollection newCollection = new CellCollection(dataset, "Subtraction");

        				for(Cell cell : collection.getCells()){

        					boolean ok = true;
        					for(Cell negCell : negative.getCollection().getCells()){
        						if(negCell.getId().equals(cell.getId())){
        							ok = false;
        						}
        					}

        					if(ok){
        						newCollection.addCell(new Cell(cell));
        					}

        				}
        				newCollection.setName("Not_in_"+negative.getName());

        				dataset.addChildCollection(newCollection);

        				if(newCollection.getNucleusCount()>0){

        					programLogger.log(Level.INFO,"Reapplying morphology...");

        					int flag = 0;
        					AnalysisDataset newDataset = dataset.getChildDataset(newCollection.getID());
        					new MorphologyAnalysisAction(newDataset, dataset, flag, mw);
        				}
        			} else {
        				programLogger.log(Level.FINE,"User cancelled split");
        			}

        			
        		} else {
        			programLogger.log(Level.INFO,"Cannot split; no children in dataset");
        		}


			} catch (Exception e1) {
				programLogger.log(Level.SEVERE,"Error splitting collection", e1);
			} finally {
				cancel();
			}
			
        } 

}
