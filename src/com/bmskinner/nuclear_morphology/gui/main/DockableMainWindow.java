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
package com.bmskinner.nuclear_morphology.gui.main;

import java.awt.BorderLayout;
import java.awt.Dimension;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Toolkit;
import java.util.logging.Level;
import java.util.logging.Logger;

import javax.swing.JPanel;
import javax.swing.JSplitPane;
import javax.swing.border.EmptyBorder;

import com.bmskinner.nuclear_morphology.components.Version;
import com.bmskinner.nuclear_morphology.gui.LogPanel;
import com.bmskinner.nuclear_morphology.gui.events.revamp.UserActionController;
import com.bmskinner.nuclear_morphology.gui.tabs.AnalysisDetailPanel;
import com.bmskinner.nuclear_morphology.gui.tabs.ClusterDetailPanel;
import com.bmskinner.nuclear_morphology.gui.tabs.DetailPanel;
import com.bmskinner.nuclear_morphology.gui.tabs.EditingDetailPanel;
import com.bmskinner.nuclear_morphology.gui.tabs.ImagesTabPanel;
import com.bmskinner.nuclear_morphology.gui.tabs.MergesDetailPanel;
import com.bmskinner.nuclear_morphology.gui.tabs.TabPanel;
import com.bmskinner.nuclear_morphology.gui.tabs.cells.CellsDetailPanel;
import com.bmskinner.nuclear_morphology.gui.tabs.comparisons.ComparisonDetailPanel;
import com.bmskinner.nuclear_morphology.gui.tabs.consensus.ConsensusNucleusPanel;
import com.bmskinner.nuclear_morphology.gui.tabs.nuclear.NuclearStatisticsPanel;
import com.bmskinner.nuclear_morphology.gui.tabs.populations.PopulationsPanel;
import com.bmskinner.nuclear_morphology.gui.tabs.profiles.NucleusProfilesPanel;
import com.bmskinner.nuclear_morphology.gui.tabs.segments.SegmentsDetailPanel;
import com.bmskinner.nuclear_morphology.gui.tabs.signals.SignalsDetailPanel;
import com.bmskinner.nuclear_morphology.io.UpdateChecker;
import com.bmskinner.nuclear_morphology.logging.LogPanelFormatter;
import com.bmskinner.nuclear_morphology.logging.LogPanelHandler;
import com.bmskinner.nuclear_morphology.logging.Loggable;
import com.javadocking.DockingManager;
import com.javadocking.dock.Position;
import com.javadocking.dock.TabDock;
import com.javadocking.dock.factory.SingleDockFactory;
import com.javadocking.dockable.DefaultDockable;
import com.javadocking.dockable.Dockable;
import com.javadocking.dockable.DockingMode;
import com.javadocking.model.FloatDockModel;

/**
 * An alternative main window for testing docking frameworks
 * 
 * @author ben
 * @since 1.14.0
 *
 */
public class DockableMainWindow extends AbstractMainWindow {

	private static final Logger LOGGER = Logger.getLogger(DockableMainWindow.class.getName());

	/**
	 * Create the frame.
	 * 
	 * @param standalone is the frame a standalone app, or launched within ImageJ?
	 * @param eh         the event handler controlling actions
	 */
	public DockableMainWindow() {
		super();

		createWindowListeners();

		createUI();

		this.setJMenuBar(new MainWindowMenuBar(this));

		// Run update check
		Version latestVersion = UpdateChecker.fetchLatestVersion();
		if (latestVersion.isNewerThan(Version.currentVersion())) {
			LOGGER.info("New version " + latestVersion + " available");
		}

	}

	/**
	 * Create the main UI
	 */
	@Override
	protected void createUI() {
		try {

			Dimension preferredSize = new Dimension(1080, 804);
			this.setPreferredSize(preferredSize);
			setBounds(100, 100, 1012, 804);
			JPanel contentPane = new JPanel();
			contentPane.setBorder(new EmptyBorder(2, 2, 2, 2));
			contentPane.setLayout(new BorderLayout(2, 2));
			setContentPane(contentPane);

			// ---------------
			// Create the log panel
			// ---------------

			FloatDockModel dockModel = new FloatDockModel();
			dockModel.addOwner("frame0", this);
			DockingManager.setDockModel(dockModel);
			SingleDockFactory floatFactory = new SingleDockFactory();
			dockModel.getFloatDock(this).setChildDockFactory(floatFactory); // ensure floating docks are not converted
																			// to tab docks

			LogPanel logPanel = new LogPanel();
			UserActionController.getInstance().setProgressBarAcceptor(logPanel);

			LogPanelHandler textHandler = new LogPanelHandler(logPanel);
			textHandler.setLevel(Level.INFO);
			textHandler.setFormatter(new LogPanelFormatter());

			// Add to the root program logger
			Logger.getLogger(Loggable.PROJECT_LOGGER).addHandler(textHandler);

			// ---------------
			// Create the consensus chart
			// ---------------
			PopulationsPanel populationsPanel = new PopulationsPanel();
			ConsensusNucleusPanel consensusNucleusPanel = new ConsensusNucleusPanel();
			detailPanels.add(consensusNucleusPanel);

			JSplitPane logAndPopulations = new JSplitPane(JSplitPane.HORIZONTAL_SPLIT, logPanel, populationsPanel);

			// Provide minimum sizes for the two components in the split pane
			Dimension minimumSize = new Dimension(300, 200);
			logPanel.setMinimumSize(minimumSize);
			populationsPanel.setMinimumSize(minimumSize);
			// ---------------
			// Make the top row panel
			// ---------------
			JPanel topRow = new JPanel();

			GridBagConstraints c = new GridBagConstraints();
			c.gridwidth = GridBagConstraints.RELATIVE; // next-to-last element
			c.fill = GridBagConstraints.BOTH; // fill both axes of container
			c.weightx = 1.0; // maximum weighting
			c.weighty = 1.0;

			topRow.setLayout(new GridBagLayout());
			topRow.add(logAndPopulations, c);

			c.gridwidth = GridBagConstraints.REMAINDER; // last element in row
			c.weightx = 0.5; // allow padding on x axis
			c.weighty = 1.0; // max weighting on y axis
			c.fill = GridBagConstraints.BOTH; // fill to bounds where possible
			topRow.add(consensusNucleusPanel, c);
			TabDock tabDock = createTabs();
			dockModel.addRootDock("tabdock", tabDock, this);

			// ---------------
			// Add the top and bottom rows to the main panel
			// ---------------
			JSplitPane panelMain = new JSplitPane(JSplitPane.VERTICAL_SPLIT, topRow, tabDock);
			contentPane.add(panelMain, BorderLayout.CENTER);

			this.pack();
			consensusNucleusPanel.restoreAutoBounds();
		} catch (Exception e) {
			LOGGER.log(Level.SEVERE, "Error initialising main view: " + e.getMessage(), e);
		}
	}

	/**
	 * Create the individual analysis tabs
	 */
	private TabDock createTabs() {

		TabDock tabDock = new TabDock();

		// Create the top level tabs in the UI
		DetailPanel analysisDetailPanel = new AnalysisDetailPanel();
		DetailPanel nucleusProfilesPanel = new NucleusProfilesPanel(); // the angle profiles
		DetailPanel cellsDetailPanel = new CellsDetailPanel();
		DetailPanel nuclearChartsPanel = new NuclearStatisticsPanel();
		DetailPanel signalsDetailPanel = new SignalsDetailPanel();
		DetailPanel clusterDetailPanel = new ClusterDetailPanel();
		DetailPanel mergesDetailPanel = new MergesDetailPanel();
		DetailPanel segmentsDetailPanel = new SegmentsDetailPanel();
		DetailPanel comparisonsPanel = new ComparisonDetailPanel();
		DetailPanel editingDetailPanel = new EditingDetailPanel();
		DetailPanel imagesTabPanel = new ImagesTabPanel();

		detailPanels.add(analysisDetailPanel);
		detailPanels.add(imagesTabPanel);

		detailPanels.add(cellsDetailPanel);

		detailPanels.add(nuclearChartsPanel);
		detailPanels.add(nucleusProfilesPanel);
		detailPanels.add(signalsDetailPanel);
		detailPanels.add(segmentsDetailPanel);

		detailPanels.add(comparisonsPanel);

		detailPanels.add(clusterDetailPanel);
		detailPanels.add(mergesDetailPanel);

		detailPanels.add(editingDetailPanel);

		int i = 0;
		for (TabPanel t : detailPanels) {
			if (t instanceof ConsensusNucleusPanel) {
				continue;
			}

			DetailPanel p = (DetailPanel) t;
			p.setMaximumSize(Toolkit.getDefaultToolkit().getScreenSize());
			Dockable d = new DefaultDockable(p.getPanelTitle(), p, p.getPanelTitle(), null, DockingMode.ALL);
			tabDock.addDockable(d, new Position(i++));
		}

		tabDock.setSelectedDockable(tabDock.getDockable(0));
//		tabDock.getTabbedPane().addChangeListener(e -> { // listen for tab switches and update charts if cells have been
//															// edited
//			boolean isUpdate = false;
//			for (TabPanel t : detailPanels) {
//				isUpdate |= t.hasCellUpdate();
//			}
//
//			if (isUpdate) {
//				recacheCharts();
//				for (TabPanel t : detailPanels) {
//					t.setCellUpdate(false);
//				}
//			}
//		});
		return tabDock;

	}

	/**
	 * Set the event listeners for each tab panel
	 */
//	@Override
//	protected void createEventHandling() {
//
//		this.addDatasetUpdateEventListener(logPanel);
//
//		populationsPanel.addDatasetEventListener(eh);
//
//		for (TabPanel d : detailPanels) {
//			d.addDatasetEventListener(eh);
//			this.addDatasetUpdateEventListener(d);
//		}
//	}
}
