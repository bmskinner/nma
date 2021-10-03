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
package com.bmskinner.nuclear_morphology.gui.tabs.comparisons;

import java.awt.BorderLayout;
import java.awt.FlowLayout;

import javax.swing.JButton;
import javax.swing.JLabel;
import javax.swing.JPanel;

import org.eclipse.jdt.annotation.NonNull;
import org.jfree.chart.JFreeChart;

import com.bmskinner.nuclear_morphology.charting.charts.AbstractChartFactory;
import com.bmskinner.nuclear_morphology.charting.charts.MorphologyChartFactory;
import com.bmskinner.nuclear_morphology.charting.charts.panels.ExportableChartPanel;
import com.bmskinner.nuclear_morphology.charting.options.ChartOptions;
import com.bmskinner.nuclear_morphology.charting.options.ChartOptionsBuilder;
import com.bmskinner.nuclear_morphology.components.profiles.IProfileSegment;
import com.bmskinner.nuclear_morphology.components.profiles.Landmark;
import com.bmskinner.nuclear_morphology.components.profiles.ProfileType;
import com.bmskinner.nuclear_morphology.core.InputSupplier;
import com.bmskinner.nuclear_morphology.gui.components.panels.ProfileAlignmentOptionsPanel.ProfileAlignment;
import com.bmskinner.nuclear_morphology.gui.dialogs.KruskalTestDialog;
import com.bmskinner.nuclear_morphology.gui.tabs.DetailPanel;

@SuppressWarnings("serial")
public class KruskalDetailPanel extends DetailPanel {

    private static final String PANEL_TITLE_LBL = "Kruskal";
    private static final String COMPARE_FRANKENPROFILE_LBL = "Compare frankenprofiles";
    private static final String COMPARE_INFO_LBL           = "Kruskal-Wallis comparison of datasets (Bonferroni-corrected p-values)";

    private ExportableChartPanel chartPanel;
    JButton                      frankenButton = new JButton(COMPARE_FRANKENPROFILE_LBL);

    public KruskalDetailPanel(@NonNull InputSupplier context) {
        super(context, PANEL_TITLE_LBL);

        createUI();

        setEnabled(false);
    }
    
    @Override
    public void setEnabled(boolean b) {
        super.setEnabled(b);
        frankenButton.setEnabled(b);

    }

    private void createUI() {
        this.setLayout(new BorderLayout());

        JPanel headerPanel = createHeaderPanel();
        this.add(headerPanel, BorderLayout.NORTH);

        createChartPanel();

        this.add(chartPanel, BorderLayout.CENTER);
    }

    private void createChartPanel() {
        JFreeChart profileChart = MorphologyChartFactory.makeBlankProbabililtyChart();
        chartPanel = new ExportableChartPanel(profileChart);
    }

    private JPanel createHeaderPanel() {
        JPanel panel = new JPanel(new FlowLayout());

        panel.add(new JLabel(COMPARE_INFO_LBL));

        frankenButton.addActionListener(e -> {
        	Runnable r = () -> new KruskalTestDialog(getDatasets().get(0), getDatasets().get(1));
        	new Thread(r).start();
        });

        panel.add(frankenButton);

        return panel;

    }

    /**
     * Create a chart showing the Kruskal-Wallis p-values of comparisons between
     * curves
     * 
     * @return
     */
    private void updateChartPanel() {

        ChartOptions options = new ChartOptionsBuilder().setDatasets(getDatasets()).setNormalised(true)
                .setAlignment(ProfileAlignment.LEFT).setTag(Landmark.REFERENCE_POINT).setShowMarkers(true)
                .setProfileType(ProfileType.ANGLE).setTarget(chartPanel).build();

        setChart(options);
    }

    @Override
    protected JFreeChart createPanelChartType(@NonNull ChartOptions options) {
        return new MorphologyChartFactory(options).makeKruskalWallisChart();
    }

    @Override
    public synchronized void setChartsAndTablesLoading() {
        super.setChartsAndTablesLoading();
        chartPanel.setChart(AbstractChartFactory.createLoadingChart());
    }

    @Override
    protected synchronized void updateSingle() {
        updateNull();
    }

    @Override
    protected synchronized void updateMultiple() {
        if (getDatasets().size() == 2) { // Only create a chart if exactly two
                                         // datasets are selected
            // Only allow a franken normlisation if datasets have the same
            // number of segments
        	setEnabled(IProfileSegment.segmentCountsMatch(getDatasets()));
            updateChartPanel();
        } else {
            updateNull();

        }
    }

    @Override
    protected synchronized void updateNull() {
        setEnabled(false);
        JFreeChart chart = MorphologyChartFactory.makeBlankProbabililtyChart();
        chartPanel.setChart(chart);
    }

}
