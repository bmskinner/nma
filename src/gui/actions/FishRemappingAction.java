package gui.actions;

import java.util.ArrayList;
import java.util.List;
import java.util.logging.Level;

import analysis.AnalysisDataset;
import components.CellCollection;
import gui.MainWindow;
import gui.dialogs.FishRemappingDialog;

/**
 * Compare morphology images with post-FISH images, and select nuclei into new
 * sub-populations
 */
public class FishRemappingAction extends ProgressableAction {

	public FishRemappingAction(List<AnalysisDataset> datasets, MainWindow mw) {
		super(null, "Remapping", mw);

		try{

			if(datasets.size()==1){
				
				final AnalysisDataset dataset = datasets.get(0);
				
				FishRemappingDialog fishMapper = new FishRemappingDialog(mw, dataset, programLogger);

				if(fishMapper.getOK()){
					
					programLogger.log(Level.INFO, "Fetching collections...");
					List<CellCollection> subs = fishMapper.getSubCollections();

					final List<AnalysisDataset> newList = new ArrayList<AnalysisDataset>();
					for(CellCollection sub : subs){

						if(sub.getNucleusCount()>0){

							dataset.addChildCollection(sub);

							AnalysisDataset subDataset = dataset.getChildDataset(sub.getID());
							newList.add(subDataset);
						}
					}
					programLogger.log(Level.INFO, "Reapplying morphology...");
					new MorphologyAnalysisAction(newList, dataset, MainWindow.ADD_POPULATION, mw);

					finished();

				} else {
					programLogger.log(Level.INFO, "Remapping cancelled");
					cancel();
				}
				
			} else {
				programLogger.log(Level.INFO, "Multiple datasets selected, cancelling");
				cancel();
			}

		} catch(Exception e){
			programLogger.log(Level.SEVERE, "Error in FISH remapping: "+e.getMessage(), e);
		}
		

	}
	
	@Override
	public void finished(){
		// Do not use super.finished(), or it will trigger another save action
		log(Level.FINE, "FISH mapping complete");
		cancel();		
		this.removeInterfaceEventListener(mw);
		this.removeDatasetEventListener(mw);		
	}
}
