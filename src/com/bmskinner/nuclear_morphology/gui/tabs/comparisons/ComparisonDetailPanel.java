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


package com.bmskinner.nuclear_morphology.gui.tabs.comparisons;

import java.awt.BorderLayout;

import javax.swing.JTabbedPane;

import org.eclipse.jdt.annotation.NonNull;

import com.bmskinner.nuclear_morphology.core.InputSupplier;
import com.bmskinner.nuclear_morphology.gui.tabs.DetailPanel;

@SuppressWarnings("serial")
public class ComparisonDetailPanel extends DetailPanel {
    
    private static final String PANEL_TITLE_LBL = "Comparisons";

    public ComparisonDetailPanel(@NonNull InputSupplier context) {
        super(context, PANEL_TITLE_LBL);

        this.setLayout(new BorderLayout());

        JTabbedPane tabPanel = new JTabbedPane(JTabbedPane.TOP);

        DetailPanel vennPanel = new VennDetailPanel(context);
        DetailPanel pairwiseVennPanel = new PairwiseVennDetailPanel(context);

        this.addSubPanel(vennPanel);
        this.addSubPanel(pairwiseVennPanel);

        tabPanel.addTab(vennPanel.getPanelTitle(), vennPanel);
        tabPanel.addTab(pairwiseVennPanel.getPanelTitle(), pairwiseVennPanel);

        this.add(tabPanel, BorderLayout.CENTER);
    }    
}
