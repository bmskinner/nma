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
package com.bmskinner.nma.gui.tabs.segments;

import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.List;
import java.util.logging.Logger;
import java.util.logging.Level;

import javax.swing.BoxLayout;
import javax.swing.JLabel;
import javax.swing.JPanel;

import org.jfree.chart.ChartPanel;
import org.jfree.chart.JFreeChart;

import com.bmskinner.nma.components.cells.CellularComponent;
import com.bmskinner.nma.components.datasets.IAnalysisDataset;
import com.bmskinner.nma.components.datasets.ICellCollection;
import com.bmskinner.nma.components.measure.Measurement;
import com.bmskinner.nma.components.profiles.IProfileSegment;
import com.bmskinner.nma.components.profiles.IProfileSegment.SegmentUpdateException;
import com.bmskinner.nma.components.profiles.MissingLandmarkException;
import com.bmskinner.nma.components.rules.OrientationMark;
import com.bmskinner.nma.core.GlobalOptions;
import com.bmskinner.nma.gui.Labels;
import com.bmskinner.nma.gui.components.panels.ExportableChartPanel;
import com.bmskinner.nma.gui.components.panels.ViolinChartPanel;
import com.bmskinner.nma.gui.events.ChartSetEventListener;
import com.bmskinner.nma.gui.events.ProfilesUpdatedListener;
import com.bmskinner.nma.gui.events.ScaleUpdatedListener;
import com.bmskinner.nma.gui.events.SwatchUpdatedListener;
import com.bmskinner.nma.gui.tabs.BoxplotsTabPanel;

import com.bmskinner.nma.visualisation.charts.AbstractChartFactory;
import com.bmskinner.nma.visualisation.options.ChartOptions;
import com.bmskinner.nma.visualisation.options.ChartOptionsBuilder;

@SuppressWarnings("serial")
public class SegmentBoxplotsPanel extends BoxplotsTabPanel
		implements ActionListener, ChartSetEventListener,
		ScaleUpdatedListener, SwatchUpdatedListener, ProfilesUpdatedListener {

	private static final Logger LOGGER = Logger.getLogger(SegmentBoxplotsPanel.class.getName());

	private static final String PANEL_TITLE_LBL = "Violin plots";
	private static final String PANEL_DESC_LBL = "Distributions of measured values with boxplots";

	private Dimension preferredSize = new Dimension(200, 300);

	public SegmentBoxplotsPanel() {
		super(CellularComponent.NUCLEAR_BORDER_SEGMENT, PANEL_TITLE_LBL, PANEL_DESC_LBL);

		JFreeChart chart = AbstractChartFactory.createEmptyChart();

		ExportableChartPanel chartPanel = new ExportableChartPanel(chart);
		chartPanel.setPreferredSize(preferredSize);
		chartPanels.put("null", chartPanel);

		mainPanel.add(chartPanel);

		uiController.addScaleUpdatedListener(this);
		uiController.addSwatchUpdatedListener(this);
		uiController.addProfilesUpdatedListener(this);

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
				segments = collection.getProfileCollection().getSegments(OrientationMark.REFERENCE);
			} catch (MissingLandmarkException | SegmentUpdateException e) {
				LOGGER.warning("Cannot get segments");
				LOGGER.log(Level.SEVERE, "Cannot get segments", e);
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
						.addStatistic(Measurement.LENGTH)
						.setScale(GlobalOptions.getInstance().getScale())
						.setSwatch(GlobalOptions.getInstance().getSwatch())
						.setSegPosition(seg.getPosition())
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
		refreshCache(datasets);
	}

	@Override
	public void scaleUpdated(IAnalysisDataset dataset) {
		refreshCache(dataset);
	}

	@Override
	public void scaleUpdated() {
		update(getDatasets());
	}

	@Override
	public void globalPaletteUpdated() {
		update(getDatasets());
	}

	@Override
	public void colourUpdated(IAnalysisDataset dataset) {
		refreshCache(dataset);
	}

	@Override
	public void profilesUpdated(List<IAnalysisDataset> datasets) {
		refreshCache(datasets);
	}

	@Override
	public void profilesUpdated(IAnalysisDataset dataset) {
		refreshCache(dataset);
	}
}
