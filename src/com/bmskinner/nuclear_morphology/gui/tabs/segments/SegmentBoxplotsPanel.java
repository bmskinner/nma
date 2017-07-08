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
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.List;

import javax.swing.BoxLayout;
import javax.swing.JLabel;
import javax.swing.JPanel;

import org.jfree.chart.ChartPanel;
import org.jfree.chart.JFreeChart;

import com.bmskinner.nuclear_morphology.analysis.profiles.ProfileException;
import com.bmskinner.nuclear_morphology.charting.charts.AbstractChartFactory;
import com.bmskinner.nuclear_morphology.charting.charts.BoxplotChartFactory;
import com.bmskinner.nuclear_morphology.charting.charts.panels.ExportableChartPanel;
import com.bmskinner.nuclear_morphology.charting.charts.panels.ViolinChartPanel;
import com.bmskinner.nuclear_morphology.charting.options.ChartOptions;
import com.bmskinner.nuclear_morphology.charting.options.ChartOptionsBuilder;
import com.bmskinner.nuclear_morphology.components.CellularComponent;
import com.bmskinner.nuclear_morphology.components.ICellCollection;
import com.bmskinner.nuclear_morphology.components.generic.Tag;
import com.bmskinner.nuclear_morphology.components.generic.UnavailableBorderTagException;
import com.bmskinner.nuclear_morphology.components.nuclear.IBorderSegment;
import com.bmskinner.nuclear_morphology.components.stats.PlottableStatistic;
import com.bmskinner.nuclear_morphology.gui.ChartSetEvent;
import com.bmskinner.nuclear_morphology.gui.ChartSetEventListener;
import com.bmskinner.nuclear_morphology.gui.Labels;
import com.bmskinner.nuclear_morphology.gui.tabs.BoxplotsTabPanel;
import com.bmskinner.nuclear_morphology.main.GlobalOptions;

@SuppressWarnings("serial")
public class SegmentBoxplotsPanel extends BoxplotsTabPanel implements ActionListener, ChartSetEventListener {

    private Dimension preferredSize = new Dimension(200, 300);

    public SegmentBoxplotsPanel() {
        super(CellularComponent.NUCLEAR_BORDER_SEGMENT);

        JFreeChart boxplot = BoxplotChartFactory.makeEmptyChart();

        ExportableChartPanel chartPanel = new ExportableChartPanel(boxplot);
        chartPanel.setPreferredSize(preferredSize);
        chartPanels.put("null", chartPanel);

        mainPanel.add(chartPanel);

    }

    @Override
    public void actionPerformed(ActionEvent e) {
        update(getDatasets());

    }

    @Override
    protected void updateSingle() {
        updateMultiple();

    }

    @Override
    protected void updateMultiple() {
        mainPanel = new JPanel();
        mainPanel.setLayout(new BoxLayout(mainPanel, BoxLayout.X_AXIS));

        finest("Dataset list is not empty");

        // Check that all the datasets have the same number of segments
        if (IBorderSegment.segmentCountsMatch(getDatasets())) { // make a
                                                                // boxplot for
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

                JFreeChart chart = AbstractChartFactory.createLoadingChart();
                ViolinChartPanel chartPanel = new ViolinChartPanel(chart);
                chartPanel.addChartSetEventListener(this);
                chartPanel.setPreferredSize(preferredSize);
                chartPanels.put(seg.getName(), chartPanel);
                mainPanel.add(chartPanel);

                ChartOptions options = new ChartOptionsBuilder().setDatasets(getDatasets())
                        .addStatistic(PlottableStatistic.LENGTH).setScale(GlobalOptions.getInstance().getScale())
                        .setSwatch(GlobalOptions.getInstance().getSwatch()).setSegPosition(seg.getPosition())
                        .setTarget(chartPanel).build();

                setChart(options);
            }

        } else { // different number of segments, blank chart
            mainPanel.setLayout(new FlowLayout());
            mainPanel.add(new JLabel(Labels.INCONSISTENT_SEGMENT_NUMBER, JLabel.CENTER));
        }
        mainPanel.revalidate();
        mainPanel.repaint();

        scrollPane.setViewportView(mainPanel);
    }

    @Override
    protected void updateNull() {
        mainPanel = new JPanel();
        mainPanel.setLayout(new BoxLayout(mainPanel, BoxLayout.X_AXIS));

        ChartPanel chartPanel = new ChartPanel(BoxplotChartFactory.makeEmptyChart());
        mainPanel.add(chartPanel);
        mainPanel.revalidate();
        mainPanel.repaint();
        scrollPane.setViewportView(mainPanel);
    }

    @Override
    public void chartSetEventReceived(ChartSetEvent e) {
        ((ViolinChartPanel) e.getSource()).restoreAutoBounds();
    }
}
