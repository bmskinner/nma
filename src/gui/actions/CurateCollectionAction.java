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
import java.util.UUID;
import java.util.logging.Level;

import javax.swing.JOptionPane;

import components.Cell;
import components.CellCollection;
import gui.MainWindow;
import gui.dialogs.ManualCellCurator;
import analysis.AnalysisDataset;
import analysis.nucleus.DatasetSegmenter.MorphologyAnalysisMode;

public class CurateCollectionAction extends ProgressableAction {

	public CurateCollectionAction(AnalysisDataset dataset, MainWindow mw) {
		super(dataset, "Curating collection", mw);

		try{
			ManualCellCurator curator = new ManualCellCurator(programLogger, dataset);

			List<UUID> manualIDs = curator.getIDsToKeep();

			CellCollection template = dataset.getCollection();

			CellCollection clusterCollection = new CellCollection(template.getFolder(), 
					template.getOutputFolderName(), 
					template.getName()+"_Curated", 
					template.getDebugFile(), 
					template.getNucleusType());

			clusterCollection.setName(template.getName()+"_Curated");

			for(Cell c : dataset.getCollection().getCells()){

				if(manualIDs.contains(c.getId())){
					clusterCollection.addCell(new Cell (c));
				}
			}

			analyseNewCollection(clusterCollection);

		}catch(Exception e){
			programLogger.log(Level.SEVERE,"Error curating collection", e);
		} finally {
			cancel();
		}
	}
	
	private void analyseNewCollection(CellCollection collection){
		if(collection.hasCells()){
			programLogger.log(Level.INFO, "Extracted "+collection.cellCount()+" cells");
			final List<AnalysisDataset> list = new ArrayList<AnalysisDataset>();
			dataset.addChildCollection(collection);

			AnalysisDataset clusterDataset = dataset.getChildDataset(collection.getID());
			clusterDataset.setRoot(false);
			list.add(clusterDataset);
			
			
			/*
			 * Decide the analysis to perform on the new dataset
			 */
			
//			Object[] options = { "Use existing cell segmentation" , "Re-segment cells", };
//			int option = JOptionPane.showOptionDialog(null, "Re-segment the new dataset?", "Re-segment?",
//
//					JOptionPane.DEFAULT_OPTION, JOptionPane.WARNING_MESSAGE,
//
//					null, options, options[1]);
//			
			
			
			int flag = MainWindow.ADD_POPULATION;
			flag |= MainWindow.ASSIGN_SEGMENTS;
			
//			if(option==0){ // use existing
//				
//				programLogger.log(Level.INFO, "Copying existing segments");
//				/*
//				 * Create a new profile collection
//				 * 
//				 * Assign the segmnent pattern from the individual nuclei
//				 * How? Use the first nucleus in the collection, or average index proportions across them? 
//				 * 
//				 * Create a new franken collection, based on the new segments in the profile collection
//				 */
//				
//			} else { // resegment
//				
//				programLogger.log(Level.INFO, "Re-segmenting the dataset");
//				
//				
//			}

			// begin a recursive morphology analysis
			new RunProfilingAction(list, flag, mw);

		} else {
			programLogger.log(Level.WARNING, "No cells found");
		}
	}

}
