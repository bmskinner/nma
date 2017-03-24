/*******************************************************************************
 *  	Copyright (C) 2016 Ben Skinner
 *   
 *     This file is part of Nuclear Morphology Analysis.
 *
 *     Nuclear Morphology Analysis is free software: you can redistribute it and/or modify
 *     it under the terms of the GNU General Public License as published by
 *     the Free Software Foundation, either version 3 of the License, or
 *     (at your option) any later version.
 *
 *     Nuclear Morphology Analysis is distributed in the hope that it will be useful,
 *     but WITHOUT ANY WARRANTY; without even the implied warranty of
 *     MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 *     GNU General Public License for more details.
 *
 *     You should have received a copy of the GNU General Public License
 *     along with Nuclear Morphology Analysis. If not, see <http://www.gnu.org/licenses/>.
 *******************************************************************************/
package com.bmskinner.nuclear_morphology.gui;

import java.awt.BorderLayout;
import java.awt.Cursor;
import java.awt.Dimension;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.event.WindowEvent;
import java.awt.event.WindowStateListener;
import java.io.File;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.concurrent.Callable;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Future;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.logging.Level;
import java.util.logging.Logger;

import javax.swing.JFrame;
import javax.swing.JPanel;
import javax.swing.JSplitPane;
import javax.swing.JTabbedPane;
import javax.swing.SwingUtilities;
import javax.swing.border.EmptyBorder;

import com.bmskinner.nuclear_morphology.analysis.IWorkspace;
import com.bmskinner.nuclear_morphology.analysis.MergeSourceExtractor;
import com.bmskinner.nuclear_morphology.analysis.profiles.DatasetSegmentationMethod.MorphologyAnalysisMode;
import com.bmskinner.nuclear_morphology.components.IAnalysisDataset;
import com.bmskinner.nuclear_morphology.components.generic.Version;
import com.bmskinner.nuclear_morphology.components.nuclei.Nucleus;
import com.bmskinner.nuclear_morphology.components.options.IMutableAnalysisOptions;
import com.bmskinner.nuclear_morphology.components.options.MissingOptionException;
import com.bmskinner.nuclear_morphology.gui.InterfaceEvent.InterfaceMethod;
import com.bmskinner.nuclear_morphology.gui.actions.AddNuclearSignalAction;
import com.bmskinner.nuclear_morphology.gui.actions.AddTailStainAction;
import com.bmskinner.nuclear_morphology.gui.actions.BuildHierarchicalTreeAction;
import com.bmskinner.nuclear_morphology.gui.actions.ClusterAnalysisAction;
import com.bmskinner.nuclear_morphology.gui.actions.DatasetArithmeticAction;
import com.bmskinner.nuclear_morphology.gui.actions.ExportStatsAction;
import com.bmskinner.nuclear_morphology.gui.actions.FishRemappingAction;
import com.bmskinner.nuclear_morphology.gui.actions.LobeDetectionAction;
import com.bmskinner.nuclear_morphology.gui.actions.MergeCollectionAction;
import com.bmskinner.nuclear_morphology.gui.actions.NewAnalysisAction;
import com.bmskinner.nuclear_morphology.gui.actions.PopulationImportAction;
import com.bmskinner.nuclear_morphology.gui.actions.ProgressableAction;
import com.bmskinner.nuclear_morphology.gui.actions.RefoldNucleusAction;
import com.bmskinner.nuclear_morphology.gui.actions.RelocateFromFileAction;
import com.bmskinner.nuclear_morphology.gui.actions.ReplaceSourceImageDirectoryAction;
import com.bmskinner.nuclear_morphology.gui.actions.RunProfilingAction;
import com.bmskinner.nuclear_morphology.gui.actions.RunSegmentationAction;
import com.bmskinner.nuclear_morphology.gui.actions.SaveDatasetAction;
import com.bmskinner.nuclear_morphology.gui.actions.SaveWorkspaceAction;
import com.bmskinner.nuclear_morphology.gui.actions.ShellAnalysisAction;
import com.bmskinner.nuclear_morphology.gui.dialogs.CellCollectionOverviewDialog;
import com.bmskinner.nuclear_morphology.gui.main.MainDragAndDropTarget;
import com.bmskinner.nuclear_morphology.gui.main.MainHeaderPanel;
import com.bmskinner.nuclear_morphology.gui.main.MainWindowCloseAdapter;
import com.bmskinner.nuclear_morphology.gui.tabs.AnalysisDetailPanel;
import com.bmskinner.nuclear_morphology.gui.tabs.ClusterDetailPanel;
import com.bmskinner.nuclear_morphology.gui.tabs.EditingDetailPanel;
import com.bmskinner.nuclear_morphology.gui.tabs.InterDatasetComparisonDetailPanel;
import com.bmskinner.nuclear_morphology.gui.tabs.MergesDetailPanel;
import com.bmskinner.nuclear_morphology.gui.tabs.NuclearStatisticsPanel;
import com.bmskinner.nuclear_morphology.gui.tabs.NucleusProfilesPanel;
import com.bmskinner.nuclear_morphology.gui.tabs.SegmentsDetailPanel;
import com.bmskinner.nuclear_morphology.gui.tabs.SignalsDetailPanel;
import com.bmskinner.nuclear_morphology.gui.tabs.TabPanel;
import com.bmskinner.nuclear_morphology.gui.tabs.cells.CellsDetailPanel;
import com.bmskinner.nuclear_morphology.gui.tabs.populations.PopulationsPanel;
import com.bmskinner.nuclear_morphology.io.MappingFileExporter;
import com.bmskinner.nuclear_morphology.logging.LogPanelFormatter;
import com.bmskinner.nuclear_morphology.logging.Loggable;
import com.bmskinner.nuclear_morphology.logging.TextAreaHandler;

/**
 * This is the core of the program UI. All display panels are contained here. 
 * Update requests are sent from here to display information, and requests to 
 * perform analyses are relayed from sub-panels to here.
 * @author bms41
 *
 */
@SuppressWarnings("serial")
public class MainWindow 
	extends JFrame 
	implements SignalChangeListener, DatasetEventListener, InterfaceEventListener, Loggable {
				
	private JPanel contentPane;
	
	private LogPanel				logPanel;				// progress and messages
	private PopulationsPanel 		populationsPanel; 		// holds and selects open datasets
	private ConsensusNucleusPanel	consensusNucleusPanel; 	// show refolded nuclei if present
	
	private static final String PROGRAM_TITLE_BAR_LBL = "Nuclear Morphology Analysis v"+Version.currentVersion().toString();
	
	private static final String ANALYSIS_SETUP_TAB_LBL= "Analysis info";
	private static final String CLUSTERS_TAB_LBL      = "Clusters";
	private static final String MERGES_TAB_LBL        = "Merges";
	private static final String CELLS_TAB_LBL         = "Cell charts";
	private static final String NUCLEI_TAB_LBL        = "Nuclear charts";
	private static final String PROFILES_TAB_LBL      = "Nuclear profiles";
	private static final String SIGNALS_TAB_LBL       = "Nuclear signals";
	private static final String SEGMENTS_TAB_LBL      = "Nuclear segments";
	private static final String COMPARISONS_TAB_LBL   = "Comparisons";
	private static final String EDITING_TAB_LBL       = "Editing";
	
	
	private JTabbedPane 			tabbedPane;				// bottom panel tabs. Contains:
	
	private NucleusProfilesPanel 	nucleusProfilesPanel; 	// the angle profiles
	private AnalysisDetailPanel		analysisDetailPanel;	// nucleus detection parameters and stats
	private SignalsDetailPanel 		signalsDetailPanel;		// nuclear signals
	private CellsDetailPanel      	cellsDetailPanel;	    // cells stats - areas, perimeters etc
	private NuclearStatisticsPanel 	nuclearChartsPanel;	// nuclear stats - areas, perimeters etc
	private SegmentsDetailPanel 	segmentsDetailPanel;	// segmented profiles
	private ClusterDetailPanel		clusterDetailPanel;		// clustering within populations
	private MergesDetailPanel		mergesDetailPanel;		// merges between populations
	private InterDatasetComparisonDetailPanel comparisonsPanel;
	private EditingDetailPanel		editingDetailPanel; 	// for altering data
	
	private List<TabPanel> detailPanels = new ArrayList<TabPanel>(); // store panels for iterating messsages
	
	private List<Object> updateListeners = new ArrayList<Object>();
	
	private List<IWorkspace> workspaces = new ArrayList<IWorkspace>();
	
	
	private boolean isStandalone = false;
	
	/*
	 * Keep a strong reference to the program logger so it can be accessed
	 * by all other classes implementing the Loggable interface
	 */
	private static final Logger programLogger =
	        Logger.getLogger(Loggable.PROGRAM_LOGGER);
	
	
	private static final ThreadManager threadManager = ThreadManager.getInstance();		
	

	/**
	 * Create the frame.
	 * @param standalone is the frame a standalone app, or launched within ImageJ?
	 */
	public MainWindow(boolean standalone) {
				
		isStandalone = standalone;

		createWindowListeners();
		
		createUI();
	
	}
	
	/**
	 * Create the listeners that handle dataset saving when the main window is closed
	 * 
	 */
	private void createWindowListeners(){
		
		this.setDefaultCloseOperation(DO_NOTHING_ON_CLOSE);
		
		this.addWindowListener(new MainWindowCloseAdapter(this));
		
		// Add a listener for panel size changes. This will cause
		// charts to redraw at the new aspect ratio rather than stretch.
		this.addWindowStateListener(new WindowStateListener() {
			public void windowStateChanged(WindowEvent e){

				Runnable r = () -> {
					  try {
						  // If the update is called immediately, then the chart size has
						  // not yet changed, and therefore will render at the wrong aspect
						  // ratio
						  
						  Thread.sleep(100);
					  } catch (InterruptedException e1) {
						  
					  }

					  for(TabPanel d : detailPanels){
						  d.updateSize();
					  }
				  };
				  ThreadManager.getInstance().submit(r);

			  }
			});
		
		
		this.setDropTarget(  new MainDragAndDropTarget(this) );
	}
	
	/**
	 * Create the main UI
	 */
	private void createUI(){
		try {
			setTitle(PROGRAM_TITLE_BAR_LBL);
			
			Dimension preferredSize = new Dimension(1012, 804);
			this.setPreferredSize(preferredSize);
			setBounds(100, 100, 1012, 804);
			contentPane = new JPanel();
			contentPane.setBorder(new EmptyBorder(5, 5, 5, 5));
			contentPane.setLayout(new BorderLayout(0, 0));
			setContentPane(contentPane);
			
			//---------------
			// Create the header buttons
			//---------------
			contentPane.add(new MainHeaderPanel(this), BorderLayout.NORTH);
		
			
			//---------------
			// Create the log panel
			//---------------
			logPanel = new LogPanel();
			logPanel.addDatasetEventListener(this);
			logPanel.addInterfaceEventListener(this);
			logPanel.addSignalChangeListener(this);
			this.addDatasetUpdateEventListener(logPanel);
			TextAreaHandler textHandler = new TextAreaHandler(logPanel);
			textHandler.setFormatter(new LogPanelFormatter());
			programLogger.addHandler(textHandler);
			programLogger.setLevel(Level.INFO); // by default do not log everything 
			
			//---------------
			// Create the consensus chart
			//---------------
			populationsPanel = new PopulationsPanel();
			populationsPanel.addSignalChangeListener(this);
			populationsPanel.addDatasetEventListener(this);
			populationsPanel.addInterfaceEventListener(this);
//			this.addDatasetUpdateEventListener(populationsPanel);
			consensusNucleusPanel = new ConsensusNucleusPanel();
			detailPanels.add(consensusNucleusPanel);

			
			//---------------
			// Create the split view
			//---------------
			JSplitPane logAndPopulations = new JSplitPane(JSplitPane.HORIZONTAL_SPLIT,
					logPanel, populationsPanel);

			//Provide minimum sizes for the two components in the split pane
			Dimension minimumSize = new Dimension(300, 200);
			logPanel.setMinimumSize(minimumSize);
			populationsPanel.setMinimumSize(minimumSize);
			
			//---------------
			// Make the top row panel
			//---------------
			JPanel topRow = new JPanel();
			
			GridBagConstraints c = new GridBagConstraints();
			c.gridwidth = GridBagConstraints.RELATIVE; 	// next-to-last element
			c.fill = GridBagConstraints.BOTH;     		// fill both axes of container
			c.weightx = 1.0;       						// maximum weighting
			c.weighty = 1.0;
			
			topRow.setLayout(new GridBagLayout());
			topRow.add(logAndPopulations, c);

			c.gridwidth = GridBagConstraints.REMAINDER; //last element in row
			c.weightx = 0.5;							// allow padding on x axis
			c.weighty = 1.0;							// max weighting on y axis
			c.fill = GridBagConstraints.BOTH;      		// fill to bounds where possible
			topRow.add(consensusNucleusPanel, c);

			createTabs();	
			
			
			//---------------
			// Register change listeners
			//---------------
			
			for(TabPanel d : detailPanels){
				d.addDatasetEventListener(this);
				d.addInterfaceEventListener(this);
				d.addSignalChangeListener(this);
				this.addDatasetUpdateEventListener(d);
			}
			
			
			signalsDetailPanel.addSignalChangeListener(editingDetailPanel);
			editingDetailPanel.addSignalChangeListener(signalsDetailPanel); // allow the panels to communicate colour updates
			
			//---------------
			// Add the top and bottom rows to the main panel
			//---------------
			JSplitPane panelMain = new JSplitPane(JSplitPane.VERTICAL_SPLIT,
					topRow, tabbedPane);
			
			contentPane.add(panelMain, BorderLayout.CENTER);
			
//			checkUpdatingState();
			
			this.pack();
			consensusNucleusPanel.restoreAutoBounds();

		} catch (Exception e) {
			logToImageJ("Error initialising Main: "+e.getMessage(), e);
		}
	}
	
	/**
	 * Create the individual analysis tabs
	 */
	private void createTabs()  {
		tabbedPane = new JTabbedPane(JTabbedPane.TOP);

		analysisDetailPanel  = new AnalysisDetailPanel();
		nucleusProfilesPanel = new NucleusProfilesPanel();
		cellsDetailPanel     = new CellsDetailPanel();
		nuclearChartsPanel   = new NuclearStatisticsPanel();	
		signalsDetailPanel   = new SignalsDetailPanel();	
		clusterDetailPanel   = new ClusterDetailPanel();
		mergesDetailPanel    = new MergesDetailPanel();
		segmentsDetailPanel  = new SegmentsDetailPanel();
		comparisonsPanel     = new InterDatasetComparisonDetailPanel();
		editingDetailPanel   = new EditingDetailPanel();
		
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
		
		tabbedPane.addTab(ANALYSIS_SETUP_TAB_LBL, analysisDetailPanel);

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
	 * Check if the program has been started as a plugin to ImageJ
	 * or as standalone 
	 * @return
	 */
	public boolean isStandalone(){
		return isStandalone;
	}
			
	public PopulationsPanel getPopulationsPanel(){
		return this.populationsPanel;
	}
	
	public LogPanel getLogPanel(){
		return this.logPanel;
	}
	
	/**
	 * Map events to the actions they should trigger
	 * @author bms41
	 * @since 1.13.4
	 *
	 */
	private class ActionFactory {
		final IAnalysisDataset selectedDataset;
		List<IAnalysisDataset> selectedDatasets;
		
		public ActionFactory(){
						
			selectedDatasets = populationsPanel.getSelectedDatasets();
			selectedDataset = populationsPanel.getSelectedDatasets().isEmpty() 
					? null 
					: populationsPanel.getSelectedDatasets().get(0);
			
		}
		
		/**
		 * Create a runnable action for the given event 
		 * @param event
		 * @return
		 */
		public Runnable create(final SignalChangeEvent event){
			
			
			
			if(event.type().equals(SignalChangeEvent.EXPORT_WORKSPACE)){
				return new SaveWorkspaceAction(DatasetListManager.getInstance().getRootDatasets(), MainWindow.this); 
			}
			
			if(event.type().equals("DatasetArithmeticAction")){
				return new DatasetArithmeticAction(selectedDatasets, MainWindow.this); 
			}
			
			if(event.type().equals("ChangeNucleusFolderAction")){
				return new ReplaceSourceImageDirectoryAction(selectedDataset, MainWindow.this);
			}
			
			if(event.type().equals(SignalChangeEvent.ADD_NUCLEAR_SIGNAL)){
				return new AddNuclearSignalAction(selectedDataset,  MainWindow.this);
			}
			
			if(event.type().equals("PostFISHRemappingAction")){
				return new FishRemappingAction(selectedDatasets, MainWindow.this);
			}
			
			if(event.type().equals(SignalChangeEvent.EXPORT_STATS)){
				
				return new ExportStatsAction(selectedDatasets, MainWindow.this);
			}
			
			if(event.type().equals(SignalChangeEvent.LOBE_DETECTION)){
				return new LobeDetectionAction(selectedDataset, MainWindow.this);
			}
			
			if(event.type().startsWith("Open|")){
				String s = event.type().replace("Open|", "");
				File f = new File(s);
				
				return new PopulationImportAction(MainWindow.this, f);
			}
			
			
			if(event.type().startsWith("New|")){
				String s = event.type().replace("New|", "");
				File f = new File(s);
				
				return new NewAnalysisAction(MainWindow.this, f);
			}
			
			return null;
		}
		
		
		/**
		 * Create a runnable action for the given event 
		 * @param event
		 * @return
		 */
		public Runnable create(final DatasetEvent event){
			
			if(event.getDatasets().isEmpty()){
				return null;
			}
			
			selectedDatasets = event.getDatasets();
			
			if(event.method().equals(DatasetEvent.PROFILING_ACTION)){
				fine("Creating new profiling and segmentation");

				int flag = 0; // set the downstream analyses to run
				flag |= ProgressableAction.ADD_POPULATION;
				flag |= ProgressableAction.STATS_EXPORT;
				flag |= ProgressableAction.NUCLEUS_ANNOTATE;
				flag |= ProgressableAction.ASSIGN_SEGMENTS;

				try {
					if(event.firstDataset().getAnalysisOptions().refoldNucleus()){
						flag |= ProgressableAction.CURVE_REFOLD;
					}
				} catch (MissingOptionException e) {
					warn("Missing analysis options");
					stack(e.getMessage(), e);
					return null;
				}
				// begin a recursive morphology analysis
				return new RunProfilingAction(selectedDatasets, flag, MainWindow.this);
			}
			
			
			if(event.method().equals(DatasetEvent.NEW_MORPHOLOGY)){
				log("Running new morphology analysis");
				final int flag = ProgressableAction.ADD_POPULATION;
				
				return new RunSegmentationAction(selectedDatasets, MorphologyAnalysisMode.NEW, flag, MainWindow.this);
			}
			
			if(event.method().equals(DatasetEvent.REFRESH_MORPHOLOGY)){
				finer("Refreshing segmentation across nuclei using existing border tags");
				final int flag = 0;
				return new RunSegmentationAction(selectedDatasets, MorphologyAnalysisMode.REFRESH, flag, MainWindow.this);
			}
			
			if(event.method().equals(DatasetEvent.RUN_SHELL_ANALYSIS)){
				return new ShellAnalysisAction(event.firstDataset(), MainWindow.this);
			}
			
			if(event.method().equals(DatasetEvent.COPY_MORPHOLOGY)){
				
				final IAnalysisDataset source = event.secondaryDataset();
				if(source==null){
					return null;
				}
				return new RunSegmentationAction(selectedDatasets, source, null, MainWindow.this);
			}
			
						
			if(event.method().equals(DatasetEvent.CLUSTER)){
				log("Clustering dataset");
				return new ClusterAnalysisAction(event.firstDataset(),  MainWindow.this);
			
			}
			
			if(event.method().equals(DatasetEvent.BUILD_TREE)){
				log("Building a tree from dataset");
				return new BuildHierarchicalTreeAction(event.firstDataset(), MainWindow.this);
			}
			
			if(event.method().equals(DatasetEvent.RECALCULATE_MEDIAN)){
				fine("Recalculating the median for the given datasets");

				return new RunProfilingAction(selectedDatasets, ProgressableAction.NO_FLAG, MainWindow.this);
			}
			
			
			return null;
		}

		
		/**
		 * Create and run an action for the given event
		 * @param event
		 */
		public void run(final SignalChangeEvent event){
			Runnable r = create(event);
			if(r!=null){
				r.run();
			}
		}
		
		/**
		 * Create and run an action for the given event
		 * @param event
		 */
		public void run(final DatasetEvent event){
			Runnable r = create(event);
			if(r!=null){
				r.run();
			}
		}
	}
	
				
	@Override
	public void signalChangeReceived(final SignalChangeEvent event) {
		
		finer("Heard signal change event: "+event.type());

		final IAnalysisDataset selectedDataset = populationsPanel.getSelectedDatasets().isEmpty() 
				? null 
				: populationsPanel.getSelectedDatasets().get(0);
		
		// Try to launch via factory
		new ActionFactory().run(event);

		if(event.type().equals("MergeCollectionAction")){
			
			Runnable task = new MergeCollectionAction(populationsPanel.getSelectedDatasets(), MainWindow.this); 
			threadManager.execute(task);
		}

		
		if(event.type().equals("CurateCollectionAction")){

				CellCollectionOverviewDialog d = new CellCollectionOverviewDialog(populationsPanel.getSelectedDatasets().get(0));
				d.addDatasetEventListener(this);

		}
				
				
		if(event.type().equals("SaveCellLocations")){
			log("Exporting cell locations...");
			if(MappingFileExporter.exportCellLocations(selectedDataset)){
				log( "Export complete");
			} else {
				log("Export failed");
			}
			
		}
		
		if(event.type().equals("RelocateCellsAction")){
			
			CountDownLatch latch = new CountDownLatch(1);
			Runnable r = new RelocateFromFileAction(selectedDataset, MainWindow.this, latch);
			r.run();
			
		}
		
		if(event.type().equals("AddTailStainAction")){
			new AddTailStainAction(selectedDataset, this);
		}
		
		if(event.type().equals("UpdatePanels")){
			fireDatasetUpdateEvent(populationsPanel.getSelectedDatasets());
		}
		
		if(event.type().equals("UpdatePanelsNull")){
			fireDatasetUpdateEvent(new ArrayList<IAnalysisDataset>());
		}
		
		if(event.type().equals("UpdatePopulationPanel")){
			this.populationsPanel.update(populationsPanel.getSelectedDatasets());
		}
				
		if(event.type().equals("SaveCollectionAction")){
			
			this.saveDataset(selectedDataset, true);
		}
				
	}

	@Override
	public void datasetEventReceived(final DatasetEvent event) {

		// Try to launch via factory
		new ActionFactory().run(event);
		
		// Remaining methods
		final List<IAnalysisDataset> list = event.getDatasets();
		if(!list.isEmpty()){
						
			if(event.method().equals(DatasetEvent.REFOLD_CONSENSUS)){
				refoldConsensus(event.firstDataset());		
			}
			
			if(event.method().equals(DatasetEvent.SELECT_DATASETS)){
				populationsPanel.selectDatasets(event.getDatasets());
			}
			
			if(event.method().equals(DatasetEvent.SELECT_ONE_DATASET)){
				populationsPanel.selectDataset(event.firstDataset());				
			}
			
			if(event.method().equals(DatasetEvent.SAVE)){
				saveDataset(event.firstDataset(), false);
			}
			
			if(event.method().equals(DatasetEvent.EXTRACT_SOURCE)){
				MergeSourceExtractor ext = new MergeSourceExtractor(list);
				ext.addDatasetEventListener(this);
				ext.extractSourceDataset();;
				
			}
			
			if(event.method().equals(DatasetEvent.REFRESH_CACHE)){
				recacheCharts(list);				
			}
			
			if(event.method().equals(DatasetEvent.CLEAR_CACHE)){
				
				clearChartCache(list);
				
			}
			
			if(event.method().equals(DatasetEvent.ADD_DATASET)){
				addDataset(event.firstDataset());
			}
						
		}
		
	}
		
	public void addWorkspace(IWorkspace w){
		this.workspaces.add(w);
	}
	
	/**
	 * Add the given dataset and all its children to the 
	 * populations panel
	 * @param dataset
	 */
	private void addDataset(final IAnalysisDataset dataset){

		fine("Adding dataset");

		populationsPanel.addDataset(dataset);
		for(IAnalysisDataset child : dataset.getAllChildDatasets() ){
			populationsPanel.addDataset(child);
		}

		finer("Ordering update of populations panel");
		
		// This will also trigger a dataset update event as the dataset
		// is selected, so don't trigger another update here.
		populationsPanel.update(dataset);
	}
	
	
	/**
	 * Begin a refolding of the consensus nucleus for the 
	 * given dataset
	 * @param dataset
	 */
	private void refoldConsensus(final IAnalysisDataset dataset){
		fine("Refolding consensus");
		finest("Refold consensus dataset method is EDT: "+SwingUtilities.isEventDispatchThread());
		
		Runnable r = () -> {
			/*
			 * The refold action needs to be able to hold up a series
			 * of following actions, when it is being used in a New Analysis.
			 * The countdown latch does nothing here, but must be retained for
			 * compatibility.
			 */
			fine("Clearing chart cache for consensus charts");
			
			final List<IAnalysisDataset> list = new ArrayList<IAnalysisDataset>();
			list.add(dataset);
			segmentsDetailPanel.clearChartCache(list);  // segment positions charts need updating
			nuclearChartsPanel.clearChartCache(list); // overlaid nuclei need updating
			signalsDetailPanel.clearChartCache(list);   // signal consensus needs updating
			consensusNucleusPanel.clearChartCache(list);    // consensus panel needs updating

			final CountDownLatch latch = new CountDownLatch(1);
			finest("Created latch: "+latch.getCount());
			Runnable task = new RefoldNucleusAction(dataset, MainWindow.this, latch);
			task.run();
			finest("Running refolding");
			try {
				fine("Awaiting latch");
				latch.await();
				fine("Latch has released from refolding");
				if(dataset.hasAnalysisOptions()){
					
					IMutableAnalysisOptions op;
					try {
						op = (IMutableAnalysisOptions) dataset.getAnalysisOptions();
						op.setRefoldNucleus(true);
						fine("Set refold status in options");
					} catch (MissingOptionException e) {
						warn("Missing analysis options");
						stack(e.getMessage(), e);

					}
					
				} else {
					fine("Dataset has no analysis options, cannot set refold state");
				}

				populationsPanel.selectDataset(dataset);

			} catch (InterruptedException e) {
				error("Interruption to thread", e);
			}
		};
			
		threadManager.execute(r);
	}
	
	
	
	/**
	 * Save all the root datasets in the populations panel
	 */
	public void saveRootDatasets(){
		
		Runnable r = () -> {
			for(IAnalysisDataset root : DatasetListManager.getInstance().getRootDatasets()){
				final CountDownLatch latch = new CountDownLatch(1);

				Runnable task = new SaveDatasetAction(root, MainWindow.this, latch, false);
				task.run();
				try {
					latch.await();
				} catch (InterruptedException e) {
					error("Interruption to thread", e);
				}
			}
			fine("All root datasets attempted to be saved");
		};
			
		threadManager.execute(r);
	}

	
	/**
	 * Save the given dataset. If it is root, save directly.
	 * If it is not root, find the root parent and save it.
	 * @param d
	 * @param saveAs should the action ask for a directory
	 */	
	public void saveDataset(final IAnalysisDataset d, boolean saveAs){
		
		if(d.isRoot()){
			finer("Dataset is root");
			finest("Creating latch");
			final CountDownLatch latch = new CountDownLatch(1);

			Runnable r = new SaveDatasetAction(d, MainWindow.this, latch, saveAs);
			finest("Passing save action to executor service");
			r.run();

			fine("Root dataset saved");
		} else {
			finest("Not a root dataset, checking for parent");
			IAnalysisDataset target = null; 
			for(IAnalysisDataset root : DatasetListManager.getInstance().getRootDatasets()){
				//					for(AnalysisDataset root : populationsPanel.getRootDatasets()){

				for(IAnalysisDataset child : root.getAllChildDatasets()){
					if(child.getUUID().equals(d.getUUID())){
						target = root;
						break;
					}
				}
				if(target!=null){
					break;
				}
			}
			if(target!=null){
				saveDataset(target, saveAs);
			}
		}
	}
	
	
	/*
	 * Trigger a recache of all charts and datasets 
	 */
	private void recacheCharts(){
		
		Runnable task = () -> {
			for(TabPanel panel : detailPanels){
				panel.refreshChartCache();
				panel.refreshTableCache();
			}
		};
		threadManager.execute(task);
	}
	
	private void clearChartCache(){
		for(TabPanel panel : detailPanels){
			panel.clearChartCache();
			panel.clearTableCache();
		}	
	}
	

	private void clearChartCache(final List<IAnalysisDataset> list){
		
		if(list==null || list.isEmpty()){
			warn("A cache clear was requested for a specific list, which was null or empty");
			clearChartCache();
			return;
		}
		for(TabPanel panel : detailPanels){
			panel.clearChartCache(list);
			panel.clearTableCache(list);
		}		
	}
	
//	private void recacheCharts(final AnalysisDataset dataset){
//		final List<AnalysisDataset> list = new ArrayList<AnalysisDataset>();
//		list.add(dataset);
//		recacheCharts(list);
//	}
	
	private void recacheCharts(final List<IAnalysisDataset> list){
		
		Runnable task = () -> {
			finer("Heard recache request for list of  "+list.size()+" datasets");
			for(TabPanel panel : detailPanels){
				panel.refreshChartCache(list);
				panel.refreshTableCache(list);
			}
		};
		threadManager.submit(task);

	}
	
//	private void resegmentDatasets(){
//		
//		Runnable task = () -> {
//			final int flag = CURVE_REFOLD; // ensure consensus is replaced
//			// Recalculate the head and hump positions for rodent sperm
//			if(populationsPanel.getSelectedDatasets().get(0).getCollection().getNucleusType().equals(NucleusType.RODENT_SPERM)){
//
//				try{
//					fine("Replacing nucleus roi patterns");
//					for( Nucleus n : populationsPanel.getSelectedDatasets().get(0).getCollection().getNuclei()){
//
//						DefaultRodentSpermNucleus r = (DefaultRodentSpermNucleus) n;  
//
//						r.splitNucleusToHeadAndHump();
//						try {
//
//							r.calculateSignalAnglesFromPoint(r.getPoint(Tag.ORIENTATION_POINT));
//						} catch (Exception e) {
//							error("Error restoring signal angles", e);
//						}
//
//					}
//
//				}catch(Exception e){
//					error("Error recalculating angles", e);
//				}
//			}
//			
//			fine("Regenerating charts");
//			for(TabPanel panel : detailPanels){
//				panel.refreshChartCache();
//				panel.refreshTableCache();
//			}
//			
//			fine("Resegmenting datasets");
//			List<IAnalysisDataset> list = populationsPanel.getSelectedDatasets();
//			Runnable r = new RunSegmentationAction(list, MorphologyAnalysisMode.NEW, flag, MainWindow.this);
//			r.run();
//		};
//		threadManager.execute(task);
//	}

	
	
	@Override
	public void interfaceEventReceived(final InterfaceEvent event) {
		
		InterfaceMethod method = event.method();
		finest("Heard interface event: "+event.method().toString());
		
		switch(method){
		
		case REFRESH_POPULATIONS:
			populationsPanel.update(populationsPanel.getSelectedDatasets()); // ensure all child datasets are included
			break;
			
		case SAVE_ROOT:
			saveRootDatasets(); // DO NOT WRAP IN A SEPARATE THREAD, IT WILL LOCK THE PROGRESS BAR

			break;
			
		case UPDATE_PANELS:{
			List<IAnalysisDataset> list = populationsPanel.getSelectedDatasets();
			finer("Updating tab panels with list of "+list.size()+" datasets");
			fireDatasetUpdateEvent(list);
//			threadManager.executeAndCancelUpdate( new PanelUpdateTask(list) );
//			this.updatePanels(list);
			break;
		}
			
			
		case RECACHE_CHARTS:
			recacheCharts();
			break;				
		case LIST_SELECTED_DATASETS:
			int count=0;
			for(IAnalysisDataset d : populationsPanel.getSelectedDatasets()){
				log(count+"\t"+d.getName());
				count++;
			}
			break;
			
		case CLEAR_LOG_WINDOW:
			logPanel.clear();
			break;
			
		case UPDATE_IN_PROGRESS:
			for(TabPanel panel : this.detailPanels){
				panel.setAnalysing(true);
			}
			this.setCursor(new Cursor(Cursor.WAIT_CURSOR));
			break;
			
		case UPDATE_COMPLETE:
			for(TabPanel panel : this.detailPanels){
				panel.setAnalysing(false);
			}
			this.setCursor(new Cursor(Cursor.DEFAULT_CURSOR));
			break;
			
			
		case DUMP_LOG_INFO:
			for(IAnalysisDataset d : populationsPanel.getSelectedDatasets()){
				
				for(Nucleus n : d.getCollection().getNuclei()){
					log(n.toString());
				}
			}
			break;
			
		case INFO:
			for(IAnalysisDataset d : populationsPanel.getSelectedDatasets()){
				
				log(d.getCollection().toString());
			}
			break;
						
		default:
			break;

		}		
	}	
	
	@Override
	public void dispose(){
//		threadManager.shutdownNow();
		super.dispose();
	}
	
	public boolean hasOpenDatasets(){
		return DatasetListManager.getInstance().getAllDatasets().size()>0;
	}
	
	/**
     * Signal listeners that the given datasets should be displayed
     * @param list
     */
    public void fireDatasetUpdateEvent(final List<IAnalysisDataset> list){
		fine("Heard dataset update event fire");
    	PanelUpdater r = new PanelUpdater(list);
    	threadManager.executeAndCancelUpdate(r);
    }
    
       
    public class PanelUpdater implements CancellableRunnable {
    	private final List<IAnalysisDataset> list;
    	
    	private AtomicBoolean isCancelled = new AtomicBoolean(false);
    	
    	public PanelUpdater(final List<IAnalysisDataset> list){ 
    		this.list = list;
    	}

		@Override
		public void run() {
			PanelLoadingUpdater loader = new PanelLoadingUpdater();

    		try {
    			
    			Future<?> f = threadManager.submit( loader );
    			
    			// Wait for loading state to be set
    			while(!f.isDone() && !isCancelled.get()){
    				fine("Waiting for chart loading set...");
    				Thread.sleep(1);
    			}
    			
    			if(isCancelled.get()){
    				return;
    			}
    			
    			Boolean ok = (Boolean) f.get();
    			fine("Chart loading is set: "+ok );
    			
    		} catch (InterruptedException e1) {
    			warn("Interrupted update");
    			error("Error setting loading state", e1);
    			error("Cause of loading state error", e1.getCause());
    			return;
    		} catch (ExecutionException e1) {
    			error("Error setting loading state", e1);
    			error("Cause of loading state error", e1.getCause());
    			return;
    		}

    		// Now fire the update
    		fine("Firing general update for "+list.size()+" datasets");
    		DatasetUpdateEvent e = new DatasetUpdateEvent(this, list);
    		Iterator<Object> iterator = updateListeners.iterator();
    		while( iterator.hasNext() ) {
    			if(isCancelled.get()){
    				return;
    			}
    			( (DatasetUpdateEventListener) iterator.next() ).datasetUpdateEventReceived( e );
    		}
			
		}

		@Override
		public void cancel() {
			fine("Cancelling thread");
			isCancelled.set(true);
//			Thread.currentThread().interrupt();
			
		}
    	
    	
    }
    
    public class PanelLoadingUpdater implements Callable {
    	public PanelLoadingUpdater(){
    		
    	}
    	
    	@Override
    	public Boolean call() {


//    		log("Setting charts and tables loading");
    		// Update charts and panels to loading

    		for(TabPanel p : detailPanels){
    			p.setChartsAndTablesLoading();
    		}
    		return true;

    	}
    	
    }
    
    /**
     * Add a listener for dataset update events.
     * @param l
     */
    public synchronized void addDatasetUpdateEventListener( DatasetUpdateEventListener l ) {
    	updateListeners.add( l );
    }
    
    public synchronized void removeDatasetUpdateEventListener( DatasetUpdateEventListener l ) {
    	updateListeners.remove( l );
    }
    
}
