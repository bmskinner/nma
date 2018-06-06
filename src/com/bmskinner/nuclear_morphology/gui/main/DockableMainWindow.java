package com.bmskinner.nuclear_morphology.gui.main;

import java.awt.BorderLayout;
import java.awt.Cursor;
import java.awt.Dimension;
import java.awt.Toolkit;
import java.awt.event.WindowEvent;
import java.awt.event.WindowStateListener;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;

import javax.swing.JPanel;
import javax.swing.JSplitPane;

import com.bmskinner.nuclear_morphology.components.IAnalysisDataset;
import com.bmskinner.nuclear_morphology.gui.ConsensusNucleusPanel;
import com.bmskinner.nuclear_morphology.gui.DatasetEvent;
import com.bmskinner.nuclear_morphology.gui.DatasetUpdateEvent;
import com.bmskinner.nuclear_morphology.gui.InterfaceEvent;
import com.bmskinner.nuclear_morphology.gui.InterfaceEvent.InterfaceMethod;
import com.bmskinner.nuclear_morphology.gui.LogPanel;
import com.bmskinner.nuclear_morphology.gui.ProgressBarAcceptor;
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
import com.bmskinner.nuclear_morphology.logging.Loggable;
import com.bmskinner.nuclear_morphology.logging.TextAreaHandler;
import com.bmskinner.nuclear_morphology.main.DatasetListManager;
import com.bmskinner.nuclear_morphology.main.EventHandler;
import com.bmskinner.nuclear_morphology.main.ThreadManager;
import com.javadocking.DockingManager;
import com.javadocking.dock.CompositeLineDock;
import com.javadocking.dock.Position;
import com.javadocking.dock.SingleDock;
import com.javadocking.dock.TabDock;
import com.javadocking.dock.factory.LeafDockFactory;
import com.javadocking.dock.factory.SingleDockFactory;
import com.javadocking.dock.factory.TabDockFactory;
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
     */
    public DockableMainWindow(boolean standalone, EventHandler eh) {
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
//            contentPane.setBorder(new EmptyBorder(5, 5, 5, 5));
            contentPane.setLayout(new BorderLayout(0, 0));
            setContentPane(contentPane);

            // ---------------
            // Create the header buttons
            // ---------------
//            contentPane.add(new MainHeaderPanel(this), BorderLayout.NORTH);

            // ---------------
            // Create the log panel
            // ---------------
            
            FloatDockModel dockModel = new FloatDockModel();
    		dockModel.addOwner("frame0", this);
    		DockingManager.setDockModel(dockModel);
    		SingleDockFactory floatFactory = new SingleDockFactory();
    		dockModel.getFloatDock(this).setChildDockFactory(floatFactory); // ensure floating docks are not converted to tab docks
            
            logPanel = new LogPanel(eh.getInputSupplier());
            TextAreaHandler textHandler = new TextAreaHandler(logPanel);
            textHandler.setFormatter(new LogPanelFormatter());
            Logger.getLogger(Loggable.PROGRAM_LOGGER).addHandler(textHandler);
            Logger.getLogger(Loggable.PROGRAM_LOGGER).setLevel(Level.INFO);
            
            Dockable dockable1 = new DefaultDockable("Window1", logPanel, "Log panel", null, DockingMode.ALL);
            SingleDock logTabDock = new SingleDock();
            logTabDock.addDockable(dockable1, new Position(0));
            
            
            CompositeLineDock lineDock1 = new CompositeLineDock(
    				CompositeLineDock.ORIENTATION_HORIZONTAL, true, new LeafDockFactory());

            lineDock1.addChildDock(logTabDock, new Position(0));
            
            
    		dockModel.addRootDock("topdock", lineDock1, this);
    		
            // ---------------
            // Create the consensus chart
            // ---------------
            populationsPanel = new PopulationsPanel(eh.getInputSupplier());
            
            Dockable popDockable = new DefaultDockable("Window2", populationsPanel, "Datasets", null, DockingMode.ALL);
            SingleDock popTabDock = new SingleDock();
            popTabDock.addDockable(popDockable, new Position(0));
            lineDock1.addChildDock(popTabDock, new Position(1));
            
            
            
            consensusNucleusPanel = new ConsensusNucleusPanel(eh.getInputSupplier());
            
            Dockable consDockable = new DefaultDockable("Window3", consensusNucleusPanel, "Consensus", null, DockingMode.ALL);
            SingleDock consTabDock = new SingleDock();
            consTabDock.addDockable(consDockable, new Position(0));
            lineDock1.addChildDock(consTabDock, new Position(2));
            detailPanels.add(consensusNucleusPanel);


            // Provide minimum sizes for the two components in the split pane
            Dimension minimumSize = new Dimension(300, 200);
            logPanel.setMinimumSize(minimumSize);
            populationsPanel.setMinimumSize(minimumSize);

            createTabs();
            dockModel.addRootDock("tabdock", tabDock, this);
            // ---------------
            // Add the top and bottom rows to the main panel
            // ---------------
            JSplitPane panelMain = new JSplitPane(JSplitPane.VERTICAL_SPLIT, lineDock1, tabDock);

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

    

    public PopulationsPanel getPopulationsPanel() {
        return this.populationsPanel;
    }

    @Override
	public ProgressBarAcceptor getProgressAcceptor() {
        return this.logPanel;
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
        
        if (event.method().equals(DatasetEvent.ADD_WORKSPACE))
        	getPopulationsPanel().update();
 
        
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
        eh.interfaceEventReceived(new InterfaceEvent(this, InterfaceMethod.UPDATE_PANELS, "MainWindow"));
    }

}
