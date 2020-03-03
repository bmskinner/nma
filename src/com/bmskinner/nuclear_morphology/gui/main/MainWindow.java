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
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;

import javax.swing.JPanel;
import javax.swing.JSplitPane;
import javax.swing.JTabbedPane;
import javax.swing.border.EmptyBorder;

import com.bmskinner.nuclear_morphology.components.IAnalysisDataset;
import com.bmskinner.nuclear_morphology.core.DatasetListManager;
import com.bmskinner.nuclear_morphology.core.EventHandler;
import com.bmskinner.nuclear_morphology.gui.ConsensusNucleusPanel;
import com.bmskinner.nuclear_morphology.gui.LogPanel;
import com.bmskinner.nuclear_morphology.gui.ProgressBarAcceptor;
import com.bmskinner.nuclear_morphology.gui.events.ChartOptionsRenderedEvent;
import com.bmskinner.nuclear_morphology.gui.events.DatasetEvent;
import com.bmskinner.nuclear_morphology.gui.events.InterfaceEvent;
import com.bmskinner.nuclear_morphology.gui.events.InterfaceEvent.InterfaceMethod;
import com.bmskinner.nuclear_morphology.gui.events.SignalChangeEvent;
import com.bmskinner.nuclear_morphology.gui.tabs.AnalysisDetailPanel;
import com.bmskinner.nuclear_morphology.gui.tabs.ClusterDetailPanel;
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
import com.bmskinner.nuclear_morphology.logging.LogPanelHandler;

/**
 * This is the core of the program UI. All display panels are contained here.
 * Update requests are sent from here to display information, and requests to
 * perform analyses are relayed from sub-panels to here.
 * 
 * @author bms41
 *
 */
@SuppressWarnings("serial")
@Deprecated
public class MainWindow extends AbstractMainWindow {

	private static final Logger LOGGER = Logger.getLogger(MainWindow.class.getName());
	
    private JPanel contentPane;

    private LogPanel logPanel;
    private PopulationsPanel populationsPanel;
    private ConsensusNucleusPanel consensusNucleusPanel;

    private JTabbedPane tabbedPane; // bottom panel tabs


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
            contentPane.add(new MainHeaderPanel(this), BorderLayout.NORTH);

            // ---------------
            // Create the log panel
            // ---------------
            
            
            
            logPanel = new LogPanel(eh.getInputSupplier(), eh);
            LogPanelHandler textHandler = new LogPanelHandler(logPanel);
            textHandler.setFormatter(new LogPanelFormatter());
            textHandler.setLevel(Level.INFO);
            Logger.getLogger(Logger.GLOBAL_LOGGER_NAME).addHandler(textHandler);
            
                		
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
            LOGGER.log(Level.SEVERE, "Error initialising Main: " + e.getMessage(), e);
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
        
        tabbedPane.addChangeListener(e->{ // listen for tab switches and update charts if cells have been edited
        	boolean isUpdate = false;
        	for(TabPanel t : detailPanels){
        		isUpdate |= t.hasCellUpdate();
        	}

        	if(isUpdate) {
        		recacheCharts();
        		for(TabPanel t : detailPanels){
        			t.setCellUpdate(false);
        		}
        	}
        });

    }

    /**
     * Set the event listeners for each tab panel
     */
    @Override
	protected void createEventHandling() {
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
	public void eventReceived(InterfaceEvent event) {
		super.eventReceived(event);
		if(event.getSource().equals(eh)){
			InterfaceMethod method = event.method();
	        
	        final List<IAnalysisDataset> selected = DatasetListManager.getInstance().getSelectedDatasets();

	        switch (method) {
	        case CLEAR_LOG_WINDOW:
	        	logPanel.clear();
	            break;
	        }
	        
	        
		}
		
	}

	@Override
	public void datasetSelectionEventReceived(DatasetSelectionEvent e) {
		getPopulationsPanel().selectDatasets(e.getDatasets());
	}

	@Override
	public void eventReceived(DatasetEvent event) {
		super.eventReceived(event);
        if (event.method().equals(DatasetEvent.ADD_DATASET))
            addDataset(event.firstDataset());
	}
	    
    /**
     * Add the given dataset and all its children to the populations panel
     * 
     * @param dataset
     */
    private synchronized void addDataset(final IAnalysisDataset dataset) {

//    	getPopulationsPanel().addDataset(dataset);
//        for (IAnalysisDataset child : dataset.getAllChildDatasets()) {
//        	getPopulationsPanel().addDataset(child);
//        }

        // This will also trigger a dataset update event as the dataset
        // is selected, so don't trigger another update here.
        getPopulationsPanel().update(dataset);
        
      //Force all panels to update with the new datasets
       eh.eventReceived(new InterfaceEvent(this, InterfaceMethod.UPDATE_PANELS, "MainWindow"));
    }

	@Override
	public void eventReceived(SignalChangeEvent event) {
		// TODO Auto-generated method stub
		
	}

	@Override
	public void eventReceived(ChartOptionsRenderedEvent event) {
		// TODO Auto-generated method stub
		
	}  
}
