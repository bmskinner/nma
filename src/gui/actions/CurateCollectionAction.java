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
import analysis.nucleus.MorphologyAnalysis;

public class CurateCollectionAction extends ProgressableAction {

	public CurateCollectionAction(AnalysisDataset dataset, MainWindow mw) {
		super(dataset, "Curating collection", "Error curating collection", mw);

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
				final int flag = MainWindow.ADD_POPULATION;
				//			SwingUtilities.invokeLater(new Runnable(){
				//				public void run(){

				new MorphologyAnalysisAction(list, MorphologyAnalysis.MODE_NEW, flag, mw);

				//			}});

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
