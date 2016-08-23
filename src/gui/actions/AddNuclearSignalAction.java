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
import gui.ThreadManager;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import analysis.AnalysisDataset;
import analysis.signals.SignalDetector;
import analysis.signals.SignalManager;
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
				ThreadManager.getInstance().submit(worker);
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
		finer("Finished signal detection");
		this.cleanup(); // remove the property change listener
		
		
		// divide population into clusters with and without signals
		List<CellCollection> signalPopulations = dividePopulationBySignals(dataset.getCollection(), signalGroup);

		List<AnalysisDataset> list = new ArrayList<AnalysisDataset>();
		
		for(CellCollection collection : signalPopulations){
			finer("Processing "+collection.getName());
			processSubPopulation(collection);
			finer("Processed "+collection.getName());
			list.add(dataset.getChildDataset(collection.getID()));
		}
		
		fine("Finished processing sub-populations");
		// we have morphology analysis to carry out, so don't use the super finished
		// use the same segmentation from the initial analysis
		int flag = 0; // set the downstream analyses to run
		flag |= MainWindow.ADD_POPULATION;
		fine("Firing cache refresh request");
		fireDatasetEvent(DatasetMethod.REFRESH_CACHE, dataset);
		fine("Running new segmentation on sub-populations");
		new RunSegmentationAction(list, dataset, flag, mw);

		cancel();
		
	}

	/**
	 * Create child datasets for signal populations
	 * and perform basic analyses
	 * @param collection
	 */
	private void processSubPopulation(CellCollection collection){

		try{
		finer("Creating new analysis dataset for "+collection.getName());
		AnalysisDataset subDataset = new AnalysisDataset(collection, dataset.getSavePath());
		subDataset.setAnalysisOptions(dataset.getAnalysisOptions());

		log("Sub-population "+collection.getName()+": "+collection.getNucleusCount()+" nuclei");
		SignalManager m = collection.getSignalManager();
		
		// Remove any signal groups that have no signals in the population
		finer(collection.getName()+" has "+collection.getSignalGroupIDs().size()+" signal groups");
		
		Set<UUID> toRemove = new HashSet<UUID>();
		
		for(UUID signalGroup : collection.getSignalGroupIDs()){// : collection.getSignalGroups()){
			
			String groupName = m.getSignalGroupName(signalGroup);
			fine("Checking signal group "+groupName);
			
			if(collection.getSignalManager().getSignalCount(signalGroup)==0){ // Signal group has no signals
				finer("Signals not present in group "+groupName);
				
				// No need to keep the group
				toRemove.add(signalGroup);
				
			}
			finer("Checked signal group "+groupName);
		}
		finer("Removing empty signal groups");
		for(UUID id : toRemove){
			collection.removeSignalGroup(id);
			finer("Removed signal group");
		}
		
		finer("Adding "+collection.getName()+" as child dataset");
		dataset.addChildDataset(subDataset);
		} catch(Exception e){
			error("Error processing signal group", e);
		}
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
				log("Signal group "+group.getGroupName()+": found nuclei with signals");
				CellCollection listCollection = new CellCollection(r.getFolder(), 
						r.getOutputFolderName(), 
                        group.getGroupName()+"_with_signals", 
						r.getNucleusType());

				for(Cell c : list){

					finer("  Added cell: "+c.getNucleus().getNameAndNumber());
					Cell newCell = new Cell(c);
					listCollection.addCell( newCell );
				}
				signalPopulations.add(listCollection);
				
				// Copy over existing signal groups
				finer("Adding existing signal groups to collection "+listCollection.getName());
				for(UUID id  : r.getSignalGroupIDs()){
					finer("Adding signal group "+r.getSignalGroup(id).getGroupName()+ " to "+listCollection.getName());
					listCollection.addSignalGroup(id, new SignalGroup(r.getSignalGroup(id)));
				}
                listCollection.addSignalGroup(signalGroup, new SignalGroup(r.getSignalGroup(signalGroup)));

                // Only add a group of cells without signals if at least one cell does havea signal

				List<Cell> notList = r.getSignalManager().getCellsWithNuclearSignals(signalGroup, false);
				if(!notList.isEmpty()){
					log("Signal group "+r.getSignalGroup(signalGroup).getGroupName()+": found nuclei without signals");
					CellCollection notListCollection = new CellCollection(r.getFolder(), 
							r.getOutputFolderName(), 
                            group.getGroupName()+"_without_signals", 
							r.getNucleusType());

					for(Cell c : notList){
						notListCollection.addCell( new Cell(c) );
						finer("  Added cell: "+c.getNucleus().getNameAndNumber());
					}
					
					// Copy over existing signal groups
					finer("Adding existing signal groups to collection "+notListCollection.getName());
					for(UUID id  : r.getSignalGroupIDs()){
						if( ! id.equals(signalGroup)){ // don't bother adding the signals that aren't there
							finer("Adding signal group "+r.getSignalGroup(signalGroup).getGroupName()+" to "+notListCollection.getName());
							notListCollection.addSignalGroup(id, new SignalGroup(r.getSignalGroup(id)));
						}
					}
					signalPopulations.add(notListCollection);
				} else {
					finest("No cells without signals");
				}

			}
			
		} catch(Exception e){
			error("Cannot create collection", e);
		}
		fine("Finished dividing populations based on signals");
		return signalPopulations;
	}
}
