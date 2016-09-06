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
import gui.components.panels.MeasurementUnitSettingsPanel;
import gui.dialogs.CellCollectionOverviewDialog;
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
import gui.tabs.populations.PopulationsPanel;
import io.MappingFileExporter;

import java.awt.BorderLayout;
import java.awt.Cursor;
import java.awt.Dimension;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.datatransfer.DataFlavor;
import java.awt.datatransfer.Transferable;
import java.awt.datatransfer.UnsupportedFlavorException;
import java.awt.dnd.DnDConstants;
import java.awt.dnd.DropTarget;
import java.awt.dnd.DropTargetDropEvent;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.awt.event.WindowStateListener;
import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.concurrent.CountDownLatch;
import java.util.logging.Level;
import java.util.logging.Logger;

import javax.swing.JButton;
import javax.swing.JFrame;
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
import analysis.profiles.DatasetSegmenter.MorphologyAnalysisMode;
import components.generic.BorderTagObject;
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
	
	
	private static final DatasetListManager datasetManager = DatasetListManager.getInstance(); 
	
	private static final GlobalOptions globalOptions = GlobalOptions.getInstance();
	
	
	private static final ThreadManager threadManager = ThreadManager.getInstance();	
	
	
	/**
	 * Create the frame.
	 */
	public MainWindow() {
				
		this.setDefaultCloseOperation(DO_NOTHING_ON_CLOSE);
		
		this.addWindowListener(new WindowAdapter() {
			
			public void windowClosing(WindowEvent e) {
				fine("Checking dataset state");

				if(datasetManager.hashCodeChanged()){
					fine("Found changed hashcode");
					Object[] options = { "Save datasets" , "Exit without saving", "Cancel exit" };
					int save = JOptionPane.showOptionDialog(MainWindow.this,
							"Datasets have changed since last save!", 
							"Save datasets?",
							JOptionPane.DEFAULT_OPTION, 
							JOptionPane.QUESTION_MESSAGE,
							null, options, options[0]);

					if(save==0){
						saveRootDatasets();

					} 
					
					if(save==1){
						fine("Exiting without save");
						close();					
					} 
					
					if(save==2){
						fine("Ignoring close");
					}
				} else {
					fine("No change found");
					close();
				}
			}
			
			public void close(){
				datasetManager.clear();
				globalOptions.setDefaults();
				dispose();
			}
			
			  public void windowClosed(WindowEvent e) {
				  close();
			  }
			  
			  
			  
			  

		});
		
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

					  for(DetailPanel d : detailPanels){
						  d.updateSize();
					  }
				  };
				  ThreadManager.getInstance().submit(r);

			  }
			});
		
		
		this.setDropTarget(new DropTarget(){
			
			@Override
            public synchronized void drop(DropTargetDropEvent dtde) {
				
				
				try {
					dtde.acceptDrop(DnDConstants.ACTION_COPY_OR_MOVE);
					Transferable t = dtde.getTransferable();
					
					List<File> fileList = new ArrayList<File>();
					
					// Check that what was provided is a list
					if(t.getTransferData(DataFlavor.javaFileListFlavor) instanceof List<?>){
						
						// Check that what is in the list is files
						List<?> tempList = (List<?>) t.getTransferData(DataFlavor.javaFileListFlavor);
						for(Object o : tempList){
							
							if(o instanceof File){
								fileList.add( (File) o);
							}
						}
						
						// Open the files - we process only *.nmd files

						for(File f : fileList){
							if(f.getName().endsWith(Constants.SAVE_FILE_EXTENSION)){
								finer("Opening file "+f.getAbsolutePath());

								Runnable task = () -> { 
									new PopulationImportAction(MainWindow.this, f);
								};
								threadManager.execute(task);


							} else {
								finer("File is not nmd, ignoring");
							}

						}
					}
					
				} catch (UnsupportedFlavorException e) {
					error("Error in DnD", e);
				} catch (IOException e) {
					error("IO error in DnD", e);
				}
               
            }
			
		});
		
		
		try {
			setTitle("Nuclear Morphology Analysis v"+Version.currentVersion().toString());
			
			Dimension preferredSize = new Dimension(1012, 804);
			this.setPreferredSize(preferredSize);
			setBounds(100, 100, 1012, 804);
//			this.setLocationRelativeTo(null); // centre on screen
			contentPane = new JPanel();
			contentPane.setBorder(new EmptyBorder(5, 5, 5, 5));
			contentPane.setLayout(new BorderLayout(0, 0));
			setContentPane(contentPane);
			
			//---------------
			// Create the header buttons
			//---------------
			contentPane.add(createHeaderButtons(), BorderLayout.NORTH);
		
			
			//---------------
			// Create the log panel
			//---------------
			logPanel = new LogPanel();
			logPanel.addDatasetEventListener(this);
			logPanel.addInterfaceEventListener(this);
			logPanel.addSignalChangeListener(this);
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
			
			this.pack();
			consensusNucleusPanel.restoreAutoBounds();

		} catch (Exception e) {
			logToImageJ("Error initialising Main: "+e.getMessage(), e);
		}
		
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
				finest("Creating import action");
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
					log("Saving root populations...");

//					Thread thr = new Thread(){
//						public void run(){
					saveRootDatasets();
//						}};
//						thr.start();
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
				
		JButton optionsButton = new JButton("Options");
		optionsButton.addActionListener(
				
				e -> { 

					MainOptionsDialog dialog = new MainOptionsDialog(MainWindow.this);
					dialog.addInterfaceEventListener(this);
			}
		);		
		panelHeader.add(optionsButton);
		
		MeasurementUnitSettingsPanel unitsPanel = new MeasurementUnitSettingsPanel();
		unitsPanel.addInterfaceEventListener(this);
		panelHeader.add(unitsPanel);
		
		return panelHeader;
	}
		
			
	/**
	 * Update the display panels with information from the given datasets
	 * @param list the datasets to display
	 */
	private void updatePanels(final List<AnalysisDataset> list){
		if(list!=null){
			fine("Updating tab panels for "+list.size()+" datasets");
		} else {
			fine("Updating tab panels with null datasets");
		}
		
		Runnable task = () -> {
			try {

				populationsPanel.repaintTreeTable();
				for(DetailPanel panel : MainWindow.this.detailPanels){
					panel.update(list);
				}

				fine("Updated tab panels");

			} catch (Exception e) {
				error("Error updating panels", e);
			}
		};
		
		threadManager.execute(task);
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

					error("Error checking update state", e);

				}
			}

		};
		thr.start();
	}
	

			
	@Override
	public void signalChangeReceived(SignalChangeEvent event) {
		
		finest("Heard signal change event: "+event.type());
		
		final AnalysisDataset selectedDataset = populationsPanel.getSelectedDatasets().isEmpty() 
				? null 
				: populationsPanel.getSelectedDatasets().get(0);
		
		
		if(event.type().equals("RunShellAnalysis")){
			Runnable task = () -> {
				log(Level.FINER, "Shell analysis selected");
				new ShellAnalysisAction(selectedDataset, MainWindow.this);
			};
			threadManager.execute(task);
		}

		if(event.type().equals("MergeCollectionAction")){
			
			Runnable task = () -> { 
				new MergeCollectionAction(populationsPanel.getSelectedDatasets(), MainWindow.this); 
			}; 
			threadManager.execute(task);
		}
		
		if(event.type().equals("DatasetArithmeticAction")){
			
			Runnable task = () -> { 
				new DatasetArithmeticAction(populationsPanel.getSelectedDatasets(), MainWindow.this); 
			}; 
			threadManager.execute(task);
		}

		
		if(event.type().equals("CurateCollectionAction")){

				CellCollectionOverviewDialog d = new CellCollectionOverviewDialog(populationsPanel.getSelectedDatasets().get(0));
				d.addDatasetEventListener(this);

//			Runnable task = () -> { 
//				new CurateCollectionAction(selectedDataset, MainWindow.this); 
//			}; 
//			threadManager.execute(task);
		}
				

		
		if(event.type().equals("ChangeNucleusFolderAction")){
			new ReplaceSourceImageDirectoryAction(selectedDataset, MainWindow.this);
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
			new RelocateFromFileAction(selectedDataset, MainWindow.this, latch);			
			
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
				
		if(event.type().equals("SaveCollectionAction")){
			
			this.saveDataset(selectedDataset, true);
		}
		
		
		if(event.type().startsWith("Open|")){
			String s = event.type().replace("Open|", "");
			File f = new File(s);
			
			Runnable task = () -> { 
				new PopulationImportAction(this, f);
			};
			threadManager.execute(task);
		}
				
	}

	@Override
	public void datasetEventReceived(final DatasetEvent event) {
		finest("Heard dataset event: "+event.method().toString());
		final List<AnalysisDataset> list = event.getDatasets();
		if(!list.isEmpty()){
			
			if(event.method().equals(DatasetMethod.PROFILING_ACTION)){
				fine("Running new profiling and segmentation");
				
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
				threadManager.execute(task);
			}
						
			if(event.method().equals(DatasetMethod.NEW_MORPHOLOGY)){
				log("Running new morphology analysis");
				final int flag = ADD_POPULATION;
				
				Runnable task = () -> { 
					new RunSegmentationAction(list, MorphologyAnalysisMode.NEW, flag, MainWindow.this);
				};
				threadManager.execute(task);
			}
			
			if(event.method().equals(DatasetMethod.REFRESH_MORPHOLOGY)){
				finer("Updating segmentation across nuclei");
				Runnable task = () -> { 
					new RunSegmentationAction(list, MorphologyAnalysisMode.REFRESH, 0, MainWindow.this);
				};
				threadManager.execute(task);
			}
			
			if(event.method().equals(DatasetMethod.COPY_MORPHOLOGY)){
				
				final AnalysisDataset source = event.secondaryDataset();
				Runnable task = () -> { 
					new RunSegmentationAction(event.getDatasets(), source, null, MainWindow.this);
				};
				threadManager.execute(task);
			}
			
						
			if(event.method().equals(DatasetMethod.CLUSTER)){
				
				Runnable task = () -> { 
					log(Level.INFO, "Clustering dataset");
					new ClusterAnalysisAction(event.firstDataset(),  MainWindow.this);
				};
				threadManager.execute(task);
			
			}
			
			if(event.method().equals(DatasetMethod.BUILD_TREE)){
				Runnable task = () -> { 
					log(Level.INFO, "Building a tree from dataset");
					new BuildHierarchicalTreeAction(event.firstDataset(), MainWindow.this);
				};
				threadManager.execute(task);
			}
			
			if(event.method().equals(DatasetMethod.REFOLD_CONSENSUS)){
				log("Refolding consensus nucleus");
				refoldConsensus(event.firstDataset());		
			}
			
			if(event.method().equals(DatasetMethod.SELECT_DATASETS)){
				populationsPanel.selectDatasets(event.getDatasets());
			}
			
			if(event.method().equals(DatasetMethod.SELECT_ONE_DATASET)){
				populationsPanel.selectDataset(event.firstDataset());				
			}
			
			if(event.method().equals(DatasetMethod.SAVE)){
				saveDataset(event.firstDataset(), false);
			}
			
			if(event.method().equals(DatasetMethod.EXTRACT_SOURCE)){
				Runnable task = () -> { 
					log("Recovering source dataset");
					for(AnalysisDataset d : list){
						d.setRoot(true);
						populationsPanel.addDataset(d);
					}
					populationsPanel.update(list);
				};
				threadManager.execute(task);			
			}
			
			if(event.method().equals(DatasetMethod.REFRESH_CACHE)){
				recacheCharts(list);				
			}
			
			if(event.method().equals(DatasetMethod.CLEAR_CACHE)){
				
				clearChartCache(list);
				
			}
			
			if(event.method().equals(DatasetMethod.ADD_DATASET)){
				addDataset(event.firstDataset());
			}
			
			if(event.method().equals(DatasetMethod.RECALCULATE_MEDIAN)){
				fine("Recalculating the median for the given datasets");
				
				Runnable task = () -> { 
					int flag = 0; // set the downstream analyses to run
					
					new RunProfilingAction(list, flag, MainWindow.this);
				
				}; 
				threadManager.execute(task);
			}
			
		}
		
	}
	
	/**
	 * Add the given dataset and all its children to the 
	 * populations panel
	 * @param dataset
	 */
	private void addDataset(final AnalysisDataset dataset){

		fine("Adding dataset");

		populationsPanel.addDataset(dataset);
		for(AnalysisDataset child : dataset.getAllChildDatasets() ){
			populationsPanel.addDataset(child);
		}

		finer("Ordering update of populations panel");
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
			
			final List<AnalysisDataset> list = new ArrayList<AnalysisDataset>();
			list.add(dataset);
			segmentsDetailPanel.clearChartCache(list);  // segment positions charts need updating
			nuclearBoxplotsPanel.clearChartCache(list); // overlaid nuclei need updating
			signalsDetailPanel.clearChartCache(list);   // signal consensus needs updating
			consensusNucleusPanel.clearChartCache(list);    // consensus panel needs updating

			final CountDownLatch latch = new CountDownLatch(1);
			finest("Created latch: "+latch.getCount());
			new RefoldNucleusAction(dataset, MainWindow.this, latch);

			finest("Running refolding");
			try {
				fine("Awaiting latch");
				latch.await();
				fine("Latch has released from refolding");
				if(dataset.hasAnalysisOptions()){
					dataset.getAnalysisOptions().setRefoldNucleus(true);
					dataset.getAnalysisOptions().setRefoldMode("Fast");
					fine("Set refold status in options");
				} else {
					fine("Dataset has no analysis options, cannot set refold state");
				}
				


//				fine("Preparing to select refolded dataset");
				populationsPanel.selectDataset(dataset);
				//					log(Level.FINE, "Clearing consensus chart cache for refolded dataset");
				//					consensusNucleusPanel.refreshChartCache();

			} catch (InterruptedException e) {
				error("Interruption to thread", e);
			}
		};
			
		threadManager.execute(r);
	}
	
	
	
	/**
	 * Save all the root datasets in the populations panel
	 */
	private void saveRootDatasets(){
		
		Runnable r = () -> {
			for(AnalysisDataset root : DatasetListManager.getInstance().getRootDatasets()){
				final CountDownLatch latch = new CountDownLatch(1);

				new SaveDatasetAction(root, MainWindow.this, latch, false);
				try {
					latch.await();
				} catch (InterruptedException e) {
					error("Interruption to thread", e);
				}
			}
			log("All root datasets saved");
		};
			
		threadManager.execute(r);
	}
	
	
	/**
	 * Save the given dataset. If it is root, save directly.
	 * If it is not root, find the root parent and save it.
	 * @param d
	 * @param saveAs should the action ask for a directory
	 */	
	public void saveDataset(final AnalysisDataset d, boolean saveAs){
		
		if(d.isRoot()){
			finer("Dataset is root");
			finest("Creating latch");
			final CountDownLatch latch = new CountDownLatch(1);

			Runnable r = () -> {
				finest("Running save action");
				new SaveDatasetAction(d, MainWindow.this, latch, saveAs);
			};
			finest("Passing save action to executor service");
			threadManager.submit(r);

			fine("Root dataset saved");
		} else {
			finest("Not a root dataset, checking for parent");
			AnalysisDataset target = null; 
			for(AnalysisDataset root : DatasetListManager.getInstance().getRootDatasets()){
				//					for(AnalysisDataset root : populationsPanel.getRootDatasets()){

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
				saveDataset(target, saveAs);
			}
		}
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
		threadManager.execute(task);
	}
	
	private void clearChartCache(){
		for(DetailPanel panel : detailPanels){
			panel.clearChartCache();
			panel.clearTableCache();
		}	
	}
	
//	private void clearChartCache(final CountDownLatch latch){
//		
//		for(DetailPanel panel : detailPanels){
//			panel.clearChartCache();
//			panel.clearTableCache();
//		}
//		latch.countDown();
//		
//	}
	
	private void clearChartCache(final List<AnalysisDataset> list){
		
		if(list==null || list.isEmpty()){
			warn("A cache clear was requested for a specific list, which was null or empty");
			clearChartCache();
			return;
		}
		for(DetailPanel panel : detailPanels){
			panel.clearChartCache(list);
			panel.clearTableCache(list);
		}		
	}
	
//	private void recacheCharts(final AnalysisDataset dataset){
//		final List<AnalysisDataset> list = new ArrayList<AnalysisDataset>();
//		list.add(dataset);
//		recacheCharts(list);
//	}
	
	private void recacheCharts(final List<AnalysisDataset> list){
		
		Runnable task = () -> {
			finer("Heard recache request for list of  "+list.size()+" datasets");
			for(DetailPanel panel : detailPanels){
				panel.refreshChartCache(list);
				panel.refreshTableCache(list);
			}
		};
		threadManager.submit(task);

	}
	
	private void resegmentDatasets(){
		
		Runnable task = () -> {
			final int flag = CURVE_REFOLD; // ensure consensus is replaced
			// Recalculate the head and hump positions for rodent sperm
			if(populationsPanel.getSelectedDatasets().get(0).getCollection().getNucleusType().equals(NucleusType.RODENT_SPERM)){

				try{
					fine("Replacing nucleus roi patterns");
					for( Nucleus n : populationsPanel.getSelectedDatasets().get(0).getCollection().getNuclei()){

						RodentSpermNucleus r = (RodentSpermNucleus) n;  

						r.splitNucleusToHeadAndHump();
						try {

							r.calculateSignalAnglesFromPoint(r.getPoint(BorderTagObject.ORIENTATION_POINT));
						} catch (Exception e) {
							error("Error restoring signal angles", e);
						}

					}

				}catch(Exception e){
					error("Error recalculating angles", e);
				}
			}
			
			fine("Regenerating charts");
			for(DetailPanel panel : detailPanels){
				panel.refreshChartCache();
				panel.refreshTableCache();
			}
			
			fine("Resegmenting datasets");
			List<AnalysisDataset> list = populationsPanel.getSelectedDatasets();
			new RunSegmentationAction(list, MorphologyAnalysisMode.NEW, flag, MainWindow.this);
		};
		threadManager.execute(task);
	}

	
	
	@Override
	public void interfaceEventReceived(InterfaceEvent event) {
		
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
			List<AnalysisDataset> list = populationsPanel.getSelectedDatasets();
			finer("Updating tab panels with list of "+list.size()+" datasets");
			this.updatePanels(list);
			break;
		}
			
			
		case RECACHE_CHARTS:
			recacheCharts();
			break;
		case LIST_DATASETS:
			int i=0;
			for(AnalysisDataset d : DatasetListManager.getInstance().getAllDatasets()){
				log(i+"\t"+d.getName());
				i++;
			}
			break;
		case RESEGMENT_SELECTED_DATASET:
			resegmentDatasets();
			break;
				
		case LIST_SELECTED_DATASETS:
			int count=0;
			for(AnalysisDataset d : populationsPanel.getSelectedDatasets()){
				log(count+"\t"+d.getName());
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
					log(n.toString());
				}
			}
			break;
			
		case INFO:
			for(AnalysisDataset d : populationsPanel.getSelectedDatasets()){
				
				log(d.getCollection().toString());
			}
			break;
			
		case KILL_ALL_TASKS:
			killAllTasks();
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
	
//	public List<AnalysisDataset> getOpenDatasets(){
//		return DatasetListManager.getInstance().getAllDatasets();
//	}
	
	public boolean hasOpenDatasets(){
		return DatasetListManager.getInstance().getAllDatasets().size()>0;
	}
	
	private void killAllTasks(){
//		threadManager.shutdownNow();
		
//		warn("Found "+logPanel.getProgressBars().size()+" active bars");
//		for(JProgressBar bar : logPanel.getProgressBars()){
//			logPanel.remove(bar);
//		}
//		warn("Killed all running tasks");
		
		log("Threads running in the JVM:");
		Set<Thread> threadSet = Thread.getAllStackTraces().keySet();
		for(Thread t : threadSet){
			log("Thread "+t.getId()+": "+t.getState());
		}
		
	}

}
