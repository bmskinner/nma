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
package com.bmskinner.nma.gui.tabs.profiles;

import java.awt.BorderLayout;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;

import javax.swing.JPanel;

import org.eclipse.jdt.annotation.NonNull;
import org.jfree.chart.JFreeChart;
import org.jfree.chart.plot.XYPlot;
import org.jfree.data.general.DatasetUtils;
import org.jfree.data.xy.XYDataset;

import com.bmskinner.nma.components.measure.MeasurementDimension;
import com.bmskinner.nma.components.profiles.ProfileType;
import com.bmskinner.nma.gui.components.panels.ProfileAlignmentOptionsPanel;
import com.bmskinner.nma.gui.components.panels.ProfileMarkersOptionsPanel;
import com.bmskinner.nma.gui.events.ProfilesUpdatedListener;
import com.bmskinner.nma.gui.events.SwatchUpdatedListener;
import com.bmskinner.nma.gui.tabs.ChartDetailPanel;
import com.bmskinner.nma.visualisation.charts.AbstractChartFactory;
import com.bmskinner.nma.visualisation.charts.panels.ExportableChartPanel;

@SuppressWarnings("serial")
public abstract class AbstractProfileDisplayPanel extends ChartDetailPanel
		implements ActionListener, ProfilesUpdatedListener, SwatchUpdatedListener {

	Dimension minimumChartSize = new Dimension(50, 100);
	Dimension preferredChartSize = new Dimension(400, 300);

	protected JPanel buttonPanel = new JPanel(new FlowLayout());
	protected ExportableChartPanel chartPanel;

	protected ProfileAlignmentOptionsPanel profileAlignmentOptionsPanel = new ProfileAlignmentOptionsPanel();
	protected ProfileMarkersOptionsPanel profileMarkersOptionsPanel = new ProfileMarkersOptionsPanel();

	protected ProfileType type;

	protected AbstractProfileDisplayPanel(ProfileType type, @NonNull String panelTitle,
			@NonNull String panelDesc) {
		super(panelTitle, panelDesc);
		this.type = type;

		this.setLayout(new BorderLayout());
		JFreeChart rawChart = AbstractChartFactory.createEmptyChart();
		chartPanel = makeProfileChartPanel(rawChart);

		chartPanel.setMinimumDrawWidth(0);
		chartPanel.setMinimumDrawHeight(0);
		this.setMinimumSize(minimumChartSize);
		this.setPreferredSize(preferredChartSize);
		this.add(chartPanel, BorderLayout.CENTER);

		// add the alignments panel to the tab

		buttonPanel.add(profileAlignmentOptionsPanel);
		profileAlignmentOptionsPanel.addActionListener(this);
		profileAlignmentOptionsPanel.setEnabled(false);

		buttonPanel.add(profileMarkersOptionsPanel);
		profileMarkersOptionsPanel.addActionListener(this);
		profileMarkersOptionsPanel.setEnabled(false);

		this.add(buttonPanel, BorderLayout.NORTH);
		uiController.addProfilesUpdatedListener(this);
		uiController.addSwatchUpdatedListener(this);
	}

	@Override
	public String getPanelTitle() {
		return type.toString();
	}

	private ExportableChartPanel makeProfileChartPanel(JFreeChart chart) {
		ExportableChartPanel panel = new ExportableChartPanel(chart) {
			@Override
			public void restoreAutoBounds() {
				XYPlot plot = (XYPlot) this.getChart().getPlot();

				int length = 100;
				for (int i = 0; i < plot.getDatasetCount(); i++) {
					XYDataset dataset = plot.getDataset(i);
					Number maximum = DatasetUtils.findMaximumDomainValue(dataset);
					length = maximum.intValue() > length ? maximum.intValue() : length;
				}

				if (type.getDimension().equals(MeasurementDimension.ANGLE)) {
					plot.getRangeAxis().setRange(0, 360);
				} else {
					plot.getRangeAxis().setAutoRange(true);
				}

				plot.getDomainAxis().setRange(0, length);
			}
		};
		// Disable entity collection for profiles - too much data
		panel.getChartRenderingInfo().setEntityCollection(null);
		return panel;
	}

	@Override
	public void setEnabled(boolean b) {
		profileAlignmentOptionsPanel.setEnabled(b);
		profileMarkersOptionsPanel.setEnabled(b);
	}

	@Override
	protected synchronized void updateSingle() {

		this.setEnabled(true);

	}

	@Override
	protected synchronized void updateMultiple() {
		// Don't allow marker selection for multiple datasets
		this.setEnabled(true);
		profileMarkersOptionsPanel.setEnabled(false);
	}

	@Override
	protected synchronized void updateNull() {
		this.setEnabled(false);
	}

	@Override
	public synchronized void setLoading() {
		super.setLoading();
		chartPanel.setChart(AbstractChartFactory.createLoadingChart());

	}

	@Override
	public void actionPerformed(ActionEvent e) {
		update(getDatasets());
	}
}
