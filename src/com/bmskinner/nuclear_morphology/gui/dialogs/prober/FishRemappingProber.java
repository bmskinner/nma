package com.bmskinner.nuclear_morphology.gui.dialogs.prober;

import java.awt.BorderLayout;
import java.io.File;
import java.util.ArrayList;
import java.util.List;

import javax.swing.JPanel;

import com.bmskinner.nuclear_morphology.components.IAnalysisDataset;
import com.bmskinner.nuclear_morphology.components.ICellCollection;
import com.bmskinner.nuclear_morphology.components.options.IAnalysisOptions;

public class FishRemappingProber extends IntegratedImageProber {
	
	private static final String DIALOG_TITLE_BAR_LBL = "FISH remapping";

	final IAnalysisDataset dataset;
	final List<IAnalysisDataset> newList = new ArrayList<IAnalysisDataset>();

	/**
	 * Create with a dataset (from which nuclei will be drawn) and a folder of images to
	 * be analysed
	 * @param dataset the analysis dataset
	 * @param folder the folder of images
	 */
	public FishRemappingProber(final IAnalysisDataset dataset, final File fishImageDir){
		this.dataset = dataset;
		
		try {
			
			// make the panel

			imageProberPanel     = new FishRemappingProberPanel(this, 
					dataset.getAnalysisOptions().getDetectionOptions(IAnalysisOptions.NUCLEUS), 
					ImageSet.FISH_REMAPPING_IMAGE_SET, 
					dataset,
					fishImageDir);
			
			JPanel footerPanel   = createFooter();
			
			this.add(imageProberPanel,     BorderLayout.CENTER);
			this.add(footerPanel,          BorderLayout.SOUTH);

			this.setTitle(DIALOG_TITLE_BAR_LBL);
			
//			optionsSettingsPanel.addProberReloadEventListener(imageProberPanel);
			
			
		} catch (Exception e){
			warn("Error launching analysis window");
			stack(e.getMessage(), e);
			this.dispose();
		}	

		this.pack();
		this.setModal(true);
		this.setLocationRelativeTo(null); // centre on screen
		this.setVisible(true);
	}

	@Override
	protected void okButtonClicked() {

		List<ICellCollection> subs = ((FishRemappingProberPanel) imageProberPanel).getSubCollections();

		if(subs.isEmpty()){
			
			return;
		}

		for(ICellCollection sub : subs){

			if(sub.hasCells()){

				dataset.addChildCollection(sub);

				final IAnalysisDataset subDataset = dataset.getChildDataset(sub.getID());
				newList.add(subDataset);
			}
		}
	}
	
	public List<IAnalysisDataset> getNewDatasets(){
		return newList;
	}

}
