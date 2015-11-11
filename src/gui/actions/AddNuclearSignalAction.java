package gui.actions;

import gui.MainWindow;
import gui.SignalDetectionSettingsWindow;

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
		super(dataset, "Signal detection", "Error in signal detection", mw);

		try{
			// add dialog for non-default detection options
			SignalDetectionSettingsWindow analysisSetup = new SignalDetectionSettingsWindow(dataset, programLogger);

			if(analysisSetup.isOK()){

				this.signalGroup = analysisSetup.getSignalGroup();
				//				this.signalGroup = newSignalGroup;
				String signalGroupName = dataset.getSignalGroupName(signalGroup);


				worker = new SignalDetector(dataset, analysisSetup.getFolder(), analysisSetup.getChannel(), dataset.getAnalysisOptions().getNuclearSignalOptions(signalGroupName), signalGroup, signalGroupName);
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
		new MorphologyAnalysisAction(list, dataset, null, mw);
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

	/*
    Given a complete collection of nuclei, split it into up to 4 populations;
      nuclei with red signals, with green signals, without red signals and without green signals
    Only include the 'without' populations if there is a 'with' population.
	 */
	private List<CellCollection> dividePopulationBySignals(CellCollection r, int signalGroup){

		List<CellCollection> signalPopulations = new ArrayList<CellCollection>(0);
		programLogger.log(Level.INFO, "Dividing population by signals...");
		try{

			List<Cell> list = r.getCellsWithNuclearSignals(signalGroup, true);
			if(!list.isEmpty()){
				programLogger.log(Level.INFO, "Found nuclei with signals in group "+signalGroup);
				CellCollection listCollection = new CellCollection(r.getFolder(), 
						r.getOutputFolderName(), 
						"Signals_in_group_"+signalGroup, 
						r.getDebugFile(), 
						r.getNucleusType());

				for(Cell c : list){
					listCollection.addCell( new Cell(c) );
				}
				signalPopulations.add(listCollection);

				List<Cell> notList = r.getCellsWithNuclearSignals(signalGroup, false);
				if(!notList.isEmpty()){
					programLogger.log(Level.INFO, "Found nuclei without signals in group "+signalGroup);
					CellCollection notListCollection = new CellCollection(r.getFolder(), 
							r.getOutputFolderName(), 
							"No_signals_in_group_"+signalGroup, 
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
