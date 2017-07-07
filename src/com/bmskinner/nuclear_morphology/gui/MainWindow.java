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


package com.bmskinner.nuclear_morphology.gui;

import java.awt.BorderLayout;
import java.awt.Dimension;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.event.WindowEvent;
import java.awt.event.WindowStateListener;
import java.util.ArrayList;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;

import javax.swing.JFrame;
import javax.swing.JPanel;
import javax.swing.JSplitPane;
import javax.swing.JTabbedPane;
import javax.swing.border.EmptyBorder;

import com.bmskinner.nuclear_morphology.analysis.IWorkspace;
import com.bmskinner.nuclear_morphology.components.generic.Version;
import com.bmskinner.nuclear_morphology.gui.main.EventHandler;
import com.bmskinner.nuclear_morphology.gui.main.MainDragAndDropTarget;
import com.bmskinner.nuclear_morphology.gui.main.MainHeaderPanel;
import com.bmskinner.nuclear_morphology.gui.main.MainWindowCloseAdapter;
import com.bmskinner.nuclear_morphology.gui.tabs.AnalysisDetailPanel;
import com.bmskinner.nuclear_morphology.gui.tabs.ClusterDetailPanel;
import com.bmskinner.nuclear_morphology.gui.tabs.EditingDetailPanel;
import com.bmskinner.nuclear_morphology.gui.tabs.ImagesTabPanel;
import com.bmskinner.nuclear_morphology.gui.tabs.InterDatasetComparisonDetailPanel;
import com.bmskinner.nuclear_morphology.gui.tabs.MergesDetailPanel;
import com.bmskinner.nuclear_morphology.gui.tabs.NuclearStatisticsPanel;
import com.bmskinner.nuclear_morphology.gui.tabs.NucleusProfilesPanel;
import com.bmskinner.nuclear_morphology.gui.tabs.SegmentsDetailPanel;
import com.bmskinner.nuclear_morphology.gui.tabs.SignalsDetailPanel;
import com.bmskinner.nuclear_morphology.gui.tabs.TabPanel;
import com.bmskinner.nuclear_morphology.gui.tabs.cells.CellsDetailPanel;
import com.bmskinner.nuclear_morphology.gui.tabs.populations.PopulationsPanel;
import com.bmskinner.nuclear_morphology.logging.LogPanelFormatter;
import com.bmskinner.nuclear_morphology.logging.Loggable;
import com.bmskinner.nuclear_morphology.logging.TextAreaHandler;

/**
 * This is the core of the program UI. All display panels are contained here.
 * Update requests are sent from here to display information, and requests to
 * perform analyses are relayed from sub-panels to here.
 * 
 * @author bms41
 *
 */
@SuppressWarnings("serial")
public class MainWindow extends JFrame implements Loggable {

    private JPanel contentPane;

    private LogPanel              logPanel;              // progress and
                                                         // messages
    private PopulationsPanel      populationsPanel;      // holds and selects
                                                         // open datasets
    private ConsensusNucleusPanel consensusNucleusPanel; // show refolded nuclei
                                                         // if present

    private static final String PROGRAM_TITLE_BAR_LBL = "Nuclear Morphology Analysis v"
            + Version.currentVersion().toString();

    private static final String ANALYSIS_SETUP_TAB_LBL = "Analysis info";
    private static final String CLUSTERS_TAB_LBL       = "Clusters";
    private static final String MERGES_TAB_LBL         = "Merges";
    private static final String CELLS_TAB_LBL          = "Cell charts";
    private static final String NUCLEI_TAB_LBL         = "Nuclear charts";
    private static final String PROFILES_TAB_LBL       = "Nuclear profiles";
    private static final String SIGNALS_TAB_LBL        = "Nuclear signals";
    private static final String SEGMENTS_TAB_LBL       = "Nuclear segments";
    private static final String COMPARISONS_TAB_LBL    = "Comparisons";
    private static final String EDITING_TAB_LBL        = "Editing";
    private static final String IMAGES_TAB_LBL         = "Images";

    private JTabbedPane tabbedPane; // bottom panel tabs. Contains:

    private NucleusProfilesPanel              nucleusProfilesPanel; // the angle
                                                                    // profiles
    private AnalysisDetailPanel               analysisDetailPanel;  // nucleus
                                                                    // detection
                                                                    // parameters
                                                                    // and stats
    private SignalsDetailPanel                signalsDetailPanel;   // nuclear
                                                                    // signals
    private CellsDetailPanel                  cellsDetailPanel;     // cells
                                                                    // stats -
                                                                    // areas,
                                                                    // perimeters
                                                                    // etc
    private NuclearStatisticsPanel            nuclearChartsPanel;   // nuclear
                                                                    // stats -
                                                                    // areas,
                                                                    // perimeters
                                                                    // etc
    private SegmentsDetailPanel               segmentsDetailPanel;  // segmented
                                                                    // profiles
    private ClusterDetailPanel                clusterDetailPanel;   // clustering
                                                                    // within
                                                                    // populations
    private MergesDetailPanel                 mergesDetailPanel;    // merges
                                                                    // between
                                                                    // populations
    private InterDatasetComparisonDetailPanel comparisonsPanel;
    private EditingDetailPanel                editingDetailPanel;   // for
                                                                    // altering
                                                                    // data
    private ImagesTabPanel                    imagesTabPanel;       // for
                                                                    // altering
                                                                    // data

    private final List<TabPanel> detailPanels = new ArrayList<TabPanel>(); // store
                                                                           // panels
                                                                           // for
                                                                           // iterating
                                                                           // messsages

    private final List<IWorkspace> workspaces = new ArrayList<IWorkspace>();

    private final EventHandler eh = new EventHandler(this);

    private boolean isStandalone = false;

    /**
     * Create the frame.
     * 
     * @param standalone
     *            is the frame a standalone app, or launched within ImageJ?
     */
    public MainWindow(boolean standalone) {

        isStandalone = standalone;

        createWindowListeners();

        createUI();

        createEventHandling();

    }

    /**
     * Create the listeners that handle dataset saving when the main window is
     * closed
     * 
     */
    private void createWindowListeners() {

        this.setDefaultCloseOperation(DO_NOTHING_ON_CLOSE);

        this.addWindowListener(new MainWindowCloseAdapter(this));

        // Add a listener for panel size changes. This will cause
        // charts to redraw at the new aspect ratio rather than stretch.
        this.addWindowStateListener(new WindowStateListener() {
            public void windowStateChanged(WindowEvent e) {

                Runnable r = () -> {
                    try {
                        // If the update is called immediately, then the chart
                        // size has
                        // not yet changed, and therefore will render at the
                        // wrong aspect
                        // ratio

                        Thread.sleep(100);
                    } catch (InterruptedException e1) {
                        return;
                    }

                    for (TabPanel d : detailPanels) {
                        d.updateSize();
                    }
                };
                ThreadManager.getInstance().submit(r);

            }
        });

        this.setDropTarget(new MainDragAndDropTarget(this));
    }

    /**
     * Create the main UI
     */
    private void createUI() {
        try {
            setTitle(PROGRAM_TITLE_BAR_LBL);

            Dimension preferredSize = new Dimension(1012, 804);
            this.setPreferredSize(preferredSize);
            setBounds(100, 100, 1012, 804);
            contentPane = new JPanel();
            contentPane.setBorder(new EmptyBorder(5, 5, 5, 5));
            contentPane.setLayout(new BorderLayout(0, 0));
            setContentPane(contentPane);

            // ---------------
            // Create the header buttons
            // ---------------
            contentPane.add(new MainHeaderPanel(this), BorderLayout.NORTH);

            // ---------------
            // Create the log panel
            // ---------------
            logPanel = new LogPanel();

            TextAreaHandler textHandler = new TextAreaHandler(logPanel);
            textHandler.setFormatter(new LogPanelFormatter());
            Logger.getLogger(Loggable.PROGRAM_LOGGER).addHandler(textHandler);
            Logger.getLogger(Loggable.PROGRAM_LOGGER).setLevel(Level.INFO); // by
                                                                            // default
                                                                            // do
                                                                            // not
                                                                            // log
                                                                            // everything

            // ---------------
            // Create the consensus chart
            // ---------------
            populationsPanel = new PopulationsPanel();
            consensusNucleusPanel = new ConsensusNucleusPanel();
            detailPanels.add(consensusNucleusPanel);

            // ---------------
            // Create the split view
            // ---------------
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

            createTabs();

            // ---------------
            // Register change listeners
            // ---------------

            signalsDetailPanel.addSignalChangeListener(editingDetailPanel);
            editingDetailPanel.addSignalChangeListener(signalsDetailPanel); // allow
                                                                            // the
                                                                            // panels
                                                                            // to
                                                                            // communicate
                                                                            // colour
                                                                            // updates

            // ---------------
            // Add the top and bottom rows to the main panel
            // ---------------
            JSplitPane panelMain = new JSplitPane(JSplitPane.VERTICAL_SPLIT, topRow, tabbedPane);

            contentPane.add(panelMain, BorderLayout.CENTER);
           
            this.pack();
            consensusNucleusPanel.restoreAutoBounds();

        } catch (Exception e) {
            logToImageJ("Error initialising Main: " + e.getMessage(), e);
        }
    }

    /**
     * Create the individual analysis tabs
     */
    private void createTabs() {
        tabbedPane = new JTabbedPane(JTabbedPane.TOP);

        analysisDetailPanel = new AnalysisDetailPanel();
        nucleusProfilesPanel = new NucleusProfilesPanel();
        cellsDetailPanel = new CellsDetailPanel();
        nuclearChartsPanel = new NuclearStatisticsPanel();
        signalsDetailPanel = new SignalsDetailPanel();
        clusterDetailPanel = new ClusterDetailPanel();
        mergesDetailPanel = new MergesDetailPanel();
        segmentsDetailPanel = new SegmentsDetailPanel();
        comparisonsPanel = new InterDatasetComparisonDetailPanel();
        editingDetailPanel = new EditingDetailPanel();
        imagesTabPanel = new ImagesTabPanel();

        detailPanels.add(analysisDetailPanel);
        detailPanels.add(clusterDetailPanel);
        detailPanels.add(mergesDetailPanel);
        detailPanels.add(cellsDetailPanel);
        detailPanels.add(nuclearChartsPanel);
        detailPanels.add(nucleusProfilesPanel);
        detailPanels.add(signalsDetailPanel);
        detailPanels.add(segmentsDetailPanel);
        detailPanels.add(comparisonsPanel);
        detailPanels.add(editingDetailPanel);
        detailPanels.add(imagesTabPanel);

        tabbedPane.addTab(ANALYSIS_SETUP_TAB_LBL, analysisDetailPanel);
        tabbedPane.addTab(IMAGES_TAB_LBL, imagesTabPanel);

        tabbedPane.addTab(CELLS_TAB_LBL, cellsDetailPanel);

        tabbedPane.addTab(NUCLEI_TAB_LBL, nuclearChartsPanel);
        tabbedPane.addTab(PROFILES_TAB_LBL, null, nucleusProfilesPanel, null);
        tabbedPane.addTab(SIGNALS_TAB_LBL, signalsDetailPanel);
        tabbedPane.addTab(SEGMENTS_TAB_LBL, null, segmentsDetailPanel, null);

        tabbedPane.addTab(COMPARISONS_TAB_LBL, null, comparisonsPanel, null);

        tabbedPane.addTab(CLUSTERS_TAB_LBL, clusterDetailPanel);
        tabbedPane.addTab(MERGES_TAB_LBL, mergesDetailPanel);

        tabbedPane.addTab(EDITING_TAB_LBL, null, editingDetailPanel, null);

    }

    /**
     * Set the event listeners for each tab panel
     */
    private void createEventHandling() {
        logPanel.addDatasetEventListener(eh);
        logPanel.addInterfaceEventListener(eh);
        logPanel.addSignalChangeListener(eh);
        eh.addDatasetUpdateEventListener(logPanel);

        populationsPanel.addSignalChangeListener(eh);
        populationsPanel.addDatasetEventListener(eh);
        populationsPanel.addInterfaceEventListener(eh);

        for (TabPanel d : detailPanels) {
            d.addDatasetEventListener(eh);
            d.addInterfaceEventListener(eh);
            d.addSignalChangeListener(eh);
            eh.addDatasetUpdateEventListener(d);
        }
    }

    /**
     * Check if the program has been started as a plugin to ImageJ or as
     * standalone
     * 
     * @return
     */
    public boolean isStandalone() {
        return isStandalone;
    }

    public PopulationsPanel getPopulationsPanel() {
        return this.populationsPanel;
    }

    public LogPanel getLogPanel() {
        return this.logPanel;
    }

    public List<TabPanel> getTabPanels() {
        return this.detailPanels;
    }

    /**
     * Get the event handler that dispatches messages and analyses
     * 
     * @return
     */
    public EventHandler getEventHandler() {
        return eh;
    }

    public void addWorkspace(IWorkspace w) {
        this.workspaces.add(w);
    }

    @Override
    public void dispose() {
        super.dispose();
    }

}
