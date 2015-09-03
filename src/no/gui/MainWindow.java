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

import no.analysis.AnalysisDataset;
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
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.CountDownLatch;

import javax.swing.AbstractAction;
import javax.swing.JFileChooser;
import javax.swing.JOptionPane;
import javax.swing.JProgressBar;
import javax.swing.JSpinner;
import javax.swing.JSplitPane;
import javax.swing.SpinnerNumberModel;
import javax.swing.SwingUtilities;
import javax.swing.SwingWorker;

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
	
	
	// Flags to pass to ProgressableActions to determine the analyses
	// to carry out in subsequently
	private static final int ADD_POPULATION		 = 1;
	private static final int STATS_EXPORT 		 = 2;
	private static final int NUCLEUS_ANNOTATE	 = 4;
	private static final int CURVE_REFOLD 		 = 8;
	private static final int EXPORT_COMPOSITE	 = 16;
	private static final int SAVE_DATASET		 = 32;
			
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
			tabbedPane.addTab("Nuclear charts", nuclearBoxplotsPanel);
				
			
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
			tabbedPane.addTab("Clusters and merges", clusterDetailPanel);
			
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
	 * Write an error message to the log panel,
	 * together with the stack trace of the exception
	 * @param message the message
	 * @param e the exception
	 */
	public void error(String message, Exception e){
		log(message+": "+e.getMessage());
		for(StackTraceElement el : e.getStackTrace()){
			log(el.toString());
		}
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

			FishMappingWindow fishMapper = new FishMappingWindow(MainWindow.this, dataset);

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
					logc("Reapplying morphology...");
					new MorphologyAnalysisAction(list, dataset, ADD_POPULATION);
				}
			};
			thr.start();
			

			
			// get the dataset craeted by adding a child collection, and put it inthe populations list
//			populationsPanel.addDataset(dataset.getChildDataset(sub.getID()));
//			populationsPanel.update();	

		} catch(Exception e){
			log("Error in FISH remapping: "+e.getMessage());
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
					
					logc("Opening dataset...");
					
					// read the dataset
					AnalysisDataset dataset = PopulationImporter.readDataset(file);
					
					if(checkVersion( dataset.getVersion() )){

						dataset.setRoot(true);
						
						populationsPanel.addDataset(dataset);
						
						for(AnalysisDataset child : dataset.getAllChildDatasets() ){
							populationsPanel.addDataset(child);
						}
						
						// update the log file to the same folder as the dataset
						File logFile = new File(file.getParent()+File.separator+file.getName().replace(Constants.SAVE_FILE_EXTENSION, Constants.LOG_FILE_EXTENSION));
						
						dataset.getCollection().setDebugFile(logFile);
						
						log("OK");
//						
	
						List<AnalysisDataset> list = new ArrayList<AnalysisDataset>(0);
						list.add(dataset);
	
						updatePanels(list);
						populationsPanel.update();
						
					} else {
						log("Unable to open dataset version: "+ dataset.getVersion());
					}
				} catch (Exception e) {
					error("Error opening dataset", e);
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
					error("Error updating panels", e);
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

	        				logc("Reapplying morphology...");
	        				
	        				AnalysisDataset newDataset = dataset.getChildDataset(newCollection.getID());
	        				new MorphologyAnalysisAction(newDataset, dataset, null);
	        			}


	        			

	        			populationsPanel.addDataset(dataset.getChildDataset(newID));
	        			populationsPanel.update();
	        			
	        		} else {
	        			log("Cannot split; no children in dataset");
	        		}


				} catch (Exception e1) {
					error("Error splitting collection", e1);
				} 
				
	        }   else {
	        	log("Cannot split multiple collections");
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
							error("Error in stats export", e1);
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
//				IJ.log("Worker finished by trigger");
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
				
				TailDetectionSettingsWindow analysisSetup = new TailDetectionSettingsWindow(dataset.getAnalysisOptions());
				
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
				MainWindow.this.error("Error in tail analysis", e);

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
				SignalDetectionSettingsWindow analysisSetup = new SignalDetectionSettingsWindow(dataset.getAnalysisOptions());
				
				final int channel = analysisSetup.getChannel();
				final String signalGroupName = analysisSetup.getSignalGroupName();


				NuclearSignalOptions options = dataset.getAnalysisOptions().getNuclearSignalOptions(signalGroupName);

				// the new signal group is one more than the highest in the collection
				int newSignalGroup = dataset.getHighestSignalGroup()+1;
				
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
				
				dataset.setSignalGroupName(newSignalGroup, signalGroupName);
				this.signalGroup = newSignalGroup;
				

				worker = new SignalDetector(dataset, folder, channel, options, newSignalGroup, signalGroupName);
				this.setProgressMessage("Signal detection: "+signalGroupName);
				worker.addPropertyChangeListener(this);
				worker.execute();
				
				
				
			} catch (Exception e){
				this.cancel();
				MainWindow.this.error("Error in signal analysis", e);
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

			log("Sub-population: "+collection.getType()+" : "+collection.getNucleusCount()+" nuclei");

			dataset.addChildDataset(subDataset);
		}

		/*
	    Given a complete collection of nuclei, split it into up to 4 populations;
	      nuclei with red signals, with green signals, without red signals and without green signals
	    Only include the 'without' populations if there is a 'with' population.
		 */
		private List<CellCollection> dividePopulationBySignals(CellCollection r, int signalGroup){

			List<CellCollection> signalPopulations = new ArrayList<CellCollection>(0);
			log("Dividing population by signals...");
			try{

				List<Cell> list = r.getCellsWithNuclearSignals(signalGroup, true);
				if(!list.isEmpty()){
					log("Found nuclei with signals in group "+signalGroup);
					CellCollection listCollection = new CellCollection(r.getFolder(), 
							r.getOutputFolderName(), 
							"Signals_in_group_"+signalGroup, 
							r.getDebugFile(), 
							r.getNucleusClass());

					for(Cell c : list){
						listCollection.addCell( c );
					}
					signalPopulations.add(listCollection);

					List<Cell> notList = r.getCellsWithNuclearSignals(signalGroup, false);
					if(!notList.isEmpty()){
						log("Found nuclei without signals in group "+signalGroup);
						CellCollection notListCollection = new CellCollection(r.getFolder(), 
								r.getOutputFolderName(), 
								"No_signals_in_group_"+signalGroup, 
								r.getDebugFile(), 
								r.getNucleusClass());

						for(Cell c : notList){
							notListCollection.addCell( c );
						}
						signalPopulations.add(notListCollection);
					}

				}

			} catch(Exception e){
				MainWindow.this.error("Cannot create collection", e);
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
//		private CountDownLatch doneSignal;
		
		public RefoldNucleusAction(AnalysisDataset dataset, CountDownLatch doneSignal) {
			super(dataset, "Refolding", "Error refolding nucleus");
			
			try{

				this.progressBar.setIndeterminate(true);
				worker = new CurveRefolder(dataset.getCollection(), 
						"Fast", 
						doneSignal);

				worker.addPropertyChangeListener(this);
				this.setProgressMessage("Refolding: "+dataset.getName());
				worker.execute();

			} catch(Exception e1){
				this.cancel();
				MainWindow.this.error("Error refolding nucleus", e1);
			}
		}
		
		@Override
		public void finished(){
			Logger logger = new Logger(dataset.getDebugFile(), "MainWindow");
    		logger.log("Refolding finished, cleaning up");
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
			Map<String, Object> options = clusterSetup.getOptions();

			if(clusterSetup.isReadyToRun()){ // if dialog was cancelled, skip

				worker = new NucleusClusterer(  (Integer) options.get("type"), dataset.getCollection() );
				((NucleusClusterer) worker).setClusteringOptions(options);
				
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

			log("Found "+((NucleusClusterer) worker).getNumberOfClusters()+" clusters");

			dataset.setClusterTree(((NucleusClusterer) worker).getNewickTree());
			
			List<AnalysisDataset> list = new ArrayList<AnalysisDataset>();

			for(int cluster=0;cluster<((NucleusClusterer) worker).getNumberOfClusters();cluster++){
				CellCollection c = ((NucleusClusterer) worker).getCluster(cluster);
				
				// attach the clusters to their parent collection
				dataset.addCluster(c);
				
				log("Cluster "+cluster+": "+c.getNucleusCount()+" nuclei");
				AnalysisDataset clusterDataset = dataset.getChildDataset(c.getID());
				list.add(clusterDataset);

			}
			
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
    		this.mode = mode;
    		this.processList = list;
    		processList.remove(0); // remove the first entry
    		
    		runNewAnalysis();
    	}
    	
    	private void runNewAnalysis(){
    		
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

    		worker = new MorphologyAnalysis(this.dataset.getCollection(), mode);
    		worker.addPropertyChangeListener(this);
    		worker.execute();
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
    		worker = new MorphologyAnalysis(dataset.getCollection(), source.getCollection());
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
    		final Logger logger = new Logger(dataset.getDebugFile(), "MainWindow");
    		logger.log("Morphology analysis finished");

    		// ensure the progress bar gets hidden even if it is not removed
    		this.progressBar.setVisible(false);

    		Thread thr = new Thread(){

    			public void run(){

    				if(  (downFlag & STATS_EXPORT) == STATS_EXPORT){
    					logger.log("Running stats export", Logger.DEBUG);
    					logc("Exporting stats...");
    					boolean ok = StatsExporter.run(dataset.getCollection());
    					if(ok){
    						log("OK");
    					} else {
    						log("Error");
    					}
    				}

    				// annotate the nuclei in the population
    				if(  (downFlag & NUCLEUS_ANNOTATE) == NUCLEUS_ANNOTATE){
    					logger.log("Running annotation", Logger.DEBUG);
    					logc("Annotating nuclei...");
    					boolean ok = NucleusAnnotator.run(dataset.getCollection());
    					if(ok){
    						log("OK");
    					} else {
    						log("Error");
    					}
    				}

    				// make a composite image of all nuclei in the collection
    				if(  (downFlag & EXPORT_COMPOSITE) == EXPORT_COMPOSITE){
    					logger.log("Running compositor", Logger.DEBUG);
    					logc("Exporting composite...");
    					boolean ok = CompositeExporter.run(dataset.getCollection());
    					if(ok){
    						log("OK");
    					} else {
    						log("Error");
    					}
    				}

    				//TODO: This runs in parallel with next mophology analysis, and never cleans up the progress
    				// bar because the thread is blocked
    				if(  (downFlag & CURVE_REFOLD) == CURVE_REFOLD){

    					final CountDownLatch latch = new CountDownLatch(1);
    					logger.log("Running curve refolder", Logger.DEBUG);

    					new RefoldNucleusAction(dataset, latch);
    					try {
    						latch.await();
    					} catch (InterruptedException e) {
    						MainWindow.this.error("Interruption to thread", e);
    					}
    				}

    				if(  (downFlag & SAVE_DATASET) == SAVE_DATASET){
    					logger.log("Saving dataset", Logger.DEBUG);
    					PopulationExporter.saveAnalysisDataset(dataset);
    				}

    				if(  (downFlag & ADD_POPULATION) == ADD_POPULATION){
    					logger.log("Adding dataset to panel", Logger.DEBUG);
    					populationsPanel.addDataset(dataset);				

    					for(AnalysisDataset child : dataset.getChildDatasets()){
    						populationsPanel.addDataset(child);
    					}
    				}

    				// if no list was provided, or no more entries remain,
    				// call the finish
    				if(processList==null){
    					logger.log("Analysis complete, process list null, cleaning up", Logger.DEBUG);
    					MorphologyAnalysisAction.super.finished();
    				} else if(processList.isEmpty()){
    					logger.log("Analysis complete, process list empty, cleaning up", Logger.DEBUG);
    					MorphologyAnalysisAction.super.finished();
    				} else {
    					logger.log("Morphology analysis continuing; removing progress bar", Logger.DEBUG);
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
				setStatus("New analysis in progress");
				
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
			
			final List<AnalysisDataset> datasets = detector.getDatasets();
			
			if(datasets.size()==0 || datasets==null){
				log("No datasets returned");
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
						
			worker = new DatasetMerger(datasets, DatasetMerger.DATASET_MERGE, saveFile);
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
			log("Shell analysis selected");
			new ShellAnalysisAction(selectedDataset);
		}
		
		if(event.type().equals("NewClusterAnalysis")){
			new ClusterAnalysisAction(selectedDataset);
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

						logc("Saving as "+saveFile.getAbsolutePath()+"...");
						boolean ok = PopulationExporter.saveAnalysisDataset(selectedDataset, saveFile);
						if(ok){
							log("OK");
						} else {
							log("Error");
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

        			logc("Extracting nuclei from collection...");
        			boolean ok = PopulationExporter.extractNucleiToFolder(selectedDataset, folder);
        			if(ok){ 
        				log("OK");
        			} else {
        				log("Error");
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
						error("Error applying morphology", e1);
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
		
		if(event.type().startsWith("Log_")){
			String s = event.type().replace("Log_", "");
			log(s);
		}
		
		if(event.type().startsWith("Status_")){
			String s = event.type().replace("Status_", "");
			setStatus(s);
		}
		
		if(event.type().startsWith("SelectDataset_")){
			String s = event.type().replace("SelectDataset_", "");
			UUID id = UUID.fromString(s);
			this.populationsPanel.selectDataset(id);
		}
		
		if(event.type().startsWith("ExtractSource_")){
			log("Recovering source dataset");
			String name = event.type().replace("ExtractSource_", "");
			// get the uuid of the dataset from the currently selected dataset
			AnalysisDataset parent = selectedDataset;
			for(UUID id : parent.getMergeSources()){
				
				AnalysisDataset child = parent.getMergeSource(id);
				
				// add the dataset to the populations panel
				if(child.getName().equals(name)){
					child.setRoot(true);
					populationsPanel.addDataset(child);
					populationsPanel.update();
				}
			}

		}
		
		if(event.type().startsWith("MorphologyRefresh_")){
			String s = event.type().replace("MorphologyRefresh_", "");
			UUID id = UUID.fromString(s);
			final AnalysisDataset d = populationsPanel.getDataset(id);
			
			SwingUtilities.invokeLater(new Runnable(){
				public void run(){
				
					new MorphologyAnalysisAction(d, MorphologyAnalysis.MODE_REFRESH, 0);
				
			}});

		}
		
		if(event.type().startsWith("MorphologyCopy_")){
			String s = event.type().replace("MorphologyCopy_", "");
			String[] array = s.split("|");
			UUID targetID = UUID.fromString(array[0]);
			UUID sourceID = UUID.fromString(array[1]);
			
			final AnalysisDataset target = populationsPanel.getDataset(targetID);
			final AnalysisDataset source = populationsPanel.getDataset(sourceID);
			
			SwingUtilities.invokeLater(new Runnable(){
				public void run(){
				
					new MorphologyAnalysisAction(target, source, null);
				
			}});
			
			
		}
		
		if(event.type().startsWith("MorphologyNew_")){
			String s = event.type().replace("MorphologyNew_", "");
			UUID id = UUID.fromString(s);
			final AnalysisDataset d = populationsPanel.getDataset(id);
			final int flag = ADD_POPULATION;
			SwingUtilities.invokeLater(new Runnable(){
				public void run(){
				
					new MorphologyAnalysisAction(d, MorphologyAnalysis.MODE_NEW, flag);
				
			}});
			
		}
		
		if(event.type().startsWith("RefoldConsensus_")){
			
			String s = event.type().replace("RefoldConsensus_", "");
			UUID id = UUID.fromString(s);
			final AnalysisDataset d = populationsPanel.getDataset(id);
			final CountDownLatch latch = new CountDownLatch(1);
			SwingUtilities.invokeLater(new Runnable(){
				public void run(){
				
					new RefoldNucleusAction(d, latch);
				
			}});
		}
		
		
		
	}	
}
