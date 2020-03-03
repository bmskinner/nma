/*******************************************************************************
 * Copyright (C) 2018 Ben Skinner
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
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 ******************************************************************************/
package com.bmskinner.nuclear_morphology.gui.dialogs.prober;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Component;
import java.awt.Point;
import java.awt.Window;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.io.File;
import java.util.Collection;
import java.util.logging.Logger;

import javax.swing.ImageIcon;
import javax.swing.JButton;
import javax.swing.JDialog;
import javax.swing.JLabel;
import javax.swing.JScrollPane;
import javax.swing.JTable;
import javax.swing.table.DefaultTableCellRenderer;
import javax.swing.table.TableModel;

import com.bmskinner.nuclear_morphology.analysis.detection.pipelines.Finder;
import com.bmskinner.nuclear_morphology.analysis.detection.pipelines.NeutrophilFinder;
import com.bmskinner.nuclear_morphology.components.ICell;
import com.bmskinner.nuclear_morphology.components.options.OptionsFactory;
import com.bmskinner.nuclear_morphology.core.ThreadManager;
import com.bmskinner.nuclear_morphology.gui.dialogs.SettingsDialog;
import com.bmskinner.nuclear_morphology.logging.Loggable;

/**
 * This test class demostrates using the DetectionEventListener interface to
 * fill a table model as detection occurs
 * 
 * @author ben
 * @since 1.13.5
 *
 */
public class DemoProber extends SettingsDialog {
	
	private static final Logger LOGGER = Logger.getLogger(DemoProber.class.getName());

    private Finder<Collection<ICell>> test;
    JButton                     runButton;

    public DemoProber(File folder) {
        super();
        try {
            ProberTableModel model = new ProberTableModel();

            test = new NeutrophilFinder(OptionsFactory.makeDefaultNeutrophilDetectionOptions(folder));
            test.addDetectionEventListener(model);

            JTable table = createTable(model);

            runButton = new JButton("Run");
            runButton.addActionListener(e -> {
                run();
            });
            add(runButton, BorderLayout.NORTH);

            JScrollPane scrollPane = new JScrollPane(table);
            add(scrollPane, BorderLayout.CENTER);
            pack();
            setModal(true);
            setVisible(true);

        } catch (Exception e) {
            LOGGER.log(Loggable.STACK, "Error in prober", e);
        }
    }

    public void run() {
        Runnable r = () -> {
            try {
                runButton.setEnabled(false);
                Collection<ICell> cells = test.find();
                runButton.setEnabled(true);
            } catch (Exception e) {
                LOGGER.log(Loggable.STACK, "Error in prober", e);
            }
        };
        ThreadManager.getInstance().submit(r);
    }

    private JTable createTable(TableModel model) {
        JTable table = new JTable(model);
        table.setRowHeight(200);
        table.getColumnModel().getColumn(1).setCellRenderer(new IconCellRenderer());

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

                    if (col == 1) {
                        ImageProberTableCell selectedData = (ImageProberTableCell) model.getValueAt(row, col);

                        if (selectedData.getLargeIcon() != null) {
                            new LargeImageDialog(selectedData, DemoProber.this);
                        }
                    }

                }
            }

        });
        return table;
    }

    /**
     * This renderer displays the small icons from an ImageProberTableCell, and
     * sets text appropriate to the label within the cell.
     * 
     * @author ben
     *
     */
    private class IconCellRenderer extends DefaultTableCellRenderer {
        @Override
        public Component getTableCellRendererComponent(JTable table, Object value, boolean isSelected, boolean hasFocus,
                int row, int column) {
            try {
                super.getTableCellRendererComponent(table, value, isSelected, hasFocus, row, column);

                if (column == 1) {

                    ImageProberTableCell info = (ImageProberTableCell) value;
                    setHorizontalAlignment(JLabel.CENTER);
                    setVerticalAlignment(JLabel.TOP); // image has no offset
                    setBackground(Color.WHITE);
                    setText("");

                    if (info == null) {
                        setText("");
                        return this;
                    }

                    if (info.hasSmallIcon()) {
                        setIcon(info.getSmallIcon());
                    } else {
                        setIcon(null);
                    }
                }
            } catch (Exception e) {
                LOGGER.log(Loggable.STACK, "Renderer error", e);
            }

            return this;
        }

    }

    /**
     * Show images in a non-modal window at IMAGE_SCREEN_PROPORTION of the
     * screen width or size
     *
     */
    public class LargeImageDialog extends JDialog {

        public static final double DEFAULT_SCREEN_PROPORTION = 0.9;

        /**
         * Create a full-scale image for the given key in this ImageProber.
         * 
         * @param key
         *            the image to show
         * @param parent
         *            the parent ImageProber window
         */
        public LargeImageDialog(final ImageProberTableCell cell, final Window parent) {
            super(parent);

            this.setDefaultCloseOperation(DISPOSE_ON_CLOSE);

            final ImageIcon icon = cell.getLargeIconFitToScreen(DEFAULT_SCREEN_PROPORTION);

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

}
