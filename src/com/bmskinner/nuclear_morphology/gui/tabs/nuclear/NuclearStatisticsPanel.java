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
package com.bmskinner.nuclear_morphology.gui.tabs.nuclear;

import java.awt.BorderLayout;

import javax.swing.JTabbedPane;
import javax.swing.SwingConstants;

import org.eclipse.jdt.annotation.NonNull;

import com.bmskinner.nuclear_morphology.core.GlobalOptions;
import com.bmskinner.nuclear_morphology.core.InputSupplier;
import com.bmskinner.nuclear_morphology.gui.tabs.DetailPanel;

@SuppressWarnings("serial")
public class NuclearStatisticsPanel extends DetailPanel {

    private static final String PANEL_TITLE_LBL = "Nuclear charts";

    private JTabbedPane tabPane;

    public NuclearStatisticsPanel(@NonNull InputSupplier context) {
        super(context);

        this.setLayout(new BorderLayout());
        tabPane = new JTabbedPane(SwingConstants.TOP);

        DetailPanel nuclearStatsPanel = new NuclearStatsPanel(context);
        DetailPanel boxplotPanel = new NuclearBoxplotsPanel(context);
        DetailPanel wilcoxonPanel = new WilcoxonDetailPanel(context);
        DetailPanel nucleusMagnitudePanel = new NucleusMagnitudePanel(context);

        DetailPanel nuclearScatterChartPanel = new NuclearScatterChartPanel(context);
        DetailPanel nuclearGlcmPanel = new NuclearGlcmPanel(context);

        addPanel(nuclearStatsPanel);
        addPanel(boxplotPanel);
        addPanel(wilcoxonPanel);
        addPanel(nucleusMagnitudePanel);
        addPanel(nuclearScatterChartPanel);

        if(GlobalOptions.getInstance().getBoolean(GlobalOptions.IS_GLCM_INTERFACE_KEY))
        	addPanel(nuclearGlcmPanel);

        this.add(tabPane, BorderLayout.CENTER);
    }
    
   
    /**
     * Register a sub panel and add to the tab pane
     * @param panel
     */
    private void addPanel(DetailPanel panel) {
    	this.addSubPanel(panel);
    	tabPane.addTab(panel.getPanelTitle(), panel);
    }

    @Override
    public String getPanelTitle(){
        return PANEL_TITLE_LBL;
    }
    
}
