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
package com.bmskinner.nuclear_morphology.gui.tabs.segments;

import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.List;
import java.util.logging.Logger;

import javax.swing.BoxLayout;
import javax.swing.JLabel;
import javax.swing.JPanel;

import org.eclipse.jdt.annotation.NonNull;
import org.jfree.chart.ChartPanel;
import org.jfree.chart.JFreeChart;

import com.bmskinner.nuclear_morphology.components.MissingLandmarkException;
import com.bmskinner.nuclear_morphology.components.cells.CellularComponent;
import com.bmskinner.nuclear_morphology.components.datasets.IAnalysisDataset;
import com.bmskinner.nuclear_morphology.components.datasets.ICellCollection;
import com.bmskinner.nuclear_morphology.components.measure.Measurement;
import com.bmskinner.nuclear_morphology.components.profiles.IProfileSegment;
import com.bmskinner.nuclear_morphology.components.profiles.Landmark;
import com.bmskinner.nuclear_morphology.components.profiles.ProfileException;
import com.bmskinner.nuclear_morphology.core.GlobalOptions;
import com.bmskinner.nuclear_morphology.core.InputSupplier;
import com.bmskinner.nuclear_morphology.gui.Labels;
import com.bmskinner.nuclear_morphology.gui.events.ChartSetEventListener;
import com.bmskinner.nuclear_morphology.gui.events.revamp.ScaleUpdatedListener;
import com.bmskinner.nuclear_morphology.gui.tabs.BoxplotsTabPanel;
import com.bmskinner.nuclear_morphology.logging.Loggable;
import com.bmskinner.nuclear_morphology.visualisation.charts.AbstractChartFactory;
import com.bmskinner.nuclear_morphology.visualisation.charts.panels.ExportableChartPanel;
import com.bmskinner.nuclear_morphology.visualisation.charts.panels.ViolinChartPanel;
import com.bmskinner.nuclear_morphology.visualisation.options.ChartOptions;
import com.bmskinner.nuclear_morphology.visualisation.options.ChartOptionsBuilder;

@SuppressWarnings("serial")
public class SegmentBoxplotsPanel extends BoxplotsTabPanel
		implements ActionListener, ChartSetEventListener, ScaleUpdatedListener {

	private static final Logger LOGGER = Logger.getLogger(SegmentBoxplotsPanel.class.getName());

	private Dimension preferredSize = new Dimension(200, 300);

	public SegmentBoxplotsPanel(@NonNull InputSupplier context) {
		super(context, CellularComponent.NUCLEAR_BORDER_SEGMENT);

		JFreeChart chart = AbstractChartFactory.createEmptyChart();

		ExportableChartPanel chartPanel = new ExportableChartPanel(chart);
		chartPanel.setPreferredSize(preferredSize);
		chartPanels.put("null", chartPanel);

		mainPanel.add(chartPanel);

		uiController.addScaleUpdatedListener(this);

	}

	@Override
	public void actionPerformed(ActionEvent e) {
		update(getDatasets());

	}

	@Override
	protected synchronized void updateSingle() {
		updateMultiple();

	}

	@Override
	protected synchronized void updateMultiple() {
		mainPanel = new JPanel();
		mainPanel.setLayout(new BoxLayout(mainPanel, BoxLayout.X_AXIS));

		LOGGER.finest("Dataset list is not empty");

		// Check that all the datasets have the same number of segments
		if (IProfileSegment.segmentCountsMatch(getDatasets())) { // make a boxplot for each segment

			ICellCollection collection = activeDataset().getCollection();
			List<IProfileSegment> segments;
			try {
				segments = collection.getProfileCollection().getSegments(Landmark.REFERENCE_POINT);
			} catch (MissingLandmarkException | ProfileException e) {
				LOGGER.warning("Cannot get segments");
				LOGGER.log(Loggable.STACK, "Cannot get segments", e);
				return;
			}

			// Get each segment as a boxplot
			for (IProfileSegment seg : segments) {
				JFreeChart chart = AbstractChartFactory.createLoadingChart();
				ViolinChartPanel chartPanel = new ViolinChartPanel(chart);
				chartPanel.addChartSetEventListener(this);
				chartPanel.setPreferredSize(preferredSize);
				chartPanels.put(seg.getName(), chartPanel);
				mainPanel.add(chartPanel);

				ChartOptions options = new ChartOptionsBuilder().setDatasets(getDatasets())
						.addStatistic(Measurement.LENGTH).setScale(GlobalOptions.getInstance().getScale())
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
	protected synchronized void updateNull() {
		mainPanel = new JPanel();
		mainPanel.setLayout(new BoxLayout(mainPanel, BoxLayout.X_AXIS));

		ChartPanel chartPanel = new ChartPanel(AbstractChartFactory.createEmptyChart());
		mainPanel.add(chartPanel);
		mainPanel.revalidate();
		mainPanel.repaint();
		scrollPane.setViewportView(mainPanel);
	}

	@Override
	public void chartSetEventReceived(ChartSetEvent e) {
		((ViolinChartPanel) e.getSource()).restoreAutoBounds();
	}

	@Override
	public void scaleUpdated(List<IAnalysisDataset> datasets) {
		refreshChartCache(datasets);
	}

	@Override
	public void scaleUpdated(IAnalysisDataset dataset) {
		refreshChartCache(dataset);
	}

	@Override
	public void scaleUpdated() {
		update(getDatasets());
	}
}
