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

import java.awt.Dimension;
import java.text.DecimalFormat;
import java.util.logging.Level;

import org.jfree.chart.JFreeChart;

import com.bmskinner.nuclear_morphology.charting.charts.HistogramChartFactory;
import com.bmskinner.nuclear_morphology.charting.charts.MorphologyChartFactory;
import com.bmskinner.nuclear_morphology.charting.charts.panels.ExportableChartPanel;
import com.bmskinner.nuclear_morphology.charting.charts.panels.SelectableChartPanel;
import com.bmskinner.nuclear_morphology.charting.options.ChartOptions;
import com.bmskinner.nuclear_morphology.charting.options.ChartOptionsBuilder;
import com.bmskinner.nuclear_morphology.components.CellularComponent;
import com.bmskinner.nuclear_morphology.components.IAnalysisDataset;
import com.bmskinner.nuclear_morphology.components.ICellCollection;
import com.bmskinner.nuclear_morphology.components.generic.MeasurementScale;
import com.bmskinner.nuclear_morphology.components.nuclear.NucleusType;
import com.bmskinner.nuclear_morphology.components.stats.PlottableStatistic;
import com.bmskinner.nuclear_morphology.gui.InterfaceEvent.InterfaceMethod;
import com.bmskinner.nuclear_morphology.gui.SignalChangeEvent;
import com.bmskinner.nuclear_morphology.gui.SignalChangeListener;
import com.bmskinner.nuclear_morphology.gui.components.HistogramsTabPanel;
import com.bmskinner.nuclear_morphology.main.GlobalOptions;

@SuppressWarnings("serial")
public class NuclearHistogramsPanel extends HistogramsTabPanel implements SignalChangeListener {

    public NuclearHistogramsPanel() {
        super(CellularComponent.NUCLEUS);

        try {

            Dimension preferredSize = new Dimension(400, 150);
            for (PlottableStatistic stat : PlottableStatistic.getNucleusStats()) {

                JFreeChart chart = HistogramChartFactory.makeEmptyChart();

                SelectableChartPanel panel = new SelectableChartPanel(chart, stat.toString());
                panel.getChartRenderingInfo().setEntityCollection(null);
                panel.setPreferredSize(preferredSize);
                panel.addSignalChangeListener(this);
                chartPanels.put(stat.toString(), panel);
                mainPanel.add(panel);

            }

        } catch (Exception e) {
            error("Error creating histogram panel", e);
        }

    }

    protected void updateSingle() {
        updateMultiple();
    }

    protected void updateMultiple() {
        this.setEnabled(true);
        // MeasurementScale scale = measurementUnitSettingsPanel.getSelected();
        boolean useDensity = useDensityPanel.isSelected();

        // NucleusType type =
        // IAnalysisDataset.getBroadestNucleusType(getDatasets());
        for (PlottableStatistic stat : PlottableStatistic.getNucleusStats()) {
            SelectableChartPanel panel = chartPanels.get(stat.toString());

            ChartOptionsBuilder builder = new ChartOptionsBuilder();
            ChartOptions options = builder.setDatasets(getDatasets()).addStatistic(stat)
                    .setScale(GlobalOptions.getInstance().getScale()).setSwatch(GlobalOptions.getInstance().getSwatch())
                    .setUseDensity(useDensity).setTarget(panel).build();

            setChart(options);
        }
    }

    protected void updateNull() {
        this.setEnabled(false);
    }

    @Override
    public void setChartsAndTablesLoading() {
        super.setChartsAndTablesLoading();
        // NucleusType type =
        // IAnalysisDataset.getBroadestNucleusType(getDatasets());
        for (PlottableStatistic stat : PlottableStatistic.getNucleusStats()) {
            ExportableChartPanel panel = chartPanels.get(stat.toString());
            panel.setChart(MorphologyChartFactory.createLoadingChart());

        }
    }

    @Override
    public void signalChangeReceived(SignalChangeEvent event) {

        if (event.type().equals("MarkerPositionUpdated")) {

            SelectableChartPanel panel = (SelectableChartPanel) event.getSource();
            filterByChartSelection(panel);

        }

    }

    /**
     * Get a statistic from its name.
     * 
     * @param name
     * @return
     */
    private PlottableStatistic getPanelStatisticFromName(String name) {
        PlottableStatistic stat = null;
        // NucleusType type =
        // IAnalysisDataset.getBroadestNucleusType(getDatasets());
        for (PlottableStatistic n : PlottableStatistic.getNucleusStats()) {
            if (n.toString().equals(name)) {
                stat = n;
            }
        }
        return stat;
    }

    /**
     * Filter the selected populations based on the region outlined on a
     * histogram panel
     * 
     * @param panel
     */
    public void filterByChartSelection(SelectableChartPanel panel) {
        // check the scale to use for selection
        MeasurementScale scale = GlobalOptions.getInstance().getScale();

        // get the parameters to filter on
        Double lower = panel.getGateLower();
        Double upper = panel.getGateUpper();
        DecimalFormat df = new DecimalFormat("#.##");

        // check the boxplot that fired
        PlottableStatistic stat = getPanelStatisticFromName(panel.getName());

        if (!lower.isNaN() && !upper.isNaN()) {

            // Make a dialog to ask if a filter should be performed
            int result = getFilterDialogResult(lower, upper);

            if (result == 0) { // button at index 0 - continue

                // create a new sub-collection with the given parameters for
                // each dataset
                for (IAnalysisDataset dataset : getDatasets()) {
                    ICellCollection collection = dataset.getCollection();
                    try {

                        log("Filtering on " + stat.toString() + ": " + df.format(lower) + " - " + df.format(upper));

                        ICellCollection subCollection = collection.filterCollection(stat, scale, lower, upper);

                        if (subCollection.hasCells()) {

                            log("Filtered " + subCollection.size() + " nuclei");
                            dataset.addChildCollection(subCollection);
                            try {
                                dataset.getCollection().getProfileManager().copyCollectionOffsets(subCollection);
                            } catch (Exception e1) {
                                error("Error applying segments", e1);
                            }
                        }

                    } catch (Exception e) {
                        error("Error filtering", e);

                    }
                }
                finest("Firing population update request");
                getInterfaceEventHandler().fireInterfaceEvent(InterfaceMethod.REFRESH_POPULATIONS);
            }
        }
    }
}
