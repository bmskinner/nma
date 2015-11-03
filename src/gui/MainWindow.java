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
import io.CompositeExporter;
import io.NucleusAnnotator;
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
import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import java.io.File;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
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
import javax.swing.JProgressBar;
import javax.swing.JSpinner;
import javax.swing.JSplitPane;
import javax.swing.JTabbedPane;
import javax.swing.SpinnerNumberModel;
import javax.swing.SwingUtilities;
import javax.swing.SwingWorker;
import javax.swing.border.EmptyBorder;
import javax.swing.filechooser.FileNameExtensionFilter;

import logging.LogPanelFormatter;
import logging.TextAreaHandler;
import utility.Constants;
import analysis.AnalysisDataset;
import analysis.AnalysisOptions;
import analysis.AnalysisOptions.NuclearSignalOptions;
import analysis.ClusteringOptions;
import analysis.nucleus.CurveRefolder;
import analysis.nucleus.DatasetMerger;
import analysis.nucleus.MorphologyAnalysis;
import analysis.nucleus.NucleusClusterer;
import analysis.nucleus.NucleusDetector;
import analysis.nucleus.ShellAnalysis;
import analysis.nucleus.SignalDetector;
import analysis.nucleus.CurveRefolder.CurveRefoldingMode;
import analysis.tail.TubulinTailDetector;
import components.Cell;
import components.CellCollection;
import components.ClusterGroup;
import components.nuclei.Nucleus;

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
	private static final int ADD_POPULATION		 = 1;
	private static final int STATS_EXPORT 		 = 2;
	private static final int NUCLEUS_ANNOTATE	 = 4;
	private static final int CURVE_REFOLD 		 = 8;
	private static final int EXPORT_COMPOSITE	 = 16;
	private static final int SAVE_DATASET		 = 32;
	
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
			detailPanels.add(populationsPanel);
			populationsPanel.addSignalChangeListener(this);
			populationsPanel.addDatasetEventListener(this);
			
			consensusNucleusPanel = new ConsensusNucleusPanel(programLogger);
			detailPanels.add(consensusNucleusPanel);
			consensusNucleusPanel.addSignalChangeListener(this);
			consensusNucleusPanel.addDatasetEventListener(this);
			
			//---------------
			// Create the log panel
			//---------------
			logPanel = new LogPanel();
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
				
				new NewMorphologyAnalysisAction();
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
						PopulationExporter.saveAnalysisDataset(d);
//						d.save();
						programLogger.log(Level.INFO, "OK");
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

			String[] names = populationsPanel.getPopulationNames().toArray(new String[0]);

			String selectedValue = (String) JOptionPane.showInputDialog(null,
					"Choose population", "FISH Remapping",
					JOptionPane.INFORMATION_MESSAGE, null,
					names, names[0]);

			UUID id = populationsPanel.getUuidFromName(selectedValue);

			final AnalysisDataset dataset = populationsPanel.getDataset(id);

			FishMappingWindow fishMapper = new FishMappingWindow(MainWindow.this, dataset, programLogger);

			List<CellCollection> subs = fishMapper.getSubCollections();
			final List<AnalysisDataset> list = new ArrayList<AnalysisDataset>();
			for(CellCollection sub : subs){

				if(sub.getNucleusCount()>0){
					
					dataset.addChildCollection(sub);
					
					AnalysisDataset subDataset = dataset.getChildDataset(sub.getID());
					list.add(subDataset);
				}
			}
			
			Thread thr = new Thread(){
				public void run(){
					programLogger.log(Level.INFO, "Reapplying morphology...");
					new MorphologyAnalysisAction(list, dataset, ADD_POPULATION);
				}
			};
			thr.start();
			

			
			// get the dataset craeted by adding a child collection, and put it inthe populations list
//			populationsPanel.addDataset(dataset.getChildDataset(sub.getID()));
//			populationsPanel.update();	

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
					
					consensusNucleusPanel.update(list);

					nucleusProfilesPanel.update(list);
					analysisDetailPanel.update(list);
					nuclearBoxplotsPanel.update(list);
					signalsDetailPanel.update(list);
					clusterDetailPanel.update(list);
					mergesDetailPanel.update(list);
					vennDetailPanel.update(list);
					wilcoxonDetailPanel.update(list);
					cellDetailPanel.updateList(list);
					segmentsDetailPanel.update(list);
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
	        					newCollection.addCell(n);
	        				}
	        			}
	        			newCollection.setName("Not_in_"+negative.getName());
	        			UUID newID = newCollection.getID();

	        			dataset.addChildCollection(newCollection);
	        			
	        			if(newCollection.getNucleusCount()>0){

	        				programLogger.log(Level.INFO,"Reapplying morphology...");
//	        				logc("Reapplying morphology...");
	        				
	        				AnalysisDataset newDataset = dataset.getChildDataset(newCollection.getID());
	        				new MorphologyAnalysisAction(newDataset, dataset, null);
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
		
	/**
	 * Contains a progress bar and handling methods for when an action
	 * is triggered as a SwingWorker. Subclassed for each action type.
	 *
	 */
	abstract class ProgressableAction implements PropertyChangeListener{

		protected AnalysisDataset dataset = null; // the dataset being worked on
		protected JProgressBar progressBar = null;
		protected String errorMessage = null;
		protected SwingWorker<Boolean, Integer> worker;
		protected Integer downFlag = 0; // store flags to tell the action what to do after finishing
		
		public ProgressableAction(AnalysisDataset dataset, String barMessage, String errorMessage){
			
			this.errorMessage 	= errorMessage;
			this.dataset 		= dataset;
			this.progressBar 	= new JProgressBar(0, 100);
			this.progressBar.setString(barMessage);
			this.progressBar.setStringPainted(true);
			
			logPanel.addProgressBar(this.progressBar);
			contentPane.revalidate();
			contentPane.repaint();

		}
		
		public ProgressableAction(AnalysisDataset dataset, String barMessage, String errorMessage, int flag){
			this(dataset, barMessage, errorMessage);
			this.downFlag = flag;
		}
		
		/**
		 * Change the progress message from the default in the constructor
		 * @param messsage the string to display
		 */
		public void setProgressMessage(String messsage){
			this.progressBar.setString(messsage);
		}
		
		private void removeProgressBar(){
			logPanel.removeProgressBar(this.progressBar);
			logPanel.revalidate();
			logPanel.repaint();
		}
		
		public void cancel(){
			removeProgressBar();
		}
		
		/**
		 * Use to manually remove the progress bar after an action is complete
		 */
		public void cleanup(){
			if (this.worker.isDone() || this.worker.isCancelled()){
				this.removeProgressBar();
			}
		}
		
		
		@Override
		public void propertyChange(PropertyChangeEvent evt) {

			int value = (Integer) evt.getNewValue(); // should be percent
//			IJ.log("Property change: "+value);
			
			if(value >=0 && value <=100){
				
				if(this.progressBar.isIndeterminate()){
					this.progressBar.setIndeterminate(false);
				}
				this.progressBar.setValue(value);
			}

			if(evt.getPropertyName().equals("Finished")){
				programLogger.log(Level.FINEST,"Worker signaled finished");
				finished();
			}

			if(evt.getPropertyName().equals("Error")){
				programLogger.log(Level.FINEST,"Worker signaled error");
				error();
			}
			
			if(evt.getPropertyName().equals("Cooldown")){
				programLogger.log(Level.FINEST,"Worker signaled cooldown");
				cooldown();
			}
			
		}
		
		/**
		 * The method run when the analysis has completed
		 */
		public void finished(){
			removeProgressBar();

			populationsPanel.update(); // get any new populations
			List<AnalysisDataset> list = new ArrayList<AnalysisDataset>(0);
			list.add(dataset);
			for(AnalysisDataset root : populationsPanel.getRootDatasets()){
				PopulationExporter.saveAnalysisDataset(root);
			}
			
			populationsPanel.selectDataset(dataset);
			updatePanels(list); // update with the current population
			
		}
		
		/**
		 * Runs when an error was encountered in the analysis
		 */
		public void error(){
			programLogger.log(Level.SEVERE, this.errorMessage);
//			log(this.errorMessage);
			removeProgressBar();
		}
		
		/**
		 * Runs if a cooldown signal is received. Use to set progress bars
		 * to an indeterminate state when no reliable progress metric is 
		 * available
		 */
		public void cooldown(){
			this.progressBar.setIndeterminate(true);
			contentPane.revalidate();
			contentPane.repaint();
		}
		
	}
	
	
	/**
	 * Add images containing tubulin stained tails
	 * @author bms41
	 *
	 */
	class AddTailStainAction extends ProgressableAction {

		public AddTailStainAction(AnalysisDataset dataset) {
			super(dataset, "Tail detection", "Error in tail detection");
			try{
				
				TailDetectionSettingsWindow analysisSetup = new TailDetectionSettingsWindow(dataset.getAnalysisOptions(), programLogger);
				
				final int channel = analysisSetup.getChannel();
				
				DirectoryChooser openDialog = new DirectoryChooser("Select directory of tubulin images...");
				String folderName = openDialog.getDirectory();

				if(folderName==null){
					this.cancel();
					return; // user cancelled
				}

				final File folder =  new File(folderName);

				if(!folder.isDirectory() ){
					this.cancel();
					return;
				}
				if(!folder.exists()){
					this.cancel();
					return; // check folder is ok
				}

				worker = new TubulinTailDetector(dataset, folder, channel);
				worker.addPropertyChangeListener(this);
				this.setProgressMessage("Tail detection:"+dataset.getName());
				worker.execute();
			} catch(Exception e){
				this.cancel();
				programLogger.log(Level.SEVERE, "Error in tail analysis", e);

			}
		}
	}
	
	/**
	 * Add images containing nuclear signals
	 *
	 */
	class AddNuclearSignalAction extends ProgressableAction {
		
		private int signalGroup = 0;
		
		public AddNuclearSignalAction(AnalysisDataset dataset) {
			super(dataset, "Signal detection", "Error in signal detection");

			try{
				// add dialog for non-default detection options
				SignalDetectionSettingsWindow analysisSetup = new SignalDetectionSettingsWindow(dataset, programLogger);

				if(analysisSetup.isOK()){

					this.signalGroup = analysisSetup.getSignalGroup();
					//				this.signalGroup = newSignalGroup;
					String signalGroupName = dataset.getSignalGroupName(signalGroup);


					worker = new SignalDetector(dataset, analysisSetup.getFolder(), analysisSetup.getChannel(), dataset.getAnalysisOptions().getNuclearSignalOptions(signalGroupName), signalGroup, signalGroupName);
					this.setProgressMessage("Signal detection: "+signalGroupName);
					worker.addPropertyChangeListener(this);
					worker.execute();
				} else {
					this.cancel();
					return;
				}

				
			} catch (Exception e){
				this.cancel();
				programLogger.log(Level.SEVERE, "Error in signal analysis", e);
			}
			
		}	
		
		@Override
		public void finished(){
			// divide population into clusters with and without signals
			List<CellCollection> signalPopulations = dividePopulationBySignals(dataset.getCollection(), signalGroup);

			List<AnalysisDataset> list = new ArrayList<AnalysisDataset>();
			for(CellCollection collection : signalPopulations){
				
				processSubPopulation(collection);
				list.add(dataset.getChildDataset(collection.getID()));
			}
			// we have morphology analysis to carry out, so don't use the super finished
			// use the same segmentation from the initial analysis
			new MorphologyAnalysisAction(list, dataset, null);
			cancel();
		}

		/**
		 * Create child datasets for signal populations
		 * and perform basic analyses
		 * @param collection
		 */
		private void processSubPopulation(CellCollection collection){

			AnalysisDataset subDataset = new AnalysisDataset(collection, dataset.getSavePath());
			subDataset.setAnalysisOptions(dataset.getAnalysisOptions());

			programLogger.log(Level.INFO, "Sub-population: "+collection.getType()+" : "+collection.getNucleusCount()+" nuclei");

			dataset.addChildDataset(subDataset);
		}

		/*
	    Given a complete collection of nuclei, split it into up to 4 populations;
	      nuclei with red signals, with green signals, without red signals and without green signals
	    Only include the 'without' populations if there is a 'with' population.
		 */
		private List<CellCollection> dividePopulationBySignals(CellCollection r, int signalGroup){

			List<CellCollection> signalPopulations = new ArrayList<CellCollection>(0);
			programLogger.log(Level.INFO, "Dividing population by signals...");
			try{

				List<Cell> list = r.getCellsWithNuclearSignals(signalGroup, true);
				if(!list.isEmpty()){
					programLogger.log(Level.INFO, "Found nuclei with signals in group "+signalGroup);
					CellCollection listCollection = new CellCollection(r.getFolder(), 
							r.getOutputFolderName(), 
							"Signals_in_group_"+signalGroup, 
							r.getDebugFile(), 
							r.getNucleusType());

					for(Cell c : list){
						listCollection.addCell( c );
					}
					signalPopulations.add(listCollection);

					List<Cell> notList = r.getCellsWithNuclearSignals(signalGroup, false);
					if(!notList.isEmpty()){
						programLogger.log(Level.INFO, "Found nuclei without signals in group "+signalGroup);
						CellCollection notListCollection = new CellCollection(r.getFolder(), 
								r.getOutputFolderName(), 
								"No_signals_in_group_"+signalGroup, 
								r.getDebugFile(), 
								r.getNucleusType());

						for(Cell c : notList){
							notListCollection.addCell( c );
						}
						signalPopulations.add(notListCollection);
					}

				}

			} catch(Exception e){
				programLogger.log(Level.SEVERE, "Cannot create collection", e);
			}

			return signalPopulations;
		}
	}
	
	/**
	 * Refold the consensus nucleus for the selected dataset using default parameters
	 */
	public class RefoldNucleusAction extends ProgressableAction {

		/**
		 * Refold the given selected dataset
		 */
		
		public RefoldNucleusAction(AnalysisDataset dataset, CountDownLatch doneSignal) {
			super(dataset, "Refolding", "Error refolding nucleus");
			programLogger.log(Level.FINEST, "Created RefoldNucleusAction");
			try{

				this.progressBar.setIndeterminate(true);
				worker = new CurveRefolder(dataset, 
						CurveRefoldingMode.FAST, 
						doneSignal, 
						programLogger);

				worker.addPropertyChangeListener(this);
				this.setProgressMessage("Refolding: "+dataset.getName());
				
				/*
				 * The SwingWorker doInBackground is off the EDT. At this point, the EDT should be free
				 * 
				 * 
				 * What thread is waiting for a signal from the worker?
				 */
				programLogger.log(Level.FINEST, "RefoldNucleusAction init is EDT: "+SwingUtilities.isEventDispatchThread());
				
				worker.execute();
				programLogger.log(Level.FINEST, "Executed CurveRefolder");

			} catch(Exception e1){
				this.cancel();
				programLogger.log(Level.SEVERE, "Error refolding nucleus", e1);
			}
		}
		
		@Override
		public void finished(){

    		programLogger.log(Level.FINE, "Refolding finished, cleaning up");
    		programLogger.log(Level.FINEST, "RefoldNucleusAction.finished() is EDT: "+SwingUtilities.isEventDispatchThread());
			
			
			// ensure the bar is gone, even if the cleanup fails
			this.progressBar.setVisible(false);
			dataset.getAnalysisOptions().setRefoldNucleus(true);
			dataset.getAnalysisOptions().setRefoldMode("Fast");
			super.finished();
			
		}

	}
		
	
	/**
	 * Run a new shell analysis on the selected dataset
	 */
	class ShellAnalysisAction extends ProgressableAction {
				
		public ShellAnalysisAction(AnalysisDataset dataset) {
			super(dataset, "Shell analysis", "Error in shell analysis");
			
			SpinnerNumberModel sModel = new SpinnerNumberModel(5, 2, 10, 1);
			JSpinner spinner = new JSpinner(sModel);
			
			int option = JOptionPane.showOptionDialog(null, 
					spinner, 
					"Select number of shells", 
					JOptionPane.OK_CANCEL_OPTION, 
					JOptionPane.QUESTION_MESSAGE, null, null, null);
			if (option == JOptionPane.CANCEL_OPTION) {
			    // user hit cancel
				this.cancel();
				return;
				
			} else if (option == JOptionPane.OK_OPTION)	{
				
				int shellCount = (Integer) spinner.getModel().getValue();
				worker = new ShellAnalysis(dataset,shellCount);

				worker.addPropertyChangeListener(this);
				worker.execute();	
			}
		}
	}

	/**
	 * Run a new clustering on the selected dataset
	 * @author bms41
	 *
	 */
	class ClusterAnalysisAction extends ProgressableAction {
				
		public ClusterAnalysisAction(AnalysisDataset dataset) {
			super(dataset, "Cluster analysis", "Error in cluster analysis");
			
			ClusteringSetupWindow clusterSetup = new ClusteringSetupWindow(MainWindow.this);
			ClusteringOptions options = clusterSetup.getOptions();
//			Map<String, Object> options = clusterSetup.getOptions();

			if(clusterSetup.isReadyToRun()){ // if dialog was cancelled, skip

//				worker = new NucleusClusterer(  (Integer) options.get("type"), dataset.getCollection() );
				worker = new NucleusClusterer( dataset , options );
//				((NucleusClusterer) worker).setClusteringOptions(options);
				
				worker.addPropertyChangeListener(this);
				worker.execute();

			} else {
				this.cancel();
			}
			clusterSetup.dispose();
		}
		
		
		/* (non-Javadoc)
		 * Overrides because we need to carry out the morphology reprofiling
		 * on each cluster
		 * @see no.gui.MainWindow.ProgressableAction#finished()
		 */
		@Override
		public void finished() {

			programLogger.log(Level.INFO, "Found "+((NucleusClusterer) worker).getNumberOfClusters()+" clusters");

			String tree = (((NucleusClusterer) worker).getNewickTree());
			
			List<AnalysisDataset> list = new ArrayList<AnalysisDataset>();
			ClusteringOptions options =  ((NucleusClusterer) worker).getOptions();
			int clusterNumber = dataset.getClusterGroups().size();
			ClusterGroup group = new ClusterGroup("ClusterGroup_"+clusterNumber, options, tree);

			for(int cluster=0;cluster<((NucleusClusterer) worker).getNumberOfClusters();cluster++){

				CellCollection c = ((NucleusClusterer) worker).getCluster(cluster);
				programLogger.log(Level.FINEST, "Cluster "+cluster+": "+c.getName());
				group.addDataset(c);
				c.setName(group.getName()+"_"+c.getName());
				programLogger.log(Level.FINEST, "Renamed cluster: "+c.getName());
				dataset.addChildCollection(c);
				// attach the clusters to their parent collection
//				dataset.addCluster(c);
				
				programLogger.log(Level.INFO, "Cluster "+cluster+": "+c.getNucleusCount()+" nuclei");
				AnalysisDataset clusterDataset = dataset.getChildDataset(c.getID());
				list.add(clusterDataset);
				

			}
			dataset.addClusterGroup(group);
			programLogger.log(Level.FINEST, "Running new morphology analysis on cluster group");
			new MorphologyAnalysisAction(list, dataset, ADD_POPULATION);

			cancel();

		}
	}
	
	
    public class MorphologyAnalysisAction extends ProgressableAction {

    	private int mode = MorphologyAnalysis.MODE_NEW;
    	private List<AnalysisDataset> processList = null;
    	private AnalysisDataset source 			= null;
    	
                    
    	/**
    	 * Carry out a morphology analysis on a dataset, giving the mode
    	 * @param dataset the dataset to work on 
    	 * @param mode the type of morphology analysis to carry out
    	 * @param downFlag the next analyses to perform
    	 */
    	public MorphologyAnalysisAction(AnalysisDataset dataset, int mode, int downFlag){
    		super(dataset, "Morphology analysis", "Error in analysis", downFlag);
    		programLogger.log(Level.FINE, "Creating morphology analysis");
    		this.mode = mode;
    		runNewAnalysis();
    	}
    	
    	/**
    	 * Carry out a morphology analysis on a dataset, giving the mode
    	 * @param list the datasets to work on 
    	 * @param mode the type of morphology analysis to carry out
    	 * @param downFlag the next analyses to perform
    	 */
    	public MorphologyAnalysisAction(List<AnalysisDataset> list, int mode, int downFlag){
    		super(list.get(0), "Morphology analysis", "Error in analysis", downFlag);
    		programLogger.log(Level.FINE, "Creating morphology analysis");
    		this.mode = mode;
    		this.processList = list;
    		processList.remove(0); // remove the first entry
    		
    		runNewAnalysis();
    	}
    	
    	private void runNewAnalysis(){

    		try{
    			String message = null;
    			switch (this.mode) {
    			case MorphologyAnalysis.MODE_COPY:  message = "Copying morphology";
    			break;

    			case MorphologyAnalysis.MODE_REFRESH: message = "Refreshing morphology";
    			break;

    			default: message = "Morphology analysis: "+dataset.getName();
    			break;  
    			}

    			this.setProgressMessage(message);
    			this.cooldown();

    			worker = new MorphologyAnalysis(this.dataset, mode, programLogger);
    			worker.addPropertyChangeListener(this);
    			programLogger.log(Level.FINE, "Running morphology analysis");
    			worker.execute();
    		} catch(Exception e){
    			this.cancel();
    			programLogger.log(Level.SEVERE, "Error in morphology analysis", e);
    		}
    	}


    	/**
    	 * Copy the morphology information from the source dataset to the dataset
    	 * @param dataset the target
    	 * @param source the source
    	 */
    	public MorphologyAnalysisAction(AnalysisDataset dataset, AnalysisDataset source, Integer downFlag){
    		super(dataset, "Copying morphology to "+dataset.getName(), "Error in analysis");

    		this.mode = MorphologyAnalysis.MODE_COPY;
    		this.source = source;
    		if(downFlag!=null){
    			this.downFlag = downFlag;
    		}
    		
    		// always copy when a source is given
    		worker = new MorphologyAnalysis(dataset, source.getCollection(), programLogger);
    		worker.addPropertyChangeListener(this);
    		worker.execute();
    	}
    	
    	/**
    	 * Copy the morphology information from the source dataset to each dataset in a list
    	 * @param list
    	 * @param source
    	 */
    	public MorphologyAnalysisAction(List<AnalysisDataset> list, AnalysisDataset source, Integer downFlag){
    		this(list.get(0), source, downFlag); // take the first entry
    		this.processList = list;
    		processList.remove(0); // remove the first entry
    	}
      
    	@Override
    	public void finished() {
//    		final utility.Logger logger = new utility.Logger(dataset.getDebugFile(), "MainWindow");
//    		logger.log(Level.INFO, "Morphology analysis finished");
//    		logger.log("Morphology analysis finished");

    		// ensure the progress bar gets hidden even if it is not removed
    		this.progressBar.setVisible(false);

    		// The analysis takes place in a new thread to accomodate refolding.
    		// See specific comment below
    		Thread thr = new Thread(){

    			public void run(){

    				if(  (downFlag & STATS_EXPORT) == STATS_EXPORT){
//    					logger.log(Level.FINE, "Running stats export");
//    					logger.log("Running stats export", utility.Logger.DEBUG);
    					programLogger.log(Level.INFO, "Exporting stats...");
    					boolean ok = StatsExporter.run(dataset);
    					if(ok){
    						programLogger.log(Level.INFO, "OK");
    					} else {
    						programLogger.log(Level.INFO, "Error");
    					}
    				}

    				// annotate the nuclei in the population
    				if(  (downFlag & NUCLEUS_ANNOTATE) == NUCLEUS_ANNOTATE){
//    					logger.log("Running annotation", utility.Logger.DEBUG);
    					programLogger.log(Level.INFO, "Annotating nuclei...");
    					boolean ok = NucleusAnnotator.run(dataset);
    					if(ok){
    						programLogger.log(Level.INFO, "OK");
    					} else {
    						programLogger.log(Level.INFO, "Error");
    					}
    				}

    				// make a composite image of all nuclei in the collection
    				if(  (downFlag & EXPORT_COMPOSITE) == EXPORT_COMPOSITE){
//    					logger.log("Running compositor", utility.Logger.DEBUG);
    					programLogger.log(Level.INFO, "Exporting composite...");
    					boolean ok = CompositeExporter.run(dataset);
    					if(ok){
    						programLogger.log(Level.INFO, "OK");
    					} else {
    						programLogger.log(Level.INFO, "Error");
    					}
    				}

    				// The new refold action is a progressable action, so must not block
    				// the EDT. Also, the current action must wait for refolding to complete,
    				// otherwise the next MorphologyAnalysisAction in the chain will block the
    				// refold from firing a done signal. Hence, put a latch on the refold to 
    				// make this thread wait until the refolding is complete.
    				if(  (downFlag & CURVE_REFOLD) == CURVE_REFOLD){

    					final CountDownLatch latch = new CountDownLatch(1);
//    					logger.log("Running curve refolder", utility.Logger.DEBUG);
    					programLogger.log(Level.FINEST, "Morphology finished() process thread is EDT: "+SwingUtilities.isEventDispatchThread());
    					
    					new RefoldNucleusAction(dataset, latch);
    					try {
    						latch.await();
    					} catch (InterruptedException e) {
    						programLogger.log(Level.SEVERE, "Interruption to thread", e);
    					}
    				}

    				if(  (downFlag & SAVE_DATASET) == SAVE_DATASET){
//    					logger.log("Saving dataset", utility.Logger.DEBUG);
    					PopulationExporter.saveAnalysisDataset(dataset);
    				}

    				if(  (downFlag & ADD_POPULATION) == ADD_POPULATION){
//    					logger.log("Adding dataset to panel", utility.Logger.DEBUG);
    					populationsPanel.addDataset(dataset);				

    					for(AnalysisDataset child : dataset.getChildDatasets()){
    						populationsPanel.addDataset(child);
    					}
    				}

    				// if no list was provided, or no more entries remain,
    				// call the finish
    				if(processList==null){
//    					logger.log("Analysis complete, process list null, cleaning up", utility.Logger.DEBUG);
    					MorphologyAnalysisAction.super.finished();
    				} else if(processList.isEmpty()){
//    					logger.log("Analysis complete, process list empty, cleaning up", utility.Logger.DEBUG);
    					MorphologyAnalysisAction.super.finished();
    				} else {
//    					logger.log("Morphology analysis continuing; removing progress bar", utility.Logger.DEBUG);
    					// otherwise analyse the next item in the list
    					cancel();
    					if(mode == MorphologyAnalysis.MODE_COPY){

    						SwingUtilities.invokeLater(new Runnable(){
    							public void run(){
    								new MorphologyAnalysisAction(processList, source, downFlag);
    							}});
    					} else {
    						SwingUtilities.invokeLater(new Runnable(){
    							public void run(){
    								new MorphologyAnalysisAction(processList, mode, downFlag);
    							}});
    					}
    				}
    			}
    		};
    		thr.start();

    	}
    }

	
	
	/**
	 * Run a new analysis
	 */
	class NewMorphologyAnalysisAction extends ProgressableAction {
				
		private AnalysisOptions options;
		private NucleusDetector detector;
		private Date startTime;
		private String outputFolderName;
		
		public static final int NEW_ANALYSIS = 0;
		
		public NewMorphologyAnalysisAction() {
			super(null, "Nucleus detection", "Error in analysis");

			AnalysisSetupWindow analysisSetup = new AnalysisSetupWindow(programLogger);
			if( analysisSetup.getOptions()!=null){

				options = analysisSetup.getOptions();

				programLogger.log(Level.INFO, "Directory: "+options.getFolder().getName());

				this.startTime = Calendar.getInstance().getTime();
				this.outputFolderName = new SimpleDateFormat("yyyy-MM-dd_HH-mm-ss").format(this.startTime);

				// craete the analysis folder early. Did not before in case folder had no images
				File analysisFolder = new File(options.getFolder().getAbsolutePath()+File.separator+outputFolderName);
				if(!analysisFolder.exists()){
					analysisFolder.mkdir();
				}
//				utility.Logger logger = new utility.Logger( new File(options.getFolder().getAbsolutePath()+File.separator+outputFolderName+File.separator+"log.debug.txt"), "AnalysisCreator");
				File logFile = new File(options.getFolder().getAbsolutePath()+File.separator+outputFolderName+File.separator+"log.debug.txt");
//				logger.log("Analysis began: "+analysisFolder.getAbsolutePath());
//				logger.log("Directory: "+options.getFolder().getName());
				setStatus("New analysis in progress");
				
				detector = new NucleusDetector(this.outputFolderName, programLogger, logFile, options);
				detector.addPropertyChangeListener(this);
				detector.execute();
				analysisSetup.dispose();
				
			} else {
								
				analysisSetup.dispose();
				this.cancel();
			}
			
			
		}
		
		@Override
		public void finished(){
			
			final List<AnalysisDataset> datasets = detector.getDatasets();
			
			if(datasets.size()==0 || datasets==null){
				programLogger.log(Level.INFO, "No datasets returned");
				this.cancel();
			} else {

				// run next analysis on a new thread to avoid blocking the EDT
				Thread thr = new Thread(){
					
					public void run(){
						
						int flag = 0; // set the downstream analyses to run
						flag |= ADD_POPULATION;
						flag |= STATS_EXPORT;
						flag |= NUCLEUS_ANNOTATE;
						flag |= EXPORT_COMPOSITE;
						flag |= SAVE_DATASET;
						
						if(datasets.get(0).getAnalysisOptions().refoldNucleus()){
							flag |= CURVE_REFOLD;
						}
						// begin a recursive morphology analysis
						new MorphologyAnalysisAction(datasets, MorphologyAnalysis.MODE_NEW, flag);
					}
					
				};
				thr.start();

				// do not call super finished, because there is no dataset for this action
				// allow the morphology action to update the panels
				cancel();
			}
		}
		
	}
	
	
	/**
	 * Merge the selected datasets
	 */
	class MergeCollectionAction extends ProgressableAction {
						
		public MergeCollectionAction(List<AnalysisDataset> datasets, File saveFile) {
			super(null, "Merging", "Error merging");
						
			worker = new DatasetMerger(datasets, DatasetMerger.DATASET_MERGE, saveFile, programLogger);
			worker.addPropertyChangeListener(this);
			worker.execute();	
		}
		
		@Override
		public void finished(){
			
			List<AnalysisDataset> datasets = ((DatasetMerger) worker).getResults();
			
			if(datasets.size()==0 || datasets==null){
				this.cancel();
			} else {
				
				int flag = ADD_POPULATION;
				flag |= SAVE_DATASET;
				new MorphologyAnalysisAction(datasets, MorphologyAnalysis.MODE_NEW, flag);
				this.cancel();
			}
		}
	}
	


	
	@Override
	public void signalChangeReceived(SignalChangeEvent event) {
		
		final AnalysisDataset selectedDataset = populationsPanel.getSelectedDatasets().get(0);
		
		if(event.type().equals("RunShellAnalysis")){
			programLogger.log(Level.INFO, "Shell analysis selected");
			new ShellAnalysisAction(selectedDataset);
		}

		if(event.type().equals("MergeCollectionAction")){
			
			Thread thr = new Thread() {
				public void run() {


					SaveDialog saveDialog = new SaveDialog("Save merged dataset as...", "Merge_of_datasets", Constants.SAVE_FILE_EXTENSION);

					String fileName = saveDialog.getFileName();
					String folderName = saveDialog.getDirectory();
					
					if(fileName!=null && folderName!=null){
						File saveFile = new File(folderName+File.separator+fileName);

						new MergeCollectionAction(populationsPanel.getSelectedDatasets(), saveFile);
					}
				}
			};

			thr.start();
			
		}
		
		if(event.type().equals("SplitCollectionAction")){
			new SplitCollectionAction();
		}
		
		if(event.type().equals("SaveCollectionAction")){

			Thread thr = new Thread() {
				public void run() {


					SaveDialog saveDialog = new SaveDialog("Save as...", selectedDataset.getName(), ".nmd");

					String fileName = saveDialog.getFileName();
					String folderName = saveDialog.getDirectory();
					
					if(fileName!=null && folderName!=null){
						File saveFile = new File(folderName+File.separator+fileName);

						programLogger.log(Level.INFO, "Saving as "+saveFile.getAbsolutePath()+"...");
						boolean ok = PopulationExporter.saveAnalysisDataset(selectedDataset, saveFile);
						if(ok){
							programLogger.log(Level.INFO, "OK");
						} else {
							programLogger.log(Level.INFO, "Error");
						}
					}
				}
			};

			thr.start();

		}
		
		if(event.type().equals("ExtractNucleiAction")){
			Thread thr = new Thread() {
        		public void run() {

        			DirectoryChooser openDialog = new DirectoryChooser("Select directory to export images...");
        			String folderName = openDialog.getDirectory();

        			if(folderName==null){
        				return; // user cancelled
        			}

        			File folder =  new File(folderName);

        			if(!folder.isDirectory() ){
        				return;
        			}
        			if(!folder.exists()){
        				return; // check folder is ok
        			}

        			programLogger.log(Level.INFO, "Extracting nuclei from collection...");
        			boolean ok = PopulationExporter.extractNucleiToFolder(selectedDataset, folder);
        			if(ok){ 
        				programLogger.log(Level.INFO, "OK");
        			} else {
        				programLogger.log(Level.INFO, "Error");
        			}
        		}
        	};
        	thr.start();
		}
		
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

							new MorphologyAnalysisAction(selectedDataset, source, null);
						}
					} catch(Exception e1){
						programLogger.log(Level.SEVERE, "Error applying morphology", e1);
					}
				
			}});
		}
		
		if(event.type().equals("AddTailStainAction")){
			new AddTailStainAction(selectedDataset);
		}
		
		if(event.type().equals("AddNuclearSignalAction")){
			new AddNuclearSignalAction(selectedDataset);
		}
		
		if(event.type().equals("NewShellAnalysisAction")){
			new ShellAnalysisAction(selectedDataset);
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
	public void datasetEventReceived(DatasetEvent event) {
		final List<AnalysisDataset> list = event.getDatasets();
		if(!list.isEmpty()){
				
			
			if(event.method().equals(DatasetMethod.NEW_MORPHOLOGY)){
				programLogger.log(Level.INFO, "Running new morphology analysis");
				final int flag = ADD_POPULATION;
				SwingUtilities.invokeLater(new Runnable(){
					public void run(){
					
						new MorphologyAnalysisAction(list, MorphologyAnalysis.MODE_NEW, flag);

				}});
			}
			
			if(event.method().equals(DatasetMethod.REFRESH_MORPHOLOGY)){
				
				SwingUtilities.invokeLater(new Runnable(){
					public void run(){
					
						new MorphologyAnalysisAction(list, MorphologyAnalysis.MODE_REFRESH, 0);
					
				}});

			}
			
			if(event.method().equals(DatasetMethod.COPY_MORPHOLOGY)){
				
				
				final AnalysisDataset target = list.get(0);
				final AnalysisDataset source = event.secondaryDataset();
				
				SwingUtilities.invokeLater(new Runnable(){
					public void run(){
					
						new MorphologyAnalysisAction(target, source, null);
					
				}});

			}
			
						
			if(event.method().equals(DatasetMethod.CLUSTER)){
				programLogger.log(Level.INFO, "Clustering dataset");
				new ClusterAnalysisAction(list.get(0));
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
						
						programLogger.log(Level.FINEST, "Refold consensus dataset method new thread is EDT: "+SwingUtilities.isEventDispatchThread());
						
						
						final CountDownLatch latch = new CountDownLatch(1);
						programLogger.log(Level.FINEST, "Created latch: "+latch.getCount());
						new RefoldNucleusAction(list.get(0), latch);
						programLogger.log(Level.FINEST, "Running refolding");
						try {
							latch.await();
							programLogger.log(Level.FINEST, "Latch counted down: "+latch.getCount());
							programLogger.log(Level.FINEST, "Latch counted on EDT: "+SwingUtilities.isEventDispatchThread());
							
						} catch (InterruptedException e) {
							programLogger.log(Level.SEVERE, "Interruption to thread", e);
						}
					}
				};
				thr.start();
			}
			
			if(event.method().equals(DatasetMethod.SELECT_DATASETS)){
				this.populationsPanel.selectDataset(list.get(0));
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
			}
			
			
		}
		
	}

	
	@Override
	public void interfaceEventReceived(InterfaceEvent event) {
		if(event.method().equals(InterfaceMethod.UPDATE_PANELS)){
			this.updatePanels(populationsPanel.getSelectedDatasets());
		}
		
		if(event.method().equals(InterfaceMethod.UPDATE_POPULATIONS)){
			this.populationsPanel.update();
		}
		
		if(event.method().equals(InterfaceMethod.REFRESH_POPULATIONS)){
			this.populationsPanel.refreshDatasets();;
		}
		
		if(event.method().equals(InterfaceMethod.SAVE_ROOT)){
			
			for(AnalysisDataset root : populationsPanel.getRootDatasets()){
				PopulationExporter.saveAnalysisDataset(root);
			}
		}
		
	}	
}
