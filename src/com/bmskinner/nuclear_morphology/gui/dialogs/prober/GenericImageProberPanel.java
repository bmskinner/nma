/*******************************************************************************
 * Copyright (C) 2017 Ben Skinner
 * 
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 * 
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 * 
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.\
 *******************************************************************************/


package com.bmskinner.nuclear_morphology.gui.dialogs.prober;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.Image;
import java.awt.Point;
import java.awt.Window;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.io.File;
import java.util.ArrayList;
import java.util.EventObject;
import java.util.Iterator;
import java.util.List;
import java.util.stream.Stream;

import javax.swing.BorderFactory;
import javax.swing.BoxLayout;
import javax.swing.ImageIcon;
import javax.swing.JButton;
import javax.swing.JDialog;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JProgressBar;
import javax.swing.JScrollPane;
import javax.swing.JTable;
import javax.swing.border.EmptyBorder;
import javax.swing.table.DefaultTableCellRenderer;
import javax.swing.table.TableModel;

import com.bmskinner.nuclear_morphology.analysis.detection.pipelines.Finder;
import com.bmskinner.nuclear_morphology.components.options.MissingOptionException;
import com.bmskinner.nuclear_morphology.core.ThreadManager;
import com.bmskinner.nuclear_morphology.io.ImageImporter;
import com.bmskinner.nuclear_morphology.logging.Loggable;

/**
 * An basic implementation of the image prober panel
 * 
 * @author bms41
 * @since 1.13.5
 *
 */
@SuppressWarnings("serial")
public class GenericImageProberPanel extends JPanel implements Loggable, ProberReloadEventListener {

    /*
     * STATIC DISPLAY VALUES
     */
    // public static final int DEFAULT_COLUMN_COUNT = 2;

    protected static final String NULL_FILE_ERROR = "File is null";

    private static final String HEADER_LBL              = "Objects meeting detection parameters "
            + "are outlined in yellow; "
            + "other objects are red. "
            + "Click an image to view larger version.";
    
    private static final String FOLDER_LBL              = "Image ";
    private static final String PREV_IMAGE_BTN          = "Prev";
    private static final String NEXT_IMAGE_BTN          = "Next";
    private static final String WORKING_LBL             = "Working...";
    private static final String LOOKING_LBL             = "Looking for images...";
    private static final double IMAGE_SCREEN_PROPORTION = 0.80;

    /*
     * PRIVATE VALUES
     */

    private Window                           parent;
    private JLabel                           imageLabel;
    private JLabel                           headerLabel; // Basic info. Default to HEADER_LBL

    private List<PanelUpdatingEventListener> updatingListeners = new ArrayList<PanelUpdatingEventListener>();

    /*
     * PROTECTED VALUES
     */

    protected Finder<?> finder;

    protected static final int SMALL_ICON_MAX_WIDTH  = 500;
    protected static final int SMALL_ICON_MAX_HEIGHT = 500;

    protected JProgressBar progressBar;
    protected JTable       table;
    protected List<File>   imageFiles;    // the list of image files
    protected File         openImage;     // the image currently open
    protected int          fileIndex = 0; // the index of the open file

    protected final File folder; // the folder of files
    
    private JButton nextButton;
    private JButton prevButton;

    /**
     * Constructor
     * @param folder the image folder
     * @param finder the finder to use
     * @param parent the parent window to the finder
     * @throws MissingOptionException if the options cannot be found
     */
    public GenericImageProberPanel(File folder, Finder<?> finder, Window parent) throws MissingOptionException {

        this.folder = folder;
        this.parent = parent;
        this.finder = finder;
        createUI();
    }

    /**
     * Import the given file as an image, detect objects and display the image
     * with annotated outlines
     * 
     * @param imageFile the image to search
     */
    protected void importAndDisplayImage(File imageFile) {
        if (imageFile == null)
            throw new IllegalArgumentException(NULL_FILE_ERROR);
        
        try {
            finer("Firing panel updating event");
            int imageNumber = fileIndex+1;
            setImageLabel(FOLDER_LBL+ imageNumber+" of "+imageFiles.size()+ ": "+imageFile.getAbsolutePath());
            firePanelUpdatingEvent(PanelUpdatingEvent.UPDATING);
            progressBar.setVisible(true);
            nextButton.setEnabled(false);
            prevButton.setEnabled(false);

            finder.findInImage(imageFile);

        } catch (Exception e) { // end try
            warn(e.getMessage());
            stack(e.getMessage(), e);
            setImageLabel("Error probing " + imageFile.getAbsolutePath());

        } finally {

            progressBar.setVisible(false);
            nextButton.setEnabled(true);
            prevButton.setEnabled(true);
            firePanelUpdatingEvent(PanelUpdatingEvent.COMPLETE);
        }
    }

    protected void createUI() {
        setLayout(new BorderLayout());

        JPanel headerPanel = createHeader();
        JPanel tablePanel = createTablePanel();

        nextButton = new JButton(NEXT_IMAGE_BTN);
        nextButton.addActionListener(e -> {
            openImage = getNextImage();
            imageLabel.setText(openImage.getAbsolutePath());
            run();
        });

        prevButton = new JButton(PREV_IMAGE_BTN);
        prevButton.addActionListener(e -> {
            openImage = getPrevImage();
            imageLabel.setText(openImage.getAbsolutePath());
            run();
        });

        this.add(headerPanel, BorderLayout.NORTH);
        this.add(tablePanel, BorderLayout.CENTER);
        this.add(nextButton, BorderLayout.EAST);
        this.add(prevButton, BorderLayout.WEST);

        createFileList(folder);
    }

    public void run() {

        ((ProberTableModel) table.getModel()).setRowCount(0);
        Runnable r = () -> {
            try {
                importAndDisplayImage(openImage);
            } catch (Exception e) {
                error("Error in prober", e);
            }
        };
        ThreadManager.getInstance().submit(r);
    }

    protected Window getWindow() {
        return parent;
    }

    /**
     * Set the text in the header label
     * 
     * @param s
     */
    protected void setImageLabel(String s) {
        imageLabel.setText(s);
    }

    /**
     * Create a list of image files in the given folder
     * 
     * @param folder
     */
    protected void createFileList(final File folder) {

        finest("Generating file list from " + folder.getAbsolutePath());

        imageFiles = importImages(folder);

        if (imageFiles.size() > 0) {
            openImage = imageFiles.get(fileIndex);
            run();
        } else {
            warn("No images found in folder");
            JOptionPane.showMessageDialog(GenericImageProberPanel.this, "No images found in folder.", "Nope.",
                    JOptionPane.ERROR_MESSAGE);
        }
    }

    /**
     * Check each file in the given folder for suitability If the folder
     * contains folders, check recursively
     * 
     * @param folder
     *            the folder to check
     * @return a list of image files
     */
    private List<File> importImages(final File folder) {

        List<File> files = new ArrayList<File>();
        if (folder.listFiles() == null) {
            return files;
        }

        Stream.of(folder.listFiles()).forEach(file -> {

            boolean ok = ImageImporter.fileIsImportable(file); // check file
                                                               // extension

            if (ok) {
                files.add(file);
            }

            if (file.isDirectory()) {
                files.addAll(importImages(file));
            }

        });

        return files;
    }

    /**
     * Get the next image in the file list
     * 
     * @return
     */
    private File getNextImage() {

        if (fileIndex >= imageFiles.size() - 1) {
            fileIndex = 0;
        } else {
            fileIndex++;
        }
        File f = imageFiles.get(fileIndex);
        return f;

    }

    /**
     * Get the previous image in the file list
     * 
     * @return
     */
    private File getPrevImage() {

        if (fileIndex <= 0) {
            fileIndex = imageFiles.size() - 1;
        } else {
            fileIndex--;
        }

        File f = imageFiles.get(fileIndex);
        return f;
    }

    public void cancel() {
        progressBar.setVisible(false);
        progressBar.setValue(0);
        firePanelUpdatingEvent(PanelUpdatingEvent.COMPLETE);
    }

    private JPanel createTablePanel() {
        JPanel panel = new JPanel(new BorderLayout());
        panel.setBorder(new EmptyBorder(5, 5, 5, 5));

        ProberTableModel model = new ProberTableModel();

        finder.addDetectionEventListener(model);

        table = createTable(model);

        JScrollPane scrollPane = new JScrollPane(table);
        panel.add(scrollPane, BorderLayout.CENTER);

        progressBar = new JProgressBar();
        progressBar.setIndeterminate(true);
        progressBar.setString(WORKING_LBL);
        progressBar.setStringPainted(true);
        progressBar.setVisible(false);
        panel.add(progressBar, BorderLayout.SOUTH);
        return panel;
    }

    /**
     * Make the header panel with status label
     * 
     * @return
     */
    private JPanel createHeader() {
        JPanel panel = new JPanel();
        panel.setLayout(new BoxLayout(panel, BoxLayout.Y_AXIS));

        headerLabel = new JLabel(HEADER_LBL);
        panel.add(headerLabel);

        imageLabel = new JLabel(LOOKING_LBL);
        panel.add(imageLabel);
        
        panel.setBorder(BorderFactory.createEmptyBorder(0, 5, 0, 5));

        return panel;
    }
    
    /**
     * Set the text on the header label of the prober.
     * @param s the new text
     */
    protected void setHeaderLabelText(String s){
        headerLabel.setText(s);
    }

    protected JTable createTable(TableModel model) {
        JTable table = new JTable(model);
        
        int maxDimension = 200;
        if(model instanceof ProberTableModel)
        	maxDimension = ((ProberTableModel) model).getMaxDimension();
        
        table.setRowHeight(maxDimension);
        for (int i = 0; i < table.getColumnCount(); i++) {
            table.getColumnModel().getColumn(i).setCellRenderer(new IconCellRenderer());
        }

        table.setTableHeader(null);
        table.setCellSelectionEnabled(true);
        table.setRowSelectionAllowed(false);
        table.setColumnSelectionAllowed(false);

        table.addMouseListener(new MouseAdapter() {

            @Override
            public void mouseClicked(MouseEvent e) {
                if (e.getClickCount() == 1) {

                    // Get the data model for this table
                    TableModel model = (TableModel) table.getModel();

                    Point pnt = e.getPoint();
                    int row = table.rowAtPoint(pnt);
                    int col = table.columnAtPoint(pnt);

                    ProberTableCell selectedData = (ProberTableCell) model.getValueAt(row, col);

                    if (selectedData != null) {

                        if (selectedData.getLargeIcon() != null) {
                            new LargeImageDialog(selectedData, parent);
                        }
                    }

                }
            }

        });
        return table;
    }

    public static class ProberTableCell {
        private ImageIcon smallIcon;
        private ImageIcon largeIcon;
        private boolean   enabled;
        private String    label;

        public ProberTableCell(ImageIcon largeIcon, String label, boolean enabled) {
            this.largeIcon = largeIcon;
            this.enabled = enabled;
            this.label = label;
        }

        public String toString() {
            if (enabled) {
                return label;
            } else {
                return label + " (disabled)";
            }
        }

        public ImageIcon getSmallIcon() {
            return smallIcon;
        }

        public ImageIcon getLargeIcon() {
            return largeIcon;
        }

        /**
         * Create a new image icon from scaling the large image to a given
         * fraction of the screen size and maintaining aspect ratio. I.E.: if
         * resizing the width to <i>fraction</i> of the screen width results in
         * the height begin greater than <i>fraction</i> of the screen height,
         * the width will be reduced so height equals <i>fraction</i> of the
         * screen height
         * 
         * @param fraction
         *            the fraction, from 0-1
         * @return
         */
        public ImageIcon getLargeIconFitToScreen(double fraction) {

            if (largeIcon == null) {
                throw new IllegalArgumentException("Large icon is null");
            }

            int originalWidth = largeIcon.getImage().getWidth(null);
            int originalHeight = largeIcon.getImage().getHeight(null);

            // keep the image aspect ratio
            double ratio = (double) originalWidth / (double) originalHeight;

            Dimension screenSize = java.awt.Toolkit.getDefaultToolkit().getScreenSize();

            // set the new width
            int newWidth = (int) (screenSize.getWidth() * fraction);
            int newHeight = (int) ((double) newWidth / ratio);

            // Check height is OK. If not, recalculate sizes
            if (newHeight >= screenSize.getHeight()) {
                newHeight = (int) (screenSize.getHeight() * fraction);
                newWidth = (int) ((double) newHeight * ratio);
            }

            // Create the image

            Image result = largeIcon.getImage().getScaledInstance(newWidth, newHeight, Image.SCALE_FAST);

            return new ImageIcon(result);
        }

        public void setSmallIcon(ImageIcon smallIcon) {
            this.smallIcon = smallIcon;
        }

        public boolean hasSmallIcon() {
            return smallIcon != null;
        }

        public boolean hasLargeIcon() {
            return largeIcon != null;
        }

        public boolean isEnabled() {
            return enabled;
        }

        public void setEnabled(boolean enabled) {
            this.enabled = enabled;
        }

        public double getFactor() {
            // Translate coordinates back to large image
            double factor = (double) largeIcon.getIconWidth() / (double) smallIcon.getIconWidth();
            return factor;
        }
    }

    /**
     * This renderer displays the small icons from an ImageProberTableCell, and
     * sets text appropriate to the label within the cell.
     * 
     * @author ben
     *
     */
    class IconCellRenderer extends DefaultTableCellRenderer {
        @Override
        public Component getTableCellRendererComponent(JTable table, Object value, boolean isSelected, boolean hasFocus,
                int row, int column) {
            try {
                super.getTableCellRendererComponent(table, value, isSelected, hasFocus, row, column);

                ProberTableCell info = (ProberTableCell) value;

                setTextHorizontalAlignment(JLabel.CENTER);
                setHorizontalTextPosition(JLabel.CENTER);
                setVerticalTextPosition(JLabel.BOTTOM);

                setHorizontalAlignment(JLabel.CENTER);
                setVerticalAlignment(JLabel.CENTER); // image has no offset
                setBackground(Color.LIGHT_GRAY); // contrast to images with
                                                 // white background
                setText("");

                if (info == null) {
                    setText("");
                    return this;
                } else {
                    setText(info.toString());
                }

                if (info.hasSmallIcon()) {
                    setIcon(info.getSmallIcon());
                } else {
                    setIcon(null);
                }

            } catch (Exception e) {
                error("Renderer error", e);
            }

            return this;
        }

        private void setTextHorizontalAlignment(int center) {
            // TODO Auto-generated method stub

        }

    }

    /**
     * Show images in a non-modal window at IMAGE_SCREEN_PROPORTION of the
     * screen width or size
     *
     */
    public class LargeImageDialog extends JDialog {

        // public static final double DEFAULT_SCREEN_PROPORTION = 0.9;

        /**
         * Create a full-scale image for the given key in this ImageProber.
         * 
         * @param key
         *            the image to show
         * @param parent
         *            the parent ImageProber window
         */
        public LargeImageDialog(final ProberTableCell cell, final Window parent) {
            super(parent);

            this.setDefaultCloseOperation(DISPOSE_ON_CLOSE);

            final ImageIcon icon = cell.getLargeIconFitToScreen(IMAGE_SCREEN_PROPORTION);

            this.setLayout(new BorderLayout());

            this.add(new JLabel(icon), BorderLayout.CENTER);
            this.setTitle(cell.toString());

            this.setModal(false);
            this.setResizable(false);
            this.pack();
            this.setLocationRelativeTo(null);
            this.setVisible(true);
        }

    }


    @Override
    public void proberReloadEventReceived(ProberReloadEvent e) {
        run();
    }

    public void addPanelUpdatingEventListener(PanelUpdatingEventListener l) {
        updatingListeners.add(l);
    }

    public void removePanelUpdatingEventListener(PanelUpdatingEventListener l) {
        updatingListeners.remove(l);
    }

    protected void firePanelUpdatingEvent(int type) {
        Iterator<PanelUpdatingEventListener> it = updatingListeners.iterator();
        PanelUpdatingEvent e = new PanelUpdatingEvent(this, type);
        while (it.hasNext()) {
            it.next().panelUpdatingEventReceived(e);
        }
    }

    public interface PanelUpdatingEventListener {
        void panelUpdatingEventReceived(PanelUpdatingEvent e);
    }

    
    public class PanelUpdatingEvent extends EventObject {

        public static final int UPDATING = 0;
        public static final int COMPLETE = 1;

        private int type;

        public PanelUpdatingEvent(Object source, int type) {
            super(source);
            this.type = type;
        }

        public int getType() {
            return type;
        }

    }

}
