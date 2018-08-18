package com.bmskinner.nuclear_morphology.gui.main;

import java.awt.BorderLayout;
import java.awt.Cursor;
import java.awt.Dimension;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Toolkit;
import java.util.List;
import java.util.logging.Logger;

import javax.swing.JPanel;
import javax.swing.JSplitPane;
import javax.swing.border.EmptyBorder;

import com.bmskinner.nuclear_morphology.components.IAnalysisDataset;
import com.bmskinner.nuclear_morphology.core.DatasetListManager;
import com.bmskinner.nuclear_morphology.core.EventHandler;
import com.bmskinner.nuclear_morphology.gui.ChartOptionsRenderedEvent;
import com.bmskinner.nuclear_morphology.gui.ConsensusNucleusPanel;
import com.bmskinner.nuclear_morphology.gui.DatasetEvent;
import com.bmskinner.nuclear_morphology.gui.InterfaceEvent;
import com.bmskinner.nuclear_morphology.gui.InterfaceEvent.InterfaceMethod;
import com.bmskinner.nuclear_morphology.gui.LogPanel;
import com.bmskinner.nuclear_morphology.gui.ProgressBarAcceptor;
import com.bmskinner.nuclear_morphology.gui.SignalChangeEvent;
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
 * @author ben
 * @since 1.14.0
 *
 */
public class DockableMainWindow extends AbstractMainWindow {
	private JPanel contentPane;

    private LogPanel logPanel;
    private PopulationsPanel populationsPanel;
    private ConsensusNucleusPanel consensusNucleusPanel;
    private TabDock tabDock; // bottom panel tabs

    /**
     * Create the frame.
     * 
     * @param standalone is the frame a standalone app, or launched within ImageJ?
     * @param eh the event handler controlling actions
     */
    public DockableMainWindow(boolean standalone, EventHandler eh) {
    	super(standalone, eh);

        createWindowListeners();

        createUI();

        this.eh.addProgressBarAcceptor(logPanel);
        createEventHandling();
               
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
    		dockModel.getFloatDock(this).setChildDockFactory(floatFactory); // ensure floating docks are not converted to tab docks
            
            logPanel = new LogPanel(eh.getInputSupplier());
            
            LogPanelHandler textHandler = new LogPanelHandler(logPanel);
            textHandler.setFormatter(new LogPanelFormatter());
            Logger.getLogger(Loggable.PROGRAM_LOGGER).addHandler(textHandler);
    		
            // ---------------
            // Create the consensus chart
            // ---------------
            populationsPanel = new PopulationsPanel(eh.getInputSupplier());            
            consensusNucleusPanel = new ConsensusNucleusPanel(eh.getInputSupplier());
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
            createTabs();
            dockModel.addRootDock("tabdock", tabDock, this);
            
            // ---------------
            // Add the top and bottom rows to the main panel
            // ---------------
            JSplitPane panelMain = new JSplitPane(JSplitPane.VERTICAL_SPLIT, topRow, tabDock);
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
    	
    	tabDock = new TabDock();

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
        
        int i=0;
        for(TabPanel t : detailPanels){
            if(t instanceof ConsensusNucleusPanel){
                continue;
            }

            DetailPanel p = (DetailPanel)t;
            p.setMaximumSize(Toolkit.getDefaultToolkit().getScreenSize());
            Dockable d = new DefaultDockable(p.getPanelTitle(), p, p.getPanelTitle(), null, DockingMode.ALL);
            tabDock.addDockable(d, new Position(i++));
        }
        
        signalsDetailPanel.addSignalChangeListener(editingDetailPanel);
        editingDetailPanel.addSignalChangeListener(signalsDetailPanel);
        
        tabDock.setSelectedDockable(tabDock.getDockable(0));
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

    
    @Override
    protected PopulationsPanel getPopulationsPanel() {
        return this.populationsPanel;
    }

    @Override
	public ProgressBarAcceptor getProgressAcceptor() {
        return this.logPanel;
    }
    
    /**
     * Add the given dataset and all its children to the populations panel
     * 
     * @param dataset
     */
    private synchronized void addDataset(final IAnalysisDataset dataset) {
    	fine("Adding dataset to populations panel: "+dataset.getName());
    	getPopulationsPanel().addDataset(dataset);
        for (IAnalysisDataset child : dataset.getAllChildDatasets()) {
        	getPopulationsPanel().addDataset(child);
        }

        // This will also trigger a dataset update event as the dataset
        // is selected, so don't trigger another update here.
        getPopulationsPanel().update(dataset);
        
        //Force all panels to update with the new datasets
        eh.eventReceived(new InterfaceEvent(this, InterfaceMethod.UPDATE_PANELS, "MainWindow"));
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
	
	@Override
	public void eventReceived(InterfaceEvent event) {
		super.eventReceived(event);
		if(event.getSource().equals(eh)){
			InterfaceMethod method = event.method();

			switch(method) {
	        case CLEAR_LOG_WINDOW: logPanel.clear();
	            break;
			}

		}
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
