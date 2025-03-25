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
package com.bmskinner.nma.gui.tabs;

import java.awt.BorderLayout;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.Toolkit;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.HashMap;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;

import javax.swing.BoxLayout;
import javax.swing.JPanel;
import javax.swing.JScrollPane;

import org.eclipse.jdt.annotation.NonNull;
import org.jfree.chart.JFreeChart;

import com.bmskinner.nma.gui.components.panels.ExportableChartPanel;
import com.bmskinner.nma.visualisation.charts.AbstractChartFactory;
import com.bmskinner.nma.visualisation.charts.ViolinChartFactory;
import com.bmskinner.nma.visualisation.options.ChartOptions;

/**
 * Base class for multiple violin plots arranged horizontally
 *
 * @author Ben Skinner
 *
 */
@SuppressWarnings("serial")
public abstract class BoxplotsTabPanel extends ChartDetailPanel implements ActionListener {

	private static final Logger LOGGER = Logger.getLogger(BoxplotsTabPanel.class.getName());

	private static final String PANEL_TITLE_LBL = "Violin plots";
	protected transient Map<String, ExportableChartPanel> chartPanels = new HashMap<>();

	protected JPanel mainPanel; // hold the charts
	protected JPanel headerPanel; // hold buttons

	protected JScrollPane scrollPane; // hold the main panel

	protected String component;

	/**
	 * Create with the default panel title label
	 * 
	 * @param context
	 * @param component
	 */
	protected BoxplotsTabPanel(String component) {
		this(component, PANEL_TITLE_LBL, PANEL_TITLE_LBL);
	}

	/**
	 * Create with a custom panel title label
	 * 
	 * @param context
	 * @param component
	 */
	protected BoxplotsTabPanel(String component, @NonNull String panelTitle,
			@NonNull String panelDesc) {
		super(panelTitle, panelDesc);
		this.component = component;
		this.setLayout(new BorderLayout());

		try {
			mainPanel = new JPanel();
			mainPanel.setLayout(new BoxLayout(mainPanel, BoxLayout.X_AXIS));

			headerPanel = new JPanel(new FlowLayout());

			this.add(headerPanel, BorderLayout.NORTH);

			// add the scroll pane to the tab
			scrollPane = new JScrollPane(mainPanel);

			Dimension screenSize = Toolkit.getDefaultToolkit().getScreenSize();
			Dimension preferredFloatingDimension = new Dimension(
					(int) (screenSize.getWidth() * 0.25),
					(int) (screenSize.getHeight() * 0.25));
			scrollPane.setPreferredSize(preferredFloatingDimension);

			this.add(scrollPane, BorderLayout.CENTER);

			this.setEnabled(false);
		} catch (Exception e) {
			LOGGER.log(Level.SEVERE, "Error creating panel", e);
		}
	}

	@Override
	public synchronized void setLoading() {
		super.setLoading();
		for (ExportableChartPanel p : chartPanels.values()) {
			p.setChart(AbstractChartFactory.createLoadingChart());
		}

	}

	@Override
	protected synchronized JFreeChart createPanelChartType(@NonNull ChartOptions options) {
		return new ViolinChartFactory(options).createStatisticPlot(component);
	}

	@Override
	public void actionPerformed(ActionEvent e) {
		this.update(getDatasets());
	}

}
