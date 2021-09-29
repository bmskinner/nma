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
package com.bmskinner.nuclear_morphology.gui.dialogs;

import java.awt.BorderLayout;
import java.awt.Component;
import java.awt.Cursor;
import java.awt.FlowLayout;
import java.util.ArrayList;
import java.util.List;

import javax.swing.JButton;
import javax.swing.JLabel;
import javax.swing.JPanel;

import org.jfree.chart.JFreeChart;

import com.bmskinner.nuclear_morphology.charting.charts.MorphologyChartFactory;
import com.bmskinner.nuclear_morphology.charting.charts.panels.ExportableChartPanel;
import com.bmskinner.nuclear_morphology.charting.options.ChartOptions;
import com.bmskinner.nuclear_morphology.charting.options.ChartOptionsBuilder;
import com.bmskinner.nuclear_morphology.components.datasets.IAnalysisDataset;
import com.bmskinner.nuclear_morphology.components.profiles.ProfileType;
import com.bmskinner.nuclear_morphology.components.profiles.Landmark;
import com.bmskinner.nuclear_morphology.gui.components.panels.ProfileAlignmentOptionsPanel.ProfileAlignment;

@SuppressWarnings("serial")
public class KruskalTestDialog extends LoadingIconDialog {

    private IAnalysisDataset dataset1;
    private IAnalysisDataset dataset2;

    private ExportableChartPanel chartPanel;

    private JButton runButton;

    public KruskalTestDialog(final IAnalysisDataset dataset1, final IAnalysisDataset dataset2) {
        super();
        this.dataset1 = dataset1;
        this.dataset2 = dataset2;
        createUI();
        this.setModal(false);
        this.pack();
        this.setVisible(true);
    }

    private void createUI() {
        this.setTitle("Kruskal test: " + dataset1.getName() + " vs " + dataset2.getName());
        this.setLayout(new BorderLayout());
        this.setLocationRelativeTo(null);

        this.add(createSettingsPanel(), BorderLayout.NORTH);

        chartPanel = new ExportableChartPanel(MorphologyChartFactory.createEmptyChart());
        this.add(chartPanel, BorderLayout.CENTER);

    }

    private JPanel createSettingsPanel() {
        JPanel panel = new JPanel(new FlowLayout());

        JLabel label = new JLabel(
                "Normalise segment lengths between datasets, and rerun the Kruskal-Wallis comparison");
        panel.add(label);

        runButton = new JButton("Run");
        runButton.addActionListener(e-> {
        	Runnable r = () -> runAnalysis();
        	new Thread(r).start();
        });
        panel.add(runButton);
        return panel;
    }

    /**
     * Toggle wait cursor on element
     * 
     * @param b
     */
    private void setAnalysing(boolean b) {
        if (b) {
            this.setEnabled(false);
            for (Component c : this.getComponents()) {
                c.setCursor(Cursor.getPredefinedCursor(Cursor.WAIT_CURSOR));
            }
            this.setCursor(Cursor.getPredefinedCursor(Cursor.WAIT_CURSOR));

        } else {
            this.setEnabled(true);
            for (Component c : this.getComponents()) {
                c.setCursor(Cursor.getDefaultCursor());
            }
            this.setCursor(Cursor.getDefaultCursor());
        }
    }

    @Override
    public void setEnabled(boolean b) {
        runButton.setEnabled(b);
    }

    private void runAnalysis() {

        setAnalysing(true);

        // Clear the old chart
        chartPanel.setChart(MorphologyChartFactory.createEmptyChart());

        List<IAnalysisDataset> list = new ArrayList<>();
        list.add(dataset1);
        list.add(dataset2);

        ChartOptions options = new ChartOptionsBuilder().setDatasets(list).setNormalised(true)
                .setAlignment(ProfileAlignment.LEFT).setTag(Landmark.REFERENCE_POINT).setShowMarkers(false)
                .setProfileType(ProfileType.FRANKEN).build();

        JFreeChart chart = new MorphologyChartFactory(options).makeKruskalWallisChart();
        chartPanel.setChart(chart);

        setAnalysing(false);
    }
}
