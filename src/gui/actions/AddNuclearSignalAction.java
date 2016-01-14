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

import gui.InterfaceEvent.InterfaceMethod;
import gui.dialogs.SignalDetectionSettingsDialog;
import gui.MainWindow;

import java.util.ArrayList;
import java.util.List;
import java.util.logging.Level;

import analysis.AnalysisDataset;
import analysis.nucleus.SignalDetector;
import components.Cell;
import components.CellCollection;

public class AddNuclearSignalAction extends ProgressableAction {
	
	private int signalGroup = 0;
	
	public AddNuclearSignalAction(AnalysisDataset dataset, MainWindow mw) {
		super(dataset, "Signal detection", mw);

		try{
			// add dialog for non-default detection options
			SignalDetectionSettingsDialog analysisSetup = new SignalDetectionSettingsDialog(dataset, programLogger);

			if(analysisSetup.isReadyToRun()){

				this.signalGroup = analysisSetup.getSignalGroup();
				//				this.signalGroup = newSignalGroup;
				String signalGroupName = dataset.getSignalGroupName(signalGroup);


				worker = new SignalDetector(dataset, analysisSetup.getFolder(), analysisSetup.getChannel(), dataset.getAnalysisOptions().getNuclearSignalOptions(signalGroupName), signalGroup, signalGroupName, mw.getProgramLogger());
				this.setProgressMessage("Signal detection: "+signalGroupName);
				worker.addPropertyChangeListener(this);
				worker.execute();
			} else {
				this.cancel();
				return;
			}

			
		} catch (Exception e){
			this.cancel();
			programLogger.log(Level.SEVERE, "Error in signal analysis", e);
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
		new RunSegmentationAction(list, dataset, null, mw);

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

		programLogger.log(Level.INFO, "Sub-population: "+collection.getType()+" : "+collection.getNucleusCount()+" nuclei");

		dataset.addChildDataset(subDataset);
	}


	/**
	 * Create two child populations for the given dataset: one with signals in the 
	 * given group, and one without signals 
	 * @param r the collection to split
	 * @param signalGroup the signal group to split on
	 * @return a list of new collections
	 */
	private List<CellCollection> dividePopulationBySignals(CellCollection r, int signalGroup){

		List<CellCollection> signalPopulations = new ArrayList<CellCollection>(0);
		programLogger.log(Level.INFO, "Dividing population by signals...");
		try{

			
			List<Cell> list = r.getCellsWithNuclearSignals(signalGroup, true);
			if(!list.isEmpty()){
				programLogger.log(Level.INFO, "Signal group "+signalGroup+": found nuclei with signals");
				CellCollection listCollection = new CellCollection(r.getFolder(), 
						r.getOutputFolderName(), 
						"SignalGroup_"+signalGroup+"_with_signals", 
						r.getDebugFile(), 
						r.getNucleusType());

				for(Cell c : list){
					programLogger.log(Level.FINEST, "  Added cell: "+c.getNucleus().getNameAndNumber());
					programLogger.log(Level.FINEST, "  Cell has: "+c.getNucleus().getSignalCount()+" signals");
					Cell newCell = new Cell(c);
					programLogger.log(Level.FINEST, "  New cell has: "+newCell.getNucleus().getSignalCount()+" signals");
					listCollection.addCell( newCell );
				}
				signalPopulations.add(listCollection);

				List<Cell> notList = r.getCellsWithNuclearSignals(signalGroup, false);
				if(!notList.isEmpty()){
					programLogger.log(Level.INFO, "Signal group "+signalGroup+": found nuclei without signals");
					CellCollection notListCollection = new CellCollection(r.getFolder(), 
							r.getOutputFolderName(), 
							"SignalGroup_"+signalGroup+"_without_signals", 
							r.getDebugFile(), 
							r.getNucleusType());

					for(Cell c : notList){
						notListCollection.addCell( new Cell(c) );
					}
					signalPopulations.add(notListCollection);
				}

			}

		} catch(Exception e){
			programLogger.log(Level.SEVERE, "Cannot create collection", e);
		}

		return signalPopulations;
	}
}
