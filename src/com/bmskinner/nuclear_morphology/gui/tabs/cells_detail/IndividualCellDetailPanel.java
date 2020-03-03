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
package com.bmskinner.nuclear_morphology.gui.tabs.cells_detail;

import java.awt.BorderLayout;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.util.logging.Level;
import java.util.logging.Logger;

import javax.swing.JPanel;
import javax.swing.JSplitPane;
import javax.swing.JTabbedPane;
import javax.swing.border.EmptyBorder;
import javax.swing.table.TableModel;

import org.eclipse.jdt.annotation.NonNull;
import org.jfree.chart.JFreeChart;

import com.bmskinner.nuclear_morphology.charting.options.ChartOptions;
import com.bmskinner.nuclear_morphology.charting.options.TableOptions;
import com.bmskinner.nuclear_morphology.core.InputSupplier;
import com.bmskinner.nuclear_morphology.gui.events.SignalChangeEvent;
import com.bmskinner.nuclear_morphology.gui.tabs.DetailPanel;
import com.bmskinner.nuclear_morphology.logging.Loggable;

@SuppressWarnings("serial")
public class IndividualCellDetailPanel extends DetailPanel {
	
	private static final Logger LOGGER = Logger.getLogger(IndividualCellDetailPanel.class.getName());
	
    private JTabbedPane tabPane;

    private static final String PANEL_TITLE_LBL = "Cells";

    /** Cells in the active dataset */
    protected CellsListPanel       cellsListPanel;
    
    /** View and modify cell segments */
    protected CellSegmentsPanel     segmentProfilePanel;
    
    /** View and cell profiles */
    protected CellProfilesPanel   cellBorderTagPanel;
    
    /** View and modify cell tags */
    protected CellOutlinePanel     outlinePanel; 
    
    /** View cell info */
    protected CellStatsPanel       cellStatsPanel;
    
    /** Choose image channels to display */
    protected ComponentListPanel   signalListPanel;
    
    /** View pairwise distances for signals */
    protected CellSignalStatsPanel cellsignalStatsPanel;
    
    /** Track the cell on display */
    private CellViewModel model = new CellViewModel(null, null);

    public IndividualCellDetailPanel(@NonNull InputSupplier context) {

        super(context);
        try {

            createSubPanels(context);

            this.setLayout(new BorderLayout());
            JPanel westPanel = createCellandSignalListPanels(context);
            this.addSubPanel(cellStatsPanel);
            this.addSubPanel(segmentProfilePanel);
            this.addSubPanel(cellBorderTagPanel);
            this.addSubPanel(outlinePanel);
            this.addSubPanel(cellsListPanel);
            this.addSubPanel(signalListPanel);
            this.addSubPanel(cellsignalStatsPanel);

            
            tabPane = new JTabbedPane(JTabbedPane.LEFT);
            tabPane.add(cellStatsPanel.getPanelTitle(), cellStatsPanel);
            tabPane.add(cellBorderTagPanel.getPanelTitle(), cellBorderTagPanel);
            tabPane.add(segmentProfilePanel.getPanelTitle(), segmentProfilePanel);
            
            tabPane.add(outlinePanel.getPanelTitle(), outlinePanel);
            tabPane.add(cellsignalStatsPanel.getPanelTitle(), cellsignalStatsPanel);
            tabPane.setSelectedComponent(outlinePanel);
            
            JSplitPane sp = new JSplitPane(JSplitPane.HORIZONTAL_SPLIT);
            sp.setLeftComponent(westPanel);
            sp.setRightComponent(tabPane);
            sp.setResizeWeight(0.25);
            add(sp, BorderLayout.CENTER);

        } catch (Exception e) {
        	LOGGER.log(Level.WARNING, "Error creating cell detail panel");
            LOGGER.log(Loggable.STACK, "Error creating cell detail panel", e);
        }
    }
    
    
    @Override
    public String getPanelTitle(){
        return PANEL_TITLE_LBL;
    }

    private void createSubPanels(@NonNull InputSupplier context) {
        segmentProfilePanel = new CellSegmentsPanel(context, model); // the nucleus angle
                                                           // profile
        cellBorderTagPanel = new CellProfilesPanel(context, model);
        outlinePanel = new CellOutlinePanel(context, model); // the outline of the cell
                                                    // and detected objects
        cellStatsPanel = new CellStatsPanel(context, model); // the stats table
        cellsignalStatsPanel = new CellSignalStatsPanel(context, model);

        model.addView(segmentProfilePanel);
        model.addView(cellBorderTagPanel);
        model.addView(outlinePanel);
        model.addView(cellStatsPanel);
        model.addView(cellsignalStatsPanel);
        // model.addView(cellSegTablePanel);
    }

    private JPanel createCellandSignalListPanels(@NonNull InputSupplier context) {
        JPanel panel = new JPanel(new GridBagLayout());

        GridBagConstraints constraints = new GridBagConstraints();
        constraints.fill = GridBagConstraints.BOTH;
        constraints.gridx = 0;
        constraints.gridy = 0;
        constraints.gridheight = 2;
        constraints.gridwidth = 1;
        constraints.weightx = 0.5;
        constraints.weighty = 0.6;
        constraints.anchor = GridBagConstraints.CENTER;

        cellsListPanel = new CellsListPanel(context, model);
        model.addView(cellsListPanel);
        cellsListPanel.setBorder(new EmptyBorder(5, 5, 5, 5));
        panel.add(cellsListPanel, constraints);

        constraints.gridx = 0;
        constraints.gridy = 2;
        constraints.gridheight = 2;
        constraints.gridwidth = 1;
        constraints.weightx = 0.5;
        constraints.weighty = 0.4;
        signalListPanel = new ComponentListPanel(context, model);
        model.addView(signalListPanel);
        signalListPanel.setBorder(new EmptyBorder(5, 5, 5, 5));
        panel.add(signalListPanel, constraints);

        return panel;
    }

    @Override
    protected void updateSingle() {

        if (model.hasCell() && !activeDataset().getCollection().containsExact(model.getCell())) {
            model.setCell(null);
        }
    }

    @Override
    protected void updateMultiple() {
        updateNull();
    }

    @Override
    protected void updateNull() {
        model.setCell(null);
        model.setComponent(null);
    }

    @Override
    protected JFreeChart createPanelChartType(@NonNull ChartOptions options) {
        return null;
    }

    @Override
    protected TableModel createPanelTableType(@NonNull TableOptions options) {
        return null;
    }

    @Override
    public void eventReceived(SignalChangeEvent event) {
        if (event.type().equals(SignalChangeEvent.SIGNAL_COLOUR_CHANGE)) {
            model.updateViews();
        }

    }

}
