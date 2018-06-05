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
import java.awt.Cursor;
import java.awt.Dimension;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.event.WindowEvent;
import java.awt.event.WindowStateListener;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Future;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.logging.Level;
import java.util.logging.Logger;

import javax.swing.JFrame;
import javax.swing.JPanel;
import javax.swing.JSplitPane;
import javax.swing.JTabbedPane;
import javax.swing.border.EmptyBorder;

import com.bmskinner.nuclear_morphology.components.IAnalysisDataset;
import com.bmskinner.nuclear_morphology.components.generic.Version;
import com.bmskinner.nuclear_morphology.gui.InterfaceEvent.InterfaceMethod;
import com.bmskinner.nuclear_morphology.gui.main.MainView;
import com.bmskinner.nuclear_morphology.gui.main.AbstractMainWindow;
import com.bmskinner.nuclear_morphology.gui.main.MainDragAndDropTarget;
import com.bmskinner.nuclear_morphology.gui.main.MainWindowCloseAdapter;
import com.bmskinner.nuclear_morphology.gui.main.MainWindowMenuBar;
import com.bmskinner.nuclear_morphology.gui.tabs.AnalysisDetailPanel;
import com.bmskinner.nuclear_morphology.gui.tabs.ClusterDetailPanel;
import com.bmskinner.nuclear_morphology.gui.tabs.DatasetSelectionListener;
import com.bmskinner.nuclear_morphology.gui.tabs.DetailPanel;
import com.bmskinner.nuclear_morphology.gui.tabs.EditingDetailPanel;
import com.bmskinner.nuclear_morphology.gui.tabs.ImagesTabPanel;
import com.bmskinner.nuclear_morphology.gui.tabs.MergesDetailPanel;
import com.bmskinner.nuclear_morphology.gui.tabs.TabPanel;
import com.bmskinner.nuclear_morphology.gui.tabs.cells.CellsDetailPanel;
import com.bmskinner.nuclear_morphology.gui.tabs.comparisons.ComparisonDetailPanel;
import com.bmskinner.nuclear_morphology.gui.tabs.nuclear.NuclearStatisticsPanel;
import com.bmskinner.nuclear_morphology.gui.tabs.populations.PopulationsPanel;
import com.bmskinner.nuclear_morphology.gui.tabs.profiles.NucleusProfilesPanel;
import com.bmskinner.nuclear_morphology.gui.tabs.segments.SegmentsDetailPanel;
import com.bmskinner.nuclear_morphology.gui.tabs.signals.SignalsDetailPanel;
import com.bmskinner.nuclear_morphology.logging.LogPanelFormatter;
import com.bmskinner.nuclear_morphology.logging.Loggable;
import com.bmskinner.nuclear_morphology.logging.TextAreaHandler;
import com.bmskinner.nuclear_morphology.main.DatasetListManager;
import com.bmskinner.nuclear_morphology.main.EventHandler;
import com.bmskinner.nuclear_morphology.main.ThreadManager;
import com.javadocking.DockingManager;
import com.javadocking.dock.Position;
import com.javadocking.dock.SplitDock;
import com.javadocking.dock.TabDock;
import com.javadocking.dockable.Dockable;
import com.javadocking.dockable.DockingMode;
import com.javadocking.dockable.DefaultDockable;
import com.javadocking.model.FloatDockModel;

/**
 * This is the core of the program UI. All display panels are contained here.
 * Update requests are sent from here to display information, and requests to
 * perform analyses are relayed from sub-panels to here.
 * 
 * @author bms41
 *
 */
@SuppressWarnings("serial")
public class MainWindow extends AbstractMainWindow {

    private JPanel contentPane;

    private LogPanel logPanel;
    private PopulationsPanel populationsPanel;
    private ConsensusNucleusPanel consensusNucleusPanel;

    private JTabbedPane tabbedPane; // bottom panel tabs

    // store panels for iterating messages
//    private final List<TabPanel> detailPanels = new ArrayList<>();


    /**
     * Create the frame.
     * 
     * @param standalone is the frame a standalone app, or launched within ImageJ?
     */
    public MainWindow(boolean standalone, EventHandler eh) {
    	super(standalone, eh);

        createWindowListeners();

        createUI();

        this.eh.addProgressBarAcceptor(logPanel);
        createEventHandling();
        eh.addInterfaceEventListener(this);
        eh.addDatasetSelectionListener(this);
        eh.addDatasetEventListener(this);
        eh.addDatasetUpdateEventListener(this);
        
        this.setJMenuBar(new MainWindowMenuBar(this));

    }

    /**
     * Create the main UI
     */
    @Override
	protected void createUI() {
        try {

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
//            contentPane.add(new MainHeaderPanel(this), BorderLayout.NORTH);

            // ---------------
            // Create the log panel
            // ---------------
            
            
            
            logPanel = new LogPanel(eh.getInputSupplier());
            TextAreaHandler textHandler = new TextAreaHandler(logPanel);
            textHandler.setFormatter(new LogPanelFormatter());
            Logger.getLogger(Loggable.PROGRAM_LOGGER).addHandler(textHandler);
            Logger.getLogger(Loggable.PROGRAM_LOGGER).setLevel(Level.INFO);
                		
            // ---------------
            // Create the consensus chart
            // ---------------
            populationsPanel = new PopulationsPanel(eh.getInputSupplier());
                        
            consensusNucleusPanel = new ConsensusNucleusPanel(eh.getInputSupplier());
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

        // Create the top level tabs in the UI
        DetailPanel analysisDetailPanel  = new AnalysisDetailPanel(eh.getInputSupplier());
        DetailPanel nucleusProfilesPanel = new NucleusProfilesPanel(eh.getInputSupplier()); // the angle profiles
        DetailPanel cellsDetailPanel     = new CellsDetailPanel(eh.getInputSupplier());
        DetailPanel nuclearChartsPanel   = new NuclearStatisticsPanel(eh.getInputSupplier());
        DetailPanel signalsDetailPanel   = new SignalsDetailPanel(eh.getInputSupplier());
        DetailPanel clusterDetailPanel   = new ClusterDetailPanel(eh.getInputSupplier());
        DetailPanel mergesDetailPanel    = new MergesDetailPanel(eh.getInputSupplier());
        DetailPanel segmentsDetailPanel  = new SegmentsDetailPanel(eh.getInputSupplier());
        DetailPanel comparisonsPanel     = new ComparisonDetailPanel(eh.getInputSupplier());
        DetailPanel editingDetailPanel   = new EditingDetailPanel(eh.getInputSupplier());
        DetailPanel imagesTabPanel       = new ImagesTabPanel(eh.getInputSupplier());

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
        
        for(TabPanel t : detailPanels){
            if(t instanceof ConsensusNucleusPanel){
                continue;
            }
            DetailPanel p = (DetailPanel)t;
            tabbedPane.addTab(p.getPanelTitle(), p);
        }
        
        signalsDetailPanel.addSignalChangeListener(editingDetailPanel);
        editingDetailPanel.addSignalChangeListener(signalsDetailPanel);

    }

    /**
     * Set the event listeners for each tab panel
     */
    @Override
	protected void createEventHandling() {
        logPanel.addDatasetEventListener(eh);
        logPanel.addInterfaceEventListener(eh);
        logPanel.addSignalChangeListener(eh);
        this.addDatasetUpdateEventListener(logPanel);

        populationsPanel.addSignalChangeListener(eh);
        populationsPanel.addDatasetEventListener(eh);
        populationsPanel.addInterfaceEventListener(eh);

        for (TabPanel d : detailPanels) {
            d.addDatasetEventListener(eh);
            d.addInterfaceEventListener(eh);
            d.addSignalChangeListener(eh);
            this.addDatasetUpdateEventListener(d);
        }
    }



    public PopulationsPanel getPopulationsPanel() {
        return this.populationsPanel;
    }

    @Override
	public ProgressBarAcceptor getProgressAcceptor() {
        return this.logPanel;
    }

    /**
     * Get the event handler that dispatches messages and analyses
     * 
     * @return
     */
    @Override
	public EventHandler getEventHandler() {
        return eh;
    }

    @Override
    public void dispose() {
        super.dispose();
    }
    
    
	@Override
	public void interfaceEventReceived(InterfaceEvent event) {
		if(event.getSource().equals(eh)){
			InterfaceMethod method = event.method();
	        
	        final List<IAnalysisDataset> selected = DatasetListManager.getInstance().getSelectedDatasets();

	        switch (method) {

	        case REFRESH_POPULATIONS:
	        	getPopulationsPanel().update(selected); // ensure all child
	                                                       // datasets are included
	            break;

	        case CLEAR_LOG_WINDOW:
	        	logPanel.clear();
	            break;

	        case UPDATE_IN_PROGRESS:
	            for (TabPanel panel : getTabPanels()) {
	                panel.setAnalysing(true);
	            }
	            setCursor(new Cursor(Cursor.WAIT_CURSOR));
	            break;

	        case UPDATE_COMPLETE:
	            for (TabPanel panel : getTabPanels()) {
	                panel.setAnalysing(false);
	            }
	            setCursor(new Cursor(Cursor.DEFAULT_CURSOR));
	            break;
	            
	        case RECACHE_CHARTS:
	            recacheCharts();
	            break;
	        }
	        
	        
		}
		
	}

	@Override
	public void datasetSelectionEventReceived(DatasetSelectionEvent e) {
		getPopulationsPanel().selectDatasets(e.getDatasets());
	}

	@Override
	public void datasetEventReceived(DatasetEvent event) {

		if (event.method().equals(DatasetEvent.REFRESH_CACHE))
            recacheCharts(event.getDatasets());
        

        if (event.method().equals(DatasetEvent.CLEAR_CACHE))
            clearChartCache(event.getDatasets());
        
        if (event.method().equals(DatasetEvent.ADD_DATASET))
            addDataset(event.firstDataset());
        
	}
	    
    /**
     * Add the given dataset and all its children to the populations panel
     * 
     * @param dataset
     */
    private synchronized void addDataset(final IAnalysisDataset dataset) {

    	getPopulationsPanel().addDataset(dataset);
        for (IAnalysisDataset child : dataset.getAllChildDatasets()) {
        	getPopulationsPanel().addDataset(child);
        }

        // This will also trigger a dataset update event as the dataset
        // is selected, so don't trigger another update here.
        getPopulationsPanel().update(dataset);
    }  
}
