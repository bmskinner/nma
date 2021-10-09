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

import java.awt.Dimension;
import java.text.DecimalFormat;
import java.util.Optional;
import java.util.logging.Logger;

import org.eclipse.jdt.annotation.NonNull;
import org.jfree.chart.JFreeChart;

import com.bmskinner.nuclear_morphology.analysis.nucleus.CellCollectionFilterBuilder;
import com.bmskinner.nuclear_morphology.analysis.nucleus.CellCollectionFilterer;
import com.bmskinner.nuclear_morphology.analysis.nucleus.FilteringOptions;
import com.bmskinner.nuclear_morphology.charting.charts.HistogramChartFactory;
import com.bmskinner.nuclear_morphology.charting.charts.MorphologyChartFactory;
import com.bmskinner.nuclear_morphology.charting.charts.panels.ExportableChartPanel;
import com.bmskinner.nuclear_morphology.charting.charts.panels.SelectableChartPanel;
import com.bmskinner.nuclear_morphology.charting.options.ChartOptions;
import com.bmskinner.nuclear_morphology.charting.options.ChartOptionsBuilder;
import com.bmskinner.nuclear_morphology.components.cells.CellularComponent;
import com.bmskinner.nuclear_morphology.components.datasets.IAnalysisDataset;
import com.bmskinner.nuclear_morphology.components.datasets.ICellCollection;
import com.bmskinner.nuclear_morphology.components.measure.Measurement;
import com.bmskinner.nuclear_morphology.components.measure.MeasurementScale;
import com.bmskinner.nuclear_morphology.core.GlobalOptions;
import com.bmskinner.nuclear_morphology.core.InputSupplier;
import com.bmskinner.nuclear_morphology.gui.components.HistogramsTabPanel;
import com.bmskinner.nuclear_morphology.gui.events.InterfaceEvent.InterfaceMethod;
import com.bmskinner.nuclear_morphology.gui.events.SignalChangeEvent;
import com.bmskinner.nuclear_morphology.logging.Loggable;

@SuppressWarnings("serial")
public class NuclearHistogramsPanel extends HistogramsTabPanel  {
	
	private static final Logger LOGGER = Logger.getLogger(NuclearHistogramsPanel.class.getName());

	public NuclearHistogramsPanel(@NonNull InputSupplier context) {
		super(context, CellularComponent.NUCLEUS);
		Dimension preferredSize = new Dimension(HISTOGRAM_CHART_WIDTH, HISTOGRAM_CHART_HEIGHT);
		for (Measurement stat : Measurement.getNucleusStats()) {

			JFreeChart chart = HistogramChartFactory.createEmptyHistogram();

			SelectableChartPanel panel = new SelectableChartPanel(chart, stat.toString());
			panel.getChartRenderingInfo().setEntityCollection(null);
			panel.setPreferredSize(preferredSize);
			panel.addSignalChangeListener(this);
			chartPanels.put(stat.toString(), panel);
			mainPanel.add(panel);

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
        for (Measurement stat : Measurement.getNucleusStats()) {
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
        for (Measurement stat : Measurement.getNucleusStats()) {
            ExportableChartPanel panel = chartPanels.get(stat.toString());
            panel.setChart(MorphologyChartFactory.createLoadingChart());

        }
    }

    @Override
    public void eventReceived(SignalChangeEvent event) {

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
    private Measurement getPanelStatisticFromName(String name) {
        Measurement stat = null;
        for (Measurement n : Measurement.getNucleusStats()) {
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
        Measurement stat = getPanelStatisticFromName(panel.getName());

        if (!lower.isNaN() && !upper.isNaN()) {

            // Make a dialog to ask if a filter should be performed
        	Optional<Integer> filterResult = getFilterDialogResult(lower, upper);
        	if(!filterResult.isPresent())
        		return;
            int result = filterResult.get();

            if (result == 0) { // button at index 0 - continue

                // create a new sub-collection with the given parameters for
                // each dataset
                for (IAnalysisDataset dataset : getDatasets()) {
                    ICellCollection collection = dataset.getCollection();
                    try {

                        LOGGER.info("Filtering on " + stat.toString() + ": " + df.format(lower) + " - " + df.format(upper));

                        FilteringOptions op = new CellCollectionFilterBuilder()
                        		.add(stat, CellularComponent.NUCLEUS, scale, lower, upper)
                        		.build();
                        ICellCollection subCollection = CellCollectionFilterer.filter(collection, op);

                        if (subCollection.hasCells()) {

                            LOGGER.info("Filtered " + subCollection.size() + " nuclei");
                            dataset.addChildCollection(subCollection);
                            try {
                                dataset.getCollection().getProfileManager().copySegmentsAndLandmarksTo(subCollection);
                            } catch (Exception e1) {
                                LOGGER.log(Loggable.STACK, "Error applying segments", e1);
                            }
                        }

                    } catch (Exception e) {
                        LOGGER.log(Loggable.STACK, "Error filtering", e);

                    }
                }
                LOGGER.finest( "Firing population update request");
                getInterfaceEventHandler().fireInterfaceEvent(InterfaceMethod.REFRESH_POPULATIONS);
            }
        }
    }
}
