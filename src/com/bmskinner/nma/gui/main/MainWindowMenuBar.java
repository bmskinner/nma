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

import java.awt.Component;
import java.awt.Desktop;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.Window;
import java.awt.event.InputEvent;
import java.awt.event.KeyEvent;
import java.awt.event.WindowEvent;
import java.awt.event.WindowListener;
import java.io.File;
import java.io.IOException;
import java.net.JarURLConnection;
import java.net.URL;
import java.nio.file.Files;
import java.security.CodeSource;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;

import javax.swing.BorderFactory;
import javax.swing.Box;
import javax.swing.ButtonGroup;
import javax.swing.JCheckBoxMenuItem;
import javax.swing.JLabel;
import javax.swing.JMenu;
import javax.swing.JMenuBar;
import javax.swing.JMenuItem;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JRadioButtonMenuItem;
import javax.swing.KeyStroke;

import com.bmskinner.nma.components.Version;
import com.bmskinner.nma.components.XMLNames;
import com.bmskinner.nma.components.datasets.IAnalysisDataset;
import com.bmskinner.nma.components.measure.MeasurementScale;
import com.bmskinner.nma.core.DatasetListManager;
import com.bmskinner.nma.core.GlobalOptions;
import com.bmskinner.nma.core.ThreadManager;
import com.bmskinner.nma.gui.ContextEnabled;
import com.bmskinner.nma.gui.Labels;
import com.bmskinner.nma.gui.MenuFactory;
import com.bmskinner.nma.gui.MenuFactory.ContextualMenu;
import com.bmskinner.nma.gui.MenuFactory.ContextualMenuItem;
import com.bmskinner.nma.gui.actions.NewAnalysisAction;
import com.bmskinner.nma.gui.components.ColourSelecter.ColourSwatch;
import com.bmskinner.nma.gui.dialogs.VersionHelpDialog;
import com.bmskinner.nma.gui.events.DatasetSelectionUpdatedListener;
import com.bmskinner.nma.gui.events.FileImportEventListener.FileImportEvent;
import com.bmskinner.nma.gui.events.UIController;
import com.bmskinner.nma.gui.events.UserActionController;
import com.bmskinner.nma.gui.events.UserActionEvent;
import com.bmskinner.nma.io.Io;
import com.bmskinner.nma.io.UpdateChecker;
import com.bmskinner.nma.logging.Loggable;
import com.bmskinner.nma.utility.FileUtils;

import ij.plugin.BrowserLauncher;

/**
 * Menu bar for the main window
 * 
 * @author Ben Skinner
 *
 */
@SuppressWarnings("serial")
public class MainWindowMenuBar extends JMenuBar implements DatasetSelectionUpdatedListener {

	private static final Logger LOGGER = Logger.getLogger(MainWindowMenuBar.class.getName());

	private static final String TASK_QUEUE_LBL = "Task queue:";
	private static final String MEMORY_LBL = "Memory:";

	private static final String FILE_MENU_LBL = "File";
	private static final String NEW_ANALYSIS_MENU_LBL = "New analysis";
	private static final String NEW_ANALYSIS_CUSTOM_LBL = "Use custom detection options";
	private static final String NEW_ANALYSIS_CUSTOM_TOOLTIP = "Configure the nucleus detection options yourself";
	private static final String NEW_ANALYSIS_SAVED_LBL = "Use saved detection options";
	private static final String NEW_ANALYSIS_SAVED_TOOLTIP = "Use options saved in a file for automatic nucleus detection";

	private static final String NEW_TEXT_ANALYSIS_LBL = "Use text file of nucleus coordinates";
	private static final String NEW_TEXT_ANALYSIS_TOOLTIP = "Use text file containing nucleus outlines";

	private static final String NEW_WORKSPACE_LBL = "New workspace";
	private static final String NEW_WORKSPACE_TOOLTIP = "Create a new workspace";

	private static final String OPEN_MENU_LBL = "Open";
	private static final String OPEN_DATASET_LBL = "Open dataset";
	private static final String OPEN_DATASET_TOOLTIP = "Open a saved dataset";
	private static final String OPEN_WORKSPACE_LBL = "Open workspace";
	private static final String OPEN_WORKSPACE_TOOLTIP = "Open a saved workspace";

	private static final String WORKSPACE_MENU_LBL = "Workspace";

	private static final String DATASETS_MENU_LBL = "Dataset";

	private static final String SAVE_DATASETS_LBL = "Save selected";
	private static final String SAVE_DATASETS_TOOLTIP = "Save selected datasets";
	private static final String SAVE_ALL_DATASETS_LBL = "Save all";
	private static final String SAVE_ALL_DATASETS_TOOLTIP = "Save all datasets";

	private static final String EXIT_LBL = "Exit";

	private static final String VIEW_MENU_LBL = "View";
	private static final String CHECK_FOR_UPDATES_ITEM_LBL = "Check for updates";
	private static final String OPEN_CONFIG_DIR_LBL = "Open config directory";
	private static final String OPEN_LOG_FILE_LBL = "Open log file";
	private static final String ABOUT_ITEM_LBL = "About";
	private static final String HELP_MENU_LBL = "Help";
	private static final String TASK_MONITOR_ITEM_LBL = "Task monitor";
	private static final String FILL_CONSENSUS_ITEM_LBL = "Fill consensus";
	private static final String SWATCH_ITEM_LBL = "Colour palette";
	private static final String SCALE_ITEM_LBL = "Scale";

	private static final String OPEN_CONFIG_FILE_LBL = "Open config file";

	private final transient MainView mw;

	private final JPanel monitorPanel;

	private transient MenuFactory fact = new MenuFactory();

	public MainWindowMenuBar(MainView mw) {
		super();
		this.mw = mw;

		add(createFileMenu());
		add(createViewMenu());
		add(createDatasetMenu());
		add(createHelpMenu());

		add(Box.createGlue());
		monitorPanel = createMonitorPanel();
		add(monitorPanel);
		UIController.getInstance().addDatasetSelectionUpdatedListener(this);
		updateSelectionContext(new ArrayList<>());
	}

	private JPanel createMonitorPanel() {
		JPanel panel = new JPanel(new FlowLayout(FlowLayout.RIGHT));
		panel.add(new JLabel(TASK_QUEUE_LBL));
		panel.add(Box.createHorizontalStrut(5));
		TaskListMonitor t = new TaskListMonitor();
		t.setPreferredSize(new Dimension(100, t.getPreferredSize().height));
		t.setBorder(BorderFactory.createBevelBorder(1));
		panel.add(t);
		panel.add(Box.createHorizontalStrut(10));
		panel.add(new JLabel(MEMORY_LBL));
		panel.add(Box.createHorizontalStrut(5));
		MemoryIndicator m = new MemoryIndicator();
		m.setPreferredSize(new Dimension(100, m.getPreferredSize().height));
		m.setBorder(BorderFactory.createBevelBorder(1));
		panel.add(m);
		panel.setVisible(false);
		panel.setOpaque(false);
		return panel;
	}

	private ContextualMenu createFileMenu() {
		ContextualMenu menu = fact.makeMenu(FILE_MENU_LBL, ContextEnabled.ALWAYS_ACTIVE);
		menu.setMnemonic(KeyEvent.VK_F);

		ContextualMenu newMenu = fact.makeMenu(NEW_ANALYSIS_MENU_LBL, ContextEnabled.ALWAYS_ACTIVE);

		JMenuItem i1 = new JMenuItem(NEW_ANALYSIS_CUSTOM_LBL);
		i1.setToolTipText(NEW_ANALYSIS_CUSTOM_TOOLTIP);
		i1.addActionListener(
				e -> new NewAnalysisAction(
						UserActionController.getInstance().getProgressBarAcceptor()).run());
		i1.setAccelerator(KeyStroke.getKeyStroke(KeyEvent.VK_N, InputEvent.CTRL_DOWN_MASK));
		newMenu.add(i1);

		newMenu.add(fact.makeItem(NEW_ANALYSIS_SAVED_LBL, UserActionEvent.IMPORT_WORKFLOW_PREFIX,
				ContextEnabled.ALWAYS_ACTIVE, NEW_ANALYSIS_SAVED_TOOLTIP));
		menu.add(newMenu);

		newMenu.add(fact.makeItem(NEW_TEXT_ANALYSIS_LBL, UserActionEvent.NEW_TEXT_FILE_ANALYSIS,
				ContextEnabled.ALWAYS_ACTIVE, NEW_TEXT_ANALYSIS_TOOLTIP));
		menu.add(newMenu);

		// End of File>New

		menu.add(fact.makeItem(NEW_WORKSPACE_LBL, UserActionEvent.NEW_WORKSPACE,
				ContextEnabled.ALWAYS_ACTIVE,
				NEW_WORKSPACE_TOOLTIP));

		// Start of File>Open

		ContextualMenu openMenu = fact.makeMenu(OPEN_MENU_LBL, ContextEnabled.ALWAYS_ACTIVE);

		JMenuItem o1 = fact.new ContextualMenuItem(OPEN_DATASET_LBL, "",
				ContextEnabled.ALWAYS_ACTIVE, OPEN_DATASET_TOOLTIP);
		o1.addActionListener(e -> UserActionController.getInstance()
				.fileImportRequested(new FileImportEvent(this, null,
						XMLNames.XML_ANALYSIS_DATASET, null)));
		o1.setAccelerator(KeyStroke.getKeyStroke(KeyEvent.VK_O, InputEvent.CTRL_DOWN_MASK));
		openMenu.add(o1);

		JMenuItem o2 = fact.new ContextualMenuItem(OPEN_WORKSPACE_LBL, "",
				ContextEnabled.ALWAYS_ACTIVE, OPEN_WORKSPACE_TOOLTIP);
		o2.addActionListener(e -> UserActionController.getInstance()
				.fileImportRequested(new FileImportEvent(this, null,
						XMLNames.XML_WORKSPACE, null)));
		openMenu.add(o2);

		menu.add(openMenu);

		// End of File>Open
		ContextualMenuItem saveData = fact.makeItem(SAVE_DATASETS_LBL,
				UserActionEvent.SAVE_SELECTED_DATASETS,
				ContextEnabled.ONLY_DATASETS, SAVE_DATASETS_TOOLTIP);
		saveData.setAccelerator(KeyStroke.getKeyStroke(KeyEvent.VK_S, InputEvent.CTRL_DOWN_MASK));
		menu.add(saveData);

		ContextualMenuItem saveAllData = fact.makeItem(SAVE_ALL_DATASETS_LBL,
				UserActionEvent.SAVE_ALL_DATASETS,
				ContextEnabled.ONLY_DATASETS, SAVE_ALL_DATASETS_TOOLTIP);
		menu.add(saveAllData);

		// Exit event routed through the main window close listener
		JMenuItem exit = new JMenuItem(EXIT_LBL);
		exit.addActionListener(e -> {
			for (WindowListener l : mw.getWindowListeners()) {
				if (l instanceof MainWindowCloseAdapter)
					l.windowClosing(new WindowEvent((Window) mw, WindowEvent.WINDOW_CLOSING));
			}
		});
		menu.add(exit);

		return menu;
	}

	private ContextualMenu createWorkspaceMenu() {
		ContextualMenu menu = fact.makeMenu(WORKSPACE_MENU_LBL,
				ContextEnabled.ACTIVE_ON_WORKSPACE | ContextEnabled.ACTIVE_ON_SINGLE_OBJECT);

		menu.add(fact.makeItem(Labels.Populations.ADD_TO_WORKSPACE_LBL,
				UserActionEvent.ADD_WORKSPACE,
				ContextEnabled.ACTIVE_ON_WORKSPACE | ContextEnabled.ACTIVE_ON_SINGLE_OBJECT));
		return menu;
	}

	private ContextualMenu createViewMenu() {
		ContextualMenu menu = fact.makeMenu(VIEW_MENU_LBL, ContextEnabled.ALWAYS_ACTIVE);
		menu.setMnemonic(KeyEvent.VK_V);

		JMenu scaleMenu = new JMenu(SCALE_ITEM_LBL);

		ButtonGroup g = new ButtonGroup();
		for (MeasurementScale m : MeasurementScale.values()) {
			JMenuItem j = new JRadioButtonMenuItem(m.toString());
			g.add(j);
			if (m.equals(GlobalOptions.getInstance().getScale())) // default config file scale
				j.setSelected(true);
			j.addActionListener(e -> {
				Runnable r = () -> {
					GlobalOptions.getInstance().setScale(m);
					UIController.getInstance().fireScaleUpdated();
				};
				ThreadManager.getInstance().execute(r);
			});
			scaleMenu.add(j);

		}
		menu.add(scaleMenu);

		JMenu swatchMenu = new JMenu(SWATCH_ITEM_LBL);
		ButtonGroup swatchGroup = new ButtonGroup();
		for (ColourSwatch c : ColourSwatch.values()) {
			JMenuItem j = new JRadioButtonMenuItem(c.toString());
			swatchGroup.add(j);
			if (c.equals(GlobalOptions.getInstance().getSwatch())) // default config file scale
				j.setSelected(true);
			j.addActionListener(e -> {
				Runnable r = () -> {
					GlobalOptions.getInstance().setSwatch(c);
					UIController.getInstance().fireSwatchUpdated();
				};
				ThreadManager.getInstance().execute(r);
			});
			swatchMenu.add(j);

		}
		menu.add(swatchMenu);

		JCheckBoxMenuItem fillConsensusItem = new JCheckBoxMenuItem(FILL_CONSENSUS_ITEM_LBL,
				GlobalOptions.getInstance().isFillConsensus());
		fillConsensusItem.addActionListener(e -> {
			GlobalOptions.getInstance().setFillConsensus(fillConsensusItem.isSelected());
			UIController.getInstance().fireConsensusNucleusFillStateChanged();
		});
		menu.add(fillConsensusItem);

		JCheckBoxMenuItem monitorItem = new JCheckBoxMenuItem(TASK_MONITOR_ITEM_LBL, false);
		monitorItem.addActionListener(e -> monitorPanel.setVisible(!monitorPanel.isVisible()));
		menu.add(monitorItem);

		return menu;
	}

	private void loadUserGuide() {

		Runnable r = () -> {

			try {

				// Where the files are located in the jar resources
				final String helpPath = "user_guide_v" + Version.currentVersion().toString();

				// Directory for the help files
				File userGuideDir = new File(Io.getConfigDir(), helpPath);

				// The file we will open
				final File mainHelpFile = new File(userGuideDir, "index.html");

				// Copy the files out of the jar if they don't exist
				if (!Files.exists(userGuideDir.toPath())) {
					LOGGER.fine(
							"User guide directory does not exist: "
									+ userGuideDir.getAbsolutePath());
					Files.createDirectories(userGuideDir.toPath());

					CodeSource src = getClass().getProtectionDomain().getCodeSource();

					// If this is run from the jar file, copy the help file out
					if (src != null) {
						File jarFile = new File(src.getLocation().toURI().getPath());

						LOGGER.fine(() -> "Copying help files from jar at: %s"
								.formatted(jarFile.toString()));
						URL fileSysUrl = new URL(
								"jar:file:" + jarFile.getAbsolutePath() + "!/user-guide");

						// Create a jar URL connection object
						LOGGER.fine(() -> "Copying help files from url at: %s"
								.formatted(fileSysUrl.toString()));
						JarURLConnection jarURLConn = (JarURLConnection) fileSysUrl
								.openConnection();
						FileUtils.copyJarResourcesRecursively(userGuideDir, jarURLConn);
					} else {
						LOGGER.fine("Code source is null; is this a jar file?");
					}
				}

				if (!Files.exists(mainHelpFile.toPath())) {
					LOGGER.fine(mainHelpFile.getAbsolutePath()
							+ " does not exist; re-extracting from jar");

					// There may be malformed files present; delete the user guide folder and
					// extract again
					org.apache.commons.io.FileUtils.deleteQuietly(userGuideDir);
					loadUserGuide();

				} else {
					LOGGER.fine(() -> "Opening %s".formatted(mainHelpFile.toURI().toString()));
					Desktop desktop = Desktop.getDesktop();

					// Especially on Linux, Desktop::browse may not be available. If so, fall back
					// to ImageJ's implementation of BrowserLauncher
					if (desktop.isSupported(Desktop.Action.BROWSE)) {
						desktop.browse(mainHelpFile.toURI());
					} else {
						BrowserLauncher.openURL(mainHelpFile.toURI().toString());
					}

				}

			} catch (Exception e) {
				LOGGER.warning("Unable to open user guide; see log for details");
				LOGGER.log(Loggable.STACK,
						"Error extracting user guide: %s".formatted(e.getMessage()), e);
			}
		};
		ThreadManager.getInstance().execute(r);
	}

	private ContextualMenu createHelpMenu() {
		ContextualMenu menu = fact.makeMenu(HELP_MENU_LBL, ContextEnabled.ALWAYS_ACTIVE);
		menu.setMnemonic(KeyEvent.VK_H);

		JMenuItem userGuideItem = new JMenuItem("Open user guide");
		userGuideItem.addActionListener(e -> loadUserGuide());
		menu.add(userGuideItem);

		JMenuItem checkItem = new JMenuItem(CHECK_FOR_UPDATES_ITEM_LBL);
		checkItem.addActionListener(e -> {
			Runnable r = () -> {
				Version v = UpdateChecker.fetchLatestVersion();
				if (v.isNewerThan(Version.currentVersion())) {
					JOptionPane.showMessageDialog(this, "A new version - " + v + " - is available!",
							"Update found!",
							JOptionPane.INFORMATION_MESSAGE);
				} else {
					JOptionPane.showMessageDialog(this,
							"You have the latest version: " + Version.currentVersion(),
							"No updates", JOptionPane.INFORMATION_MESSAGE);
				}
			};
			ThreadManager.getInstance().submit(r);
		});

		menu.add(checkItem);

		JMenuItem configFileItem = new JMenuItem(OPEN_CONFIG_FILE_LBL);
		configFileItem.addActionListener(e -> {
			try {
				Desktop.getDesktop().open(Io.getConfigFile());
			} catch (IOException e1) {
				LOGGER.log(Level.SEVERE, "Unable to open config file", e1);
			}
		});
		menu.add(configFileItem);

		JMenuItem logFileItem = new JMenuItem(OPEN_LOG_FILE_LBL);
		logFileItem.addActionListener(e -> {
			try {
				Desktop.getDesktop().open(Io.getLogFile());
			} catch (IOException e1) {
				LOGGER.log(Level.SEVERE, "Unable to open log file", e1);
			}
		});
		menu.add(logFileItem);

		JMenuItem logItem = new JMenuItem(OPEN_CONFIG_DIR_LBL);
		logItem.addActionListener(e -> {
			Runnable r = () -> {
				try {
					Desktop.getDesktop().open(Io.getConfigDir());
				} catch (IOException ex) {
					LOGGER.log(Level.SEVERE, "Unable to open folder", ex);
					JOptionPane.showMessageDialog(this,
							"Unable to open log folder: " + Io.getConfigDir().getAbsolutePath(),
							"Error opening log folder", JOptionPane.ERROR_MESSAGE);
				}
			};
			ThreadManager.getInstance().submit(r);
		});
		menu.add(logItem);

		JMenuItem aboutItem = new JMenuItem(ABOUT_ITEM_LBL);
		aboutItem.addActionListener(e -> new VersionHelpDialog(mw));
		menu.add(aboutItem);

		return menu;
	}

	private ContextualMenu createDatasetMenu() {
		ContextualMenu menu = fact.makeMenu(DATASETS_MENU_LBL, ContextEnabled.ONLY_DATASETS);
		menu.setMnemonic(KeyEvent.VK_D);

		ContextualMenu addSubMenu = fact.makeMenu(Labels.Populations.ADD,
				ContextEnabled.ACTIVE_ON_ROOT_DATASET
						| ContextEnabled.ACTIVE_ON_CHILD_DATASET
						| ContextEnabled.ACTIVE_ON_SINGLE_OBJECT);

		addSubMenu.add(fact.makeItem(Labels.Populations.ADD_NUCLEAR_SIGNAL_LBL,
				UserActionEvent.ADD_NUCLEAR_SIGNAL,
				ContextEnabled.ACTIVE_ON_ROOT_DATASET
						| ContextEnabled.ACTIVE_ON_SINGLE_OBJECT,
				Labels.Populations.ADD_NUCLEAR_SIGNAL_TIP));

		addSubMenu.add(fact.makeItem(Labels.Populations.ADD_SHELLS_SIGNAL_LBL,
				UserActionEvent.RUN_SHELL_ANALYSIS,
				ContextEnabled.ACTIVE_ON_ROOT_DATASET
						| ContextEnabled.ACTIVE_ON_SINGLE_OBJECT
						| ContextEnabled.ACTIVE_WITH_SIGNALS,
				Labels.Populations.ADD_SHELLS_SIGNAL_TIP));

		addSubMenu.add(fact.makeItem(Labels.Populations.WARP_BTN_LBL,
				UserActionEvent.SIGNAL_WARPING,
				ContextEnabled.ACTIVE_ON_ROOT_DATASET | ContextEnabled.ACTIVE_ON_CHILD_DATASET
						| ContextEnabled.ACTIVE_ON_SINGLE_OBJECT
						| ContextEnabled.ACTIVE_WITH_CONSENSUS
						| ContextEnabled.ACTIVE_WITH_SIGNALS,
				Labels.Populations.WARP_BTN_TOOLTIP));

		addSubMenu.add(fact.makeItem(Labels.Populations.POST_FISH_MAPPING_LBL,
				UserActionEvent.POST_FISH_MAPPING,
				ContextEnabled.ACTIVE_ON_ROOT_DATASET
						| ContextEnabled.ACTIVE_ON_SINGLE_OBJECT,
				Labels.Populations.POST_FISH_MAPPING_TOOLTIP));

		addSubMenu.add(fact.makeItem(Labels.Populations.ADD_CHILD_CELLS_LBL,
				UserActionEvent.RELOCATE_CELLS,
				ContextEnabled.ACTIVE_ON_ROOT_DATASET
						| ContextEnabled.ACTIVE_ON_CHILD_DATASET
						| ContextEnabled.ACTIVE_ON_SINGLE_OBJECT,
				Labels.Populations.ADD_CHILD_CELLS_TOOLTIP));

		menu.add(addSubMenu);

		ContextualMenu clusterSubMenu = fact.makeMenu(Labels.Populations.CLUSTER,
				ContextEnabled.ACTIVE_ON_ROOT_DATASET
						| ContextEnabled.ACTIVE_ON_CHILD_DATASET
						| ContextEnabled.ACTIVE_ON_SINGLE_OBJECT);

		clusterSubMenu.add(fact.makeItem(Labels.Populations.ADD_CLUSTER_MANUAL_LBL,
				UserActionEvent.CLUSTER_MANUALLY,
				ContextEnabled.ACTIVE_ON_ROOT_DATASET
						| ContextEnabled.ACTIVE_ON_CHILD_DATASET
						| ContextEnabled.ACTIVE_ON_SINGLE_OBJECT,
				Labels.Populations.ADD_CLUSTER_MANUAL_TOOLTIP));

		clusterSubMenu.add(fact.makeItem(Labels.Populations.ADD_CLUSTER_AUTO_LBL,
				UserActionEvent.CLUSTER_AUTOMATICALLY,
				ContextEnabled.ACTIVE_ON_ROOT_DATASET
						| ContextEnabled.ACTIVE_ON_CHILD_DATASET
						| ContextEnabled.ACTIVE_ON_SINGLE_OBJECT,
				Labels.Populations.ADD_CLUSTER_AUTO_TOOLTIP));

		clusterSubMenu.add(fact.makeItem(Labels.Populations.ADD_CLUSTER_FILE_LBL,
				UserActionEvent.CLUSTER_FROM_FILE,
				ContextEnabled.ACTIVE_ON_ROOT_DATASET
						| ContextEnabled.ACTIVE_ON_CHILD_DATASET
						| ContextEnabled.ACTIVE_ON_SINGLE_OBJECT,
				Labels.Populations.ADD_CLUSTER_FILE_TOOLTIP));

		menu.add(clusterSubMenu);

		menu.addSeparator();

		menu.add(fact.makeItem(Labels.Populations.CURATE_LBL, UserActionEvent.CURATE_DATASET,
				ContextEnabled.ACTIVE_ON_ROOT_DATASET
						| ContextEnabled.ACTIVE_ON_CHILD_DATASET
						| ContextEnabled.ACTIVE_ON_SINGLE_OBJECT));

		menu.add(fact.makeItem(Labels.Populations.CHANGE_SCALE_LBL, UserActionEvent.CHANGE_SCALE,
				ContextEnabled.ACTIVE_ON_ROOT_DATASET
						| ContextEnabled.ACTIVE_ON_SINGLE_OBJECT
						| ContextEnabled.ACTIVE_ON_MULTI_OBJECTS));

		menu.add(fact.makeItem(Labels.Populations.MERGE_LBL, UserActionEvent.MERGE_DATASETS_ACTION,
				ContextEnabled.ACTIVE_ON_ROOT_DATASET
						| ContextEnabled.ACTIVE_ON_CHILD_DATASET
						| ContextEnabled.ACTIVE_ON_MULTI_OBJECTS));

		menu.add(
				fact.makeItem(Labels.Populations.ARITHMETIC_LBL, UserActionEvent.DATASET_ARITHMETIC,
						ContextEnabled.ACTIVE_ON_ROOT_DATASET
								| ContextEnabled.ACTIVE_ON_CHILD_DATASET
								| ContextEnabled.ACTIVE_ON_MULTI_OBJECTS));

		menu.addSeparator();

		menu.add(fact.makeItem(Labels.Populations.ADD_TO_WORKSPACE_LBL,
				UserActionEvent.ADD_TO_WORKSPACE,
				ContextEnabled.ACTIVE_ON_ROOT_DATASET
						| ContextEnabled.ACTIVE_ON_SINGLE_OBJECT
						| ContextEnabled.ACTIVE_ON_MULTI_OBJECTS));

		menu.add(fact.makeItem(Labels.Populations.REMOVE_FROM_WORKSPACE_LBL,
				UserActionEvent.REMOVE_FROM_WORKSPACE,
				ContextEnabled.ACTIVE_ON_ROOT_DATASET
						| ContextEnabled.ACTIVE_ON_SINGLE_OBJECT
						| ContextEnabled.ACTIVE_ON_MULTI_OBJECTS));

		menu.add(fact.makeItem(
				Labels.Populations.DELETE_LBL, UserActionEvent.DELETE_DATASET,
				ContextEnabled.ACTIVE_ON_CHILD_DATASET
						| ContextEnabled.ACTIVE_ON_SINGLE_OBJECT
						| ContextEnabled.ACTIVE_ON_MULTI_OBJECTS,
				Labels.Populations.DELETE_TOOLTIP));

		menu.add(fact.makeItem(
				Labels.Populations.CLOSE_LBL, UserActionEvent.DELETE_DATASET,
				ContextEnabled.ACTIVE_ON_ROOT_DATASET
						| ContextEnabled.ACTIVE_ON_SINGLE_OBJECT
						| ContextEnabled.ACTIVE_ON_MULTI_OBJECTS,
				Labels.Populations.CLOSE_TOOLTIP));

		menu.addSeparator();

		menu.add(createExportMenu());

		return menu;
	}

	private ContextualMenu createExportMenu() {
		ContextualMenu exportMenu = fact.makeMenu(Labels.Populations.EXPORT,
				ContextEnabled.ACTIVE_ON_ROOT_DATASET | ContextEnabled.ACTIVE_ON_CHILD_DATASET
						| ContextEnabled.ACTIVE_ON_MULTI_OBJECTS
						| ContextEnabled.ACTIVE_ON_SINGLE_OBJECT);

		exportMenu.add(fact.makeItem(Labels.Populations.EXPORT_STATS, UserActionEvent.EXPORT_STATS,
				ContextEnabled.ACTIVE_ON_ROOT_DATASET | ContextEnabled.ACTIVE_ON_CHILD_DATASET
						| ContextEnabled.ACTIVE_ON_MULTI_OBJECTS
						| ContextEnabled.ACTIVE_ON_SINGLE_OBJECT));
		exportMenu.add(fact.makeItem(Labels.Populations.EXPORT_PROFILES,
				UserActionEvent.EXPORT_PROFILES,
				ContextEnabled.ACTIVE_ON_ROOT_DATASET | ContextEnabled.ACTIVE_ON_CHILD_DATASET
						| ContextEnabled.ACTIVE_ON_MULTI_OBJECTS
						| ContextEnabled.ACTIVE_ON_SINGLE_OBJECT));
		exportMenu.add(fact.makeItem(Labels.Populations.EXPORT_OUTLINES,
				UserActionEvent.EXPORT_OUTLINES,
				ContextEnabled.ACTIVE_ON_ROOT_DATASET | ContextEnabled.ACTIVE_ON_CHILD_DATASET
						| ContextEnabled.ACTIVE_ON_MULTI_OBJECTS
						| ContextEnabled.ACTIVE_ON_SINGLE_OBJECT));

		exportMenu.addSeparator();

		exportMenu.add(fact.makeItem(Labels.Populations.EXPORT_SIGNALS,
				UserActionEvent.EXPORT_SIGNALS,
				ContextEnabled.ACTIVE_ON_ROOT_DATASET | ContextEnabled.ACTIVE_ON_CHILD_DATASET
						| ContextEnabled.ACTIVE_ON_MULTI_OBJECTS
						| ContextEnabled.ACTIVE_ON_SINGLE_OBJECT));
		exportMenu.add(fact.makeItem(Labels.Populations.EXPORT_SHELLS,
				UserActionEvent.EXPORT_SHELLS,
				ContextEnabled.ACTIVE_ON_ROOT_DATASET | ContextEnabled.ACTIVE_ON_CHILD_DATASET
						| ContextEnabled.ACTIVE_ON_MULTI_OBJECTS
						| ContextEnabled.ACTIVE_ON_SINGLE_OBJECT));

		exportMenu.addSeparator();

		JMenuItem consensus = fact.new ContextualMenuItem(Labels.Populations.EXPORT_CONSENSUS, "",
				ContextEnabled.ACTIVE_ON_ROOT_DATASET | ContextEnabled.ACTIVE_ON_CHILD_DATASET
						| ContextEnabled.ACTIVE_ON_MULTI_OBJECTS
						| ContextEnabled.ACTIVE_ON_SINGLE_OBJECT,
				null);
		consensus.addActionListener(e -> UserActionController.getInstance()
				.consensusSVGExportRequestReceived(
						DatasetListManager.getInstance().getSelectedDatasets()));
		exportMenu.add(consensus);

		exportMenu.add(fact.makeItem(Labels.Populations.EXPORT_CELL_IMAGES,
				UserActionEvent.EXPORT_SINGLE_CELL_IMAGES,
				ContextEnabled.ACTIVE_ON_ROOT_DATASET | ContextEnabled.ACTIVE_ON_CHILD_DATASET
						| ContextEnabled.ACTIVE_ON_MULTI_OBJECTS
						| ContextEnabled.ACTIVE_ON_SINGLE_OBJECT));

		exportMenu.add(fact.makeItem(Labels.Populations.EXPORT_CELL_LOCS,
				UserActionEvent.EXPORT_CELL_LOCS,
				ContextEnabled.ACTIVE_ON_ROOT_DATASET | ContextEnabled.ACTIVE_ON_CHILD_DATASET
						| ContextEnabled.ACTIVE_ON_MULTI_OBJECTS
						| ContextEnabled.ACTIVE_ON_SINGLE_OBJECT));

		exportMenu.addSeparator();

		exportMenu.add(fact.makeItem(Labels.Populations.EXPORT_OPTIONS,
				UserActionEvent.EXPORT_OPTIONS,
				ContextEnabled.ACTIVE_ON_ROOT_DATASET | ContextEnabled.ACTIVE_ON_CHILD_DATASET
						| ContextEnabled.ACTIVE_ON_MULTI_OBJECTS
						| ContextEnabled.ACTIVE_ON_SINGLE_OBJECT));

		exportMenu.add(fact.makeItem(Labels.Populations.EXPORT_RULESETS,
				UserActionEvent.EXPORT_RULESETS,
				ContextEnabled.ACTIVE_ON_ROOT_DATASET | ContextEnabled.ACTIVE_ON_CHILD_DATASET
						| ContextEnabled.ACTIVE_ON_MULTI_OBJECTS
						| ContextEnabled.ACTIVE_ON_SINGLE_OBJECT));

		return exportMenu;
	}

	@Override
	public void datasetSelectionUpdated(List<IAnalysisDataset> datasets) {
		updateSelectionContext(datasets);
	}

	@Override
	public void datasetSelectionUpdated(IAnalysisDataset dataset) {
		updateSelectionContext(List.of(dataset));
	}

	private void updateSelectionContext(Collection<?> obj) {
		for (Component c : this.getComponents()) {
			if (c instanceof ContextEnabled con) {
				con.updateSelectionContext(obj);
			}
		}
	}

}
