package com.bmskinner.nuclear_morphology.gui.dialogs.prober;

import java.awt.Dimension;
import java.awt.Window;
import java.io.File;
import java.util.Set;

import com.bmskinner.nuclear_morphology.components.IAnalysisDataset;
import com.bmskinner.nuclear_morphology.components.nuclei.Nucleus;
import com.bmskinner.nuclear_morphology.components.options.IAnalysisOptions;
import com.bmskinner.nuclear_morphology.components.options.IDetectionOptions;
import com.bmskinner.nuclear_morphology.gui.dialogs.prober.workers.SignalProberWorker;

@SuppressWarnings("serial")
public class SignalImageProberPanel extends ImageProberPanel {

	private final IAnalysisDataset dataset;
	
	public SignalImageProberPanel(final Window parent, final IDetectionOptions options, final ImageSet set, final IAnalysisDataset dataset){
		super(parent, options, set);
		this.dataset = dataset;
		createFileList(options.getFolder());
	}
	
	/**
	 * Import the given file as an image, detect nuclei and
	 * display the image with annotated nuclear outlines
	 * @param imageFile
	 */
	@Override
	protected void importAndDisplayImage(File imageFile){

		if(imageFile==null){
			throw new IllegalArgumentException(NULL_FILE_ERROR);
		}
		
		try {

			setImageLabel(imageFile.getAbsolutePath());
			
			
			table.setModel(createEmptyTableModel(rows, cols));
			
			for(int col=0; col<cols; col++){
	        	table.getColumnModel().getColumn(col).setCellRenderer(new IconCellRenderer());
	        }
			
			// fetch nuclei based on file names
			File nucleusFolder = dataset.getAnalysisOptions()
					.getDetectionOptions(IAnalysisOptions.NUCLEUS)
					.getFolder();
			
			String imageName = imageFile.getName();
			
			File nucleusFile = new File(nucleusFolder, imageName);
			
			Set<Nucleus> list = dataset.getCollection().getNuclei(nucleusFile);
			
			worker = new SignalProberWorker(imageFile, 
					options, 
					imageSet, 
					list,
					table.getModel());
			
			worker.setSmallIconSize(new Dimension(SMALL_ICON_MAX_WIDTH, table.getRowHeight()-30));
			
			worker.addPropertyChangeListener(this);
			progressBar.setVisible(true);
			firePanelUpdatingEvent(PanelUpdatingEvent.UPDATING);
			worker.execute();


		} catch (Exception e) { // end try
			error(e.getMessage(), e);
		} 
	}
}
