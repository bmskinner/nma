package com.bmskinner.nuclear_morphology.gui.dialogs.prober;

import javax.swing.ImageIcon;
import javax.swing.table.DefaultTableModel;

import com.bmskinner.nuclear_morphology.analysis.detection.pipelines.NeutrophilDetectonTest.DetectionEvent;
import com.bmskinner.nuclear_morphology.analysis.detection.pipelines.NeutrophilDetectonTest.DetectionEventListener;
import com.bmskinner.nuclear_morphology.analysis.image.ImageFilterer;

import ij.process.ImageProcessor;

public class ProberTableModel extends DefaultTableModel implements DetectionEventListener {

	private static final long serialVersionUID = 1L;

	public ProberTableModel(){
		super();	
		this.setColumnCount(2);
		this.setColumnIdentifiers(new Object[]{ "Desc", "Preview"});
	}

	@Override
	public void detectionEventReceived(DetectionEvent e) {
		ImageProberTableCell cell = makeIconCell(e.getProcessor(), true, DetectionImageType.DETECTED_OBJECTS);
		addRow( new Object[] {e.getMessage(), cell});
		
	}
	
	/**
	 * Create a table cell from the given image, specifying the image type and enabled
	 * @param ip
	 * @param enabled
	 * @param type
	 * @return
	 */
	protected ImageProberTableCell makeIconCell(ImageProcessor ip, boolean enabled, ImageType type){
		
		ImageFilterer filt = new ImageFilterer(ip);
//		ImageIcon ic = filt.fitToScreen().toImageIcon(); // This causes problems when drawing overlay nuclei based on original image size
		ImageIcon ic = filt.toImageIcon();
		ImageProberTableCell iconCell = new ImageProberTableCell( ic, type, enabled, 0);
		
		ImageIcon small = filt.resize( (int) 200, (int) 200)
				.toImageIcon();
						
		iconCell.setSmallIcon( small );
		return iconCell;
	}

}
