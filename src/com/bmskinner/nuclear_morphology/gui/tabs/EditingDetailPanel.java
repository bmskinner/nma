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


package com.bmskinner.nuclear_morphology.gui.tabs;

import java.awt.BorderLayout;

import javax.swing.JTabbedPane;

import org.eclipse.jdt.annotation.NonNull;

import com.bmskinner.nuclear_morphology.core.InputSupplier;
import com.bmskinner.nuclear_morphology.gui.DatasetEventListener;
import com.bmskinner.nuclear_morphology.gui.InterfaceEventListener;
import com.bmskinner.nuclear_morphology.gui.SignalChangeEvent;
import com.bmskinner.nuclear_morphology.gui.SignalChangeListener;
import com.bmskinner.nuclear_morphology.gui.tabs.cells_detail.IndividualCellDetailPanel;
import com.bmskinner.nuclear_morphology.gui.tabs.editing.BorderTagEditingPanel;
import com.bmskinner.nuclear_morphology.gui.tabs.editing.SegmentsEditingPanel;

@SuppressWarnings("serial")
public class EditingDetailPanel extends DetailPanel
        implements SignalChangeListener, DatasetEventListener, InterfaceEventListener {
    
    private static final String PANEL_TITLE_LBL = "Editing";

    public EditingDetailPanel(@NonNull InputSupplier context) {
        super(context);

        this.setLayout(new BorderLayout());
        JTabbedPane tabPane = new JTabbedPane();
        this.add(tabPane, BorderLayout.CENTER);

        DetailPanel cellDetailPanel = new IndividualCellDetailPanel(context);
        DetailPanel segmentsEditingPanel = new SegmentsEditingPanel(context);
        DetailPanel borderTagEditingPanel = new BorderTagEditingPanel(context);

        this.addSubPanel(cellDetailPanel);
        this.addSubPanel(segmentsEditingPanel);
        this.addSubPanel(borderTagEditingPanel);

        this.addSignalChangeListener(cellDetailPanel);
        this.addSignalChangeListener(segmentsEditingPanel);
        this.addSignalChangeListener(borderTagEditingPanel);

        tabPane.addTab(cellDetailPanel.getPanelTitle(), cellDetailPanel);

        /*
         * Signals come from the segment panel to this container Signals can be
         * sent to the segment panel Events come from the panel only
         */
        segmentsEditingPanel.addSignalChangeListener(this);
        borderTagEditingPanel.addSignalChangeListener(this);

        tabPane.addTab("Segmentation", segmentsEditingPanel);
        tabPane.addTab("Border tags", borderTagEditingPanel);

    }
    
    @Override
    public String getPanelTitle(){
        return PANEL_TITLE_LBL;
    }

    @Override
    public void signalChangeReceived(SignalChangeEvent event) {

        super.signalChangeReceived(event);
        finer("Editing panel heard signal: " + event.type());

        // Pass downwards if the signal was not generated internally
        if (!this.getSubPanels().contains(event.getSource())) {
           getSignalChangeEventHandler().fireSignalChangeEvent(event.type());
        }
    }
}
