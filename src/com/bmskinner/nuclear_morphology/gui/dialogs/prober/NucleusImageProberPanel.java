package com.bmskinner.nuclear_morphology.gui.dialogs.prober;

import java.awt.Dimension;
import java.awt.Window;
import java.io.File;

import com.bmskinner.nuclear_morphology.components.options.IDetectionOptions;
import com.bmskinner.nuclear_morphology.gui.dialogs.prober.workers.NucleusProberWorker;


@SuppressWarnings("serial")
public class NucleusImageProberPanel extends ImageProberPanel {
	
	private static final int SMALL_ICON_WIDTH = 500;
	private static final String NULL_FILE_ERROR = "File is null";
		
	public NucleusImageProberPanel(final Window parent, final IDetectionOptions options, final ImageSet set){
		super(parent, options, set);
		
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
			
			worker = new NucleusProberWorker(imageFile, 
					options, 
					imageSet, 
					table.getModel());
			
			worker.setSmallIconSize(new Dimension(SMALL_ICON_WIDTH, table.getRowHeight()-30));
			
			worker.addPropertyChangeListener(this);
			progressBar.setVisible(true);
			worker.execute();


		} catch (Exception e) { // end try
			error(e.getMessage(), e);
		} 
	}

}
