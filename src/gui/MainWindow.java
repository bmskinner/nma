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
import gui.actions.ReplaceSourceImageDirectoryAction;
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
import gui.tabs.NuclearBoxplotsPanel;
import gui.tabs.NucleusProfilesPanel;
import gui.tabs.SegmentsDetailPanel;
import gui.tabs.SignalsDetailPanel;
import ij.IJ;

import java.awt.BorderLayout;
import java.awt.Cursor;
import java.awt.Dimension;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CountDownLatch;
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
import logging.TextAreaHandler;
import utility.Constants;
import analysis.AnalysisDataset;
import analysis.nucleus.DatasetSegmenter.MorphologyAnalysisMode;
import components.generic.BorderTag;
import components.nuclear.NucleusType;
import components.nuclei.Nucleus;
import components.nuclei.sperm.RodentSpermNucleus;

public class MainWindow extends JFrame implements SignalChangeListener, DatasetEventListener, InterfaceEventListener {
				
	private static final long serialVersionUID = 1L;
	private JPanel contentPane;

	private JLabel lblStatusLine = new JLabel("No analysis open");
	
	private LogPanel				logPanel;				// progress and messages
	private PopulationsPanel 		populationsPanel; 		// holds and selects open datasets
	private ConsensusNucleusPanel	consensusNucleusPanel; 	// show refolded nuclei if present
	
	private JTabbedPane 			tabbedPane;				// bottom panel tabs. Contains:
	
	private NucleusProfilesPanel 	nucleusProfilesPanel; 	// the angle profiles
	private AnalysisDetailPanel		analysisDetailPanel;	// nucleus detection parameters and stats
	private SignalsDetailPanel 		signalsDetailPanel;		// nuclear signals
	private NuclearBoxplotsPanel 	nuclearBoxplotsPanel;	// nuclear stats - areas, perimeters etc
	private SegmentsDetailPanel 	segmentsDetailPanel;	// segmented profiles
	private ClusterDetailPanel		clusterDetailPanel;		// clustering within populations
	private MergesDetailPanel		mergesDetailPanel;		// merges between populations
	private InterDatasetComparisonDetailPanel interdatasetDetailPanel;
	private EditingDetailPanel		editingDetailPanel; 	// for altering data
	
	private List<DetailPanel> detailPanels = new ArrayList<DetailPanel>(); // store panels for iterating messsages
	
	private ColourSwatch activeSwatch = ColourSwatch.REGULAR_SWATCH;
		
	// Flags to pass to ProgressableActions to determine the analyses
	// to carry out in subsequently
	public static final int ADD_POPULATION		 = 1;
	public static final int STATS_EXPORT 		 = 2;
	public static final int NUCLEUS_ANNOTATE	 = 4;
	public static final int CURVE_REFOLD 		 = 8;
	public static final int EXPORT_COMPOSITE	 = 16;
	public static final int SAVE_DATASET		 = 32;
	public static final int ASSIGN_SEGMENTS		 = 64;
	
	private static final Logger programLogger =
	        Logger.getLogger(MainWindow.class.getName()); // the program logger will report status and errors in the running of the program, not involving datasets 
			
	/**
	 * Create the frame.
	 */
	public MainWindow() {
		
		this.setDefaultCloseOperation(DISPOSE_ON_CLOSE);
		try {
			setTitle("Nuclear Morphology Analysis v"+getVersion());
			setBounds(100, 100, 1012, 604);
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
			// Create the consensus chart
			//---------------
			populationsPanel = new PopulationsPanel(programLogger);
			populationsPanel.addSignalChangeListener(this);
			populationsPanel.addDatasetEventListener(this);
			
			consensusNucleusPanel = new ConsensusNucleusPanel(programLogger);
			detailPanels.add(consensusNucleusPanel);

			
			//---------------
			// Create the log panel
			//---------------
			logPanel = new LogPanel(programLogger);
			logPanel.addDatasetEventListener(this);
			logPanel.addInterfaceEventListener(this);
			TextAreaHandler textHandler = new TextAreaHandler(logPanel);
			textHandler.setFormatter(new LogPanelFormatter());
			programLogger.addHandler(textHandler);
			programLogger.setLevel(Level.INFO); // by default do not log everything 
			
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
			nucleusProfilesPanel = new NucleusProfilesPanel(programLogger);
			detailPanels.add(nucleusProfilesPanel);
			tabbedPane.addTab("Profiles", null, nucleusProfilesPanel, null);

			//---------------
			// Create the general stats page
			//---------------
			analysisDetailPanel = new AnalysisDetailPanel(programLogger);
			detailPanels.add(analysisDetailPanel);
			tabbedPane.addTab("Analysis info", analysisDetailPanel);

			//---------------
			// Create panel for split boxplots
			//---------------
			nuclearBoxplotsPanel  = new NuclearBoxplotsPanel(programLogger);
			detailPanels.add(nuclearBoxplotsPanel);
			tabbedPane.addTab("Nuclear charts", nuclearBoxplotsPanel);
				
			
			//---------------
			// Create the signals tab panel
			//---------------
			signalsDetailPanel  = new SignalsDetailPanel(programLogger);
			detailPanels.add(signalsDetailPanel);
			tabbedPane.addTab("Signals", signalsDetailPanel);
			

			//---------------
			// Create the clusters panel
			//---------------
			clusterDetailPanel = new ClusterDetailPanel(programLogger);
			detailPanels.add(clusterDetailPanel);
			tabbedPane.addTab("Clusters", clusterDetailPanel);
			
			//---------------
			// Create the merges panel
			//---------------
			mergesDetailPanel = new MergesDetailPanel(programLogger);
			detailPanels.add(mergesDetailPanel);
			tabbedPane.addTab("Merges", mergesDetailPanel);

			//---------------
			// Create the segments boxplot panel
			//---------------
			segmentsDetailPanel = new SegmentsDetailPanel(programLogger);
			detailPanels.add(segmentsDetailPanel);
			tabbedPane.addTab("Segments", null, segmentsDetailPanel, null);

			
			//---------------
			// Create the inter-dataset panel
			//---------------
			interdatasetDetailPanel = new InterDatasetComparisonDetailPanel(programLogger);
			detailPanels.add(interdatasetDetailPanel);
			tabbedPane.addTab("Inter-dataset comparisons", null, interdatasetDetailPanel, null);
			
			
			editingDetailPanel = new EditingDetailPanel(programLogger);
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
			IJ.log("Error initialising Main: "+e.getMessage());
			for(StackTraceElement el : e.getStackTrace()){
				IJ.log(el.toString());
			}
		}
		
	}
	
	public Logger getProgramLogger(){
		return MainWindow.programLogger;
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
		btnNewAnalysis.addMouseListener(new MouseAdapter() {
			@Override
			public void mouseClicked(MouseEvent arg0) {
				
				new NewAnalysisAction(MainWindow.this);

			}
		});
		panelHeader.add(btnNewAnalysis);

		//---------------
		// load saved dataset button
		//---------------

		JButton btnLoadSavedDataset = new JButton("Load analysis dataset");
		btnLoadSavedDataset.addMouseListener(new MouseAdapter() {
			@Override
			public void mouseClicked(MouseEvent arg0) {
//				
				SwingUtilities.invokeLater(new Runnable(){
					public void run(){
					
						new PopulationImportAction(MainWindow.this);

				}});
						
				
				
			}
		});
		panelHeader.add(btnLoadSavedDataset);

		//---------------
		// save button
		//---------------

		JButton btnSavePopulation = new JButton("Save all");
		btnSavePopulation.addMouseListener(new MouseAdapter() {
			@Override
			public void mouseClicked(MouseEvent arg0) {
				programLogger.log(Level.INFO, "Saving root populations...");
				
				Thread thr = new Thread(){
					public void run(){
						saveRootDatasets();
					}};
				thr.start();
			}
		});
		panelHeader.add(btnSavePopulation);

		//---------------
		// FISH mapping button
		//---------------

		JButton btnPostanalysisMapping = new JButton("Post-FISH mapping");
		btnPostanalysisMapping.addMouseListener(new MouseAdapter() {
			@Override
			public void mouseClicked(MouseEvent arg0) {
//				fishMapping();
				new FishRemappingAction(populationsPanel.getSelectedDatasets(), MainWindow.this);
			}
		});
		panelHeader.add(btnPostanalysisMapping);
				
		JButton btnSetLogLevel = new JButton("Options");
		btnSetLogLevel.addMouseListener(new MouseAdapter() {
			@Override
			public void mouseClicked(MouseEvent arg0) {

				MainOptionsDialog dialog = new MainOptionsDialog(MainWindow.this);
				if(dialog.isReadyToRun()){
					for(DetailPanel panel : MainWindow.this.detailPanels){
						panel.refreshChartCache();
						panel.refreshTableCache();
					}

                    updatePanels(populationsPanel.getSelectedDatasets());
                    
				}
			}
		});		
		panelHeader.add(btnSetLogLevel);
		return panelHeader;
	}
	
	/**
	 * Get the program version
	 * @return the version
	 */
	public String getVersion(){
		return Constants.VERSION_MAJOR+"."+Constants.VERSION_REVISION+"."+Constants.VERSION_BUGFIX;
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
	public void updatePanels(final List<AnalysisDataset> list){
		programLogger.log(Level.FINE, "Updating tab panels");
		Thread thr = new Thread() {
			public void run() {
				try {
					
					for(DetailPanel panel : MainWindow.this.detailPanels){
						panel.update(list);
					}
					
					programLogger.log(Level.FINE, "Updated tab panels");

				} catch (Exception e) {
					programLogger.log(Level.SEVERE,"Error updating panels", e);
				}
			}
		};
		thr.start();
	}
	
	
	volatile private boolean isRunning = false;
	
	private synchronized boolean checkRunning(){
		if (isRunning) 
			return true;
		else 
			return false;
	}
	
	public synchronized void checkUpdatingState(){
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

					IJ.log("Error checking update state: "+e.getMessage());
					for(StackTraceElement el : e.getStackTrace()){
						IJ.log(el.toString());
					}
				}
			}

		};
		thr.start();
	}

//	class ExportDatasetStatsAction extends AbstractAction {
//
//		private static final long serialVersionUID = 1L;
//		public ExportDatasetStatsAction() {
//			super("Export stats");
//		}
//		// note - this will overwrite the stats for any collection with the same name in the output folder
//		public void actionPerformed(ActionEvent e) {
//
//			final List<AnalysisDataset> datasets = populationsPanel.getSelectedDatasets();
//
//			if(datasets.size()==1){
//
//				Thread thr = new Thread() {
//					public void run() {
//
//						AnalysisDataset d = datasets.get(0);
//						try{
//
//							programLogger.log(Level.INFO, "Exporting stats...");
//							boolean ok = StatsExporter.run(d);
//							if(ok){
//								programLogger.log(Level.INFO, "OK");
//							} else {
//								programLogger.log(Level.INFO, "Error");
//							}
//						} catch(Exception e1){
//							programLogger.log(Level.SEVERE, "Error in stats export", e1);
//						}
//					}
//				};
//				thr.run();
//			}
//
//		}
//	}
			
	@Override
	public void signalChangeReceived(SignalChangeEvent event) {
		
		programLogger.log(Level.FINEST, "Heard signal change event: "+event.type());
		
		final AnalysisDataset selectedDataset = populationsPanel.getSelectedDatasets().isEmpty() 
				? null 
				: populationsPanel.getSelectedDatasets().get(0);
		
		
		if(event.type().equals("RunShellAnalysis")){
			programLogger.log(Level.FINER, "Shell analysis selected");
			new ShellAnalysisAction(selectedDataset, this);
		}

		if(event.type().equals("MergeCollectionAction")){
			
			Thread thr = new Thread() {
				public void run() {

					new MergeCollectionAction(populationsPanel.getSelectedDatasets(), MainWindow.this);
				}
			};

			thr.start();
			
		}
		
		if(event.type().equals("DatasetArithmeticAction")){
			new DatasetArithmeticAction(selectedDataset, populationsPanel.getAllDatasets(), MainWindow.this);
		}
		
		if(event.type().equals("SaveCollectionAction")){
			CountDownLatch latch = new CountDownLatch(1);
			new SaveDatasetAction(selectedDataset, MainWindow.this, latch, true);
		}
		
		
		
		if(event.type().equals("CurateCollectionAction")){
			
			new CurateCollectionAction(selectedDataset, MainWindow.this);		
			
		}
				

		
		if(event.type().equals("ChangeNucleusFolderAction")){
			new ReplaceSourceImageDirectoryAction(selectedDataset, MainWindow.this);
		}
		
		if(event.type().equals("ExportDatasetStatsAction")){
			//TODO: Replace this with more robust action
//			new ExportDatasetStatsAction();
			programLogger.log(Level.WARNING, "Function disabled");
		}
		
		if(event.type().equals("ReapplySegmentProfileAction")){
			
			SwingUtilities.invokeLater(new Runnable(){
				public void run(){
				
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

							new RunSegmentationAction(selectedDataset, source, null, MainWindow.this);
						}
					} catch(Exception e1){
						programLogger.log(Level.SEVERE, "Error applying morphology", e1);
					}
				
			}});
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
			this.populationsPanel.update();
		}
		
		if(event.type().equals("RefreshPopulationPanelDatasets")){
			this.populationsPanel.refreshDatasets();;
		}
		
		if(event.type().startsWith("Log_")){
			String s = event.type().replace("Log_", "");
			programLogger.log(Level.INFO, s);
		}
		
		if(event.type().startsWith("Status_")){
			String s = event.type().replace("Status_", "");
			setStatus(s);
		}
				
	}

	@Override
	public void datasetEventReceived(final DatasetEvent event) {
		programLogger.log(Level.FINEST, "Heard dataset event: "+event.method().toString());
		final List<AnalysisDataset> list = event.getDatasets();
		if(!list.isEmpty()){
						
			if(event.method().equals(DatasetMethod.NEW_MORPHOLOGY)){
				programLogger.log(Level.INFO, "Running new morphology analysis");
				final int flag = ADD_POPULATION;
				SwingUtilities.invokeLater(new Runnable(){
					public void run(){
					
						new RunSegmentationAction(list, MorphologyAnalysisMode.NEW, flag, MainWindow.this);

				}});
			}
			
			if(event.method().equals(DatasetMethod.REFRESH_MORPHOLOGY)){
				programLogger.log(Level.INFO, "Updating segmentation across nuclei");
				SwingUtilities.invokeLater(new Runnable(){
					public void run(){
					
						new RunSegmentationAction(list, MorphologyAnalysisMode.REFRESH, 0, MainWindow.this);
					
				}});

			}
			
			if(event.method().equals(DatasetMethod.COPY_MORPHOLOGY)){
				
				final AnalysisDataset source = event.secondaryDataset();
				
				SwingUtilities.invokeLater(new Runnable(){
					public void run(){
					
						new RunSegmentationAction(event.getDatasets(), source, null, MainWindow.this);
					
				}});

			}
			
						
			if(event.method().equals(DatasetMethod.CLUSTER)){
				programLogger.log(Level.INFO, "Clustering dataset");
				new ClusterAnalysisAction(event.firstDataset(), this);
			}
			
			if(event.method().equals(DatasetMethod.BUILD_TREE)){
				programLogger.log(Level.INFO, "Building a tree from dataset");
				new BuildHierarchicalTreeAction(event.firstDataset(), this);
			}
			
			if(event.method().equals(DatasetMethod.REFOLD_CONSENSUS)){
				programLogger.log(Level.INFO, "Refolding consensus");
				programLogger.log(Level.FINEST, "Refold consensus dataset method is EDT: "+SwingUtilities.isEventDispatchThread());
				
				Thread thr = new Thread(){

					public void run(){
						
						/*
						 * The refold action needs to be able to hold up a series
						 * of following actions, when it is being used in a New Analysis.
						 * The countdown latch does nothing here, but must be retained for
						 * compatibility.
						 */
						
						final CountDownLatch latch = new CountDownLatch(1);
						programLogger.log(Level.FINEST, "Created latch: "+latch.getCount());
						new RefoldNucleusAction(event.firstDataset(), MainWindow.this, latch);

						programLogger.log(Level.FINEST, "Running refolding");
						try {
							latch.await();
							programLogger.log(Level.FINEST, "Latch counted down: "+latch.getCount());
						} catch (InterruptedException e) {
							programLogger.log(Level.SEVERE, "Interruption to thread", e);
						}
					}
				};
				thr.start();
			}
			
			if(event.method().equals(DatasetMethod.SELECT_DATASETS)){
				this.populationsPanel.selectDataset(event.firstDataset());
			}
			
			if(event.method().equals(DatasetMethod.EXTRACT_SOURCE)){
				programLogger.log(Level.INFO, "Recovering source dataset");
				for(AnalysisDataset d : list){
					d.setRoot(true);
					populationsPanel.addDataset(d);
					populationsPanel.update();
				}

			}
			
			if(event.method().equals(DatasetMethod.RECALCULATE_CACHE)){
				for(DetailPanel panel : detailPanels){
					panel.refreshChartCache(list);
				}
				this.updatePanels(list);
			}
			
			if(event.method().equals(DatasetMethod.ADD_DATASET)){
				event.firstDataset().setSwatch(activeSwatch);
				populationsPanel.addDataset(event.firstDataset());
				for(AnalysisDataset child : event.firstDataset().getAllChildDatasets() ){
					populationsPanel.addDataset(child);
				}
				
				
			}

//			if(event.method().equals(DatasetMethod.SAVE_AS)){
//				programLogger.log(Level.FINEST, "Dataset save as event heard");
//				final AnalysisDataset selectedDataset = event.firstDataset();
//
//				final CountDownLatch latch = new CountDownLatch(1);
//
//				Thread thr = new Thread(){
//					public void run(){
//
//						new SaveDatasetAction(selectedDataset, MainWindow.this, latch, true);
//						try {
//							latch.await();
//						} catch (InterruptedException e) {
//							programLogger.log(Level.SEVERE, "Interruption to thread", e);
//						}
//					}};
//					thr.start();
//			}

		}
		
	}
	
	private void saveRootDatasets(){
		for(AnalysisDataset root : populationsPanel.getRootDatasets()){
			final CountDownLatch latch = new CountDownLatch(1);

			new SaveDatasetAction(root, MainWindow.this, latch, false);
			try {
				latch.await();
			} catch (InterruptedException e) {
				programLogger.log(Level.SEVERE, "Interruption to thread", e);
			}
		}
		programLogger.log(Level.INFO, "All root datasets saved");
	}

	
	@Override
	public void interfaceEventReceived(InterfaceEvent event) {
		
		InterfaceMethod method = event.method();
		programLogger.log(Level.FINEST, "Heard interface event: "+event.method().toString());
		
		switch(method){
		
		case REFRESH_POPULATIONS:
			this.populationsPanel.refreshDatasets();
			break;
			
		case SAVE_ROOT:
			Thread thr = new Thread(){
				public void run(){
					saveRootDatasets();
				}};
				thr.start();
			break;
			
		case UPDATE_PANELS:
			this.updatePanels(populationsPanel.getSelectedDatasets());
			break;
			
		case UPDATE_POPULATIONS:
			this.populationsPanel.update();
			break;
			
		case RECACHE_CHARTS:
			for(DetailPanel panel : this.detailPanels){
				panel.refreshChartCache();
				panel.refreshTableCache();
			}
			this.updatePanels(populationsPanel.getSelectedDatasets());
			break;
		case LIST_DATASETS:
			int i=0;
			for(AnalysisDataset d : populationsPanel.getAllDatasets()){
				programLogger.log(Level.INFO, i+"\t"+d.getName());
				i++;
			}
			break;
		case RESEGMENT_SELECTED_DATASET:

				final int flag = CURVE_REFOLD; // ensure consensus is replaced
				SwingUtilities.invokeLater(new Runnable(){
					public void run(){
												
						// Recalculate the head and hump positions for rodent sperm
						if(populationsPanel.getSelectedDatasets().get(0).getCollection().getNucleusType().equals(NucleusType.RODENT_SPERM)){

							try{
								programLogger.log(Level.INFO, "Replacing nucleus roi patterns");
								for( Nucleus n : populationsPanel.getSelectedDatasets().get(0).getCollection().getNuclei()){

									RodentSpermNucleus r = (RodentSpermNucleus) n;  

									r.splitNucleusToHeadAndHump();
									try {

										r.calculateSignalAnglesFromPoint(r.getPoint(BorderTag.ORIENTATION_POINT));
									} catch (Exception e) {
										programLogger.log(Level.SEVERE, "Error restoring signal angles", e);
									}

								}

							}catch(Exception e){
								programLogger.log(Level.SEVERE, "Error recalculating angles", e);
							}
						}
						
						programLogger.log(Level.INFO, "Regenerating charts");
						for(DetailPanel panel : detailPanels){
							panel.refreshChartCache();
							panel.refreshTableCache();
						}
						
						programLogger.log(Level.INFO, "Resegmenting datasets");
						List<AnalysisDataset> list = populationsPanel.getSelectedDatasets();
						new RunSegmentationAction(list, MorphologyAnalysisMode.NEW, flag, MainWindow.this);

				}});
				break;
				
		case LIST_SELECTED_DATASETS:
			int count=0;
			for(AnalysisDataset d : populationsPanel.getSelectedDatasets()){
				programLogger.log(Level.INFO, count+"\t"+d.getName());
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
			
		default:
			break;

		}		
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
