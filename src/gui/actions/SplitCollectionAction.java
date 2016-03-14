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

import java.util.concurrent.CountDownLatch;
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
        			log(Level.INFO, "Splitting collection...");

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

        					log(Level.INFO,"Reapplying morphology...");

        					int flag = 0;
        					AnalysisDataset newDataset = dataset.getChildDataset(newCollection.getID());
        					final CountDownLatch latch = new CountDownLatch(1);
        					new RunSegmentationAction(newDataset, dataset, flag, mw, latch);
        				}
        			} else {
        				log(Level.FINE,"User cancelled split");
        			}

        			
        		} else {
        			log(Level.INFO,"Cannot split; no children in dataset");
        		}


			} catch (Exception e1) {
				logError("Error splitting collection", e1);
			} finally {
				cancel();
			}
			
        } 

}
