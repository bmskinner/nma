package com.bmskinner.nuclear_morphology.gui.dialogs.prober;

import javax.swing.ImageIcon;
import javax.swing.table.DefaultTableModel;

import com.bmskinner.nuclear_morphology.analysis.detection.pipelines.Finder.DetectionEvent;
import com.bmskinner.nuclear_morphology.analysis.detection.pipelines.Finder.DetectionEventListener;
import com.bmskinner.nuclear_morphology.analysis.image.ImageConverter;
import com.bmskinner.nuclear_morphology.analysis.image.ImageFilterer;
import com.bmskinner.nuclear_morphology.gui.dialogs.prober.GenericImageProberPanel.ProberTableCell;

import ij.process.ImageProcessor;

public class ProberTableModel extends DefaultTableModel implements DetectionEventListener {

    private static final long serialVersionUID = 1L;

    /**
     * Default constructor with two columns 
     */
    public ProberTableModel() {
        super();
        this.setColumnCount(2);
        // this.setColumnIdentifiers(new Object[]{ "Process", "Preview"});
    }

    @Override
    public void detectionEventReceived(DetectionEvent e) {
        ProberTableCell cell = makeIconCell(e.getProcessor(), e.getMessage(), true);

        ProberTableCell blank = makeIconCell(ImageConverter.createBlankImage(200, 200), "", true);

        if (getRowCount() == 0) {
            addRow(new ProberTableCell[] { cell, blank });
            return;
        }

        ProberTableCell existing = (ProberTableCell) getValueAt(getRowCount() - 1, 1);

        if (existing.toString().equals("")) { // Blank processor
            this.setValueAt(cell, getRowCount() - 1, 1);
        } else {
            addRow(new ProberTableCell[] { cell, blank });
        }

    }

    /**
     * Create a table cell from the given image, specifying the image type and
     * enabled
     * 
     * @param ip the image processor
     * @param label the label for the cell
     * @param enabled is the cell enabled
     * @return a table cell for the image prober
     */
    protected ProberTableCell makeIconCell(ImageProcessor ip, String label, boolean enabled) {

        ImageFilterer filt = new ImageFilterer(ip);
        ImageIcon ic = filt.toImageIcon();
        ProberTableCell iconCell = new ProberTableCell(ic, label, enabled);

        ImageIcon small = filt.resize((int) 200, (int) 200).toImageIcon();

        iconCell.setSmallIcon(small);
        return iconCell;
    }

}
