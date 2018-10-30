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
package com.bmskinner.nuclear_morphology.gui.tabs.signals;

import java.awt.BorderLayout;

import javax.swing.JTabbedPane;

import org.eclipse.jdt.annotation.NonNull;

import com.bmskinner.nuclear_morphology.core.InputSupplier;
import com.bmskinner.nuclear_morphology.gui.events.SignalChangeEvent;
import com.bmskinner.nuclear_morphology.gui.tabs.DetailPanel;
import com.bmskinner.nuclear_morphology.gui.tabs.TabPanel;

/**
 * The top level tab panel showing information on signals at the dataset level
 * 
 * @author ben
 *
 */
@SuppressWarnings("serial")
public class SignalsDetailPanel extends DetailPanel {

    private static final String PANEL_TITLE_LBL = "Nuclear signals";
    private JTabbedPane signalsTabPane;

    /**
     * Create the panel.
     */
    public SignalsDetailPanel(@NonNull InputSupplier context) {
        super(context, PANEL_TITLE_LBL);
        try {

            this.setLayout(new BorderLayout());

            signalsTabPane = new JTabbedPane(JTabbedPane.TOP);

            DetailPanel overviewPanel = new SignalsOverviewPanel(context);
            DetailPanel countsPanel  = new SignalCountsPanel(context);
            DetailPanel boxplotPanel = new SignalsBoxplotPanel(context);
            DetailPanel shellsPanel = new SignalShellsPanel(context);
            DetailPanel detectionSettingsPanel = new SignalsAnalysisPanel(context);
            DetailPanel signalScatterChartPanel = new SignalScatterChartPanel(context);
            DetailPanel colocalistionPanel = new SignalsColocalisationPanel(context);

            signalsTabPane.addTab(overviewPanel.getPanelTitle(), overviewPanel);
            signalsTabPane.addTab(detectionSettingsPanel.getPanelTitle(), detectionSettingsPanel);
            signalsTabPane.addTab(countsPanel.getPanelTitle(), countsPanel);
            signalsTabPane.addTab(boxplotPanel.getPanelTitle(), boxplotPanel);

            signalsTabPane.addTab(signalScatterChartPanel.getPanelTitle(), signalScatterChartPanel);
            signalsTabPane.addTab(shellsPanel.getPanelTitle(), shellsPanel);
            signalsTabPane.addTab(colocalistionPanel.getPanelTitle(), colocalistionPanel);

            this.addSubPanel(overviewPanel);
            this.addSubPanel(countsPanel);
            this.addSubPanel(boxplotPanel);

            this.addSubPanel(shellsPanel);
            this.addSubPanel(detectionSettingsPanel);
            this.addSubPanel(signalScatterChartPanel);
            this.addSubPanel(colocalistionPanel);

            this.add(signalsTabPane, BorderLayout.CENTER);

        } catch (Exception e) {
            error("Error making signal panel", e);
        }
    }
    
    @Override
    public void eventReceived(SignalChangeEvent event) {
        super.eventReceived(event);
        if (event.type().equals(SignalChangeEvent.SIGNAL_COLOUR_CHANGE)) {
            update(getDatasets());
        }

        if (event.type().startsWith(SignalChangeEvent.GROUP_VISIBLE_PREFIX)) {

            for (TabPanel p : this.getSubPanels()) {
            	p.refreshChartCache(getDatasets());
//                p.update(getDatasets());
            }
        }
    }

}
