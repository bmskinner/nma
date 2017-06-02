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

import java.awt.Dimension;
import java.awt.FlowLayout;
import java.util.List;

import javax.swing.BoxLayout;
import javax.swing.JLabel;
import javax.swing.JPanel;

import org.jfree.chart.JFreeChart;

import com.bmskinner.nuclear_morphology.analysis.profiles.ProfileException;
import com.bmskinner.nuclear_morphology.charting.charts.AbstractChartFactory;
import com.bmskinner.nuclear_morphology.charting.charts.HistogramChartFactory;
import com.bmskinner.nuclear_morphology.charting.charts.panels.SelectableChartPanel;
import com.bmskinner.nuclear_morphology.charting.options.ChartOptions;
import com.bmskinner.nuclear_morphology.charting.options.ChartOptionsBuilder;
import com.bmskinner.nuclear_morphology.components.CellularComponent;
import com.bmskinner.nuclear_morphology.components.ICellCollection;
import com.bmskinner.nuclear_morphology.components.generic.Tag;
import com.bmskinner.nuclear_morphology.components.generic.UnavailableBorderTagException;
import com.bmskinner.nuclear_morphology.components.nuclear.IBorderSegment;
import com.bmskinner.nuclear_morphology.components.stats.PlottableStatistic;
import com.bmskinner.nuclear_morphology.gui.GlobalOptions;
import com.bmskinner.nuclear_morphology.gui.Labels;
import com.bmskinner.nuclear_morphology.gui.components.HistogramsTabPanel;

@SuppressWarnings("serial")
public class SegmentHistogramsPanel extends HistogramsTabPanel {

    private Dimension preferredSize = new Dimension(200, 100);

    public SegmentHistogramsPanel() {
        super(CellularComponent.NUCLEAR_BORDER_SEGMENT);

        JFreeChart chart = HistogramChartFactory.createHistogram(null, "Segment", "Length");
        SelectableChartPanel panel = new SelectableChartPanel(chart, "null");
        panel.setPreferredSize(preferredSize);
        SegmentHistogramsPanel.this.chartPanels.put("null", panel);
        SegmentHistogramsPanel.this.mainPanel.add(panel);

    }

    @Override
    protected void updateSingle() {
        updateMultiple();
    }

    @Override
    protected void updateMultiple() {
        this.setEnabled(true);
        mainPanel = new JPanel();
        mainPanel.setLayout(new BoxLayout(mainPanel, BoxLayout.Y_AXIS));

        boolean useDensity = this.useDensityPanel.isSelected();

        finest("Dataset list is not empty");

        // Check that all the datasets have the same number of segments
        if (IBorderSegment.segmentCountsMatch(getDatasets())) { // make a
                                                                // histogram for
                                                                // each segment

            ICellCollection collection = activeDataset().getCollection();

            List<IBorderSegment> segments;
            try {
                segments = collection.getProfileCollection().getSegments(Tag.REFERENCE_POINT);
            } catch (UnavailableBorderTagException | ProfileException e) {
                warn("Cannot get segments");
                fine("Cannot get segments", e);
                return;
            }

            // Get each segment as a boxplot
            for (IBorderSegment seg : segments) {

                // Create a new chart panel with a loading state, and add it to
                // this panel
                JFreeChart chart = AbstractChartFactory.createLoadingChart();
                SelectableChartPanel chartPanel = new SelectableChartPanel(chart, seg.getName());
                chartPanel.setPreferredSize(preferredSize);
                mainPanel.add(chartPanel);

                // Make the options for the chart, and render it in the
                // background
                ChartOptions options = new ChartOptionsBuilder().setDatasets(getDatasets())
                        .addStatistic(PlottableStatistic.LENGTH).setScale(GlobalOptions.getInstance().getScale())
                        .setSwatch(GlobalOptions.getInstance().getSwatch()).setUseDensity(useDensity)
                        .setSegPosition(seg.getPosition()).setTarget(chartPanel).build();

                finest("Made options for segment histogram " + seg.getName());

                setChart(options);

            }

        } else { // different number of segments, blank chart
            this.setEnabled(false);
            mainPanel.setLayout(new FlowLayout());
            mainPanel.add(new JLabel(Labels.INCONSISTENT_SEGMENT_NUMBER, JLabel.CENTER));
            scrollPane.setViewportView(mainPanel);
        }
        mainPanel.revalidate();
        mainPanel.repaint();
        scrollPane.setViewportView(mainPanel);

    }

    @Override
    protected void updateNull() {
        this.setEnabled(true);
        mainPanel = new JPanel();
        mainPanel.setLayout(new BoxLayout(mainPanel, BoxLayout.Y_AXIS));

        JFreeChart chart = HistogramChartFactory.createHistogram(null, "Segment", "Length");
        SelectableChartPanel panel = new SelectableChartPanel(chart, "null");
        panel.setPreferredSize(preferredSize);
        SegmentHistogramsPanel.this.chartPanels.put("null", panel);
        SegmentHistogramsPanel.this.mainPanel.add(panel);
        mainPanel.revalidate();
        mainPanel.repaint();
        scrollPane.setViewportView(mainPanel);
        this.setEnabled(false);
    }

}
