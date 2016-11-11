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

import gui.tabs.signals.SignalDetectionSettingsDialog;
import gui.DatasetEvent;
import gui.MainWindow;
import gui.ThreadManager;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.UUID;

import analysis.IAnalysisDataset;
import analysis.signals.SignalDetectionWorker;
import components.ICell;
import components.ICellCollection;
import components.active.ChildAnalysisDataset;
import components.active.VirtualCellCollection;
import components.active.generic.UnavailableSignalGroupException;
import components.nuclear.ISignalGroup;

public class AddNuclearSignalAction extends ProgressableAction {
	
	private UUID signalGroup = null;
	
	public AddNuclearSignalAction(IAnalysisDataset dataset, MainWindow mw) {
		super(dataset, "Signal detection", mw);
	}
	
	@Override
	public void run(){
		try{
			// add dialog for non-default detection options
			SignalDetectionSettingsDialog analysisSetup = new SignalDetectionSettingsDialog(dataset);

			if(analysisSetup.isReadyToRun()){

				this.signalGroup = analysisSetup.getSignalGroup();

				String signalGroupName = dataset.getCollection()
						.getSignalGroup(signalGroup)
						.getGroupName();



				worker = new SignalDetectionWorker(dataset, 
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
			error("Error in signal analysis", e);
		}
	}
	
	@Override
	public void finished(){
		finer("Finished signal detection");
		this.cleanup(); // remove the property change listener
		
		List<ICellCollection> signalPopulations = dividePopulationBySignals(dataset.getCollection(), signalGroup);

		List<IAnalysisDataset> list = new ArrayList<IAnalysisDataset>();
		
		for(ICellCollection collection : signalPopulations){
			finer("Processing "+collection.getName());
			processSubPopulation(collection);
			finer("Processed "+collection.getName());
			list.add(dataset.getChildDataset(collection.getID()));
		}
		
		fine("Finished processing sub-populations");
		// we have morphology analysis to carry out, so don't use the super finished
		// use the same segmentation from the initial analysis
//		int flag = 0; // set the downstream analyses to run
//		flag |= MainWindow.ADD_POPULATION;
//		fine("Firing cache refresh request");
		fireDatasetEvent(DatasetEvent.ADD_DATASET, dataset);
//		fireDatasetEvent(DatasetEvent.REFRESH_CACHE, dataset);
//		fine("Running new segmentation on sub-populations");
//		new RunSegmentationAction(list, dataset, flag, mw);

		cancel();
		
	}

	/**
	 * Create child datasets for signal populations
	 * and perform basic analyses
	 * @param collection
	 */
	private void processSubPopulation(ICellCollection collection){

		try{
		finer("Creating new analysis dataset for "+collection.getName());
		
		IAnalysisDataset subDataset = new ChildAnalysisDataset(dataset, collection );

		dataset.addChildDataset(subDataset);
		dataset.getCollection().getProfileManager().copyCollectionOffsets(collection);
		
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
	private List<ICellCollection> dividePopulationBySignals(ICellCollection r, UUID signalGroup){

		List<ICellCollection> signalPopulations = new ArrayList<ICellCollection>(0);
		log("Dividing population by signals...");

		ISignalGroup group;
		try {
			group = r.getSignalGroup(signalGroup);

			group.setVisible(true);

			Set<ICell> list = r.getSignalManager().getCellsWithNuclearSignals(signalGroup, true);
			if(!list.isEmpty()){
				log("Signal group "+group.getGroupName()+": found nuclei with signals");
				ICellCollection listCollection = new VirtualCellCollection(dataset, group.getGroupName()+"_with_signals");


				for(ICell c : list){

					finer("  Added cell: "+c.getNucleus().getNameAndNumber());
					//				ICell newCell = new DefaultCell(c);
					listCollection.addCell( c );
				}
				signalPopulations.add(listCollection);

				// Only add a group of cells without signals if at least one cell does havea signal

				Set<ICell> notList = r.getSignalManager().getCellsWithNuclearSignals(signalGroup, false);
				if(!notList.isEmpty()){
					log("Signal group "+r.getSignalGroup(signalGroup).getGroupName()+": found nuclei without signals");

					ICellCollection notListCollection = new VirtualCellCollection(dataset, group.getGroupName()+"_without_signals");

					for(ICell c : notList){
						notListCollection.addCell( c);
					}

					signalPopulations.add(notListCollection);
				} else {
					finest("No cells without signals");
				}

			}

		} catch (UnavailableSignalGroupException e) {
			error("Cannot get signal group from collection", e);
			return new ArrayList<ICellCollection>(0);
		}
			
		fine("Finished dividing populations based on signals");
		return signalPopulations;
	}
}
