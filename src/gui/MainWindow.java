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
package gui;

import gui.DatasetEvent.DatasetMethod;
import gui.InterfaceEvent.InterfaceMethod;
import gui.actions.AddNuclearSignalAction;
import gui.actions.AddTailStainAction;
import gui.actions.BuildHierarchicalTreeAction;
import gui.actions.ClusterAnalysisAction;
import gui.actions.CurateCollectionAction;
import gui.actions.DatasetArithmeticAction;
import gui.actions.FishRemappingAction;
import gui.actions.MergeCollectionAction;
import gui.actions.RefoldNucleusAction;
import gui.actions.RelocateFromFileAction;
import gui.actions.ReplaceSourceImageDirectoryAction;
import gui.actions.RunProfilingAction;
import gui.actions.RunSegmentationAction;
import gui.actions.NewAnalysisAction;
import gui.actions.PopulationImportAction;
import gui.actions.SaveDatasetAction;
import gui.actions.ShellAnalysisAction;
import gui.components.ColourSelecter.ColourSwatch;
import gui.dialogs.MainOptionsDialog;
import gui.tabs.AnalysisDetailPanel;
import gui.tabs.ClusterDetailPanel;
import gui.tabs.DetailPanel;
import gui.tabs.EditingDetailPanel;
import gui.tabs.InterDatasetComparisonDetailPanel;
import gui.tabs.MergesDetailPanel;
import gui.tabs.NuclearStatisticsPanel;
import gui.tabs.NucleusProfilesPanel;
import gui.tabs.SegmentsDetailPanel;
import gui.tabs.SignalsDetailPanel;
import io.MappingFileExporter;

import java.awt.BorderLayout;
import java.awt.Cursor;
import java.awt.Dimension;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.logging.Level;
import java.util.logging.Logger;

import javax.swing.JButton;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JSplitPane;
import javax.swing.JTabbedPane;
import javax.swing.SwingUtilities;
import javax.swing.border.EmptyBorder;

import logging.LogPanelFormatter;
import logging.Loggable;
import logging.TextAreaHandler;
import utility.Constants;
import utility.Version;
import analysis.AnalysisDataset;
import analysis.nucleus.DatasetSegmenter.MorphologyAnalysisMode;
import components.generic.BorderTag;
import components.nuclear.NucleusType;
import components.nuclei.Nucleus;
import components.nuclei.sperm.RodentSpermNucleus;

/**
 * @author bms41
 *
 */
@SuppressWarnings("serial")
public class MainWindow 
	extends JFrame 
	implements SignalChangeListener, DatasetEventListener, InterfaceEventListener, Loggable {
				
	private JPanel contentPane;

	private JLabel lblStatusLine = new JLabel("No analysis open");
	
	private LogPanel				logPanel;				// progress and messages
	private PopulationsPanel 		populationsPanel; 		// holds and selects open datasets
	private ConsensusNucleusPanel	consensusNucleusPanel; 	// show refolded nuclei if present
	
	private JTabbedPane 			tabbedPane;				// bottom panel tabs. Contains:
	
	private NucleusProfilesPanel 	nucleusProfilesPanel; 	// the angle profiles
	private AnalysisDetailPanel		analysisDetailPanel;	// nucleus detection parameters and stats
	private SignalsDetailPanel 		signalsDetailPanel;		// nuclear signals
	private NuclearStatisticsPanel 	nuclearBoxplotsPanel;	// nuclear stats - areas, perimeters etc
	private SegmentsDetailPanel 	segmentsDetailPanel;	// segmented profiles
	private ClusterDetailPanel		clusterDetailPanel;		// clustering within populations
	private MergesDetailPanel		mergesDetailPanel;		// merges between populations
	private InterDatasetComparisonDetailPanel interdatasetDetailPanel;
	private EditingDetailPanel		editingDetailPanel; 	// for altering data
	
	private List<DetailPanel> detailPanels = new ArrayList<DetailPanel>(); // store panels for iterating messsages
	
	private ColourSwatch activeSwatch = ColourSwatch.REGULAR_SWATCH;
	
	private final Version version = new Version(Constants.VERSION_MAJOR, Constants.VERSION_MINOR, Constants.VERSION_REVISION);
		
	// Flags to pass to ProgressableActions to determine the analyses
	// to carry out in subsequently
	public static final int ADD_POPULATION		 = 1;
	public static final int STATS_EXPORT 		 = 2;
	public static final int NUCLEUS_ANNOTATE	 = 4;
	public static final int CURVE_REFOLD 		 = 8;
	public static final int EXPORT_COMPOSITE	 = 16;
	public static final int SAVE_DATASET		 = 32;
	public static final int ASSIGN_SEGMENTS		 = 64;
	
	/*
	 * Keep a strong reference to the program logger so it can be accessed
	 * by all other classes implementing the Loggable interface
	 */
	private static final Logger programLogger =
	        Logger.getLogger(Loggable.PROGRAM_LOGGER); // the program logger will report status and errors in the running of the program, not involving datasets 
	
	/*
	 * Handle threading
	 */
	
	public static final int corePoolSize = 8;
	public static final int maximumPoolSize = 16;
	public static final int keepAliveTime = 5000;

	ExecutorService executorService = new ThreadPoolExecutor(corePoolSize, maximumPoolSize,
			keepAliveTime, TimeUnit.MILLISECONDS,
			new LinkedBlockingQueue<Runnable>());
	
	
	/**
	 * Create the frame.
	 */
	public MainWindow() {
		
		this.setDefaultCloseOperation(DISPOSE_ON_CLOSE);
		try {
			setTitle("Nuclear Morphology Analysis v"+getVersion().toString());
			setBounds(100, 100, 1012, 804);
			this.setLocationRelativeTo(null); // centre on screen
			contentPane = new JPanel();
			contentPane.setBorder(new EmptyBorder(5, 5, 5, 5));
			contentPane.setLayout(new BorderLayout(0, 0));
			setContentPane(contentPane);
			
			//---------------
			// Create the header buttons
			//---------------
			contentPane.add(createHeaderButtons(), BorderLayout.NORTH);

			//---------------
			// Footer
			//---------------
			contentPane.add(createFooterRow(), BorderLayout.SOUTH);
			
			
			//---------------
			// Create the log panel
			//---------------
			logPanel = new LogPanel();
			logPanel.addDatasetEventListener(this);
			logPanel.addInterfaceEventListener(this);
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
			
			consensusNucleusPanel = new ConsensusNucleusPanel();
			detailPanels.add(consensusNucleusPanel);

			
			//---------------
			// Create the split view
			//---------------
			JSplitPane logAndPopulations = new JSplitPane(JSplitPane.HORIZONTAL_SPLIT,
					logPanel, populationsPanel);

			//Provide minimum sizes for the two components in the split pane
			Dimension minimumSize = new Dimension(200, 200);
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

			tabbedPane = new JTabbedPane(JTabbedPane.TOP);

			//---------------
			// Create the profiles
			//---------------
			nucleusProfilesPanel = new NucleusProfilesPanel();
			detailPanels.add(nucleusProfilesPanel);
			tabbedPane.addTab("Profiles", null, nucleusProfilesPanel, null);

			//---------------
			// Create the general stats page
			//---------------
			analysisDetailPanel = new AnalysisDetailPanel();
			detailPanels.add(analysisDetailPanel);
			tabbedPane.addTab("Analysis info", analysisDetailPanel);

			//---------------
			// Create panel for split boxplots
			//---------------
			nuclearBoxplotsPanel  = new NuclearStatisticsPanel();
			detailPanels.add(nuclearBoxplotsPanel);
			tabbedPane.addTab("Nuclear charts", nuclearBoxplotsPanel);
				
			
			//---------------
			// Create the signals tab panel
			//---------------
			signalsDetailPanel  = new SignalsDetailPanel();
			detailPanels.add(signalsDetailPanel);
			tabbedPane.addTab("Signals", signalsDetailPanel);
			

			//---------------
			// Create the clusters panel
			//---------------
			clusterDetailPanel = new ClusterDetailPanel();
			detailPanels.add(clusterDetailPanel);
			tabbedPane.addTab("Clusters", clusterDetailPanel);
			
			//---------------
			// Create the merges panel
			//---------------
			mergesDetailPanel = new MergesDetailPanel();
			detailPanels.add(mergesDetailPanel);
			tabbedPane.addTab("Merges", mergesDetailPanel);

			//---------------
			// Create the segments boxplot panel
			//---------------
			segmentsDetailPanel = new SegmentsDetailPanel();
			detailPanels.add(segmentsDetailPanel);
			tabbedPane.addTab("Segments", null, segmentsDetailPanel, null);

			
			//---------------
			// Create the inter-dataset panel
			//---------------
			interdatasetDetailPanel = new InterDatasetComparisonDetailPanel();
			detailPanels.add(interdatasetDetailPanel);
			tabbedPane.addTab("Inter-dataset comparisons", null, interdatasetDetailPanel, null);
			
			
			editingDetailPanel = new EditingDetailPanel();
			detailPanels.add(editingDetailPanel);
			tabbedPane.addTab("Editing", null, editingDetailPanel, null);
			
			//---------------
			// Register change listeners
			//---------------
			
			for(DetailPanel d : detailPanels){
				d.addDatasetEventListener(this);
				d.addInterfaceEventListener(this);
				d.addSignalChangeListener(this);
			}
			
			
			signalsDetailPanel.addSignalChangeListener(editingDetailPanel);
			editingDetailPanel.addSignalChangeListener(signalsDetailPanel); // allow the panels to communicate colour updates
			
			//---------------
			// Add the top and bottom rows to the main panel
			//---------------
			JSplitPane panelMain = new JSplitPane(JSplitPane.VERTICAL_SPLIT,
					topRow, tabbedPane);
			
			contentPane.add(panelMain, BorderLayout.CENTER);
			
			checkUpdatingState();

		} catch (Exception e) {
			logToImageJ("Error initialising Main", e);
		}
		
	}
		
	public ColourSwatch getColourSwatch(){
		return activeSwatch;
	}
	
	public void setColourSwatch(ColourSwatch swatch){
		activeSwatch = swatch;
	}
	
	public PopulationsPanel getPopulationsPanel(){
		return this.populationsPanel;
	}
	
	public LogPanel getLogPanel(){
		return this.logPanel;
	}
		
	/**
	 * Create the panel of primary buttons
	 * @return a panel
	 */
	private JPanel createHeaderButtons(){

		JPanel panelHeader = new JPanel();

		JButton btnNewAnalysis = new JButton("New analysis");
		btnNewAnalysis.addActionListener(
				e ->new NewAnalysisAction(MainWindow.this)
		);
		panelHeader.add(btnNewAnalysis);

		//---------------
		// load saved dataset button
		//---------------

		JButton btnLoadSavedDataset = new JButton("Load analysis dataset");
		
		btnLoadSavedDataset.addActionListener(	
			e -> {
				log(Level.FINEST, "Creating import action");
				new PopulationImportAction(MainWindow.this);
			}
		);
			
		panelHeader.add(btnLoadSavedDataset);

		//---------------
		// save button
		//---------------

		JButton btnSavePopulation = new JButton("Save all");
		btnSavePopulation.addActionListener(

				e -> {
					log(Level.INFO, "Saving root populations...");

					Thread thr = new Thread(){
						public void run(){
							saveRootDatasets();
						}};
						thr.start();
				}
		);

		panelHeader.add(btnSavePopulation);

		//---------------
		// FISH mapping button
		//---------------

		JButton btnPostanalysisMapping = new JButton("Post-FISH mapping");
		btnPostanalysisMapping.addActionListener(
				e -> new FishRemappingAction(populationsPanel.getSelectedDatasets(), MainWindow.this)
		);
		panelHeader.add(btnPostanalysisMapping);
				
		JButton btnSetLogLevel = new JButton("Options");
		btnSetLogLevel.addActionListener(
				
				e -> { 

					MainOptionsDialog dialog = new MainOptionsDialog(MainWindow.this);
					if(dialog.isReadyToRun()){
	
						try {
							/*
							 * If the recache is not waited on, the update conflicts
							 * with the updating status
							 */
							log(Level.FINEST, "Options closed, clearing all caches");
							
							CountDownLatch l = new CountDownLatch(1);
							clearChartCache(l);
							l.await();
							log(Level.FINEST, "Options closed, updating charts");
		                    updatePanels(populationsPanel.getSelectedDatasets());
							
						} catch (InterruptedException e1) {
							log(Level.SEVERE, "Interruption to recaching", e1);
						}
	
					} else {
						log(Level.FINEST, "Options cancelled");
					}
			}
		);		
		panelHeader.add(btnSetLogLevel);
		return panelHeader;
	}
	
	/**
	 * Get the program version
	 * @return the version
	 */
	public Version getVersion(){
		return version;
	}
	
	/**
	 * Create the status panel at the base of the window
	 * @return the panel
	 */
	private JPanel createFooterRow(){
		JPanel panel = new JPanel();
		panel.add(lblStatusLine);
		return panel;
	}
		
	public void setStatus(String message){
		lblStatusLine.setText(message);
	}
			
	/**
	 * Update the display panels with information from the given datasets
	 * @param list the datasets to display
	 */
	private void updatePanels(final List<AnalysisDataset> list){
		if(list!=null){
			log(Level.FINE, "Updating tab panels for "+list.size()+" datasets");
		} else {
			log(Level.FINE, "Updating tab panels with null datasets");
		}
		
		Runnable task = () -> {
			try {

				for(DetailPanel panel : MainWindow.this.detailPanels){
					panel.update(list);
				}

				log(Level.FINE, "Updated tab panels");

			} catch (Exception e) {
				log(Level.SEVERE,"Error updating panels", e);
			}
		};
		
		executorService.execute(task);
	}
	
	
	volatile private boolean isRunning = false;
	
	private synchronized boolean checkRunning(){
		if (isRunning) 
			return true;
		else 
			return false;
	}
	
	
	
	/**
	 * Check if any of the detail panels are in the process
	 * of updating, and if so, set the busy cursor. Waits 500ms
	 * between checks. 
	 */
	private synchronized void checkUpdatingState(){
		Thread thr = new Thread() {
			public void run() {
				try {
					while(true){
						isRunning = false;

						for(DetailPanel panel : MainWindow.this.detailPanels){
							if(panel.isUpdating()){
								isRunning = true;
//								IJ.log(isRunning+": Panel: "+panel.getClass().getSimpleName());
							}
							
						}

						if(checkRunning()){
							MainWindow.this.setCursor(Cursor.getPredefinedCursor(Cursor.WAIT_CURSOR));
						} else {
							MainWindow.this.setCursor(Cursor.getDefaultCursor());
						}

						Thread.sleep(500);
					}
				} catch (InterruptedException e) {

					log(Level.SEVERE,"Error checking update state", e);

				}
			}

		};
		thr.start();
	}
	

			
	@Override
	public void signalChangeReceived(SignalChangeEvent event) {
		
		log(Level.FINEST, "Heard signal change event: "+event.type());
		
		final AnalysisDataset selectedDataset = populationsPanel.getSelectedDatasets().isEmpty() 
				? null 
				: populationsPanel.getSelectedDatasets().get(0);
		
		
		if(event.type().equals("RunShellAnalysis")){
			Runnable task = () -> {
				log(Level.FINER, "Shell analysis selected");
				new ShellAnalysisAction(selectedDataset, MainWindow.this);
			};
			executorService.execute(task);
		}

		if(event.type().equals("MergeCollectionAction")){
			
			Runnable task = () -> { new MergeCollectionAction(populationsPanel.getSelectedDatasets(), MainWindow.this); }; 
			executorService.execute(task);
		}
		
		if(event.type().equals("DatasetArithmeticAction")){
			
			Runnable task = () -> { new DatasetArithmeticAction(selectedDataset, populationsPanel.getAllDatasets(), MainWindow.this); }; 
			executorService.execute(task);
		}

		
		if(event.type().equals("CurateCollectionAction")){

			Runnable task = () -> { new CurateCollectionAction(selectedDataset, MainWindow.this); }; 
			executorService.execute(task);
		}
				

		
		if(event.type().equals("ChangeNucleusFolderAction")){
			new ReplaceSourceImageDirectoryAction(selectedDataset, MainWindow.this);
		}
				
		if(event.type().equals("SaveCellLocations")){
			log(Level.INFO, "Exporting cell locations...");
			if(MappingFileExporter.exportCellLocations(selectedDataset)){
				log(Level.INFO, "Export complete");
			} else {
				log(Level.INFO, "Export failed");
			}
			
		}
		
		if(event.type().equals("RelocateCellsAction")){
			
			CountDownLatch latch = new CountDownLatch(1);
			new RelocateFromFileAction(selectedDataset, MainWindow.this, latch);			
			
		}
		
		
		
		if(event.type().equals("ReapplySegmentProfileAction")){
			
			Runnable task = () -> {
				try{

					// get the names of other populations
					List<String> nameList = populationsPanel.getPopulationNames();
					nameList.remove(selectedDataset.getName());
					
					String[] names = nameList.toArray(new String[0]);

					String selectedValue = (String) JOptionPane.showInputDialog(null,
							"Choose population to take segments from", "Reapply segmentation",
							JOptionPane.INFORMATION_MESSAGE, null,
							names, names[0]);

					if(selectedValue!=null){

						AnalysisDataset source = populationsPanel.getDataset(selectedValue);
						final CountDownLatch latch = new CountDownLatch(1);
						new RunSegmentationAction(selectedDataset, source, null, MainWindow.this, latch);
						latch.await();
					}
				} catch(Exception e1){
					log(Level.SEVERE, "Error applying morphology", e1);
				}

			};
			
			SwingUtilities.invokeLater(task);
		}
		
		if(event.type().equals("AddTailStainAction")){
			new AddTailStainAction(selectedDataset, this);
		}
		
		if(event.type().equals("AddNuclearSignalAction")){
			new AddNuclearSignalAction(selectedDataset, this);
		}
		
		if(event.type().equals("NewShellAnalysisAction")){
			new ShellAnalysisAction(selectedDataset, this);
		}
		
		if(event.type().equals("UpdatePanels")){
			this.updatePanels(populationsPanel.getSelectedDatasets());
		}
		
		if(event.type().equals("UpdatePanelsNull")){
			this.updatePanels(new ArrayList<AnalysisDataset>());
		}
		
		if(event.type().equals("UpdatePopulationPanel")){
			this.populationsPanel.update(populationsPanel.getSelectedDatasets());
		}
		
		if(event.type().equals("RefreshPopulationPanelDatasets")){
			this.populationsPanel.refreshDatasets();;
		}
		
		if(event.type().startsWith("Log_")){
			String s = event.type().replace("Log_", "");
			log(Level.INFO, s);
		}
		
		if(event.type().startsWith("Status_")){
			String s = event.type().replace("Status_", "");
			setStatus(s);
		}
				
	}

	@Override
	public void datasetEventReceived(final DatasetEvent event) {
		log(Level.FINEST, "Heard dataset event: "+event.method().toString());
		final List<AnalysisDataset> list = event.getDatasets();
		if(!list.isEmpty()){
			
			
			
			if(event.method().equals(DatasetMethod.PROFILING_ACTION)){
				log(Level.FINE, "Running new profiling and segmentation");
				
				Runnable task = () -> { 
					int flag = 0; // set the downstream analyses to run
					flag |= MainWindow.ADD_POPULATION;
					flag |= MainWindow.STATS_EXPORT;
					flag |= MainWindow.NUCLEUS_ANNOTATE;
					flag |= MainWindow.ASSIGN_SEGMENTS;
					
					if(event.firstDataset().getAnalysisOptions().refoldNucleus()){
						flag |= MainWindow.CURVE_REFOLD;
					}
					// begin a recursive morphology analysis
					new RunProfilingAction(list, flag, MainWindow.this);
				
				}; 
				executorService.execute(task);
			}
						
			if(event.method().equals(DatasetMethod.NEW_MORPHOLOGY)){
				log(Level.INFO, "Running new morphology analysis");
				final int flag = ADD_POPULATION;
				
				Runnable task = () -> { 
					new RunSegmentationAction(list, MorphologyAnalysisMode.NEW, flag, MainWindow.this);
				};
				executorService.execute(task);
			}
			
			if(event.method().equals(DatasetMethod.REFRESH_MORPHOLOGY)){
				log(Level.FINEST, "Updating segmentation across nuclei");
				Runnable task = () -> { 
					new RunSegmentationAction(list, MorphologyAnalysisMode.REFRESH, 0, MainWindow.this);
				};
				executorService.execute(task);
			}
			
			if(event.method().equals(DatasetMethod.COPY_MORPHOLOGY)){
				
				final AnalysisDataset source = event.secondaryDataset();
				Runnable task = () -> { 
					new RunSegmentationAction(event.getDatasets(), source, null, MainWindow.this);
				};
				executorService.execute(task);
			}
			
						
			if(event.method().equals(DatasetMethod.CLUSTER)){
				
				Runnable task = () -> { 
					log(Level.INFO, "Clustering dataset");
					new ClusterAnalysisAction(event.firstDataset(),  MainWindow.this);
				};
				executorService.execute(task);
			
			}
			
			if(event.method().equals(DatasetMethod.BUILD_TREE)){
				Runnable task = () -> { 
					log(Level.INFO, "Building a tree from dataset");
					new BuildHierarchicalTreeAction(event.firstDataset(), MainWindow.this);
				};
				executorService.execute(task);
			}
			
			if(event.method().equals(DatasetMethod.REFOLD_CONSENSUS)){
				
				Runnable task = () -> { 
					log(Level.INFO, "Refolding consensus nucleus");
					refoldConsensus(event.firstDataset());
				};
				executorService.execute(task);			
			}
			
			if(event.method().equals(DatasetMethod.SELECT_DATASETS)){
				
				Runnable task = () -> { 
					populationsPanel.selectDatasets(event.getDatasets());
				};
				executorService.execute(task);						
			}
			
			if(event.method().equals(DatasetMethod.SELECT_ONE_DATASET)){
				
				Runnable task = () -> { 
					populationsPanel.selectDataset(event.firstDataset());
				};
				executorService.execute(task);					
			}
			
			if(event.method().equals(DatasetMethod.SAVE)){
				
				Runnable task = () -> { saveDataset(event.firstDataset());};
				executorService.execute(task);
				
			}
			
			if(event.method().equals(DatasetMethod.EXTRACT_SOURCE)){
				Runnable task = () -> { 
					log(Level.INFO, "Recovering source dataset");
					for(AnalysisDataset d : list){
						d.setRoot(true);
						populationsPanel.addDataset(d);
					}
					populationsPanel.update(list);
				};
				executorService.execute(task);			
			}
			
			if(event.method().equals(DatasetMethod.REFRESH_CACHE)){
				Runnable task = () -> { recacheCharts(list);};
				executorService.execute(task);				
			}
			
			if(event.method().equals(DatasetMethod.CLEAR_CACHE)){
				
				Runnable task = () -> { clearChartCache(list); };
				executorService.execute(task);	
				
			}
			
			if(event.method().equals(DatasetMethod.ADD_DATASET)){
				
				Runnable task = () -> { addDataset(event.firstDataset()); };
				executorService.execute(task);	
			}
		}
		
	}
	
	/**
	 * Add the given dataset and all its children to the 
	 * populations panel
	 * @param dataset
	 */
	private void addDataset(final AnalysisDataset dataset){

		log(Level.FINEST, "Adding dataset");
		dataset.setSwatch(activeSwatch);
		populationsPanel.addDataset(dataset);
		for(AnalysisDataset child : dataset.getAllChildDatasets() ){
			populationsPanel.addDataset(child);
		}

		log(Level.FINEST, "Ordering update of populations panel");
		final List<AnalysisDataset> list = new ArrayList<AnalysisDataset>();
		list.add(dataset);
		populationsPanel.update(list);


	}
	
	
	
	/**
	 * Begin a refolding of the consensus nucleus for the 
	 * given dataset
	 * @param dataset
	 */
	private void refoldConsensus(final AnalysisDataset dataset){
		log(Level.FINE, "Refolding consensus");
		log(Level.FINEST, "Refold consensus dataset method is EDT: "+SwingUtilities.isEventDispatchThread());
		
		executorService.execute(new Runnable() {
			public void run() {
				/*
				 * The refold action needs to be able to hold up a series
				 * of following actions, when it is being used in a New Analysis.
				 * The countdown latch does nothing here, but must be retained for
				 * compatibility.
				 */
				consensusNucleusPanel.clearChartCache();
				
				final CountDownLatch latch = new CountDownLatch(1);
				log(Level.FINEST, "Created latch: "+latch.getCount());
				new RefoldNucleusAction(dataset, MainWindow.this, latch);

				log(Level.FINEST, "Running refolding");
				try {
					latch.await();
					dataset.getAnalysisOptions().setRefoldNucleus(true);
					dataset.getAnalysisOptions().setRefoldMode("Fast");
					
					
					
					log(Level.FINE, "Set refold status in options");
					final List<AnalysisDataset> list = new ArrayList<AnalysisDataset>();
					list.add(dataset);
					segmentsDetailPanel.refreshChartCache(list); // segment positions charts need updating

					log(Level.FINE, "Preparing to select refolded dataset");
					populationsPanel.selectDataset(dataset);
//					log(Level.FINE, "Clearing consensus chart cache for refolded dataset");
//					consensusNucleusPanel.refreshChartCache();
					
					log(Level.FINEST, "Latch counted down: "+latch.getCount());
				} catch (InterruptedException e) {
					log(Level.SEVERE, "Interruption to thread", e);
				}
			}
			
		});
	}
	
	
	
	/**
	 * Save all the root datasets in the populations panel
	 */
	private void saveRootDatasets(){
		executorService.execute(new Runnable() {
			public void run() {
				for(AnalysisDataset root : populationsPanel.getRootDatasets()){
					final CountDownLatch latch = new CountDownLatch(1);

					new SaveDatasetAction(root, MainWindow.this, latch, false);
					try {
						latch.await();
					} catch (InterruptedException e) {
						log(Level.SEVERE, "Interruption to thread", e);
					}
				}
				log(Level.INFO, "All root datasets saved");
			}
			
		});
	}
	
	
	/**
	 * Save the given dataset. If it is root, save directly.
	 * If it is not root, find the root parent and save it.
	 * @param d
	 */	
	private void saveDataset(final AnalysisDataset d){
		
		executorService.execute(new Runnable() {
			public void run() {
				if(d.isRoot()){

					final CountDownLatch latch = new CountDownLatch(1);
					new SaveDatasetAction(d, MainWindow.this, latch, false);
					try {
						log(Level.FINEST, "Awaiting latch for save action");
						latch.await();
					} catch (InterruptedException e) {
						log(Level.SEVERE, "Interruption to thread", e);
					}

					log(Level.FINE, "Root dataset saved");
				} else {

					AnalysisDataset target = null; 
					for(AnalysisDataset root : populationsPanel.getRootDatasets()){

						for(AnalysisDataset child : root.getAllChildDatasets()){
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
						saveDataset(target);
					}
				}
			}

		});
	}
	
	
	/*
	 * Trigger a recache of all charts and datasets 
	 */
	private void recacheCharts(){
		
		Runnable task = () -> {
			for(DetailPanel panel : detailPanels){
				panel.refreshChartCache();
				panel.refreshTableCache();
			}
		};
		executorService.execute(task);
	}
	
	private void clearChartCache(){
		for(DetailPanel panel : detailPanels){
			panel.clearChartCache();
			panel.clearTableCache();
		}	
	}
	
	private void clearChartCache(final CountDownLatch latch){
		
		for(DetailPanel panel : detailPanels){
			panel.clearChartCache();
			panel.clearTableCache();
		}
		latch.countDown();
		
	}
	
	private void clearChartCache(final List<AnalysisDataset> list){
		
		if(list==null || list.isEmpty()){
			log(Level.WARNING, "A cache clear was requested for a specific list, which was null or empty");
			clearChartCache();
			return;
		}
		for(DetailPanel panel : detailPanels){
			panel.clearChartCache(list);
			panel.clearTableCache(list);
		}		
	}
	
	private void recacheCharts(final AnalysisDataset dataset){
		final List<AnalysisDataset> list = new ArrayList<AnalysisDataset>();
		list.add(dataset);
		recacheCharts(list);
	}
	
	private void recacheCharts(final List<AnalysisDataset> list){
		
		Runnable task = () -> {
			for(DetailPanel panel : detailPanels){
				panel.refreshChartCache(list);
				panel.refreshTableCache(list);
			}
		};
		executorService.execute(task);

	}
	
	private void resegmentDatasets(){
		
		Runnable task = () -> {
			final int flag = CURVE_REFOLD; // ensure consensus is replaced
			// Recalculate the head and hump positions for rodent sperm
			if(populationsPanel.getSelectedDatasets().get(0).getCollection().getNucleusType().equals(NucleusType.RODENT_SPERM)){

				try{
					log(Level.INFO, "Replacing nucleus roi patterns");
					for( Nucleus n : populationsPanel.getSelectedDatasets().get(0).getCollection().getNuclei()){

						RodentSpermNucleus r = (RodentSpermNucleus) n;  

						r.splitNucleusToHeadAndHump();
						try {

							r.calculateSignalAnglesFromPoint(r.getPoint(BorderTag.ORIENTATION_POINT));
						} catch (Exception e) {
							log(Level.SEVERE, "Error restoring signal angles", e);
						}

					}

				}catch(Exception e){
					log(Level.SEVERE, "Error recalculating angles", e);
				}
			}
			
			log(Level.INFO, "Regenerating charts");
			for(DetailPanel panel : detailPanels){
				panel.refreshChartCache();
				panel.refreshTableCache();
			}
			
			log(Level.INFO, "Resegmenting datasets");
			List<AnalysisDataset> list = populationsPanel.getSelectedDatasets();
			new RunSegmentationAction(list, MorphologyAnalysisMode.NEW, flag, MainWindow.this);
		};
		executorService.execute(task);
	}

	
	
	@Override
	public void interfaceEventReceived(InterfaceEvent event) {
		
		InterfaceMethod method = event.method();
		log(Level.FINEST, "Heard interface event: "+event.method().toString());
		
		switch(method){
		
		case REFRESH_POPULATIONS:
//			this.populationsPanel.refreshDatasets();
			populationsPanel.update(populationsPanel.getSelectedDatasets()); // ensure all child datasets are included
			break;
			
		case SAVE_ROOT:
			saveRootDatasets(); // DO NOT WRAP IN A SEPARATE THREAD, IT WILL LOCK THE PROGRESS BAR

			break;
			
		case UPDATE_PANELS:
//			populationsPanel.refreshDatasets();
//			populationsPanel.update(populationsPanel.getSelectedDatasets()); // ensure all child datasets are included
			this.updatePanels(populationsPanel.getSelectedDatasets());
			break;
			
			
		case RECACHE_CHARTS:
			recacheCharts();
			break;
		case LIST_DATASETS:
			int i=0;
			for(AnalysisDataset d : populationsPanel.getAllDatasets()){
				log(Level.INFO, i+"\t"+d.getName());
				i++;
			}
			break;
		case RESEGMENT_SELECTED_DATASET:
			resegmentDatasets();
			break;
				
		case LIST_SELECTED_DATASETS:
			int count=0;
			for(AnalysisDataset d : populationsPanel.getSelectedDatasets()){
				log(Level.INFO, count+"\t"+d.getName());
				count++;
			}
			break;
			
		case CLEAR_LOG_WINDOW:
			logPanel.clear();
			break;
			
		case UPDATE_IN_PROGRESS:
			for(DetailPanel panel : this.detailPanels){
				panel.setAnalysing(true);
			}
			this.setCursor(new Cursor(Cursor.WAIT_CURSOR));
			break;
			
		case UPDATE_COMPLETE:
			for(DetailPanel panel : this.detailPanels){
				panel.setAnalysing(false);
			}
			this.setCursor(new Cursor(Cursor.DEFAULT_CURSOR));
			break;
			
			
		case DUMP_LOG_INFO:
			for(AnalysisDataset d : populationsPanel.getSelectedDatasets()){
				
				for(Nucleus n : d.getCollection().getNuclei()){
					log(Level.INFO, n.printLog());
				}
			}
			break;
			
		default:
			break;

		}		
	}	
	
	@Override
	public void dispose(){
		executorService.shutdownNow();
		super.dispose();
	}
	
//	@SuppressWarnings("serial")
//	class GlassPane extends JPanel implements ItemListener {
//        
//    	public GlassPane(){
//    		JLabel l = new JLabel();
//            l.setText("Hello");
//            l.setBorder(new LineBorder(Color.BLACK, 1));
//            l.setBounds(10, 10, 50, 20);
//            l.setBackground(Color.RED);
//            l.setOpaque(true);
//            l.setPreferredSize(l.getSize());
//           add(l);
//    	}
//    	
//        //React to change button clicks.
//        public void itemStateChanged(ItemEvent e) {
//            setVisible(e.getStateChange() == ItemEvent.SELECTED);
//        }
//
//    }
}
