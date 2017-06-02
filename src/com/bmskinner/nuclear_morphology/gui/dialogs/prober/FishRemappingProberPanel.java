package com.bmskinner.nuclear_morphology.gui.dialogs.prober;

import java.awt.Color;
import java.awt.Dimension;
import java.awt.Graphics2D;
import java.awt.Image;
import java.awt.Point;
import java.awt.Rectangle;
import java.awt.Shape;
import java.awt.Window;
import java.awt.event.InputEvent;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;
import java.io.File;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;

import javax.swing.ImageIcon;
import javax.swing.JTable;
import javax.swing.table.TableModel;

import com.bmskinner.nuclear_morphology.analysis.detection.pipelines.Finder;
import com.bmskinner.nuclear_morphology.components.IAnalysisDataset;
import com.bmskinner.nuclear_morphology.components.ICell;
import com.bmskinner.nuclear_morphology.components.ICellCollection;
import com.bmskinner.nuclear_morphology.components.VirtualCellCollection;
import com.bmskinner.nuclear_morphology.components.generic.IPoint;
import com.bmskinner.nuclear_morphology.components.nuclei.Nucleus;
import com.bmskinner.nuclear_morphology.components.options.IAnalysisOptions;
import com.bmskinner.nuclear_morphology.components.options.MissingOptionException;
import com.bmskinner.nuclear_morphology.gui.ThreadManager;
import com.bmskinner.nuclear_morphology.gui.components.ColourSelecter;

/**
 * The image panel for FISH remapping. Stores the cells selected for remapping
 * and colours them appropriately.
 * 
 * @author ben
 * @since 1.13.4
 *
 */
@SuppressWarnings("serial")
public class FishRemappingProberPanel extends GenericImageProberPanel {

    private static final int    ORIGINAL_IMG_COL        = 0;
    private static final int    ORIGINAL_IMG_ROW        = 0;
    private static final double PANEL_SCREEN_WIDTH_PROP = 0.7;
    
    private static final String HEADER_LBL = "Unselected nuclei are blue. Use left and right mouse buttons to select nuclei.";

    private final IAnalysisDataset dataset;


    
    /**
     * Nuclei selected with the left button
     */
    private List<UUID> selectedNucleiLeft  = new ArrayList<UUID>(96);
    
    /**
     * Nuclei selected with the right button
     */
    private List<UUID> selectedNucleiRight = new ArrayList<UUID>(96);

    private Set<ICell> openCells = new HashSet<ICell>();

    public FishRemappingProberPanel(IAnalysisDataset dataset, Finder<?> finder, Window parent)
            throws MissingOptionException {

        super(dataset.getAnalysisOptions().getDetectionOptions(IAnalysisOptions.NUCLEUS).getFolder(), finder, parent);

        this.setHeaderLabelText(HEADER_LBL);
        this.dataset = dataset;


        // // Make sure the images fit in the table
        Dimension minPanelSize = getPreferredSize();
        minPanelSize.width = (int) (java.awt.Toolkit.getDefaultToolkit().getScreenSize().getWidth()
                * PANEL_SCREEN_WIDTH_PROP);
        setPreferredSize(minPanelSize);

        // createFileList(options.getFolder());
    }

    @Override
    protected JTable createTable(TableModel model) {
        JTable table = super.createTable(model);

        for (MouseListener l : table.getMouseListeners()) {
            table.removeMouseListener(l);
        }

        // Add listener for nucleus click
        table.addMouseListener(new MouseAdapter() {

            @Override
            public void mouseClicked(MouseEvent e) {
                if (e.getClickCount() == 1) {

                    Point pnt = e.getPoint();
                    int row = table.rowAtPoint(pnt);
                    int col = table.columnAtPoint(pnt);

                    if (row == ORIGINAL_IMG_ROW && col == ORIGINAL_IMG_COL) {

                        Runnable r = () -> {
                            smallImageClicked(e, pnt);
                        };

                        ThreadManager.getInstance().execute(r);

                    } else { // Show a large image for the FISH image when
                             // clicked

                        TableModel model = (TableModel) table.getModel();

                        ProberTableCell selectedData = (ProberTableCell) model.getValueAt(row, col);

                        if (selectedData.getLargeIcon() != null) {
                            new LargeImageDialog(selectedData, getWindow());
                        }
                    }

                }
            }

        });
        return table;
    }

    /**
     * Import the given file as an image, detect nuclei and display the image
     * with annotated nuclear outlines
     * 
     * @param imageFile
     */
    @Override
    protected void importAndDisplayImage(File imageFile) {

        // Get the cells open in this image
        super.importAndDisplayImage(imageFile);

        if (dataset.getCollection().hasCells(imageFile)) {
            openCells = dataset.getCollection().getCells(imageFile);

            ProberTableCell infoCell = (ProberTableCell) table.getModel().getValueAt(ORIGINAL_IMG_COL,
                    ORIGINAL_IMG_ROW);

            Image largeImage = infoCell.getLargeIcon().getImage();

            // Get the cells matching the imageFile
            for (ICell c : openCells) {
                drawNucleus(c, largeImage);
            }

            // Update the small icon
            infoCell.setSmallIcon(new ImageIcon(scaleImage(infoCell.getLargeIcon())));
            table.repaint();
        }

    }

    private void smallImageClicked(MouseEvent e, Point pnt) {

        IPoint p = getPointInOriginalImage(pnt);

        finer("Clicked in large image " + p.toString());

        // See if the clicked position is in a nucleus

        int row = table.rowAtPoint(pnt);
        int col = table.columnAtPoint(pnt);
        // Get the rectangle covering the cell of the table

        Rectangle cellRectangle = table.getCellRect(row, col, false);

        // Get the icon cell at the clicked row and column
        ProberTableCell selectedData = (ProberTableCell) table.getModel().getValueAt(row, col);

        for (ICell c : openCells) {

            for (Nucleus n : c.getNuclei()) {
                if (n.containsOriginalPoint(p)) {

                    updateSelectedNuclei(e, c);
                    fine("Click is in nucleus");

                    drawNucleus(c, selectedData.getLargeIcon().getImage());
                    // Update the small icon
                    selectedData.setSmallIcon(new ImageIcon(scaleImage(selectedData.getLargeIcon())));
                    table.repaint(cellRectangle); // repaint the affected cell
                                                  // only
                    return; // don't keep searching

                }
            }

        }
    }

    /**
     * Convert the coordinates clicked in the small icon to coordinates in the
     * original image.
     * 
     * @param e
     * @param pnt
     * @return
     */
    private IPoint getPointInOriginalImage(Point pnt) {
        // Get the data model for this table
        TableModel model = (TableModel) table.getModel();

        int row = table.rowAtPoint(pnt);
        int col = table.columnAtPoint(pnt);

        // The coordinates are relative to the cell of the table.
        // The height of the image is less than the table height, so
        // subtract the y difference
        double x = pnt.getX();
        double y = pnt.getY();

        finer("Clicked " + x + " : " + y);

        /*
         * The coordinates within the cell must be converted to coordinates
         * within the small image in the IconCell.
         * 
         * The x coordinates are not always correct. The IconCell is aligned
         * horizontally, so the difference in width between the IconCell and the
         * table cell can be used as an offset
         * 
         * The imageprober has vertical alignment to the top of the cell, so y
         * coordinates should also be correct
         * 
         * 
         */

        // Get the rectangle covering the cell of the table
        Rectangle cellRectangle = table.getCellRect(row, col, false);

        // Get the icon cell at the clicked row and column
        ProberTableCell selectedData = (ProberTableCell) model.getValueAt(row, col);

        // Get the width of the icon in the icon cell
        int iconWidth = selectedData.getSmallIcon().getIconWidth();

        // // Get the width of the column of interest
        int columnWidth = cellRectangle.width;

        finer("Column width is " + columnWidth);
        finer("IconCell width is " + iconWidth);

        // Split the difference
        int offset = (columnWidth - iconWidth) >> 1;

        x = x - offset;

        finer("Clicked in small image " + x + " : " + y);

        if (x < 0 || x > iconWidth) {
            return null; // out of bounds of icon
        }

        if (y > selectedData.getSmallIcon().getIconHeight()) {
            return null; // out of image bounds in cell
        }

        // Translate coordinates back to large image
        double factor = selectedData.getFactor();

        double largeX = x * factor;
        double largeY = y * factor;

        IPoint p = IPoint.makeNew(largeX, largeY);
        return p;
    }

    /**
     * Draw the cell on the given image
     * 
     * @param c
     * @param image
     */
    private void drawNucleus(ICell c, Image image) {

        Graphics2D g2 = (Graphics2D) image.getGraphics();

        Color oldColor = g2.getColor();
        g2.setColor(getCellColour(c));
        
        for(Nucleus n : c.getNuclei()){
            Shape p = n.toOriginalShape();
            g2.fill(p);
        }

        
        g2.setColor(oldColor);

    }

    /**
     * Choose the colour to fill nuclei based on whether
     * they are selected.
     * 
     * @param c
     *            the cell
     * @return the nucleus colour
     */
    private Color getCellColour(ICell c) {
        Color color = Color.BLUE;
        if (selectedNucleiLeft.contains(c.getId())) {
            color = ColourSelecter.getRemappingColour(0);

        }
        if (selectedNucleiRight.contains(c.getId())) {
            color = ColourSelecter.getRemappingColour(1);
        }

        return color;
    }

    /**
     * Create a copy of the given processor, and scale it fit the maximum
     * dimensions specified by setSmallIconSize(). The aspect ratio is
     * preserved.
     * 
     * @param ip
     * @return
     */
    protected Image scaleImage(ImageIcon ic) {

        double aspect = (double) ic.getIconWidth() / (double) ic.getIconHeight();

        Dimension smallDimension = new Dimension(500, table.getRowHeight() - 30);

        double finalWidth = smallDimension.getHeight() * aspect; // fix height
        finalWidth = finalWidth > smallDimension.getWidth() ? smallDimension.getWidth() : finalWidth; // but
                                                                                                      // constrain
                                                                                                      // width
                                                                                                      // too

        return ic.getImage().getScaledInstance((int) finalWidth, -1, Image.SCALE_SMOOTH);
    }

    /**
     * Get a list of CellCollections, containing the selected nuclei. If no
     * nuclei were selected, the list is empty
     * 
     * @return
     * @throws Exception
     */
    public List<ICellCollection> getSubCollections() {
        List<ICellCollection> result = new ArrayList<ICellCollection>(0);

        if (!selectedNucleiLeft.isEmpty()) {
            ICellCollection subCollectionLeft = new VirtualCellCollection(dataset, "SubCollectionLeft");
            for (UUID id : selectedNucleiLeft) {
                ICell cell = dataset.getCollection().getCell(id);
                subCollectionLeft.addCell(cell);
            }
            result.add(subCollectionLeft);
        }

        if (!selectedNucleiRight.isEmpty()) {
            ICellCollection subCollectionRight = new VirtualCellCollection(dataset, "SubCollectionRight");
            for (UUID id : selectedNucleiRight) {
                ICell cell = dataset.getCollection().getCell(id);
                subCollectionRight.addCell(cell);
            }
            result.add(subCollectionRight);
        }

        return result;
    }

    /**
     * Update the lists of selected nuclei based on a click.
     * 
     * @param e
     * @param c
     */
    private synchronized void updateSelectedNuclei(MouseEvent e, ICell c) {

        // if present in list, remove it, otherwise add it
        if (selectedNucleiLeft.contains(c.getId()) || selectedNucleiRight.contains(c.getId())) {

            selectedNucleiLeft.remove(c.getId());
            selectedNucleiRight.remove(c.getId());

        } else {

            if ((e.getModifiers() & InputEvent.BUTTON3_MASK) == InputEvent.BUTTON3_MASK) { // right
                                                                                           // button
                selectedNucleiRight.add(c.getId());
                selectedNucleiLeft.remove(c.getId());
            }

            if ((e.getModifiers() & InputEvent.BUTTON1_MASK) == InputEvent.BUTTON1_MASK) { // left
                                                                                           // button
                selectedNucleiLeft.add(c.getId());
                selectedNucleiRight.remove(c.getId());
            }

        }

    }
}