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


package com.bmskinner.nuclear_morphology.gui.tabs.segments;

import java.awt.BorderLayout;
import java.awt.Dimension;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;

import javax.swing.JPanel;
import javax.swing.JTabbedPane;
import javax.swing.border.EmptyBorder;

import org.eclipse.jdt.annotation.NonNull;

import com.bmskinner.nuclear_morphology.core.InputSupplier;
import com.bmskinner.nuclear_morphology.gui.tabs.DetailPanel;

@SuppressWarnings("serial")
public class SegmentsDetailPanel extends DetailPanel {

    private static final String PANEL_TITLE_LBL = "Nuclear segments";

    public SegmentsDetailPanel(@NonNull InputSupplier context) {
        super(context);
        this.setLayout(new BorderLayout());

        JTabbedPane tabPanel = new JTabbedPane(JTabbedPane.TOP);
        tabPanel.setBorder(new EmptyBorder(5, 5, 5, 5));

        DetailPanel segmentProfilePanel = new SegmentProfilePanel(context);
        DetailPanel segmentBoxplotsPanel = new SegmentBoxplotsPanel(context);
        DetailPanel segmentHistogramsPanel = new SegmentHistogramsPanel(context);
        DetailPanel segmentWilcoxonPanel = new SegmentWilcoxonPanel(context);
        DetailPanel segmentMagnitudePanel = new SegmentMagnitudePanel(context);
        DetailPanel segmentStatsPanel = new SegmentStatsPanel(context);

        Dimension minimumChartSize = new Dimension(100, 100);

        segmentProfilePanel.setMinimumSize(minimumChartSize);
        segmentProfilePanel.setBorder(new EmptyBorder(5, 5, 5, 5));
        segmentBoxplotsPanel.setMinimumSize(minimumChartSize);
        segmentHistogramsPanel.setMinimumSize(minimumChartSize);
        segmentWilcoxonPanel.setMinimumSize(minimumChartSize);
        segmentMagnitudePanel.setMinimumSize(minimumChartSize);
        segmentStatsPanel.setMinimumSize(minimumChartSize);
        segmentStatsPanel.setBorder(new EmptyBorder(5, 5, 5, 5));

        this.addSubPanel(segmentProfilePanel);
        this.addSubPanel(segmentBoxplotsPanel);
        this.addSubPanel(segmentHistogramsPanel);
        this.addSubPanel(segmentWilcoxonPanel);
        this.addSubPanel(segmentMagnitudePanel);
        this.addSubPanel(segmentStatsPanel);

        tabPanel.addTab(segmentBoxplotsPanel.getPanelTitle(), segmentBoxplotsPanel);
        tabPanel.addTab(segmentHistogramsPanel.getPanelTitle(), segmentHistogramsPanel);
        tabPanel.addTab(segmentWilcoxonPanel.getPanelTitle(), segmentWilcoxonPanel);
        tabPanel.addTab(segmentMagnitudePanel.getPanelTitle(), segmentMagnitudePanel);

        JPanel mainPanel = new JPanel(new GridBagLayout());
        GridBagConstraints constraints = new GridBagConstraints();
        constraints.fill = GridBagConstraints.BOTH;
        constraints.gridx = 0;
        constraints.gridy = 0;
        constraints.gridheight = 1;
        constraints.gridwidth = 1;
        constraints.weightx = 1;
        constraints.weighty = 0.5;
        constraints.anchor = GridBagConstraints.CENTER;

        mainPanel.add(segmentStatsPanel, constraints);

        constraints.gridx = 0;
        constraints.gridy = 1;
        constraints.gridheight = 1;
        constraints.gridwidth = 1;
        constraints.weighty = 1;
        mainPanel.add(segmentProfilePanel, constraints);

        constraints.gridx = 1;
        constraints.gridy = 0;
        constraints.gridheight = 2;
        constraints.gridwidth = 1;
        mainPanel.add(tabPanel, constraints);

        this.add(mainPanel, BorderLayout.CENTER);

    }
    
    @Override
    public String getPanelTitle(){
        return PANEL_TITLE_LBL;
    }
    
}
