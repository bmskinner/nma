package com.bmskinner.nuclear_morphology.gui.dialogs.prober;

import javax.swing.ImageIcon;
import javax.swing.table.DefaultTableModel;

import com.bmskinner.nuclear_morphology.analysis.detection.pipelines.Finder.*;
import com.bmskinner.nuclear_morphology.analysis.image.ImageFilterer;
import com.bmskinner.nuclear_morphology.gui.dialogs.prober.AbstractImageProberPanel.ProberTableCell;

import ij.process.ImageProcessor;

public class ProberTableModel extends DefaultTableModel implements DetectionEventListener {

	private static final long serialVersionUID = 1L;

	public ProberTableModel(){
		super();	
		this.setColumnCount(2);
		this.setColumnIdentifiers(new Object[]{ "Process", "Preview"});
	}

	@Override
	public void detectionEventReceived(DetectionEvent e) {
		ProberTableCell cell = makeIconCell(e.getProcessor(), true);
		addRow( new Object[] {e.getMessage(), cell});
		
	}
	
	/**
	 * Create a table cell from the given image, specifying the image type and enabled
	 * @param ip
	 * @param enabled
	 * @param type
	 * @return
	 */
	protected ProberTableCell makeIconCell(ImageProcessor ip, boolean enabled){
		
		ImageFilterer filt = new ImageFilterer(ip);
//		ImageIcon ic = filt.fitToScreen().toImageIcon(); // This causes problems when drawing overlay nuclei based on original image size
		ImageIcon ic = filt.toImageIcon();
		ProberTableCell iconCell = new ProberTableCell( ic, enabled);
		
		ImageIcon small = filt.resize( (int) 200, (int) 200)
				.toImageIcon();
						
		iconCell.setSmallIcon( small );
		return iconCell;
	}

}
