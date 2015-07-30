package no.gui;

import ij.IJ;
import ij.io.DirectoryChooser;
import ij.io.SaveDialog;

import java.awt.BorderLayout;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;

import javax.swing.JFrame;
import javax.swing.JPanel;
import javax.swing.border.EmptyBorder;
import javax.swing.filechooser.FileNameExtensionFilter;
import javax.swing.JLabel;
import javax.swing.JButton;

import no.analysis.AnalysisCreator;
import no.analysis.AnalysisDataset;
import no.analysis.CollectionFilterer;
import no.analysis.CurveRefolder;
import no.analysis.DatasetMerger;
import no.analysis.MorphologyAnalysis;
import no.analysis.NucleusClusterer;
import no.analysis.NucleusDetector;
import no.analysis.ShellAnalysis;
import no.analysis.SignalDetector;
import no.collections.CellCollection;
import no.components.AnalysisOptions;
import no.components.AnalysisOptions.NuclearSignalOptions;
import no.export.CompositeExporter;
import no.export.NucleusAnnotator;
import no.export.PopulationExporter;
import no.export.StatsExporter;
import no.imports.PopulationImporter;
import no.nuclei.Nucleus;

import java.awt.event.ActionEvent;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.io.File;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ExecutionException;

import javax.swing.AbstractAction;
import javax.swing.JFileChooser;
import javax.swing.JOptionPane;
import javax.swing.JProgressBar;
import javax.swing.JSplitPane;

import java.awt.Dimension;
import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;

import cell.Cell;
import cell.analysis.TubulinTailDetector;
import utility.Constants;
import utility.Logger;

import javax.swing.JTabbedPane;

public class MainWindow extends JFrame implements SignalChangeListener {
				
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
			
	/**
	 * Create the frame.
	 */
	public MainWindow() {
		
		this.setDefaultCloseOperation(DISPOSE_ON_CLOSE);
		try {
			setTitle("Nuclear Morphology Analysis v"+getVersion());
			setBounds(100, 100, 1012, 604);
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
			populationsPanel = new PopulationsPanel();
			populationsPanel.addSignalChangeListener(this);
			
			consensusNucleusPanel = new ConsensusNucleusPanel();
			consensusNucleusPanel.addSignalChangeListener(this);
			
			//---------------
			// Create the log panel
			//---------------
			logPanel = new LogPanel();
			
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
			tabbedPane.addTab("Profiles", null, nucleusProfilesPanel, null);

			//---------------
			// Create the general stats page
			//---------------
			analysisDetailPanel = new AnalysisDetailPanel();
			tabbedPane.addTab("Analysis info", analysisDetailPanel);

			//---------------
			// Create panel for split boxplots
			//---------------
			nuclearBoxplotsPanel  = new NuclearBoxplotsPanel();
			tabbedPane.addTab("Boxplots", nuclearBoxplotsPanel);
				
			
			//---------------
			// Create the signals tab panel
			//---------------
			signalsDetailPanel  = new SignalsDetailPanel();
			signalsDetailPanel.addSignalChangeListener(this);
			tabbedPane.addTab("Signals", signalsDetailPanel);
			

			//---------------
			// Create the clusters panel
			//---------------
			clusterDetailPanel = new ClusterDetailPanel();
			clusterDetailPanel.addSignalChangeListener(this);
			tabbedPane.addTab("Clusters", clusterDetailPanel);
			
			//---------------
			// Create the Venn panel
			//---------------
			vennDetailPanel = new VennDetailPanel();
			tabbedPane.addTab("Venn", null, vennDetailPanel, null);
			
			
			//---------------
			// Create the Wilcoxon test panel
			//---------------
			wilcoxonDetailPanel = new WilcoxonDetailPanel();
			tabbedPane.addTab("Wilcoxon", null, wilcoxonDetailPanel, null);
			
			//---------------
			// Create the segments boxplot panel
			//---------------
			segmentsDetailPanel = new SegmentsDetailPanel();
			tabbedPane.addTab("Segments", null, segmentsDetailPanel, null);

			//---------------
			// Create the cells panel
			//---------------
			cellDetailPanel = new CellDetailPanel();
			tabbedPane.addTab("Cells", null, cellDetailPanel, null);
			
			
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
				log("Saving root populations...");
				for(AnalysisDataset d : populationsPanel.getRootDatasets()){
					if(d.isRoot()){
						logc("Saving dataset "+d.getCollection().getName()+"...");
						PopulationExporter.saveAnalysisDataset(d);
//						d.save();
						log("OK");
					}
				}
				log("All root datasets saved");
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
			log("No version info found: functions may not work as expected");
			return true;
		}
		
		String[] parts = version.split("\\.");
		
		// major version MUST be the same
		if(Integer.valueOf(parts[0])!=Constants.VERSION_MAJOR){
			ok = false;
		}
		// dataset revision should be equal or greater to program
		if(Integer.valueOf(parts[1])<Constants.VERSION_REVISION){
			log("Dataset was created with an older version of the program");
			log("Some functionality may not work as expected");
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
	
	/**
	 * Standard log - append a newline
	 * @param s the string to log
	 */
	public void log(String s){
		logPanel.log(s);
	}
	
	/**
	 * Continuous log - do not append a newline
	 * @param s the string to log
	 */
	public void logc(String s){
		logPanel.logc(s);
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

			AnalysisDataset dataset = populationsPanel.getDataset(id);

			FishMappingWindow fishMapper = new FishMappingWindow(MainWindow.this, dataset);

			List<CellCollection> subs = fishMapper.getSubCollections();
			for(CellCollection sub : subs){

				if(sub.getNucleusCount()>0){

					logc("Reapplying morphology...");
					boolean ok = MorphologyAnalysis.reapplyProfiles(sub, dataset.getCollection());
					if(ok){
						log("OK");
					} else {
						log("Error");
					}

					dataset.addChildCollection(sub);
					
					// get the dataset craeted by adding a child collection, and put it inthe populations list
					populationsPanel.addDataset(dataset.getChildDataset(sub.getID()));

				}
			}
			populationsPanel.update();	

		} catch(Exception e){
			log("Error in FISH remapping: "+e.getMessage());
		}
	}
	
	
	/**
	 * Call the setup of a new analysis, and add the results to the dataset list 
	 */
//	public void newAnalysis(){
//
//		Thread thr = new Thread() {
//			public void run() {
//				lblStatusLine.setText("New analysis in progress");
//								
//				AnalysisCreator analysisCreator = new AnalysisCreator(MainWindow.this);
////				analysisCreator.run();
//				analysisCreator.execute();
//
//				List<AnalysisDataset> datasets = analysisCreator.getDatasets();
//				
//				if(datasets.size()==0 || datasets==null){
//					log("No datasets returned");
//				}
//				
//				// new style datasets
//				for(AnalysisDataset d : datasets){
//					
//					populationsPanel.addDataset(d);				
//					
//					for(AnalysisDataset child : d.getChildDatasets()){
//						populationsPanel.addDataset(child);
//					}
//					PopulationExporter.saveAnalysisDataset(d);
//
//				}
//								
//				lblStatusLine.setText("New analysis complete: "
//										+populationsPanel.getDatasetCount()
//										+" populations ready to view");
//				populationsPanel.update();		
//				
//			}
//		};
//		thr.start();		
//	}
		
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
					
					logc("Opening dataset...");
					
					// read the dataset
					AnalysisDataset dataset = PopulationImporter.readDataset(file);
					
					if(checkVersion( dataset.getVersion() )){

						dataset.setRoot(true);
						
						populationsPanel.addDataset(dataset);
						
						for(AnalysisDataset child : dataset.getAllChildDatasets() ){
							populationsPanel.addDataset(child);
						}
						
						log("OK");
//						log("Opened dataset: "+dataset.getName());
	
						List<AnalysisDataset> list = new ArrayList<AnalysisDataset>(0);
						list.add(dataset);
	
						updatePanels(list);
						populationsPanel.update();
					} else {
						log("Unable to open dataset version: "+ dataset.getVersion());
					}
				} catch (Exception e) {
					log("Error");
					log("Error opening dataset: "+e.getMessage());
					for(StackTraceElement el : e.getStackTrace()){
						IJ.log(el.toString());
					}
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

		Thread thr = new Thread() {
			public void run() {
				try {

					consensusNucleusPanel.update(list);
					
					nucleusProfilesPanel.update(list);
					analysisDetailPanel.update(list);
					nuclearBoxplotsPanel.update(list);
					signalsDetailPanel.update(list);
					clusterDetailPanel.update(list);
					vennDetailPanel.update(list);
					wilcoxonDetailPanel.update(list);
					cellDetailPanel.updateList(list);
					segmentsDetailPanel.update(list);
	
				} catch (Exception e) {
					log("Error updating panels: "+e.getMessage());
					for(StackTraceElement el : e.getStackTrace()){
						log(el.toString());
					}
				}
			}
		};
		thr.start();
	}
				
	
	/**
	 * Create a new CellCollection of the same class as the given dataset
	 * @param template the dataset to base on for analysis options, folders
	 * @param name the collection name
	 * @return a new empty collection
	 */
	public static CellCollection makeNewCollection(AnalysisDataset template, String name){

		CellCollection newCollection = null;

		try {

			CellCollection templateCollection = template.getCollection();

			newCollection = new CellCollection(templateCollection.getFolder(), 
					templateCollection.getOutputFolderName(), 
					name, 
					templateCollection.getDebugFile(),
					templateCollection.getNucleusClass()
					);

		} catch (Exception e) {
			IJ.log(e.getMessage());
			for(StackTraceElement el : e.getStackTrace()){
				IJ.log(el.toString());
			}
		} 
		return newCollection;
	}
	
//	class MergeCollectionAction extends AbstractAction {
//
//		private static final long serialVersionUID = 1L;
//		public MergeCollectionAction() {
//			super("Merge");
//		}
//
//		public void actionPerformed(ActionEvent e) {
//
//			final List<AnalysisDataset> datasets = populationsPanel.getSelectedDatasets();
//
//			if(datasets.size()>1){
//				log("Prepare to merge");
//
//				if(datasets.size()==2){ // check we are not merging a parent and child (would just get child)
//					if(datasets.get(0).hasChild(datasets.get(1))  || datasets.get(1).hasChild(datasets.get(0)) ){
//						log("No. That would be silly.");
//						return;
//					}
//				}
//
//				Thread thr = new Thread() {
//					public void run() {
//
//						log("Merging collection...");
//
//						// check all collections are of the same type
//						boolean newRoot = false;
//						Class<?> testClass = datasets.get(0).getAnalysisOptions().getNucleusClass();
//						for(AnalysisDataset d : datasets){
//
//							if(d.getAnalysisOptions().getNucleusClass()!=testClass){
//								log("Error: cannot merge collections of different class");
//								return;
//							}
//
//							// check if a root population is included in the merge;
//							// if so, we must make the result a root population too
//							// otherwise, it may be a subpopulation
//							if(d.isRoot()){
//								newRoot = true;
//							}
//						}
//
//						AnalysisDataset mergeParent = null;
//						if(!newRoot) { // unless we have forced root above
//							// check if all datasets are children of one root dataset
//							for(AnalysisDataset parent : populationsPanel.getAllDatasets()){
//								if(parent.isRoot()){ // only look at top level datasets for now
//									boolean ok = true; 
//									for(AnalysisDataset d : datasets){
//										if(!parent.hasChild(d)){
//											ok = false;
//										}
//									}
//									if(ok){
//										mergeParent = parent;
//									}
//								}
//							}
//
//							// if a merge parent was found, new collection is not root
//							if(mergeParent!=null){
//								newRoot = false;
//							} else {
//								newRoot = true; // if we cannot find a consistent parent, make a new root population
//							}
//						}
//
//
//						// add the nuclei from each population to the new collection
//						CellCollection newCollection = makeNewCollection(datasets.get(0), "Merged");
//						for(AnalysisDataset d : datasets){
//
//							for(Cell n : d.getCollection().getCells()){
//								if(!newCollection.getCells().contains(n)){
//									newCollection.addCell(n);
//								}
//							}
//
//						}
//
//						// create the dataset; has no analysis options at present
//						AnalysisDataset newDataset = new AnalysisDataset(newCollection);
//						newDataset.setName("Merge_of_datasets");
//						newDataset.setRoot(newRoot);
//
//						// if applicable, add the new dataset to a parent
//						if(newRoot==false && mergeParent!=null){
//
//							logc("Reapplying morphology...");
//							boolean ok = MorphologyAnalysis.reapplyProfiles(newCollection, mergeParent.getCollection());
//							if(ok){
//								log("OK");
//							} else {
//								log("Error");
//							}
//							newDataset.setAnalysisOptions(mergeParent.getAnalysisOptions());
//							newDataset.getAnalysisOptions().setRefoldNucleus(false);
//
//							mergeParent.addChildDataset(newDataset);
//						} else {
//							// otherwise, it is a new root population
//							// we need to run a fresh morphology analysis
//							logc("Running morphology analysis...");
//							boolean ok = MorphologyAnalysis.run(newDataset.getCollection());
//							if(ok){
//								log("OK");
//							} else {
//								log("Error");
//							}
//							newDataset.setAnalysisOptions(datasets.get(0).getAnalysisOptions());
//							newDataset.getAnalysisOptions().setRefoldNucleus(false);
//						}
//
//						// add the new collection to the list
//						populationsPanel.addDataset(newDataset);
//						populationsPanel.update();
//					}
//				};
//				thr.start();
//
//			} else {
//				log("Cannot merge single dataset");
//			}
//
//		}
//	}
	
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
	        			log("Splitting collection...");

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

	        			CellCollection newCollection = makeNewCollection(dataset, "Subtraction");

	        			for(Cell n : collection.getCells()){
	        				if(! negative.getCollection().getCells().contains(n)){
	        					newCollection.addCell(n);
	        				}
	        			}
	        			newCollection.setName("Not_in_"+negative.getName());
	        			UUID newID = newCollection.getID();

	        			if(newCollection.getNucleusCount()>0){

	        				logc("Reapplying morphology...");
	        				boolean ok = MorphologyAnalysis.reapplyProfiles(newCollection, dataset.getCollection());
	        				if(ok){
	        					log("OK");
	        				} else {
	        					log("Error");
	        				}
	        			}


	        			dataset.addChildCollection(newCollection);

	        			populationsPanel.addDataset(dataset.getChildDataset(newID));
	        			populationsPanel.update();
	        			
	        		} else {
	        			log("Cannot split; no children in dataset");
	        		}


				} catch (Exception e1) {
					e1.printStackTrace();
				} 
				
	        }   else {
	        	log("Cannot split multiple collections");
	        }
	    }
	}
	
	class SaveCollectionAction extends AbstractAction {

		private static final long serialVersionUID = 1L;
		public SaveCollectionAction() {
			super("Save as...");
		}

		public void actionPerformed(ActionEvent e) {

			final List<AnalysisDataset> datasets = populationsPanel.getSelectedDatasets();

			if(datasets.size()==1){

				Thread thr = new Thread() {
					public void run() {

						AnalysisDataset d = datasets.get(0);
						SaveDialog saveDialog = new SaveDialog("Save as...", d.getName(), ".nmd");

						String fileName = saveDialog.getFileName();
						String folderName = saveDialog.getDirectory();

						File saveFile = new File(folderName+File.separator+fileName);

						logc("Saving as "+saveFile.getAbsolutePath()+"...");
						PopulationExporter.saveAnalysisDataset(d, saveFile);
						log("OK");
						log("Saved dataset "+d.getCollection().getName());
					}
				};

				thr.start();
			}
		}
	}
	
	class ExtractNucleiCollectionAction extends AbstractAction {

		private static final long serialVersionUID = 1L;
		public ExtractNucleiCollectionAction() {
	        super("Extract nuclei...");
	    }
		
	    public void actionPerformed(ActionEvent e) {
	        
	        final List<AnalysisDataset> datasets = populationsPanel.getSelectedDatasets();

	        if(datasets.size()==1){

	        	Thread thr = new Thread() {
	        		public void run() {

	        			AnalysisDataset d = datasets.get(0);

	        			DirectoryChooser openDialog = new DirectoryChooser("Select directory to export images...");
	        			String folderName = openDialog.getDirectory();

	        			if(folderName==null) return; // user cancelled

	        			File folder =  new File(folderName);

	        			if(!folder.isDirectory() ){
	        				return;
	        			}
	        			if(!folder.exists()){
	        				return; // check folder is ok
	        			}

	        			logc("Extracting nuclei from collection...");
	        			boolean ok = PopulationExporter.extractNucleiToFolder(d, folder);
	        			if(ok){ 
	        				log("OK");
	        			} else {
	        				log("Error");
	        			}
	        		}
	        	};
	        	thr.start();
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
					log("Updating folder to "+folderName );

					AnalysisDataset d = datasets.get(0);
					for(Nucleus n : d.getCollection().getNuclei()){
						try{
							n.updateSourceFolder(newFolder);
						} catch(Exception e1){
							log("Error renaming nucleus: "+e1.getMessage());
						}
					}
					log("Folder updated");
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

							logc("Exporting stats...");
							boolean ok = StatsExporter.run(d.getCollection());
							if(ok){
								log("OK");
							} else {
								log("Error");
							}
						} catch(Exception e1){
							log("Error in stats export: "+e1.getMessage());
						}
					}
				};
				thr.run();
			}

		}
	}
	
	class ApplySegmentProfileAction extends AbstractAction {

		private static final long serialVersionUID = 1L;
		public ApplySegmentProfileAction() {
			super("Apply segmentation profile");
		}
		// note - this will overwrite the stats for any collection with the same name in the output folder
		public void actionPerformed(ActionEvent e) {

			final List<AnalysisDataset> datasets = populationsPanel.getSelectedDatasets();

			if(datasets.size()==1){
				// TODO make new thread
				Thread thr = new Thread() {
					public void run() {

						AnalysisDataset d = datasets.get(0);
						try{
							
							// get the desired population
							String[] names = populationsPanel.getPopulationNames().toArray(new String[0]);

							String selectedValue = (String) JOptionPane.showInputDialog(null,
									"Choose population", "Reapply segmentation",
									JOptionPane.INFORMATION_MESSAGE, null,
									names, names[0]);

							if(selectedValue!=null){

								AnalysisDataset source = populationsPanel.getDataset(selectedValue);

								logc("Reapplying morphology...");
								boolean ok = MorphologyAnalysis.reapplyProfiles(d.getCollection(), source.getCollection());
								if(ok){
									log("OK");
								} else {
									log("Error");
								}
							}
						} catch(Exception e1){
							log("Error applying morphology: "+e1.getMessage());
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

		protected AnalysisDataset d = null;
		protected List<AnalysisDataset> datasets;
		protected JProgressBar progressBar = null;
		protected String errorMessage = null;
		
		public ProgressableAction(String barMessage, String errorMessage){
			this.progressBar = new JProgressBar(0, 100);
			this.progressBar.setString(barMessage);
			this.progressBar.setStringPainted(true);
			this.errorMessage = errorMessage;
			this.datasets = populationsPanel.getSelectedDatasets();
			
//			if(datasets.size()>1){
				
//				log("Unable to analyse more than one dataset");
//			} else {
				if(datasets.size()==1){
					d = datasets.get(0);
				}
				logPanel.addProgressBar(this.progressBar);
				contentPane.revalidate();
				contentPane.repaint();
//			}
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
			contentPane.revalidate();
			contentPane.repaint();
		}
		
		public void cancel(){
			removeProgressBar();
		}
		
		
		@Override
		public void propertyChange(PropertyChangeEvent evt) {
			
			int value = (Integer) evt.getNewValue(); // should be percent
			
			if(value >=0 && value <=100){
				
				if(this.progressBar.isIndeterminate()){
					this.progressBar.setIndeterminate(false);
				}
				
				this.progressBar.setValue(value);
			}

			if(evt.getPropertyName().equals("Finished")){
				finished();
			}

			if(evt.getPropertyName().equals("Error")){
				error();
			}
			
			if(evt.getPropertyName().equals("Cooldown")){
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
			list.add(d);
			updatePanels(list); // update with the current population
		}
		
		/**
		 * Runs when an error was encountered in the analysis
		 */
		public void error(){
			log(this.errorMessage);
			removeProgressBar();
		}
		
		/**
		 * Runs if a cooldown signal is received. Use to set progress bars
		 * to an indeterminate state when no reliable progress metric is 
		 * available
		 */
		public void cooldown(){
			this.progressBar.setIndeterminate(true);
		}
		
	}
	
	
	/**
	 * Add images containing tubulin stained tails
	 * @author bms41
	 *
	 */
	class AddTailStainAction extends ProgressableAction {

		public AddTailStainAction() {
			super("Tail detection", "Error in tail detection");
			try{
				
				TailDetectionSettingsWindow analysisSetup = new TailDetectionSettingsWindow(d.getAnalysisOptions());
				
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

				TubulinTailDetector t = new TubulinTailDetector(d, folder, channel);
				t.addPropertyChangeListener(this);
				this.setProgressMessage("Tail detection:"+d.getName());
				t.execute();
			} catch(Exception e){
				this.cancel();
				IJ.log("Error in tail analysis: "+e.getMessage());
				for(StackTraceElement e1 : e.getStackTrace()){
					IJ.log(e1.toString());
				}
			}
		}
	}
	
	/**
	 * Add images containing nuclear signals
	 *
	 */
	class AddNuclearSignalAction extends ProgressableAction {
		
		public AddNuclearSignalAction() {
			super("Signal detection", "Error in signal detection");

			try{
				// add dialog for non-default detection options
				SignalDetectionSettingsWindow analysisSetup = new SignalDetectionSettingsWindow(d.getAnalysisOptions());
				
				final int channel = analysisSetup.getChannel();
				final String signalGroupName = analysisSetup.getSignalGroupName();


				NuclearSignalOptions options = d.getAnalysisOptions().getNuclearSignalOptions(signalGroupName);

				// the new signal group is one more than the highest in the collection
				int newSignalGroup = d.getHighestSignalGroup()+1;
				
				// get the folder of images
				DirectoryChooser openDialog = new DirectoryChooser("Select directory of signal images...");
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
				
				d.setSignalGroupName(newSignalGroup, signalGroupName);
				

				SignalDetector t = new SignalDetector(d, folder, channel, options, newSignalGroup, signalGroupName);
				this.setProgressMessage("Signal detection: "+signalGroupName);
				t.addPropertyChangeListener(this);
				t.execute();
				
				
				
			} catch (Exception e){
				this.cancel();
				IJ.log("Error in signal analysis: "+e.getMessage());
				for(StackTraceElement e1 : e.getStackTrace()){
					IJ.log(e1.toString());
				}
			}
			
		}		
	}
	
	/**
	 * Refold the consensus nucleus for the selected dataset using default parameters
	 */
	class RefoldNucleusAction extends ProgressableAction {

		public RefoldNucleusAction() {
			super("Curve refolding", "Error refolding nucleus");

			try{

				CurveRefolder refolder = new CurveRefolder(d.getCollection(), 
						d.getAnalysisOptions().getNucleusClass(), 
						"Fast");

				refolder.addPropertyChangeListener(this);
				this.setProgressMessage("Curve refolding:"+d.getName());
				refolder.execute();

			} catch(Exception e1){
				this.cancel();
				log("Error refolding nucleus");
			}
		}
		
		@Override
		public void finished(){
			d.getAnalysisOptions().setRefoldNucleus(true);
			d.getAnalysisOptions().setRefoldMode("Fast");
			super.finished();
		}

	}
		
	
	/**
	 * Run a new shell analysis on the selected dataset
	 */
	class ShellAnalysisAction extends ProgressableAction {
				
		public ShellAnalysisAction() {
			super("Shell analysis", "Error in shell analysis");
			
			String shellString = JOptionPane.showInputDialog(MainWindow.this, "Number of shells", 5);
			
			// validate
			int shellCount = 0;
			if(!shellString.isEmpty() && shellString!=null){
				shellCount = Integer.parseInt(shellString);
			} else {
				this.cancel();
				return;
			}
			
			ShellAnalysis analysis = new ShellAnalysis(d,shellCount);

			analysis.addPropertyChangeListener(this);
			analysis.execute();	
		}
	}

	/**
	 * Run a new clustering on the selected dataset
	 * @author bms41
	 *
	 */
	class ClusterAnalysisAction extends ProgressableAction {
		
		NucleusClusterer clusterer = null;
		
		public ClusterAnalysisAction() {
			super("Cluster analysis", "Error in cluster analysis");
			
			ClusteringSetupWindow clusterSetup = new ClusteringSetupWindow(MainWindow.this);
			Map<String, Object> options = clusterSetup.getOptions();

			if(clusterSetup.isReadyToRun()){ // if dialog was cancelled, skip
				
				progressBar = new JProgressBar(0, 100);
				progressBar.setString("Cluster analysis in progress");
				progressBar.setIndeterminate(true);
				progressBar.setStringPainted(true);
				
				logPanel.addProgressBar(progressBar);
				contentPane.revalidate();
				contentPane.repaint();


				clusterer = new NucleusClusterer(  (Integer) options.get("type"), d.getCollection() );
				clusterer.setClusteringOptions(options);
				
				clusterer.addPropertyChangeListener(this);
				clusterer.execute();

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

			log("Found "+clusterer.getNumberOfClusters()+" clusters");

			d.setClusterTree(clusterer.getNewickTree());

			for(int cluster=0;cluster<clusterer.getNumberOfClusters();cluster++){
				CellCollection c = clusterer.getCluster(cluster);
				log("Cluster "+cluster+":");

				logc("Reapplying morphology...");
				boolean ok = MorphologyAnalysis.reapplyProfiles(c, d.getCollection());
				if(ok){
					log("OK");
				} else {
					log("Error");
				}

				// attach the clusters to their parent collection
				d.addCluster(c);

				populationsPanel.addDataset(d.getChildDataset(c.getID()));

			}
			populationsPanel.update();
			super.finished();

		}
	}
	
	
	/**
	 * Run a new shell analysis on the selected dataset
	 */
	class NewMorphologyAnalysisAction extends ProgressableAction {
				
		private AnalysisOptions options;
		private NucleusDetector detector;
		private Date startTime;
		private String outputFolderName;
		
		public NewMorphologyAnalysisAction() {
			super("Nuclear analysis", "Error in analysis");
									
			lblStatusLine.setText("New analysis in progress");
			
			
			AnalysisSetupWindow analysisSetup = new AnalysisSetupWindow();
			if( analysisSetup.getOptions()!=null){

				options = analysisSetup.getOptions();

				log("Directory: "+options.getFolder().getName());

				this.startTime = Calendar.getInstance().getTime();
				this.outputFolderName = new SimpleDateFormat("yyyy-MM-dd_HH-mm-ss").format(this.startTime);

				// craete the analysis folder early. Did not before in case folder had no images
				File analysisFolder = new File(options.getFolder().getAbsolutePath()+File.separator+outputFolderName);
				if(!analysisFolder.exists()){
					analysisFolder.mkdir();
				}
				Logger logger = new Logger( new File(options.getFolder().getAbsolutePath()+File.separator+outputFolderName+File.separator+"log.debug.txt"), "AnalysisCreator");

				logger.log("Analysis began: "+analysisFolder.getAbsolutePath());
				logger.log("Directory: "+options.getFolder().getName());
				
				detector = new NucleusDetector(this.outputFolderName, MainWindow.this, logger.getLogfile(), options);
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
			
			List<AnalysisDataset> datasets = detector.getDatasets();
			
			if(datasets.size()==0 || datasets==null){
				log("No datasets returned");
				this.cancel();
			} else {
				// new style datasets
				for(AnalysisDataset d : datasets){
					
					populationsPanel.addDataset(d);				
					
					for(AnalysisDataset child : d.getChildDatasets()){
						populationsPanel.addDataset(child);
					}
					PopulationExporter.saveAnalysisDataset(d);

				}
				
				this.d = datasets.get(0); // avoid nulls
								
				lblStatusLine.setText("New analysis complete: "
										+populationsPanel.getDatasetCount()
										+" populations ready to view");
				
				log("--------\nAll done!\n--------");	
				super.finished();
			}
		}
		
	}
	
	
	/**
	 * Run a new shell analysis on the selected dataset
	 */
	class MergeCollectionAction extends ProgressableAction {
				
		
		DatasetMerger merger;
		
		public MergeCollectionAction() {
			super("Merging", "Error merging");
			
			List<AnalysisDataset> datasets = populationsPanel.getSelectedDatasets();
			
			merger = new DatasetMerger(datasets, DatasetMerger.DATASET_MERGE);

			merger.addPropertyChangeListener(this);
			merger.execute();	
		}
		
		@Override
		public void finished(){
			
//			IJ.log("Finishing");
			List<AnalysisDataset> datasets = merger.getResults();
			
			if(datasets.size()==0 || datasets==null){
				this.cancel();
			} else {
				// new style datasets
				for(AnalysisDataset d : datasets){
					
					populationsPanel.addDataset(d);				
					
					for(AnalysisDataset child : d.getChildDatasets()){
						populationsPanel.addDataset(child);
					}
					PopulationExporter.saveAnalysisDataset(d);

				}
				
				this.d = datasets.get(0); // avoid nulls
								
				lblStatusLine.setText("Merge complete: "
										+populationsPanel.getDatasetCount()
										+" populations ready to view");
				
				log("Merge complete");	
				super.finished();
			}
		}
	}


	
	@Override
	public void signalChangeReceived(SignalChangeEvent event) {
		
		if(event.type().equals("RefoldNucleusFired")){
			new RefoldNucleusAction();
		}
		
		
		if(event.type().equals("RunShellAnalysis")){
			log("Shell analysis selected");
			new ShellAnalysisAction();
		}
		
		if(event.type().equals("NewClusterAnalysis")){
			new ClusterAnalysisAction();
		}
		
		if(event.type().equals("MergeCollectionAction")){
			new MergeCollectionAction();
		}
		
		if(event.type().equals("SplitCollectionAction")){
			new SplitCollectionAction();
		}
		
		if(event.type().equals("SaveCollectionAction")){
			new SaveCollectionAction();
		}
		
		if(event.type().equals("ExtractNucleiAction")){
			new ExtractNucleiCollectionAction();
		}
		
		if(event.type().equals("ChangeNucleusFolderAction")){
			new ReplaceNucleusFolderAction();
		}
		
		if(event.type().equals("ExportDatasetStatsAction")){
			new ExportDatasetStatsAction();
		}
		
		if(event.type().equals("ReapplySegmentProfileAction")){
			new ApplySegmentProfileAction();
		}
		
		if(event.type().equals("AddTailStainAction")){
			new AddTailStainAction();
		}
		
		if(event.type().equals("AddNuclearSignalAction")){
			new AddNuclearSignalAction();
		}
		
		if(event.type().equals("NewShellAnalysisAction")){
			new ShellAnalysisAction();
		}
		
		if(event.type().equals("UpdatePanels")){
			this.updatePanels(populationsPanel.getSelectedDatasets());
		}
		
		if(event.type().startsWith("Log_")){
			String s = event.type().replace("Log_", "");
			log(s);
		}
	}	
}
