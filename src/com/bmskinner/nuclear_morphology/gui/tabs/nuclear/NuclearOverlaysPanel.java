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


package com.bmskinner.nuclear_morphology.gui.tabs.nuclear;

import java.awt.BorderLayout;
import java.awt.FlowLayout;
import java.awt.event.ComponentAdapter;
import java.awt.event.ComponentEvent;
import java.util.logging.Level;

import javax.swing.JButton;
import javax.swing.JPanel;
import javax.swing.table.TableModel;

import org.jfree.chart.JFreeChart;

import com.bmskinner.nuclear_morphology.charting.charts.AbstractChartFactory;
import com.bmskinner.nuclear_morphology.charting.charts.OutlineChartFactory;
import com.bmskinner.nuclear_morphology.charting.charts.panels.ExportableChartPanel;
import com.bmskinner.nuclear_morphology.charting.datasets.AbstractDatasetCreator;
import com.bmskinner.nuclear_morphology.charting.options.ChartOptions;
import com.bmskinner.nuclear_morphology.charting.options.ChartOptionsBuilder;
import com.bmskinner.nuclear_morphology.charting.options.TableOptions;
import com.bmskinner.nuclear_morphology.components.IAnalysisDataset;
import com.bmskinner.nuclear_morphology.components.nuclear.IBorderSegment;
import com.bmskinner.nuclear_morphology.components.nuclear.NucleusBorderSegment;
import com.bmskinner.nuclear_morphology.gui.components.panels.GenericCheckboxPanel;
import com.bmskinner.nuclear_morphology.gui.dialogs.ConsensusCompareDialog;
import com.bmskinner.nuclear_morphology.gui.tabs.DetailPanel;

/**
 * This class is designed to display the vertically oriented nuclei within the
 * collection, overlaid atop each other
 * 
 * @author bms41
 *
 */
@SuppressWarnings("serial")
public class NuclearOverlaysPanel extends DetailPanel {

    private static final String PANEL_TITLE_LBL = "Overlays";
    private ExportableChartPanel chartPanel;            // hold the nuclei
    private GenericCheckboxPanel checkBoxPanel;         // use for aligning
                                                        // nuclei
    private JButton              compareConsensusButton;
    private JButton              makeOverlayChartButton;

    public NuclearOverlaysPanel() {

        this.setLayout(new BorderLayout());

        JPanel header = createHeader();
        this.add(header, BorderLayout.NORTH);

        try {

            ChartOptions options = new ChartOptionsBuilder().setDatasets(getDatasets()).build();

            JFreeChart chart = getChart(options);

            chartPanel = new ExportableChartPanel(chart);
            chartPanel.setFixedAspectRatio(true);

            this.add(chartPanel, BorderLayout.CENTER);

            makeCreateButton();

        } catch (Exception e) {
            warn("Error creating overlays panel");
            stack("Error creating overlays panel", e);
        }

    }
    
    @Override
    public String getPanelTitle(){
        return PANEL_TITLE_LBL;
    }

    private JPanel createHeader() {
        JPanel panel = new JPanel(new FlowLayout());

        checkBoxPanel = new GenericCheckboxPanel("Align nuclei to consensus");
        checkBoxPanel.addActionListener(a -> update(getDatasets()));
        checkBoxPanel.setEnabled(false);

        compareConsensusButton = new JButton("Compare consensus");
        compareConsensusButton.addActionListener(a -> createConsensusCompareDialog());
        compareConsensusButton.setEnabled(false);

        panel.add(checkBoxPanel);
        panel.add(compareConsensusButton);
        return panel;
    }

    private void makeCreateButton() {
        makeOverlayChartButton = new JButton("Create chart");
        makeOverlayChartButton.addActionListener(a -> createSafeChart(getChartOptions()));
        makeOverlayChartButton.setEnabled(true);
        chartPanel.add(makeOverlayChartButton);
        makeOverlayChartButton.setVisible(false);
    }

    private void createConsensusCompareDialog() {

        boolean ok = true;

        int segCount = activeDataset().getCollection().getProfileManager().getSegmentCount();
        for (IAnalysisDataset d : getDatasets()) {

            if (!d.getCollection().hasConsensus()) {
                ok = false;
                warn("Dataset " + d.getName() + " does not have a consensus nucleus");
            }

            if (d.getCollection().getProfileManager().getSegmentCount() != segCount) {
                ok = false;
                warn("Dataset " + d.getName() + " has a different segment pattern to " + activeDataset().getName());
            }

        }

        if (ok) {
            finer("Creating consensus compare dialog");
            new ConsensusCompareDialog(getDatasets());
        } else {
            warn("Unable to create consensus compare dialog");
        }
    }

    /**
     * Get the chart options object for the selected datasets and parameters
     * 
     * @return
     */
    private ChartOptions getChartOptions() {
        boolean alignNuclei = checkBoxPanel.isSelected();

        return new ChartOptionsBuilder().setDatasets(getDatasets()).setNormalised(alignNuclei).setTarget(chartPanel)
                .build();
    }

    @Override
    protected void updateSingle() {

        compareConsensusButton.setEnabled(false);
        makeOverlayChartButton.setVisible(false);

        boolean hasConsensus = activeDataset().getCollection().hasConsensus();
        checkBoxPanel.setEnabled(hasConsensus);

        ChartOptions options = getChartOptions();

        /*
         * Insert a button to generate the chart if not present
         */
        if (this.chartCache.has(options)) {

            JFreeChart chart = getChart(options);
            chartPanel.setChart(chart);
            chartPanel.restoreAutoBounds();
        } else {

            options = new ChartOptionsBuilder().build();

            JFreeChart chart = getChart(options);
            chartPanel.setChart(chart);
            makeOverlayChartButton.setVisible(true);
        }

    }

    /**
     * Create the overlay chart. Invoked by makeOverlayChartButton
     * 
     * @param options
     */
    private void createSafeChart(ChartOptions options) {
        makeOverlayChartButton.setVisible(false);
        setAnalysing(true);
        fine("Creating overlay chart on button click");
        try {
            getChart(options);
            update(getDatasets());
        } catch (Exception e) {
            stack("Error making chart", e);
            update(getDatasets());
        } finally {
            setAnalysing(false);
        }
    }

    @Override
    protected void updateMultiple() {
        makeOverlayChartButton.setVisible(false);
        updateSingle();

        /*
         * Check if all selected datasets have a consensus
         */
        boolean setConsensusButton = true;
        for (IAnalysisDataset d : getDatasets()) {
            if (!d.getCollection().hasConsensus()) {
                setConsensusButton = false;
            }
        }

        if (!IBorderSegment.segmentCountsMatch(getDatasets())) {
            setConsensusButton = false;
        }

        compareConsensusButton.setEnabled(setConsensusButton);

        checkBoxPanel.setEnabled(false);
    }

    @Override
    protected void updateNull() {
        compareConsensusButton.setEnabled(false);
        checkBoxPanel.setEnabled(false);
        makeOverlayChartButton.setVisible(false);

        ChartOptions options = new ChartOptionsBuilder().setDatasets(getDatasets()).setNormalised(false)
                .setTarget(chartPanel).build();

        setChart(options);

    }

    @Override
    public void setChartsAndTablesLoading() {
        super.setChartsAndTablesLoading();
        chartPanel.setChart(AbstractChartFactory.createLoadingChart());
    }

    @Override
    protected TableModel createPanelTableType(TableOptions options) {
        return null;
    }

    @Override
    protected JFreeChart createPanelChartType(ChartOptions options) {
        finest("Creating nuclear overlay chart");
        return new OutlineChartFactory(options).createVerticalNucleiChart();
    }

}
