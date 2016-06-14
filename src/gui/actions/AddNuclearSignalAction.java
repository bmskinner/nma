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

import gui.dialogs.SignalDetectionSettingsDialog;
import gui.DatasetEvent.DatasetMethod;
import gui.MainWindow;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.logging.Level;

import analysis.AnalysisDataset;
import analysis.signals.SignalDetector;
import components.Cell;
import components.CellCollection;
import components.nuclear.SignalGroup;

public class AddNuclearSignalAction extends ProgressableAction {
	
	private UUID signalGroup = null;
	
	public AddNuclearSignalAction(AnalysisDataset dataset, MainWindow mw) {
		super(dataset, "Signal detection", mw);

		try{
			// add dialog for non-default detection options
			SignalDetectionSettingsDialog analysisSetup = new SignalDetectionSettingsDialog(dataset);

			if(analysisSetup.isReadyToRun()){

				this.signalGroup = analysisSetup.getSignalGroup();

                String signalGroupName = dataset.getCollection().getSignalGroup(signalGroup).getGroupName();//.getSignalGroupName(signalGroup);



                worker = new SignalDetector(dataset, 
                        analysisSetup.getFolder(), 
                        analysisSetup.getChannel(), 
                        dataset.getAnalysisOptions().getNuclearSignalOptions(signalGroup), 
                        signalGroup, 
                        signalGroupName);

               
				this.setProgressMessage("Signal detection: "+signalGroupName);
				worker.addPropertyChangeListener(this);
				worker.execute();
			} else {
				this.cancel();
				return;
			}

			
		} catch (Exception e){
			this.cancel();
			logError("Error in signal analysis", e);
		}
		
	}	
	
	@Override
	public void finished(){
		// divide population into clusters with and without signals
		List<CellCollection> signalPopulations = dividePopulationBySignals(dataset.getCollection(), signalGroup);

		List<AnalysisDataset> list = new ArrayList<AnalysisDataset>();
		for(CellCollection collection : signalPopulations){
			
			processSubPopulation(collection);
			list.add(dataset.getChildDataset(collection.getID()));
		}
		// we have morphology analysis to carry out, so don't use the super finished
		// use the same segmentation from the initial analysis
		int flag = 0; // set the downstream analyses to run
		flag |= MainWindow.ADD_POPULATION;
		fireDatasetEvent(DatasetMethod.REFRESH_CACHE, dataset);
		new RunSegmentationAction(list, dataset, flag, mw);
//		fireInterfaceEvent(InterfaceMethod.RECACHE_CHARTS);
		cancel();
	}

	/**
	 * Create child datasets for signal populations
	 * and perform basic analyses
	 * @param collection
	 */
	private void processSubPopulation(CellCollection collection){

		AnalysisDataset subDataset = new AnalysisDataset(collection, dataset.getSavePath());
		subDataset.setAnalysisOptions(dataset.getAnalysisOptions());

		log("Sub-population: "+collection.getNucleusCount()+" nuclei");

		dataset.addChildDataset(subDataset);
	}


	/**
	 * Create two child populations for the given dataset: one with signals in the 
	 * given group, and one without signals 
	 * @param r the collection to split
	 * @param signalGroup the signal group to split on
	 * @return a list of new collections
	 */
	private List<CellCollection> dividePopulationBySignals(CellCollection r, UUID signalGroup){

		List<CellCollection> signalPopulations = new ArrayList<CellCollection>(0);
		log("Dividing population by signals...");
		try{

            SignalGroup group = r.getSignalGroup(signalGroup);
            group.setVisible(true);

			List<Cell> list = r.getSignalManager().getCellsWithNuclearSignals(signalGroup, true);
			if(!list.isEmpty()){
				log("Signal group "+signalGroup+": found nuclei with signals");
				CellCollection listCollection = new CellCollection(r.getFolder(), 
						r.getOutputFolderName(), 
                        group.getGroupName()+"_with_signals", 
						r.getNucleusType());

				for(Cell c : list){

					finest("  Added cell: "+c.getNucleus().getNameAndNumber());
					finest("  Cell has: "+c.getNucleus().getSignalCollection().numberOfSignals()+" signals");
					Cell newCell = new Cell(c);
					finest("  New cell has: "+newCell.getNucleus().getSignalCollection().numberOfSignals()+" signals");

					listCollection.addCell( newCell );
				}
				signalPopulations.add(listCollection);
				
				// Copy over existing signal groups
				for(UUID id  : r.getSignalGroupIDs()){
					listCollection.addSignalGroup(id, new SignalGroup(r.getSignalGroup(id)));
				}
                listCollection.addSignalGroup(signalGroup, new SignalGroup(r.getSignalGroup(signalGroup)));


				List<Cell> notList = r.getSignalManager().getCellsWithNuclearSignals(signalGroup, false);
				if(!notList.isEmpty()){
					log("Signal group "+signalGroup+": found nuclei without signals");
					CellCollection notListCollection = new CellCollection(r.getFolder(), 
							r.getOutputFolderName(), 
                            group.getGroupName()+"_without_signals", 
							r.getNucleusType());

					for(Cell c : notList){
						notListCollection.addCell( new Cell(c) );
					}
					
					// Copy over existing signal groups
					for(UUID id  : r.getSignalGroupIDs()){
						notListCollection.addSignalGroup(id, new SignalGroup(r.getSignalGroup(id)));
					}
					signalPopulations.add(notListCollection);
				}

			}

		} catch(Exception e){
			error("Cannot create collection", e);
		}

		return signalPopulations;
	}
}
