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


			if(clusterCollection.hasCells()){
				programLogger.log(Level.INFO, "Extracted "+clusterCollection.size()+" cells");
				final List<AnalysisDataset> list = new ArrayList<AnalysisDataset>();
				dataset.addChildCollection(clusterCollection);

				AnalysisDataset clusterDataset = dataset.getChildDataset(clusterCollection.getID());
				clusterDataset.setRoot(false);
				list.add(clusterDataset);

				programLogger.log(Level.INFO, "Running new morphology analysis");
				int flag = MainWindow.ADD_POPULATION;
				flag |= MainWindow.ASSIGN_SEGMENTS;
//				flag |= MainWindow.SAVE_DATASET;

				// begin a recursive morphology analysis
				new RunProfilingAction(list, flag, mw);

			} else {
				programLogger.log(Level.WARNING, "No cells found");
			}
		}catch(Exception e){
			programLogger.log(Level.SEVERE,"Error curating collection", e);
		} finally {
			cancel();
		}
	}

}
