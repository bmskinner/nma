/*******************************************************************************
 *  	Copyright (C) 2015 Ben Skinner
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
import gui.actions.MergeCollectionAction;
import gui.actions.MorphologyAnalysisAction;
import gui.actions.RefoldNucleusAction;
import gui.actions.NewAnalysisAction;
import gui.actions.SaveDatasetAction;
import gui.actions.ShellAnalysisAction;
import gui.components.ColourSelecter.ColourSwatch;
import gui.tabs.AnalysisDetailPanel;
import gui.tabs.CellDetailPanel;
import gui.tabs.ClusterDetailPanel;
import gui.tabs.DetailPanel;
import gui.tabs.MergesDetailPanel;
import gui.tabs.NuclearBoxplotsPanel;
import gui.tabs.NucleusProfilesPanel;
import gui.tabs.SegmentsDetailPanel;
import gui.tabs.SignalsDetailPanel;
import gui.tabs.VennDetailPanel;
import gui.tabs.WilcoxonDetailPanel;
import ij.IJ;
import ij.io.DirectoryChooser;
import ij.io.SaveDialog;
import io.PopulationExporter;
import io.PopulationImporter;
import io.StatsExporter;

import java.awt.BorderLayout;
import java.awt.Dimension;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.event.ActionEvent;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.io.File;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.CountDownLatch;
import java.util.logging.Level;
import java.util.logging.Logger;

import javax.swing.AbstractAction;
import javax.swing.JButton;
import javax.swing.JFileChooser;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JSplitPane;
import javax.swing.JTabbedPane;
import javax.swing.SwingUtilities;
import javax.swing.border.EmptyBorder;
import javax.swing.filechooser.FileNameExtensionFilter;

import logging.LogPanelFormatter;
import logging.TextAreaHandler;
import utility.Constants;
import analysis.AnalysisDataset;
import analysis.nucleus.MorphologyAnalysis;
import components.Cell;
import components.CellCollection;
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
	private WilcoxonDetailPanel 	wilcoxonDetailPanel;	// stats test between populations
	private SegmentsDetailPanel 	segmentsDetailPanel;	// segmented profiles
	private CellDetailPanel 		cellDetailPanel;		// cell by cell in a population
	private VennDetailPanel			vennDetailPanel; 		// overlaps between populations
	private ClusterDetailPanel		clusterDetailPanel;		// clustering within populations
	private MergesDetailPanel		mergesDetailPanel;		// merges between populations
	
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
//			detailPanels.add(populationsPanel);
			populationsPanel.addSignalChangeListener(this);
			populationsPanel.addDatasetEventListener(this);
			
			consensusNucleusPanel = new ConsensusNucleusPanel(programLogger);
			detailPanels.add(consensusNucleusPanel);
			consensusNucleusPanel.addSignalChangeListener(this);
			consensusNucleusPanel.addDatasetEventListener(this);
			
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
			nuclearBoxplotsPanel.addSignalChangeListener(this);
			nuclearBoxplotsPanel.addDatasetEventListener(this);
			tabbedPane.addTab("Nuclear charts", nuclearBoxplotsPanel);
				
			
			//---------------
			// Create the signals tab panel
			//---------------
			signalsDetailPanel  = new SignalsDetailPanel(programLogger);
			detailPanels.add(signalsDetailPanel);
			signalsDetailPanel.addSignalChangeListener(this);
			signalsDetailPanel.addDatasetEventListener(this);
			tabbedPane.addTab("Signals", signalsDetailPanel);
			

			//---------------
			// Create the clusters panel
			//---------------
			clusterDetailPanel = new ClusterDetailPanel(programLogger);
			detailPanels.add(clusterDetailPanel);
			clusterDetailPanel.addSignalChangeListener(this);
			clusterDetailPanel.addDatasetEventListener(this);
			tabbedPane.addTab("Clusters", clusterDetailPanel);
			
			//---------------
			// Create the merges panel
			//---------------
			mergesDetailPanel = new MergesDetailPanel(programLogger);
			detailPanels.add(mergesDetailPanel);
			mergesDetailPanel.addSignalChangeListener(this);
			mergesDetailPanel.addDatasetEventListener(this);
			tabbedPane.addTab("Merges", mergesDetailPanel);
			
			//---------------
			// Create the Venn panel
			//---------------
			vennDetailPanel = new VennDetailPanel(programLogger);
			detailPanels.add(vennDetailPanel);
			vennDetailPanel.addDatasetEventListener(this);
			tabbedPane.addTab("Venn", null, vennDetailPanel, null);
			
			
			//---------------
			// Create the Wilcoxon test panel
			//---------------
			wilcoxonDetailPanel = new WilcoxonDetailPanel(programLogger);
			detailPanels.add(wilcoxonDetailPanel);
			wilcoxonDetailPanel.addDatasetEventListener(this);
			tabbedPane.addTab("Wilcoxon", null, wilcoxonDetailPanel, null);
			
			//---------------
			// Create the segments boxplot panel
			//---------------
			segmentsDetailPanel = new SegmentsDetailPanel(programLogger);
			detailPanels.add(segmentsDetailPanel);
			segmentsDetailPanel.addDatasetEventListener(this);
			segmentsDetailPanel.addInterfaceEventListener(this);
			tabbedPane.addTab("Segments", null, segmentsDetailPanel, null);

			//---------------
			// Create the cells panel
			//---------------
			cellDetailPanel = new CellDetailPanel(programLogger);
			detailPanels.add(cellDetailPanel);
			cellDetailPanel.addDatasetEventListener(this);
			tabbedPane.addTab("Cells", null, cellDetailPanel, null);
			cellDetailPanel.addSignalChangeListener(this);
			
			
			//---------------
			// Register change listeners
			//---------------
			signalsDetailPanel.addSignalChangeListener(cellDetailPanel);
			cellDetailPanel.addSignalChangeListener(signalsDetailPanel); // allow the panels to communicate colour updates
			
			//---------------
			// Add the top and bottom rows to the main panel
			//---------------
			JSplitPane panelMain = new JSplitPane(JSplitPane.VERTICAL_SPLIT,
					topRow, tabbedPane);
			
			contentPane.add(panelMain, BorderLayout.CENTER);

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
//				new NewMorphologyAnalysisAction();
//				newAnalysis();
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
				loadDataset();
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
				for(AnalysisDataset d : populationsPanel.getRootDatasets()){
					if(d.isRoot()){
						programLogger.log(Level.INFO, "Saving dataset "+d.getCollection().getName()+"...");
						
						new SaveDatasetAction(d, "Saving dataset", "Error saving dataset", MainWindow.this);
						
//						PopulationExporter.saveAnalysisDataset(d);
//						d.save();
						programLogger.log(Level.INFO, "Saved dataset OK");
					}
				}
				programLogger.log(Level.INFO, "All root datasets saved");
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
				fishMapping();
			}
		});
		panelHeader.add(btnPostanalysisMapping);
		
		
		JButton btnSetSwatch = new JButton("Set swatch");
		btnSetSwatch.addMouseListener(new MouseAdapter() {
			@Override
			public void mouseClicked(MouseEvent arg0) {
									
					ColourSwatch[] nameArray = ColourSwatch.values();
					
					ColourSwatch option = (ColourSwatch) JOptionPane.showInputDialog(null, 
							"Choose swatch",
							"Swatch",
							JOptionPane.QUESTION_MESSAGE, 
							null, 
							nameArray, 
							activeSwatch);
					
					if(option!=null){
						// a choice was made
						for(AnalysisDataset d : populationsPanel.getAllDatasets()){
							d.setSwatch(option);
						}
						activeSwatch = option;
						updatePanels(populationsPanel.getSelectedDatasets());
					}
				
				
				
			}
		});
		panelHeader.add(btnSetSwatch);
		
		
		JButton btnSetLogLevel = new JButton("Set logging level");
		btnSetLogLevel.addMouseListener(new MouseAdapter() {
			@Override
			public void mouseClicked(MouseEvent arg0) {

				Level[] nameArray = { Level.INFO, Level.FINE, Level.FINEST };
				Level option = (Level) JOptionPane.showInputDialog(null, 
						"Choose swatch",
						"Swatch",
						JOptionPane.QUESTION_MESSAGE, 
						null, 
						nameArray, 
						programLogger.getLevel());

				if(option!=null){
					// a choice was made
					programLogger.setLevel(option);
					programLogger.log(Level.SEVERE, "Set the logging level to "+option.toString());

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
	 * Check a version string to see if the program will be able to open a 
	 * dataset. The major version must be the same, while the revision of the
	 * dataset must be equal to or greater than the program revision. Bugfixing
	 * versions are not checked for.
	 * @param version
	 * @return a pass or fail
	 */
	public boolean checkVersion(String version){
		boolean ok = true;
		
		if(version==null){ // allow for debugging, but warn
			programLogger.log(Level.SEVERE, "No version info found: functions may not work as expected");
			return true;
		}
		
		String[] parts = version.split("\\.");
		
		// major version MUST be the same
		if(Integer.valueOf(parts[0])!=Constants.VERSION_MAJOR){
			ok = false;
		}
		// dataset revision should be equal or greater to program
		if(Integer.valueOf(parts[1])<Constants.VERSION_REVISION){
			programLogger.log(Level.SEVERE, "Dataset was created with an older version of the program");
			programLogger.log(Level.SEVERE, "Some functionality may not work as expected");
		}
		return ok;
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
	 * Compare morphology images with post-FISH images, and select nuclei into new
	 * sub-populations
	 */
	public void fishMapping(){

		try{

//			String[] names = populationsPanel.getPopulationNames().toArray(new String[0]);
//
//			String selectedValue = (String) JOptionPane.showInputDialog(null,
//					"Choose population", "FISH Remapping",
//					JOptionPane.INFORMATION_MESSAGE, null,
//					names, names[0]);
//
//			UUID id = populationsPanel.getUuidFromName(selectedValue);
			List<AnalysisDataset> list = populationsPanel.getSelectedDatasets();
			if(list.size()==1){
				final AnalysisDataset dataset = list.get(0);
				
				FishMappingWindow fishMapper = new FishMappingWindow(MainWindow.this, dataset, programLogger);

				List<CellCollection> subs = fishMapper.getSubCollections();
				
				final List<AnalysisDataset> newList = new ArrayList<AnalysisDataset>();
				for(CellCollection sub : subs){

					if(sub.getNucleusCount()>0){
						
						dataset.addChildCollection(sub);
						
						AnalysisDataset subDataset = dataset.getChildDataset(sub.getID());
						list.add(subDataset);
					}
				}
				
				SwingUtilities.invokeLater(new Runnable(){
					public void run(){
						programLogger.log(Level.INFO, "Reapplying morphology...");
						new MorphologyAnalysisAction(newList, dataset, ADD_POPULATION, MainWindow.this);

					}});

				
				
			}

		} catch(Exception e){
			programLogger.log(Level.SEVERE, "Error in FISH remapping: "+e.getMessage(), e);
		}
	}
	
			
	/**
	 * Call an open dialog to choose a saved .nbd dataset. The opened dataset
	 * will be added to the bottom of the dataset list.
	 */
	public void loadDataset(){
		Thread thr = new Thread() {
			public void run() {
				try {
					
					FileNameExtensionFilter filter = new FileNameExtensionFilter("Nuclear morphology datasets", "nmd");
					
					File defaultDir = new File("J:\\Protocols\\Scripts and macros\\");
					JFileChooser fc = new JFileChooser("Select a saved dataset...");
					if(defaultDir.exists()){
						fc = new JFileChooser(defaultDir);
					}
					fc.setFileFilter(filter);

					int returnVal = fc.showOpenDialog(fc);
					if (returnVal != 0)	{
						return;
					}
					File file = fc.getSelectedFile();
//					
					if(file.isDirectory()){
						return;
					}
					
					programLogger.log(Level.INFO, "Opening dataset...");
					
					// read the dataset
					AnalysisDataset dataset = PopulationImporter.readDataset(file, programLogger);
					
					if(checkVersion( dataset.getVersion() )){

						dataset.setRoot(true);
						
						populationsPanel.addDataset(dataset);
						
						for(AnalysisDataset child : dataset.getAllChildDatasets() ){
							populationsPanel.addDataset(child);
						}
						
						// update the log file to the same folder as the dataset
						File logFile = new File(file.getParent()+File.separator+file.getName().replace(Constants.SAVE_FILE_EXTENSION, Constants.LOG_FILE_EXTENSION));
						
						dataset.getCollection().setDebugFile(logFile);
						
						dataset.setSwatch(activeSwatch);
						
						programLogger.log(Level.INFO, "OK");
//						
	
						List<AnalysisDataset> list = new ArrayList<AnalysisDataset>(0);
						list.add(dataset);
	
						updatePanels(list);
						populationsPanel.update();
						
					} else {
						programLogger.log(Level.SEVERE, "Unable to open dataset version: "+ dataset.getVersion());
					}
				} catch (Exception e) {
					programLogger.log(Level.SEVERE, "Error opening dataset", e);
				}
			}
		};
		thr.start();
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
//					consensusNucleusPanel.update(list);
//
//					nucleusProfilesPanel.update(list);
//					analysisDetailPanel.update(list);
//					nuclearBoxplotsPanel.update(list);
//					signalsDetailPanel.update(list);
//					clusterDetailPanel.update(list);
//					mergesDetailPanel.update(list);
//					vennDetailPanel.update(list);
//					wilcoxonDetailPanel.update(list);
//					cellDetailPanel.updateList(list);
//					segmentsDetailPanel.update(list);
					programLogger.log(Level.FINE, "Updated tab panels");
				} catch (Exception e) {
					programLogger.log(Level.SEVERE,"Error updating panels", e);
				}
			}
		};
		thr.start();
	}
						
	class SplitCollectionAction extends AbstractAction {

		private static final long serialVersionUID = 1L;
		public SplitCollectionAction() {
	        super("Split");
	    }
		
	    public void actionPerformed(ActionEvent e) {
	        
	        List<AnalysisDataset> datasets = populationsPanel.getSelectedDatasets();

	        if(datasets.size()==1){
	        	try {
	        		
	        		AnalysisDataset dataset = datasets.get(0);

	        		if(dataset.hasChildren()){
	        			programLogger.log(Level.INFO, "Splitting collection...");

	        			// create a JDialog of options
	        			// get the names of subpopulations
	        			List<String> nameList = new ArrayList<String>(0);
	        			for(AnalysisDataset child : dataset.getAllChildDatasets()){
	        				nameList.add(child.getName());
	        			}

	        			String[] names = nameList.toArray(new String[0]);

	        			String selectedValue = (String) JOptionPane.showInputDialog(null,
	        					"Give me nuclei that are NOT present within the following population", "Split population",
	        					JOptionPane.PLAIN_MESSAGE, null,
	        					names, names[0]);

	        			// find the population to subtract
	        			AnalysisDataset negative = populationsPanel.getDataset(selectedValue);

	        			// prepare a new collection
	        			CellCollection collection = dataset.getCollection();

	        			CellCollection newCollection = new CellCollection(dataset, "Subtraction");

	        			for(Cell n : collection.getCells()){
	        				if(! negative.getCollection().getCells().contains(n)){
	        					newCollection.addCell(new Cell(n));
	        				}
	        			}
	        			newCollection.setName("Not_in_"+negative.getName());
	        			UUID newID = newCollection.getID();

	        			dataset.addChildCollection(newCollection);
	        			
	        			if(newCollection.getNucleusCount()>0){

	        				programLogger.log(Level.INFO,"Reapplying morphology...");
//	        				logc("Reapplying morphology...");
	        				
	        				AnalysisDataset newDataset = dataset.getChildDataset(newCollection.getID());
	        				new MorphologyAnalysisAction(newDataset, dataset, null, MainWindow.this);
	        			}


	        			

	        			populationsPanel.addDataset(dataset.getChildDataset(newID));
	        			populationsPanel.update();
	        			
	        		} else {
	        			programLogger.log(Level.INFO,"Cannot split; no children in dataset");
	        		}


				} catch (Exception e1) {
					programLogger.log(Level.SEVERE,"Error splitting collection", e1);
				} 
				
	        }   else {
	        	programLogger.log(Level.INFO,"Cannot split multiple collections");
	        }
	    }
	}
				
	class ReplaceNucleusFolderAction extends AbstractAction {

		private static final long serialVersionUID = 1L;
		public ReplaceNucleusFolderAction() {
			super("Change folder");
		}

		public void actionPerformed(ActionEvent e) {

			DirectoryChooser localOpenDialog = new DirectoryChooser("Select new directory of images...");
			String folderName = localOpenDialog.getDirectory();

			if(folderName!=null) { 
				
				

				File newFolder = new File(folderName);

				final List<AnalysisDataset> datasets = populationsPanel.getSelectedDatasets();

				if(datasets.size()==1){
					programLogger.log(Level.INFO, "Updating folder to "+folderName );

					AnalysisDataset d = datasets.get(0);
					for(Nucleus n : d.getCollection().getNuclei()){
						try{
							n.updateSourceFolder(newFolder);
						} catch(Exception e1){
							programLogger.log(Level.SEVERE, "Error renaming nucleus: "+e1.getMessage(), e);
						}
					}
					programLogger.log(Level.INFO, "Folder updated");
				}
				
			}

		}
	}

	class ExportDatasetStatsAction extends AbstractAction {

		private static final long serialVersionUID = 1L;
		public ExportDatasetStatsAction() {
			super("Export stats");
		}
		// note - this will overwrite the stats for any collection with the same name in the output folder
		public void actionPerformed(ActionEvent e) {

			final List<AnalysisDataset> datasets = populationsPanel.getSelectedDatasets();

			if(datasets.size()==1){

				Thread thr = new Thread() {
					public void run() {

						AnalysisDataset d = datasets.get(0);
						try{

							programLogger.log(Level.INFO, "Exporting stats...");
							boolean ok = StatsExporter.run(d);
							if(ok){
								programLogger.log(Level.INFO, "OK");
							} else {
								programLogger.log(Level.INFO, "Error");
							}
						} catch(Exception e1){
							programLogger.log(Level.SEVERE, "Error in stats export", e1);
						}
					}
				};
				thr.run();
			}

		}
	}
			
	@Override
	public void signalChangeReceived(SignalChangeEvent event) {
		
		final AnalysisDataset selectedDataset = populationsPanel.getSelectedDatasets().get(0);
		
		if(event.type().equals("RunShellAnalysis")){
			programLogger.log(Level.INFO, "Shell analysis selected");
			new ShellAnalysisAction(selectedDataset, this);
		}

		if(event.type().equals("MergeCollectionAction")){
			
			Thread thr = new Thread() {
				public void run() {


					SaveDialog saveDialog = new SaveDialog("Save merged dataset as...", "Merge_of_datasets", Constants.SAVE_FILE_EXTENSION);

					String fileName = saveDialog.getFileName();
					String folderName = saveDialog.getDirectory();
					
					if(fileName!=null && folderName!=null){
						File saveFile = new File(folderName+File.separator+fileName);

						new MergeCollectionAction(populationsPanel.getSelectedDatasets(), saveFile, MainWindow.this);
					}
				}
			};

			thr.start();
			
		}
		
		if(event.type().equals("SplitCollectionAction")){
			new SplitCollectionAction();
		}
				
//		if(event.type().equals("ExtractNucleiAction")){
//			Thread thr = new Thread() {
//        		public void run() {
//
//        			DirectoryChooser openDialog = new DirectoryChooser("Select directory to export images...");
//        			String folderName = openDialog.getDirectory();
//
//        			if(folderName==null){
//        				return; // user cancelled
//        			}
//
//        			File folder =  new File(folderName);
//
//        			if(!folder.isDirectory() ){
//        				return;
//        			}
//        			if(!folder.exists()){
//        				return; // check folder is ok
//        			}
//
//        			programLogger.log(Level.INFO, "Extracting nuclei from collection...");
//        			boolean ok = PopulationExporter.extractNucleiToFolder(selectedDataset, folder);
//        			if(ok){ 
//        				programLogger.log(Level.INFO, "OK");
//        			} else {
//        				programLogger.log(Level.INFO, "Error");
//        			}
//        		}
//        	};
//        	thr.start();
//		}
		
		if(event.type().equals("ChangeNucleusFolderAction")){
			new ReplaceNucleusFolderAction();
		}
		
		if(event.type().equals("ExportDatasetStatsAction")){
			new ExportDatasetStatsAction();
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

							new MorphologyAnalysisAction(selectedDataset, source, null, MainWindow.this);
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
					
						new MorphologyAnalysisAction(list, MorphologyAnalysis.MODE_NEW, flag, MainWindow.this);

				}});
			}
			
			if(event.method().equals(DatasetMethod.REFRESH_MORPHOLOGY)){
				
				SwingUtilities.invokeLater(new Runnable(){
					public void run(){
					
						new MorphologyAnalysisAction(list, MorphologyAnalysis.MODE_REFRESH, 0, MainWindow.this);
					
				}});

			}
			
			if(event.method().equals(DatasetMethod.COPY_MORPHOLOGY)){
				
				final AnalysisDataset source = event.secondaryDataset();
				
				SwingUtilities.invokeLater(new Runnable(){
					public void run(){
					
						new MorphologyAnalysisAction(event.getDatasets(), source, null, MainWindow.this);
					
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
				populationsPanel.addDataset(event.firstDataset());
			}
			
			if(event.method().equals(DatasetMethod.SAVE_AS)){
			
				AnalysisDataset selectedDataset = event.firstDataset();
				SaveDialog saveDialog = new SaveDialog("Save as...", selectedDataset.getName(), ".nmd");
				
				String fileName = saveDialog.getFileName();
				String folderName = saveDialog.getDirectory();
				
				if(fileName!=null && folderName!=null){
					File saveFile = new File(folderName+File.separator+fileName);

					programLogger.log(Level.INFO, "Saving as "+saveFile.getAbsolutePath()+"...");
					new SaveDatasetAction(selectedDataset, saveFile, "Saving dataset", "Error saving dataset", MainWindow.this);
				}
			}
							
		}
		
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
			for(AnalysisDataset root : populationsPanel.getRootDatasets()){
				new SaveDatasetAction(root, "Saving dataset", "Error saving dataset", MainWindow.this);
//				
//				PopulationExporter.saveAnalysisDataset(root);
			}
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
//				programLogger.log(Level.INFO, "Resegmenting selected datasets");
				final int flag = CURVE_REFOLD; // ensure consensus is replaced
				SwingUtilities.invokeLater(new Runnable(){
					public void run(){
												
						// Recalculate the head and hump positions for rodent sperm
						if(populationsPanel.getSelectedDatasets().get(0).getCollection().getNucleusType().equals(NucleusType.RODENT_SPERM)){

							try{
								programLogger.log(Level.INFO, "Replacing nucleus roi patterns");
								for( Nucleus n : populationsPanel.getSelectedDatasets().get(0).getCollection().getNuclei()){
//									IJ.log(n.getNameAndNumber());
									RodentSpermNucleus r = (RodentSpermNucleus) n;  
//									IJ.log("Splitting nuclcus");
									r.splitNucleusToHeadAndHump();
									try {
//										IJ.log("Calculating angles");
										r.calculateSignalAnglesFromPoint(r.getPoint(BorderTag.ORIENTATION_POINT));
									} catch (Exception e) {
										programLogger.log(Level.SEVERE, "Error restoring signal angles", e);
									}
//									IJ.log("Finished calculating angles");
								}
//								programLogger.log(Level.INFO, "Replaced nucleus roi patterns");
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
						new MorphologyAnalysisAction(list, MorphologyAnalysis.MODE_NEW, flag, MainWindow.this);

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
			
		default:
			break;

		}		
	}	
}
