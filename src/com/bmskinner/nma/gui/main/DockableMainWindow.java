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
package com.bmskinner.nma.gui.main;

import java.awt.BorderLayout;
import java.awt.Dimension;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Toolkit;
import java.net.URL;
import java.util.logging.Level;
import java.util.logging.Logger;

import javax.swing.BorderFactory;
import javax.swing.ImageIcon;
import javax.swing.JPanel;
import javax.swing.JSplitPane;
import javax.swing.border.EmptyBorder;

import com.bmskinner.nma.components.Version;
import com.bmskinner.nma.core.GlobalOptions;
import com.bmskinner.nma.gui.LogPanel;
import com.bmskinner.nma.gui.events.UserActionController;
import com.bmskinner.nma.gui.tabs.AnalysisDetailPanel;
import com.bmskinner.nma.gui.tabs.ClusterDetailPanel;
import com.bmskinner.nma.gui.tabs.DetailPanel;
import com.bmskinner.nma.gui.tabs.ImagesTabPanel;
import com.bmskinner.nma.gui.tabs.MergesDetailPanel;
import com.bmskinner.nma.gui.tabs.TabPanel;
import com.bmskinner.nma.gui.tabs.cells_detail.IndividualCellDetailPanel;
import com.bmskinner.nma.gui.tabs.comparisons.ComparisonDetailPanel;
import com.bmskinner.nma.gui.tabs.consensus.ConsensusNucleusPanel;
import com.bmskinner.nma.gui.tabs.editing.DatasetEditingPanel;
import com.bmskinner.nma.gui.tabs.nuclear.NuclearStatisticsPanel;
import com.bmskinner.nma.gui.tabs.populations.DatasetSelectionPanel;
import com.bmskinner.nma.gui.tabs.profiles.NucleusProfilesPanel;
import com.bmskinner.nma.gui.tabs.segments.SegmentsDetailPanel;
import com.bmskinner.nma.gui.tabs.signals.SignalsDetailPanel;
import com.bmskinner.nma.io.UpdateChecker;
import com.bmskinner.nma.logging.LogPanelFormatter;
import com.bmskinner.nma.logging.LogPanelHandler;
import com.bmskinner.nma.logging.Loggable;
import com.javadocking.DockingManager;
import com.javadocking.dock.Position;
import com.javadocking.dock.TabDock;
import com.javadocking.dock.factory.SingleDockFactory;
import com.javadocking.dockable.DefaultDockable;
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

	private static final long serialVersionUID = 1L;

	private static final Logger LOGGER = Logger.getLogger(DockableMainWindow.class.getName());

	private TabDock tabDock;

	/**
	 * Create the frame.
	 * 
	 * @param standalone is the frame a standalone app, or launched within ImageJ?
	 * @param eh         the event handler controlling actions
	 */
	public DockableMainWindow() {
		super();
		loadImageIcon();
		createWindowListeners();

		createUI();

		this.setJMenuBar(new MainWindowMenuBar(this));

		// Run update check if allowed in config
		if (GlobalOptions.getInstance().getBoolean(GlobalOptions.ALLOW_UPDATE_CHECK_KEY)) {
			Version latestVersion = UpdateChecker.fetchLatestVersion();
			if (latestVersion.isNewerThan(Version.currentVersion())) {
				LOGGER.info(() -> "New version %s available".formatted(latestVersion));
			}
		} else {
			LOGGER.fine(
					"Skipping update check because config setting CHECK_FOR_UPDATES is false");
		}

	}

	private void loadImageIcon() {
		ClassLoader cl = this.getClass().getClassLoader();
		URL url = cl.getResource("icons/icon.png");
		ImageIcon icon = new ImageIcon(url);
		setIconImage(icon.getImage());
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

			// Create the log panel
			FloatDockModel dockModel = new FloatDockModel();
			dockModel.addOwner("frame0", this);
			DockingManager.setDockModel(dockModel);
			SingleDockFactory floatFactory = new SingleDockFactory();
			dockModel.getFloatDock(this).setChildDockFactory(floatFactory); // ensure floating docks
																			// are not converted
																			// to tab docks

			DatasetSelectionPanel populationsPanel = new DatasetSelectionPanel();
			populationsPanel.setBorder(BorderFactory.createEmptyBorder());
			ConsensusNucleusPanel consensusNucleusPanel = new ConsensusNucleusPanel();
			consensusNucleusPanel.setBorder(BorderFactory.createEmptyBorder());

			detailPanels.add(consensusNucleusPanel);

			LogPanel logPanel = createLogPanel();
			JPanel topPanel = createTopPanel(logPanel, populationsPanel, consensusNucleusPanel);

			tabDock = createTabs();
			dockModel.addRootDock("tabdock", tabDock, this);

			// Add the top and bottom rows to the main panel
			JSplitPane panelMain = new JSplitPane(JSplitPane.VERTICAL_SPLIT, topPanel, tabDock);
			panelMain.setBorder(BorderFactory.createEmptyBorder());
			contentPane.add(panelMain, BorderLayout.CENTER);

			this.pack();
			consensusNucleusPanel.restoreAutoBounds();
		} catch (Exception e) {
			LOGGER.log(Level.SEVERE, "Error initialising main view: %s".formatted(e.getMessage()),
					e);
		}
	}

	/**
	 * Create the log panel and register the handler with the logger
	 * 
	 * @return
	 */
	private LogPanel createLogPanel() {
		LogPanel logPanel = new LogPanel();
		logPanel.setBorder(BorderFactory.createEmptyBorder());

		// Set up the log panel
		UserActionController.getInstance().setProgressBarAcceptor(logPanel);
		LogPanelHandler textHandler = new LogPanelHandler(logPanel);
		textHandler.setLevel(Level.INFO);
		textHandler.setFormatter(new LogPanelFormatter());
		Logger.getLogger(Loggable.PROJECT_LOGGER).addHandler(textHandler);
		return logPanel;
	}

	private JPanel createTopPanel(LogPanel logPanel, DatasetSelectionPanel populationsPanel,
			ConsensusNucleusPanel consensusNucleusPanel) {

		// Provide minimum sizes for the components
		Dimension minimumSize = new Dimension(300, 200);
		logPanel.setMinimumSize(minimumSize);
		populationsPanel.setMinimumSize(minimumSize);
		consensusNucleusPanel.setMaximumSize(minimumSize);

		// Create a container for the datasets and consensus panels
		JPanel dataPanel = new JPanel();
		dataPanel.setBorder(BorderFactory.createEmptyBorder());
		dataPanel.setLayout(new GridBagLayout());
		GridBagConstraints c = new GridBagConstraints();
		c.gridwidth = 1;
		c.fill = GridBagConstraints.BOTH; // fill both axes of container
		c.weightx = 1.0; // maximum weighting
		c.weighty = 1.0;
		c.gridx = 0;
		c.gridy = 0;

		dataPanel.add(populationsPanel, c);

		c.gridx = 1;
		c.weightx = 0.5;

		dataPanel.add(consensusNucleusPanel, c);

		// Create split pane to separate log panel from others
		JSplitPane splitPane = new JSplitPane(JSplitPane.HORIZONTAL_SPLIT, logPanel, dataPanel);
		splitPane.setBorder(BorderFactory.createEmptyBorder());

		JPanel topRow = new JPanel(new BorderLayout());
		topRow.setBorder(BorderFactory.createEmptyBorder());
		topRow.add(splitPane);
		return topRow;
	}

	/**
	 * Create the individual analysis tabs
	 */
	private TabDock createTabs() {

		tabDock = new TabDock();

		// Create the top level tabs in the UI
		DetailPanel analysisDetailPanel = new AnalysisDetailPanel();
		DetailPanel nucleusProfilesPanel = new NucleusProfilesPanel(); // the angle profiles
		DetailPanel cellsDetailPanel = new IndividualCellDetailPanel();
		DetailPanel nuclearChartsPanel = new NuclearStatisticsPanel();
		DetailPanel signalsDetailPanel = new SignalsDetailPanel();
		DetailPanel clusterDetailPanel = new ClusterDetailPanel();
		DetailPanel mergesDetailPanel = new MergesDetailPanel();
		DetailPanel segmentsDetailPanel = new SegmentsDetailPanel();
		DetailPanel comparisonsPanel = new ComparisonDetailPanel();
		DetailPanel editingDetailPanel = new DatasetEditingPanel();
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
			DefaultDockable d = new DefaultDockable(p.getPanelTitle(), p, p.getPanelTitle(), null,
					DockingMode.ALL);
			d.setDescription(p.getPanelDescription());
			tabDock.addDockable(d, new Position(i++));
		}

		tabDock.setSelectedDockable(tabDock.getDockable(0));
		return tabDock;

	}
}
