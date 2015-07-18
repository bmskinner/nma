package no.gui;

import ij.IJ;
import ij.io.DirectoryChooser;
import ij.io.SaveDialog;

import java.awt.BorderLayout;

import javax.swing.JFrame;
import javax.swing.JPanel;
import javax.swing.border.EmptyBorder;
import javax.swing.event.ListSelectionEvent;
import javax.swing.event.ListSelectionListener;
import javax.swing.event.TreeSelectionEvent;
import javax.swing.event.TreeSelectionListener;
import javax.swing.filechooser.FileNameExtensionFilter;
import javax.swing.table.TableModel;
import javax.swing.text.DefaultCaret;
import javax.swing.tree.DefaultMutableTreeNode;
import javax.swing.tree.TreeSelectionModel;
import javax.swing.JLabel;
import javax.swing.JButton;

import org.jdesktop.swingx.JXTreeTable;
import org.jdesktop.swingx.treetable.AbstractMutableTreeTableNode;
import org.jdesktop.swingx.treetable.DefaultMutableTreeTableNode;
import org.jdesktop.swingx.treetable.DefaultTreeTableModel;

import no.analysis.AnalysisCreator;
import no.analysis.AnalysisDataset;
import no.analysis.CurveRefolder;
import no.analysis.MorphologyAnalysis;
import no.analysis.NucleusClusterer;
import no.analysis.ShellAnalysis;
import no.analysis.SignalDetector;
import no.collections.CellCollection;
import no.components.AnalysisOptions.NuclearSignalOptions;
import no.export.PopulationExporter;
import no.export.StatsExporter;
import no.imports.PopulationImporter;
import no.nuclei.Nucleus;

import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.ComponentAdapter;
import java.awt.event.ComponentEvent;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;

import javax.swing.JTextArea;

import java.awt.SystemColor;
import java.io.File;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

import javax.swing.AbstractAction;
import javax.swing.Box;
import javax.swing.ButtonGroup;
import javax.swing.ComboBoxModel;
import javax.swing.DefaultComboBoxModel;
import javax.swing.JCheckBox;
import javax.swing.JComboBox;
import javax.swing.JFileChooser;
import javax.swing.JMenuItem;
import javax.swing.JOptionPane;
import javax.swing.JPopupMenu;
import javax.swing.JProgressBar;
import javax.swing.JRadioButton;
import javax.swing.JScrollPane;
import javax.swing.BoxLayout;
import javax.swing.SwingConstants;

import java.awt.Font;

import javax.swing.JTable;

import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.GridLayout;
import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;

import javax.swing.ListSelectionModel;

import org.jfree.chart.ChartFactory;
import org.jfree.chart.ChartPanel;
import org.jfree.chart.JFreeChart;
import org.jfree.chart.plot.PlotOrientation;
import org.jfree.chart.plot.ValueMarker;
import org.jfree.chart.plot.XYPlot;
import org.jfree.data.statistics.BoxAndWhiskerCategoryDataset;
import org.jfree.data.statistics.DefaultBoxAndWhiskerCategoryDataset;
import org.jfree.data.xy.DefaultXYDataset;
import org.jfree.data.xy.XYDataset;

import cell.Cell;
import cell.analysis.TubulinTailDetector;
import datasets.NucleusDatasetCreator;
import utility.Constants;
import utility.TreeOrderHashMap;

import javax.swing.JTabbedPane;

public class MainWindow extends JFrame implements ActionListener {
			
	private static final int PROFILE_TAB = 0;
	private static final int STATS_TAB = 1;
	private static final int ANALYSIS_TAB = 2;
	private static final int BOXPLOTS_TAB = 3;
	private static final int SIGNALS_TAB = 4;
	private static final int CLUSTERS_TAB = 5;
	private static final int VENN_TAB = 6;
	
	private static final long serialVersionUID = 1L;
	private JPanel contentPane;
	private JTextArea textArea = new JTextArea();;
	private JLabel lblStatusLine = new JLabel("No analysis open");

	private JTable tablePopulationStats;
	private JTable tableAnalysisParamters;
	private final JPanel panelGeneralData = new JPanel(); // holds the tabs
	
	private JPanel logPanel;
	private JPanel progressPanel;
	private final JPanel panelPopulations = new JPanel(); // holds list of active populations
	private JTable populationTable;
	private JXTreeTable treeTable;
	private PopulationListPopupMenu populationPopup;
	
	private JTabbedPane tabbedPane;
	
	private NucleusProfilesPanel nucleusProfilesPanel;
	
	private SignalsDetailPanel signalsDetailPanel;
		
	private ChartPanel consensusChartPanel;

	private JButton runRefoldingButton;
	
	private NuclearBoxplotsPanel nuclearBoxplotsPanel;
		
	private JPanel clusteringPanel;// container for clustering options and display
	
	private JPanel vennPanel; // show overlaps between populations
	private JTable vennTable;
	
	private WilcoxonDetailPanel wilcoxonDetailPanel;
	
	private ChartPanel segmentsBoxplotChartPanel; // for displaying the legnth of a given segment
	private ChartPanel segmentsProfileChartPanel; // for displaying the profiles of a given segment
	
	// 
	private JCheckBox    normSegmentCheckBox = new JCheckBox("Normalised");	// to toggle raw or normalised segment profiles in segmentsProfileChartPanel
	private JRadioButton rawSegmentLeftButton  = new JRadioButton("Left"); // left align raw segment profiles in segmentsProfileChartPanel
	private JRadioButton rawSegmentRightButton = new JRadioButton("Right"); // right align raw segment profiles in segmentsProfileChartPanel
	
	
	
	private JPanel segmentsBoxplotPanel;// container for boxplots chart and decoration
	private JComboBox<String> segmentSelectionBox; // choose which segments to compare
	
	private CellDetailPanel cellDetailPanel;

	private HashMap<String, UUID> populationNames = new HashMap<String, UUID>();
	
	private HashMap<UUID, AnalysisDataset> analysisDatasets = new HashMap<UUID, AnalysisDataset>();
	
	private TreeOrderHashMap treeOrderMap = new TreeOrderHashMap(); // order the root datasets


	/**
	 * Create the frame.
	 */
	public MainWindow() {
		
		this.setDefaultCloseOperation(DISPOSE_ON_CLOSE);
		try {
			setTitle("Nuclear Morphology Analysis v"+getVersion());
			setDefaultCloseOperation(JFrame.DISPOSE_ON_CLOSE);
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
			// General data
			//---------------
			
			contentPane.add(panelGeneralData, BorderLayout.CENTER);
			panelGeneralData.setLayout(new GridLayout(2, 0, 0, 0));
			
			// make a panel for the populations and consensus chart
			final JPanel topGeneralPanel = new JPanel();
//			topGeneralPanel.setLayout(new GridLayout(0, 2, 0, 0));
			topGeneralPanel.setLayout(new GridBagLayout());
			
//			GridBagConstraints generalPanelConstraints = new GridBagConstraints();
//			generalPanelConstraints.gridwidth = GridBagConstraints.REMAINDER;     // last
//			generalPanelConstraints.fill = GridBagConstraints.BOTH;
//			generalPanelConstraints.weightx = 1.0;
			
			panelGeneralData.add(topGeneralPanel);
			
			//reset to default
			
			//---------------
			// Create the consensus chart
			//---------------
			createPopulationsPanel();
			createConsensusChartPanel();
			
			//---------------
			// Create the log panel
			//---------------
			GridBagConstraints c = new GridBagConstraints();			
			c.gridwidth = 1;     // frist
			c.fill = GridBagConstraints.BOTH;
			c.weightx = 1.0;
			
			logPanel = createLogPanel();
			
			topGeneralPanel.add(logPanel, c);
			
			c.gridwidth = GridBagConstraints.RELATIVE;     //next to last
			topGeneralPanel.add(panelPopulations, c);	
			
			c.anchor = GridBagConstraints.WEST;
			c.gridwidth = GridBagConstraints.REMAINDER; //end of row
			c.fill = GridBagConstraints.BOTH;      //reset to default
			c.weightx = 0.0;   
			topGeneralPanel.add(consensusChartPanel, c);
			

	        topGeneralPanel.addComponentListener(new ComponentAdapter() {
	            @Override
	            public void componentResized(ComponentEvent e) {
	                resizePreview(consensusChartPanel, topGeneralPanel);
	            }
	        });
			
			

			tabbedPane = new JTabbedPane(JTabbedPane.TOP);
			panelGeneralData.add(tabbedPane);
			
			//---------------
			// Create the profiles
			//---------------
			nucleusProfilesPanel = new NucleusProfilesPanel();
			tabbedPane.addTab("Profiles", null, nucleusProfilesPanel, null);

			//---------------
			// Create the general stats page
			//---------------
			JScrollPane statsPanel = createStatsPanel();
			tabbedPane.addTab("Basic statistics", null, statsPanel, null);
			
			JScrollPane parametersPanel = createAnalysisParametersPanel();
			tabbedPane.addTab("Analysis info", null, parametersPanel, null);

			//---------------
			// Create panel for split boxplots
			//---------------
			nuclearBoxplotsPanel  = new NuclearBoxplotsPanel();
			tabbedPane.addTab("Boxplots", nuclearBoxplotsPanel);
				
			
			//---------------
			// Create the signals tab panel
			//---------------
			signalsDetailPanel  = new SignalsDetailPanel();
			tabbedPane.addTab("Signals", signalsDetailPanel);
			

			//---------------
			// Create the clusters panel
			//---------------
			clusteringPanel = new JPanel();
			clusteringPanel.setLayout(new BoxLayout(clusteringPanel, BoxLayout.Y_AXIS));
			tabbedPane.addTab("Clusters", null, clusteringPanel, null);
			
			//---------------
			// Create the Venn panel
			//---------------
			vennPanel = new JPanel();
			vennPanel.setLayout(new BorderLayout());
			tabbedPane.addTab("Venn", null, vennPanel, null);
			
			vennTable = new JTable();
			vennPanel.add(vennTable, BorderLayout.CENTER);
			vennTable.setEnabled(false);
			vennPanel.add(vennTable.getTableHeader(), BorderLayout.NORTH);
			vennTable.setModel(NucleusDatasetCreator.createVennTable(null));
			
			
			//---------------
			// Create the Wilcoxon test panel
			//---------------
			wilcoxonDetailPanel = new WilcoxonDetailPanel();
			tabbedPane.addTab("Wilcoxon", null, wilcoxonDetailPanel, null);
			
			//---------------
			// Create the segments boxplot panel
			//---------------
			JPanel segmentsPanel = new JPanel();
			segmentsPanel.setLayout(new GridLayout(0,2,0,0));
			
			JPanel segmentProfilePanel  = createSegmentProfilePanel();
			segmentsBoxplotPanel = createSegmentBoxplotsPanel();
			
			segmentsPanel.add(segmentProfilePanel);
			segmentsPanel.add(segmentsBoxplotPanel);
			tabbedPane.addTab("Segments", null, segmentsPanel, null);

			//---------------
			// Create the cells panel
			//---------------
			cellDetailPanel = new CellDetailPanel();
			tabbedPane.addTab("Cells", null, cellDetailPanel, null);

		} catch (Exception e) {
			IJ.log("Error initialising Main: "+e.getMessage());
			for(StackTraceElement el : e.getStackTrace()){
				IJ.log(el.toString());
			}
		}
		
	}
	
	/**
	 * Create the log panel for updates
	 * @return a scrollable panel
	 */
	private JPanel createLogPanel(){
		JPanel panel = new JPanel();
		panel.setLayout(new BorderLayout());
		JScrollPane scrollPane = new JScrollPane();
		textArea.setFont(new Font("Monospaced", Font.PLAIN, 13));
		DefaultCaret caret = (DefaultCaret)textArea.getCaret();
		caret.setUpdatePolicy(DefaultCaret.ALWAYS_UPDATE);
		
		scrollPane.setViewportView(textArea);
		textArea.setBackground(SystemColor.menu);
		textArea.setEditable(false);
		textArea.setRows(9);
		textArea.setColumns(40);
		
		
		JLabel lblAnalysisLog = new JLabel("Analysis Log");
		lblAnalysisLog.setHorizontalAlignment(SwingConstants.CENTER);
		scrollPane.setColumnHeaderView(lblAnalysisLog);
		panel.add(scrollPane, BorderLayout.CENTER);

		progressPanel = new JPanel();
		progressPanel.setLayout(new BoxLayout(progressPanel, BoxLayout.Y_AXIS));
//		progressPanel.add(new JLabel("Analyses in progress:"));
		panel.add(progressPanel, BorderLayout.NORTH);
		
		return panel;
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
				newAnalysis();
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
				for(AnalysisDataset d : MainWindow.this.analysisDatasets.values()){
					if(d.isRoot()){
						logc("Saving dataset "+d.getCollection().getName()+"...");
						d.save();
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
				postAnalysis();
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
	 * Create the tree for populations analysed
	 */
	private void createPopulationsPanel(){
		//---------------
		// Create the populations list
		//---------------
		panelPopulations.setMinimumSize(new Dimension(100, 100));

		panelPopulations.setLayout(new BoxLayout(panelPopulations, BoxLayout.Y_AXIS));
					
		// tree table approach
		List<String> columns = new ArrayList<String>();
		columns.add("Population");
		columns.add("Nuclei");
		columns.add("");

		DefaultTreeTableModel treeTableModel = new DefaultTreeTableModel();
		DefaultMutableTreeTableNode  root = new DefaultMutableTreeTableNode ("root node");
		treeTableModel.setRoot(root);
		treeTableModel.setColumnIdentifiers(columns);
		
		treeTable = new JXTreeTable(treeTableModel);
		treeTable.setEnabled(true);
		treeTable.setCellSelectionEnabled(false);
		treeTable.setColumnSelectionAllowed(false);
		treeTable.setRowSelectionAllowed(true);
		treeTable.getColumnModel().getColumn(2).setCellRenderer(new PopulationTableCellRenderer());
		treeTable.getColumnModel().getColumn(0).setPreferredWidth(120);
		treeTable.getColumnModel().getColumn(2).setPreferredWidth(5);
		
		populationPopup = new PopulationListPopupMenu();
		populationPopup.disableAll();
		
		treeTable.setComponentPopupMenu(populationPopup);
		
		treeTable.addMouseListener(new MouseAdapter() {
			@Override
			public void mouseClicked(MouseEvent e) {
				
				JXTreeTable table = (JXTreeTable) e.getSource();
				
				// double click
				if (e.getClickCount() == 2) {
					int index = table.rowAtPoint((e.getPoint()));
					if (index >= 0) {
						Object o = table.getModel().getValueAt(index, 0);
						UUID id = MainWindow.this.populationNames.get(o.toString());
						renameCollection(MainWindow.this.analysisDatasets.get(id));
					}
				}

			}
		});

		TreeSelectionModel tableSelectionModel = treeTable.getTreeSelectionModel();
		tableSelectionModel.addTreeSelectionListener(new TreeSelectionHandler());
		
		JScrollPane populationScrollPane = new JScrollPane(treeTable);		
		
		panelPopulations.add(populationScrollPane);
	}
	
	/**
	 * Create the chart that will hold the refolded consensus nucleus
	 */
	private void createConsensusChartPanel(){
		JFreeChart consensusChart = ChartFactory.createXYLineChart(null,
				null, null, null);
		XYPlot consensusPlot = consensusChart.getXYPlot();
		consensusPlot.setBackgroundPaint(Color.WHITE);
		consensusPlot.getDomainAxis().setVisible(false);
		consensusPlot.getRangeAxis().setVisible(false);
		
		consensusChartPanel = new ChartPanel(consensusChart);

		runRefoldingButton = new JButton("Refold");
//		runRefoldingButton.addActionListener(new RefoldNucleusAction());

		runRefoldingButton.addMouseListener(new MouseAdapter() {
			@Override
			public void mouseClicked(MouseEvent arg0) {
				new RefoldNucleusAction();
			}
		});
		runRefoldingButton.setVisible(false);
		
		consensusChartPanel.add(runRefoldingButton);
		consensusChartPanel.setMinimumSize(new Dimension(200, 200));
	}
	
	
	
	/**
	 * Alter the size of the given panel to keep the aspect ratio constant.
	 * The minimum of the width or height of the container is used as the
	 * preferred size of the chart
	 * @param innerPanel the chart to constrain
	 * @param container the contining panel
	 */
	private static void resizePreview(ChartPanel innerPanel, JPanel container) {
        int w = container.getWidth();
        int h = container.getHeight();
        int size =  Math.min(w, h);
        innerPanel.setPreferredSize(new Dimension(size, size));
        container.revalidate();
    }
		
	private JScrollPane createStatsPanel(){
		
		JScrollPane scrollPane = new JScrollPane();
		JPanel panelGeneralStats = new JPanel();
		
		panelGeneralStats.setLayout(new BorderLayout(0, 0));

		tablePopulationStats = new JTable();
		panelGeneralStats.add(tablePopulationStats, BorderLayout.CENTER);
		tablePopulationStats.setEnabled(false);

		scrollPane.setViewportView(panelGeneralStats);
		scrollPane.setColumnHeaderView(tablePopulationStats.getTableHeader());
		tablePopulationStats.setModel(NucleusDatasetCreator.createStatsTable(null));
		return scrollPane;
	}
	
	private JScrollPane createAnalysisParametersPanel(){
		
		JScrollPane scrollPane = new JScrollPane();
		JPanel panel = new JPanel();
		
		panel.setLayout(new BorderLayout(0, 0));

		tableAnalysisParamters = new JTable();
		tableAnalysisParamters.setModel(NucleusDatasetCreator.createAnalysisParametersTable(null));
		tableAnalysisParamters.setEnabled(false);
		panel.add(tableAnalysisParamters, BorderLayout.CENTER);

		scrollPane.setViewportView(panel);
		scrollPane.setColumnHeaderView(tableAnalysisParamters.getTableHeader());
		return scrollPane;
	}
		
	private JPanel createSegmentProfilePanel(){
		
		JPanel panel = new JPanel();
		panel.setLayout(new BorderLayout());
		Dimension minimumChartSize = new Dimension(50, 100);
		Dimension preferredChartSize = new Dimension(400, 300);
		JFreeChart profileChart = ChartFactory.createXYLineChart(null,
	            "Position", "Angle", null);
		XYPlot plot = profileChart.getXYPlot();
		plot.getDomainAxis().setRange(0,100);
		plot.getRangeAxis().setRange(0,360);
		plot.setBackgroundPaint(Color.WHITE);
		
		segmentsProfileChartPanel = new ChartPanel(profileChart);
		segmentsProfileChartPanel.setMinimumSize(minimumChartSize);
		segmentsProfileChartPanel.setPreferredSize(preferredChartSize);
		segmentsProfileChartPanel.setMinimumDrawWidth( 0 );
		segmentsProfileChartPanel.setMinimumDrawHeight( 0 );
		panel.add(segmentsProfileChartPanel, BorderLayout.CENTER);
		
		
		// checkbox to select raw or normalised profiles
		normSegmentCheckBox.setSelected(true);
		normSegmentCheckBox.setActionCommand("NormalisedSegmentProfile");
		normSegmentCheckBox.addActionListener(this);
		
		// make buttons to select raw profiles
		rawSegmentLeftButton.setSelected(true);
		rawSegmentLeftButton.setActionCommand("LeftAlignSegmentProfile");
		rawSegmentRightButton.setActionCommand("RightAlignSegmentProfile");
		rawSegmentLeftButton.addActionListener(this);
		rawSegmentRightButton.addActionListener(this);
		rawSegmentLeftButton.setEnabled(false);
		rawSegmentRightButton.setEnabled(false);
		

		//Group the radio buttons.
		final ButtonGroup alignGroup = new ButtonGroup();
		alignGroup.add(rawSegmentLeftButton);
		alignGroup.add(rawSegmentRightButton);
		
		JPanel alignPanel = new JPanel();
		alignPanel.setLayout(new BoxLayout(alignPanel, BoxLayout.X_AXIS));

		alignPanel.add(normSegmentCheckBox);
		alignPanel.add(rawSegmentLeftButton);
		alignPanel.add(rawSegmentRightButton);
		panel.add(alignPanel, BorderLayout.NORTH);
		return panel;
	}
	
	private JPanel createSegmentBoxplotsPanel(){
		JPanel panel = new JPanel(); // main container in tab

		panel.setLayout(new BorderLayout());
		
		JFreeChart boxplot = ChartFactory.createBoxAndWhiskerChart(null, null, null, new DefaultBoxAndWhiskerCategoryDataset(), false);	        

		segmentsBoxplotChartPanel = new ChartPanel(boxplot);
		panel.add(segmentsBoxplotChartPanel, BorderLayout.CENTER);
		
		segmentSelectionBox = new JComboBox<String>();
		segmentSelectionBox.setActionCommand("SegmentBoxplotChoice");
		segmentSelectionBox.addActionListener(this);
		panel.add(segmentSelectionBox, BorderLayout.NORTH);
		


		return panel;
	}
			
	@Override
	public void actionPerformed(ActionEvent e) {
		
		List<AnalysisDataset> list = getSelectedRowsFromTreeTable();

		if(e.getActionCommand().equals("SegmentBoxplotChoice")){
			String segName = (String) segmentSelectionBox.getSelectedItem();
			// create the appropriate chart
			//TODO
			updateSegmentsBoxplot(list, segName);
			
			if(  normSegmentCheckBox.isSelected()){
				updateSegmentsProfile(list, (String) segmentSelectionBox.getSelectedItem(), true, false);
			} else {
				
				if(  rawSegmentLeftButton.isSelected()){
					updateSegmentsProfile(list, (String) segmentSelectionBox.getSelectedItem(), false, false);
				} else {
					updateSegmentsProfile(list, (String) segmentSelectionBox.getSelectedItem(), false, true);
				}
			}
			
		}
				
		if(e.getActionCommand().equals("LeftAlignSegmentProfile")){
			updateSegmentsProfile(getSelectedRowsFromTreeTable(), (String) segmentSelectionBox.getSelectedItem(), false, false);
		}
		
		if(e.getActionCommand().equals("RightAlignSegmentProfile")){
			updateSegmentsProfile(getSelectedRowsFromTreeTable(), (String) segmentSelectionBox.getSelectedItem(), false, true);
		}
		
		if(e.getActionCommand().equals("NormalisedSegmentProfile")){

			if(  normSegmentCheckBox.isSelected()){
				rawSegmentLeftButton.setEnabled(false);
				rawSegmentRightButton.setEnabled(false);
				updateSegmentsProfile(getSelectedRowsFromTreeTable(), (String) segmentSelectionBox.getSelectedItem(), true, false);
			} else {
				rawSegmentLeftButton.setEnabled(true);
				rawSegmentRightButton.setEnabled(true);
				
				if(  rawSegmentLeftButton.isSelected()){
					updateSegmentsProfile(getSelectedRowsFromTreeTable(), (String) segmentSelectionBox.getSelectedItem(), false, false);
				} else {
					updateSegmentsProfile(getSelectedRowsFromTreeTable(), (String) segmentSelectionBox.getSelectedItem(), false, true);
				}
			}
			
		}
	}
	
	/**
	 * Standard log - append a newline
	 * @param s the string to log
	 */
	public void log(String s){
		logc(s+"\n");
	}
	
	/**
	 * Continuous log - do not append a newline
	 * @param s the string to log
	 */
	public void logc(String s){
		textArea.append(s);
	}
	
	
	/**
	 * Compare morphology images with post-FISH images, and select nuclei into new
	 * sub-populations
	 */
	public void postAnalysis(){

		try{

			String[] names = this.populationNames.keySet().toArray(new String[0]);

			String selectedValue = (String) JOptionPane.showInputDialog(null,
					"Choose population", "FISH Remapping",
					JOptionPane.INFORMATION_MESSAGE, null,
					names, names[0]);

			UUID id = this.populationNames.get(selectedValue);

			AnalysisDataset dataset = MainWindow.this.analysisDatasets.get(id);

			//					IJ.log("Creating analysis");
			FishMappingWindow fishMapper = new FishMappingWindow(MainWindow.this, dataset);

			List<CellCollection> subs = fishMapper.getSubCollections();
			for(CellCollection sub : subs){
				//					IJ.log("Found subcollection: "+sub.getName()+" with "+sub.getNucleusCount()+" nuclei");
				if(sub.getNucleusCount()>0){

					logc("Reapplying morphology...");
					boolean ok = MorphologyAnalysis.reapplyProfiles(sub, MainWindow.this.analysisDatasets.get(id).getCollection());
					if(ok){
						log("OK");
					} else {
						log("Error");
					}

					dataset.addChildCollection(sub);

					MainWindow.this.analysisDatasets.put(sub.getID(), dataset.getChildDataset(sub.getID()));
					MainWindow.this.populationNames.put(sub.getName(), sub.getID());

				}
			}
			updatePopulationList();	

		} catch(Exception e){
			log("Error in FISH remapping: "+e.getMessage());
		}
	}
	
	
	/**
	 * Call the setup of a new analysis, and add the results to the dataset list 
	 */
	public void newAnalysis(){

		Thread thr = new Thread() {
			public void run() {
				lblStatusLine.setText("New analysis in progress");
								
				AnalysisCreator analysisCreator = new AnalysisCreator(MainWindow.this);
				analysisCreator.run();

				List<AnalysisDataset> datasets = analysisCreator.getDatasets();
				
				if(datasets.size()==0 || datasets==null){
					log("No datasets returned");
				}
				
				// new style datasets
				for(AnalysisDataset d : datasets){
					
					addDataset(d);				
					
					for(AnalysisDataset child : d.getChildDatasets()){
						addDataset(child);
					}
					
					d.save();

				}
								
				lblStatusLine.setText("New analysis complete: "+MainWindow.this.analysisDatasets.size()+" populations ready to view");
				updatePopulationList();			
				
			}
		};
		thr.start();		
	}
	
	/**
	 * Add the given dataset to the main population list
	 * Check that the name is valid, and update if needed
	 * @param d the dataset to add
	 */
	public void addDataset(AnalysisDataset d){
		d.setName(checkName(d.getName()));
		MainWindow.this.analysisDatasets.put(d.getUUID(), d);
		MainWindow.this.populationNames.put(d.getName(), d.getUUID());
		
		if(d.isRoot()){ // add to the list of datasets that can be ordered
			treeOrderMap.put(d.getUUID(), treeOrderMap.size()); // add to the end of the list
		}
	}
	
	/**
	 * Check that the name of the dataset is not already in the list of datasets
	 * If the name is used, adjust and check again
	 * @param name the suggested name
	 * @return a valid name
	 */
	public String checkName(String name){
		String result = name;
		if(MainWindow.this.populationNames.containsKey(name)){
			result = checkName(name+"_1");
		}
		return result;
	}
	

	/**
	 * Rename an existing dataset and update the population list.
	 * @param dataset the dataset to rename
	 */
	public void renameCollection(AnalysisDataset dataset){
		CellCollection collection = dataset.getCollection();
		String newName = JOptionPane.showInputDialog(this, "Rename collection", collection.getName());
		// validate
		if(!newName.isEmpty() && newName!=null){
		
			if(this.populationNames.containsKey(newName)){
				log("Name exists, aborting");
			} else {
				String oldName = collection.getName();
				collection.setName(newName);
				this.populationNames.put(newName, collection.getID());
				this.populationNames.remove(oldName);
				log("Collection renamed: "+newName);
				updatePopulationList();
				
				List<AnalysisDataset> list = new ArrayList<AnalysisDataset>(0);
				list.add(dataset);
				updatePanels(list);
			}
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
						
						addDataset(dataset);
						
						for(AnalysisDataset child : dataset.getAllChildDatasets() ){
							addDataset(child);
						}
						
						log("OK");
//						log("Opened dataset: "+dataset.getName());
	
						List<AnalysisDataset> list = new ArrayList<AnalysisDataset>(0);
						list.add(dataset);
	
						updatePanels(list);
						updatePopulationList();
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
					updateStatsPanel(list);
					updateAnalysisParametersPanel(list);
					
					nucleusProfilesPanel.update(list);
					updateConsensusImage(list);
					
					nuclearBoxplotsPanel.update(list);
					
					signalsDetailPanel.update(list);

					updateClusteringPanel(list);
					updateVennPanel(list);
					wilcoxonDetailPanel.update(list);
					
					cellDetailPanel.updateList(list);
					
					if(list!=null){
						// get the list of segments from the datasets
						ComboBoxModel<String> aModel = new DefaultComboBoxModel<String>(list.get(0).getCollection().getSegmentNames().toArray(new String[0]));
						segmentSelectionBox.setModel(aModel);
						segmentSelectionBox.setSelectedIndex(0);
						updateSegmentsBoxplot(list, (String) segmentSelectionBox.getSelectedItem()); // get segname from panel
						updateSegmentsProfile(list, (String) segmentSelectionBox.getSelectedItem(), true, false); // get segname from panel
						 
					}
					
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
	 * Get a series or dataset index for colour selection when drawing charts. The index
	 * is set in the DatasetCreator as part of the label. The format is Name_index_other
	 * @param label the label to extract the index from 
	 * @return the index found
	 */
	private int getIndexFromLabel(String label){
		String[] names = label.split("_");
		return Integer.parseInt(names[1]);
	}
	
	/**
	 * Update the analysis panel with data from the given datasets
	 * @param list the datasets
	 */
	public void updateAnalysisParametersPanel(List<AnalysisDataset> list){
		// format the numbers and make into a tablemodel
		TableModel model = NucleusDatasetCreator.createAnalysisParametersTable(list);
		tableAnalysisParamters.setModel(model);
	}
	
	
	
	/**
	 * Update the stats panel with data from the given datasets
	 * @param list the datasets
	 */
	public void updateStatsPanel(List<AnalysisDataset> list){
		// format the numbers and make into a tablemodel
		TableModel model = NucleusDatasetCreator.createStatsTable(list);
		tablePopulationStats.setModel(model);
	}
	
	/**
	 * Update the venn panel with data from the given datasets
	 * @param list the datasets
	 */
	public void updateVennPanel(List<AnalysisDataset> list){
		// format the numbers and make into a tablemodel
		TableModel model = NucleusDatasetCreator.createVennTable(list);
		vennTable.setModel(model);
		int columns = vennTable.getColumnModel().getColumnCount();
		for(int i=1;i<columns;i++){
			vennTable.getColumnModel().getColumn(i).setCellRenderer(new VennTableCellRenderer());

		}
	}
	
	
	public JFreeChart makeProfileChart(XYDataset ds){
		JFreeChart chart = 
				ChartFactory.createXYLineChart(null,
				                "Position", "Angle", ds, PlotOrientation.VERTICAL, true, true,
				                false);
		
		
		XYPlot plot = chart.getXYPlot();
		plot.getDomainAxis().setRange(0,100);
		plot.getRangeAxis().setRange(0,360);
		plot.setBackgroundPaint(Color.WHITE);
		plot.addRangeMarker(new ValueMarker(180, Color.BLACK, new BasicStroke(2.0f)));

		int seriesCount = plot.getSeriesCount();

		for (int i = 0; i < seriesCount; i++) {
			plot.getRenderer().setSeriesVisibleInLegend(i, Boolean.FALSE);
			String name = (String) ds.getSeriesKey(i);
			if(name.startsWith("Seg_")){
				int colourIndex = getIndexFromLabel(name);
				plot.getRenderer().setSeriesStroke(i, new BasicStroke(3));
				plot.getRenderer().setSeriesPaint(i, ColourSelecter.getSegmentColor(colourIndex));
			} 
			if(name.startsWith("Nucleus_")){
				plot.getRenderer().setSeriesStroke(i, new BasicStroke(1));
				plot.getRenderer().setSeriesPaint(i, Color.LIGHT_GRAY);
			} 
			if(name.startsWith("Q")){
				plot.getRenderer().setSeriesStroke(i, new BasicStroke(2));
				plot.getRenderer().setSeriesPaint(i, Color.DARK_GRAY);
			} 
			
		}	
		return chart;
	}
	

	/**
	 * Create a consenusus chart for the given nucleus collection
	 * @param collection the NucleusCollection to draw the consensus from
	 * @return the consensus chart
	 */
	public JFreeChart makeConsensusChart(CellCollection collection){
		XYDataset ds = NucleusDatasetCreator.createNucleusOutline(collection);
		JFreeChart chart = 
				ChartFactory.createXYLineChart(null,
						null, null, null, PlotOrientation.VERTICAL, true, true,
						false);
		

		double maxX = Math.max( Math.abs(collection.getConsensusNucleus().getMinX()) , Math.abs(collection.getConsensusNucleus().getMaxX() ));
		double maxY = Math.max( Math.abs(collection.getConsensusNucleus().getMinY()) , Math.abs(collection.getConsensusNucleus().getMaxY() ));

		// ensure that the scales for each axis are the same
		double max = Math.max(maxX, maxY);

		// ensure there is room for expansion of the target nucleus due to IQR
		max *=  1.25;		

		XYPlot plot = chart.getXYPlot();
		plot.setDataset(0, ds);
		plot.getDomainAxis().setRange(-max,max);
		plot.getRangeAxis().setRange(-max,max);

		plot.getDomainAxis().setVisible(false);
		plot.getRangeAxis().setVisible(false);

		plot.setBackgroundPaint(Color.WHITE);
		plot.addRangeMarker(new ValueMarker(0, Color.LIGHT_GRAY, new BasicStroke(1.0f)));
		plot.addDomainMarker(new ValueMarker(0, Color.LIGHT_GRAY, new BasicStroke(1.0f)));

		int seriesCount = plot.getSeriesCount();

		for (int i = 0; i < seriesCount; i++) {
			plot.getRenderer().setSeriesVisibleInLegend(i, Boolean.FALSE);
			String name = (String) ds.getSeriesKey(i);
			
			if(name.startsWith("Seg_")){
				int colourIndex = getIndexFromLabel(name);
				plot.getRenderer().setSeriesStroke(i, new BasicStroke(3));
				plot.getRenderer().setSeriesPaint(i, ColourSelecter.getSegmentColor(colourIndex));
			} 
			if(name.startsWith("Q")){
				plot.getRenderer().setSeriesStroke(i, new BasicStroke(2));
				plot.getRenderer().setSeriesPaint(i, Color.DARK_GRAY);
			} 

		}	
		return chart;
	}
	
	
	/**
	 * Update the consensus nucleus panel with data from the given datasets. Produces a blank
	 * chart if no refolded nuclei are present
	 * @param list the datasets
	 */	
	public void updateConsensusImage(List<AnalysisDataset> list){

		CellCollection collection = list.get(0).getCollection();
		try {
			if(list.size()==1){
				if(!collection.hasConsensusNucleus()){
					// add button to run analysis
					JFreeChart consensusChart = ChartFactory.createXYLineChart(null,
							null, null, null);
					XYPlot consensusPlot = consensusChart.getXYPlot();
					consensusPlot.setBackgroundPaint(Color.WHITE);
					consensusPlot.getDomainAxis().setVisible(false);
					consensusPlot.getRangeAxis().setVisible(false);
					consensusChartPanel.setChart(consensusChart);
					
					runRefoldingButton.setVisible(true);


				} else {
					runRefoldingButton.setVisible(false);
					JFreeChart chart = makeConsensusChart(collection);
					consensusChartPanel.setChart(chart);
				} 
			}else {
				// multiple nuclei
				XYDataset ds = NucleusDatasetCreator.createMultiNucleusOutline(list);
				JFreeChart chart = 
						ChartFactory.createXYLineChart(null,
								null, null, ds, PlotOrientation.VERTICAL, true, true,
								false);
				XYPlot plot = chart.getXYPlot();
				plot.getDomainAxis().setVisible(false);
				plot.getRangeAxis().setVisible(false);
				plot.setBackgroundPaint(Color.WHITE);
				plot.addRangeMarker(new ValueMarker(0, Color.LIGHT_GRAY, new BasicStroke(1.0f)));
				plot.addDomainMarker(new ValueMarker(0, Color.LIGHT_GRAY, new BasicStroke(1.0f)));
				
				int seriesCount = plot.getSeriesCount();
				
				for (int i = 0; i < seriesCount; i++) {
					plot.getRenderer().setSeriesVisibleInLegend(i, Boolean.FALSE);
					String name = (String) ds.getSeriesKey(i);
					plot.getRenderer().setSeriesStroke(i, new BasicStroke(2));

					// get the group id from the name, and make colour
					String[] names = name.split("_");
					plot.getRenderer().setSeriesPaint(i, ColourSelecter.getSegmentColor(Integer.parseInt(names[1])));
					if(name.startsWith("Q")){
						// make the IQR distinct from the median
						plot.getRenderer().setSeriesPaint(i, ColourSelecter.getSegmentColor(Integer.parseInt(names[1])).darker());
					}
					
				}
				consensusChartPanel.setChart(chart);
				for(Component c : consensusChartPanel.getComponents() ){
					if(c.getClass()==JButton.class){
						c.setVisible(false);
					}
				}
			}
		} catch (Exception e) {
			log("Error drawing consensus nucleus: "+e.getMessage());
			e.printStackTrace();
		}
	}
		
	public void updateSegmentsBoxplot(List<AnalysisDataset> list, String segName){
		BoxAndWhiskerCategoryDataset ds = NucleusDatasetCreator.createSegmentLengthDataset(list, segName);
		JFreeChart boxplotChart = ChartFactory.createBoxAndWhiskerChart(null, null, null, ds, false); 
//		formatBoxplotChart(boxplotChart);
		segmentsBoxplotChartPanel.setChart(boxplotChart);
	}
	
	public void updateSegmentsProfile(List<AnalysisDataset> list, String segName, boolean normalised, boolean rightAlign){
		
		DefaultXYDataset ds = null;
		if(normalised){
			ds = NucleusDatasetCreator.createMultiProfileSegmentDataset(list, segName);
		} else {
			ds = NucleusDatasetCreator.createRawMultiProfileSegmentDataset(list, segName, rightAlign);
		}
		try {
				
				JFreeChart chart = 
						ChartFactory.createXYLineChart(null,
						                "Position", "Angle", ds, PlotOrientation.VERTICAL, true, true,
						                false);

				XYPlot plot = chart.getXYPlot();
				
				if(!normalised){
					int length = 100;
					for(AnalysisDataset d : list){
						if(   (int) d.getCollection().getMedianArrayLength()>length){
							length = (int) d.getCollection().getMedianArrayLength();
						}
					}
					plot.getDomainAxis().setRange(0,length);
				} else {
					plot.getDomainAxis().setRange(0,100);
				}

				plot.getRangeAxis().setRange(0,360);
				plot.setBackgroundPaint(Color.WHITE);
				plot.addRangeMarker(new ValueMarker(180, Color.BLACK, new BasicStroke(2.0f)));
				
				for (int i = 0; i < plot.getSeriesCount(); i++) {
					plot.getRenderer().setSeriesVisibleInLegend(i, Boolean.FALSE);
					String name = (String) ds.getSeriesKey(i);
					if(name.startsWith("Seg_")){
						int colourIndex = getIndexFromLabel(name);
						plot.getRenderer().setSeriesStroke(i, new BasicStroke(4));
						plot.getRenderer().setSeriesPaint(i, ColourSelecter.getSegmentColor(colourIndex));
					} 
					if(name.startsWith("Profile_")){
						plot.getRenderer().setSeriesStroke(i, new BasicStroke(1));
						plot.getRenderer().setSeriesPaint(i, Color.LIGHT_GRAY);
					} 
					
				}	
								
				segmentsProfileChartPanel.setChart(chart);
			
			
		} catch (Exception e) {
			log("Error in plotting segment profile");
		} 
	}
	
	
	/**
	 *  Find the populations in memory, and display them in the population chooser. 
	 *  Root populations are ordered according to position in the treeListOrder map.
	 */
	public void updatePopulationList(){
					
		// new method using treetable
		List<String> columns = new ArrayList<String>();
		columns.add("Population");
		columns.add("Nuclei");
		columns.add("");

		DefaultTreeTableModel treeTableModel = new DefaultTreeTableModel();
		PopulationTreeTableNode  root = new PopulationTreeTableNode (java.util.UUID.randomUUID());
		treeTableModel.setRoot(root);
		treeTableModel.setColumnIdentifiers(columns);
		
//		treeOrderMap.print();
		
		if(this.analysisDatasets.size()>0){
			for(UUID id : treeOrderMap.getIDs()){
				
				AnalysisDataset rootDataset = analysisDatasets.get(id);
				root.add( addTreeTableChildNodes(rootDataset.getUUID()));
			}
		}
		
		treeTable.setTreeTableModel(treeTableModel);

		int row = 0;
		while (row < treeTable.getRowCount()) {
			treeTable.expandRow(row);
			row++;
		}
		
		treeTable.getColumnModel().getColumn(0).setPreferredWidth(120);
		treeTable.getColumnModel().getColumn(2).setPreferredWidth(5);
	}
	
	
	private PopulationTreeTableNode addTreeTableChildNodes(UUID id){
		AnalysisDataset dataset = MainWindow.this.analysisDatasets.get(id);
		PopulationTreeTableNode category = new PopulationTreeTableNode(dataset.getUUID());
		category.setValueAt(dataset.getName(), 0);
		category.setValueAt(dataset.getCollection().getNucleusCount(), 1);
				
		Set<UUID> childIDList = dataset.getChildUUIDs();
		for(UUID childID : childIDList){
			PopulationTreeTableNode childNode = addTreeTableChildNodes(childID);
			category.add(childNode);
		}
		return category;
	}
				

	
	private void updateClusteringPanel(List<AnalysisDataset> list){
		
		if(list.size()==1){
			AnalysisDataset dataset = list.get(0);
//			CellCollection collection = dataset.getCollection();
			
			clusteringPanel = new JPanel();
			clusteringPanel.setLayout(new BoxLayout(clusteringPanel, BoxLayout.Y_AXIS));
			
			if(!dataset.hasClusters()){ // only allow clustering once per population

				JButton btnNewClusterAnalysis = new JButton("Cluster population");
				btnNewClusterAnalysis.addMouseListener(new MouseAdapter() {
					@Override
					public void mouseClicked(MouseEvent arg0) {
						new ClusterAnalysisAction();

					}
				});
				clusteringPanel.add(btnNewClusterAnalysis);
				
			} else { // clusters present, show the tree if available
				JTextArea label = new JTextArea(dataset.getClusterTree());
				label.setLineWrap(true);
				
				JScrollPane treeView = new JScrollPane(label);
				clusteringPanel.add(treeView);
			}
			
			tabbedPane.setComponentAt(MainWindow.CLUSTERS_TAB, clusteringPanel);
			
		} else {
			clusteringPanel = new JPanel();
			clusteringPanel.setLayout(new BoxLayout(clusteringPanel, BoxLayout.Y_AXIS));
			tabbedPane.setComponentAt(MainWindow.CLUSTERS_TAB, clusteringPanel);
		}
		
	}
	
	private DefaultMutableTreeNode addChildNodes(UUID id){
		AnalysisDataset dataset = MainWindow.this.analysisDatasets.get(id);
		DefaultMutableTreeNode category = new DefaultMutableTreeNode(dataset.getCollection().getName());
		List<UUID> childIDList = dataset.getClusterIDs();
		for(UUID childID : childIDList){
			DefaultMutableTreeNode childNode = addChildNodes(childID);
			category.add(childNode);
		}
		return category;
	}
	
	
	/**
	 * Listen for selections to the population list. Switch between single population,
	 * or multiple selections
	 * 
	 */
	class ListSelectionHandler implements ListSelectionListener {
		public void valueChanged(ListSelectionEvent e) {
			
			List<AnalysisDataset> datasets = new ArrayList<AnalysisDataset>(0);
			
			ListSelectionModel lsm = (ListSelectionModel)e.getSource();
			
			List<Integer> selectedIndexes = new ArrayList<Integer>(0);

			if (!lsm.isSelectionEmpty()) {
				// Find out which indexes are selected.
				int minIndex = lsm.getMinSelectionIndex();
				int maxIndex = lsm.getMaxSelectionIndex();
				for (int i = minIndex; i <= maxIndex; i++) {
					if (lsm.isSelectedIndex(i)) {

						String key = (String) populationTable.getModel().getValueAt(i, 0); // row i, column 0
						if(!key.equals("No populations")){
							
							// get uuid from populationNames, then population via uuid from analysisDatasets
//							list.add(analysisPospulations.get(populationNames.get(key)));
							datasets.add(analysisDatasets.get(populationNames.get(key)));
							selectedIndexes.add(i);
							
						}

					}
				}
				String count = datasets.size() == 1 ? "population" : "populations"; // it matters to ME
				lblStatusLine.setText(datasets.size()+" "+count+"  selected");
//				populationTable.getColumnModel().getColumn(2).setCellRenderer(new PopulationTableCellRenderer(selectedIndexes));
				treeTable.getColumnModel().getColumn(2).setCellRenderer(new PopulationTableCellRenderer(selectedIndexes));

				if(datasets.isEmpty()){
					log("Error: list is empty");
				} else {
					updatePanels(datasets);
				}
			}

		}
	}
	
	
	public List<AnalysisDataset> getSelectedRowsFromTreeTable(){

		List<AnalysisDataset> datasets = new ArrayList<AnalysisDataset>(0);

		TreeSelectionModel lsm = treeTable.getTreeSelectionModel();

		List<Integer> selectedIndexes = new ArrayList<Integer>(0);

		if (!lsm.isSelectionEmpty()) {
			// Find out which indexes are selected.
			int minIndex = lsm.getMinSelectionRow();
			int maxIndex = lsm.getMaxSelectionRow();
			for (int i = minIndex; i <= maxIndex; i++) {
				if (lsm.isRowSelected(i)) {

					String key = (String) treeTable.getModel().getValueAt(i, 0); // row i, column 0
					if(!key.equals("No populations")){

						// get uuid from populationNames, then population via uuid from analysisDatasets
						datasets.add(analysisDatasets.get(populationNames.get(key)));
						selectedIndexes.add(i);

					}

				}
			}
		}
		return datasets;
	}
	
	/**
	 * Establish the rows in the population tree that are currently selected.
	 * Set the possible menu options accordingly, and call the panel updates
	 */
	class TreeSelectionHandler implements TreeSelectionListener {
		public void valueChanged(TreeSelectionEvent e) {
			
//			List<AnalysisDataset> datasets = getSelectedRowsFromTreeTable();
			List<AnalysisDataset> datasets = new ArrayList<AnalysisDataset>(0);
			
			TreeSelectionModel lsm = (TreeSelectionModel)e.getSource();
			
			List<Integer> selectedIndexes = new ArrayList<Integer>(0);

			if (!lsm.isSelectionEmpty()) {
				// Find out which indexes are selected.
				int minIndex = lsm.getMinSelectionRow();
				int maxIndex = lsm.getMaxSelectionRow();
				for (int i = minIndex; i <= maxIndex; i++) {
					if (lsm.isRowSelected(i)) {
						

						String key = (String) treeTable.getModel().getValueAt(i, 0); // row i, column 0
						if(!key.equals("No populations")){
							
							// get uuid from populationNames, then population via uuid from analysisDatasets
							datasets.add(analysisDatasets.get(populationNames.get(key)));
							selectedIndexes.add(i);
							
						}

					}
				}
				String count = datasets.size() == 1 ? "population" : "populations"; // it matters to ME
				lblStatusLine.setText(datasets.size()+" "+count+" selected");
				treeTable.getColumnModel().getColumn(2).setCellRenderer(new PopulationTableCellRenderer(selectedIndexes));

				if(datasets.isEmpty()){
					log("Error: list is empty");
					populationPopup.disableAll();
				} else {
					
					if(datasets.size()>1){ // multiple populations
						populationPopup.disableAll();
						populationPopup.enableMerge();
						populationPopup.enableDelete();
						
					} else { // single population
						AnalysisDataset d = datasets.get(0);
						populationPopup.enableDelete();
						populationPopup.disableMerge();
						populationPopup.enableSave();
						populationPopup.enableExtract();
						populationPopup.enableExportStats();
						
						// check if we can move the dataset
						if(d.isRoot()){

							if(treeOrderMap.size()>1){
								populationPopup.enableApplySegmentation();

								// check if the selected dataset is at the top of the list
								if(treeOrderMap.get(0)==d.getUUID()){
									populationPopup.disableMenuUp();
//									populationPopup.disableMenuDown();
								} else {
									populationPopup.enableMenuUp();
//									populationPopup.enableMenuDown();
								}

								// check if the selected dataset is at the bottom of the list
								if(treeOrderMap.get(treeOrderMap.size()-1)==d.getUUID()){
									populationPopup.disableMenuDown();
//									populationPopup.disableMenuUp();
								} else {
//									populationPopup.enableMenuUp();
									populationPopup.enableMenuDown();
								}

							} else { // only one or zero datasets in the pogram 
								populationPopup.disableMenuUp();
								populationPopup.disableMenuDown();
							}

							// only root datasets can replace folder mappings
							populationPopup.enableReplaceFolder();

						} else { // not root
							
							if(treeOrderMap.size()>1){
								populationPopup.enableApplySegmentation();
							}
							
							populationPopup.disableReplaceFolder();
							populationPopup.disableMenuUp();
							populationPopup.disableMenuDown();
						}

						if(!d.hasChildren()){ // cannot split population without children yet
							populationPopup.disableSplit();
						} else {
							populationPopup.enableSplit();
						}
					}
					updatePanels(datasets);
				}
			}
		}
	}
	
	
	/**
	 * Allows for cell background to be coloured based on poition in a list
	 *
	 */
	public class PopulationTableCellRenderer extends javax.swing.table.DefaultTableCellRenderer {

		private static final long serialVersionUID = 1L;
		List<Integer> list;
		
		public PopulationTableCellRenderer(List<Integer> list){
			super();
			this.list = list;
		}
		
		public PopulationTableCellRenderer(){
			super();
			this.list = new ArrayList<Integer>(0);
		}

		public java.awt.Component getTableCellRendererComponent(javax.swing.JTable table, java.lang.Object value, boolean isSelected, boolean hasFocus, int row, int column) {
	        
	      //Cells are by default rendered as a JLabel.
	        JLabel l = (JLabel) super.getTableCellRendererComponent(table, value, isSelected, hasFocus, row, column);

	        if (list.contains(row)) {
	          l.setBackground(ColourSelecter.getSegmentColor(list.indexOf(row)));
	        } else {
	          l.setBackground(Color.WHITE);
	        }

	      //Return the JLabel which renders the cell.
	      return l;
	    }
	}

	
	/**
	 * Colour table cell background to show pairwise comparisons. All cells are white, apart
	 * from the diagonal, which is made light grey
	 */
	public class VennTableCellRenderer extends javax.swing.table.DefaultTableCellRenderer {

		private static final long serialVersionUID = 1L;

		public java.awt.Component getTableCellRendererComponent(javax.swing.JTable table, java.lang.Object value, boolean isSelected, boolean hasFocus, int row, int column) {
	        
	      //Cells are by default rendered as a JLabel.
	        JLabel l = (JLabel) super.getTableCellRendererComponent(table, value, isSelected, hasFocus, row, column);

	        String cellContents = l.getText();
	        if(cellContents!=null && !cellContents.equals("")){ // ensure value
//	        	IJ.log(cellContents);
	        	String[] array = cellContents.split("%");
//	        	 IJ.log(array[0]);
		        String[] array2 = array[0].split("\\(");
//		        IJ.log(array2[1]);
		        double pct = Double.valueOf(array2[1]);
		        
//		        IJ.log("Pct: "+pct);
		        double colourIndex = 255 - ((pct/100) * 255);
		        
		        Color colour = new Color((int) colourIndex,(int) colourIndex, 255);
		        l.setBackground(colour);
		        
		        if(pct>60){
		        	l.setForeground(Color.WHITE);
		        } else {
		        	l.setForeground(Color.black);
		        }
		        
	        } else {
	            l.setBackground(Color.LIGHT_GRAY);
	        }

	      //Return the JLabel which renders the cell.
	      return l;
	    }
	}
	
	
	class PopulationTreeTableNode extends AbstractMutableTreeTableNode {
		
		Object[] columnData = new Object[3];
		UUID nodeID;

		PopulationTreeTableNode(UUID id) {
			super(id.toString());
			this.nodeID = id;
		}
		
		public UUID getID(){
			return this.nodeID;
		}
		
		public int getColumnCount() {
		    return 3;
		}

		public Object getValueAt(int column){
			return columnData[column];
		}
		
		public void setValueAt(Object aValue, int column){
			columnData[column] = aValue;
		}
	}
	
	public class PopulationListPopupMenu extends JPopupMenu {   //implements ActionListener{
				
		private static final long serialVersionUID = 1L;
		JMenuItem mergeMenuItem = new JMenuItem(new MergeCollectionAction());;
		JMenuItem deleteMenuItem = new JMenuItem(new DeleteCollectionAction());
		JMenuItem splitMenuItem = new JMenuItem(new SplitCollectionAction());
		JMenuItem saveMenuItem = new JMenuItem(new SaveCollectionAction());
		JMenuItem extractMenuItem = new JMenuItem(new ExtractNucleiCollectionAction());
		JMenuItem moveUpMenuItem = new JMenuItem(new MoveDatasetUpAction());
		JMenuItem moveDownMenuItem = new JMenuItem(new MoveDatasetDownAction());
		JMenuItem replaceFolderMenuItem = new JMenuItem(new ReplaceNucleusFolderAction());
		JMenuItem exportStatsMenuItem = new JMenuItem(new ExportDatasetStatsAction());
		JMenuItem applySegmentationMenuItem = new JMenuItem(new ApplySegmentProfileAction());
		
		JMenuItem addTailStainMenuItem = new JMenuItem( new AbstractAction("Add tail stain"){
			private static final long serialVersionUID = 1L;
			@Override
			public void actionPerformed(ActionEvent arg0) {
				new AddTailStainAction();				
			}
		});
		
		JMenuItem addNuclearSignalMenuItem = new JMenuItem( new AbstractAction("Add nuclear signal"){
			private static final long serialVersionUID = 1L;
			@Override
			public void actionPerformed(ActionEvent arg0) {
				new AddNuclearSignalAction();				
			}
		});
		
		
		JMenuItem performShellAnalysisMenuItem = new JMenuItem( new AbstractAction("Run shell analysis"){
			private static final long serialVersionUID = 1L;
			@Override
			public void actionPerformed(ActionEvent arg0) {
				new ShellAnalysisAction();				
			}
		});
				
				
		public PopulationListPopupMenu() {
			
			super("Popup");
			
			this.add(moveUpMenuItem);
			this.add(moveDownMenuItem);
			this.addSeparator();
			this.add(mergeMenuItem);
			this.add(deleteMenuItem);
			this.add(splitMenuItem);
			this.addSeparator();
			this.add(saveMenuItem);
			this.add(extractMenuItem);
			this.add(exportStatsMenuItem);
			this.addSeparator();
			this.add(replaceFolderMenuItem);
			this.add(applySegmentationMenuItem);
			this.addSeparator();
			this.add(addTailStainMenuItem);
			this.add(addNuclearSignalMenuItem);
			this.add(performShellAnalysisMenuItem);
	    }
		
		public void enableAll(){
			enableMerge();
			enableDelete();
			enableSplit();
			enableSave();
			enableExtract();
			enableMenuUp();
			enableMenuDown();
			enableReplaceFolder();
			enableExportStats();
			enableApplySegmentation();
			
		}
		
		public void disableAll(){
			disableMerge();
			disableDelete();
			disableSplit();
			disableSave();
			disableExtract();
			disableMenuUp();
			disableMenuDown();
			disableReplaceFolder();
			disableExportStats();
			disableApplySegmentation();
		}
		
		public void enableMerge(){
			mergeMenuItem.setEnabled(true);
		}
		
		public void disableMerge(){
			mergeMenuItem.setEnabled(false);
		}
		
		public void enableDelete(){
			deleteMenuItem.setEnabled(true);
		}
		
		public void disableDelete(){
			deleteMenuItem.setEnabled(false);
		}
		
		public void enableSplit(){
			splitMenuItem.setEnabled(true);
		}
		
		public void disableSplit(){
			splitMenuItem.setEnabled(false);
		}
		
		public void enableSave(){
			saveMenuItem.setEnabled(true);
		}
		
		public void disableSave(){
			saveMenuItem.setEnabled(false);
		}
		
		public void enableExtract(){
			extractMenuItem.setEnabled(true);
		}
		
		public void disableExtract(){
			extractMenuItem.setEnabled(false);
		}
		
		public void enableMenuUp(){
			moveUpMenuItem.setEnabled(true);
		}
		
		public void disableMenuUp(){
			moveUpMenuItem.setEnabled(false);
		}
		
		public void enableMenuDown(){
			moveDownMenuItem.setEnabled(true);
		}
		
		public void disableMenuDown(){
			moveDownMenuItem.setEnabled(false);
		}
		
		public void enableReplaceFolder(){
			replaceFolderMenuItem.setEnabled(true);
		}
		
		public void disableReplaceFolder(){
			replaceFolderMenuItem.setEnabled(false);
		}
		
		public void enableExportStats(){
			exportStatsMenuItem.setEnabled(true);
		}
		
		public void disableExportStats(){
			exportStatsMenuItem.setEnabled(false);
		}
		
		public void enableApplySegmentation(){
			applySegmentationMenuItem.setEnabled(true);
		}
		
		public void disableApplySegmentation(){
			applySegmentationMenuItem.setEnabled(false);
		}
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
	
	class MergeCollectionAction extends AbstractAction {

		private static final long serialVersionUID = 1L;
		public MergeCollectionAction() {
			super("Merge");
		}

		public void actionPerformed(ActionEvent e) {

			final List<AnalysisDataset> datasets = getSelectedRowsFromTreeTable();

			if(datasets.size()>1){

				if(datasets.size()==2){ // check we are not merging a parent and child (would just get child)
					if(datasets.get(0).hasChild(datasets.get(1))  || datasets.get(1).hasChild(datasets.get(0)) ){
						log("No. That would be silly.");
						return;
					}
				}

				Thread thr = new Thread() {
					public void run() {

						log("Merging collection...");

						// check all collections are of the same type
						boolean newRoot = false;
						Class<?> testClass = datasets.get(0).getAnalysisOptions().getNucleusClass();
						for(AnalysisDataset d : datasets){

							if(d.getAnalysisOptions().getNucleusClass()!=testClass){
								log("Error: cannot merge collections of different class");
								return;
							}

							// check if a root population is included in the merge;
							// if so, we must make the result a root population too
							// otherwise, it may be a subpopulation
							if(d.isRoot()){
								newRoot = true;
							}
						}

						AnalysisDataset mergeParent = null;
						if(!newRoot) { // unless we have forced root above
							// check if all datasets are children of one root dataset
							for(AnalysisDataset parent : analysisDatasets.values()){
								if(parent.isRoot()){ // only look at top level datasets for now
									boolean ok = true; 
									for(AnalysisDataset d : datasets){
										if(!parent.hasChild(d)){
											ok = false;
										}
									}
									if(ok){
										mergeParent = parent;
									}
								}
							}

							// if a merge parent was found, new collection is not root
							if(mergeParent!=null){
								newRoot = false;
							} else {
								newRoot = true; // if we cannot find a consistent parent, make a new root population
							}
						}


						// add the nuclei from each population to the new collection
						CellCollection newCollection = makeNewCollection(datasets.get(0), "Merged");
						for(AnalysisDataset d : datasets){

							for(Cell n : d.getCollection().getCells()){
								if(!newCollection.getCells().contains(n)){
									newCollection.addCell(n);
								}
							}

						}

						// create the dataset; has no analysis options at present
						AnalysisDataset newDataset = new AnalysisDataset(newCollection);
						newDataset.setName("Merge_of_datasets");
						newDataset.setRoot(newRoot);

						// if applicable, add the new dataset to a parent
						if(newRoot==false && mergeParent!=null){

							logc("Reapplying morphology...");
							boolean ok = MorphologyAnalysis.reapplyProfiles(newCollection, mergeParent.getCollection());
							if(ok){
								log("OK");
							} else {
								log("Error");
							}
							newDataset.setAnalysisOptions(mergeParent.getAnalysisOptions());
							newDataset.getAnalysisOptions().setRefoldNucleus(false);

							mergeParent.addChildDataset(newDataset);
						} else {
							// otherwise, it is a new root population
							// we need to run a fresh morphology analysis
							logc("Running morphology analysis...");
							boolean ok = MorphologyAnalysis.run(newDataset.getCollection());
							if(ok){
								log("OK");
							} else {
								log("Error");
							}
							newDataset.setAnalysisOptions(datasets.get(0).getAnalysisOptions());
							newDataset.getAnalysisOptions().setRefoldNucleus(false);
						}

						// add the new collection to the list
						addDataset(newDataset);
						updatePopulationList();
					}
				};
				thr.start();

			} else {
				log("Cannot merge single dataset");
			}

		}
	}

	class DeleteCollectionAction extends AbstractAction {

		private static final long serialVersionUID = 1L;
		public DeleteCollectionAction() {
	        super("Delete");
	    }
		
		public void actionPerformed(ActionEvent e) {

			List<AnalysisDataset> datasets = getSelectedRowsFromTreeTable();

			if(!datasets.isEmpty()){
				
				if(datasets.size()==1){
					logc("Deleting collection...");
				} else {
					logc("Deleting collections...");
				}
				
//				TODO: this still has problems with multiple datasets
				
				// get the ids as a list, so we don't iterate over datasets
				// when we could delete a child of the list in progress
				List<UUID> list = new ArrayList<UUID>(0);
				for(AnalysisDataset d : datasets){
					list.add(d.getUUID());
				}

				for(UUID id : list){
					// check dataset still exists
					if(analysisDatasets.containsKey(id)){

						AnalysisDataset d = analysisDatasets.get(id);

//						if(d.isRoot()){
								
							// remove all children of the collection
						for(UUID u : d.getAllChildUUIDs()){
							String name = analysisDatasets.get(u).getName();
//							IJ.log("   Removing children");
							if(analysisDatasets.containsKey(u)){
								analysisDatasets.remove(u);
							}
							
							if(populationNames.containsValue(u)){
								populationNames.remove(name);
							}						

							d.deleteChild(u);
//
						}
						
						for(UUID parentID : analysisDatasets.keySet()){
							AnalysisDataset parent = analysisDatasets.get(parentID);
							if(parent.hasChild(id)){
								parent.deleteChild(id);
							}
						}
						populationNames.remove(d.getName());
						analysisDatasets.remove(id);

						if(d.isRoot()){
							treeOrderMap.remove(id);
						}
					}
				}
				updatePopulationList();	
				log("OK");
			}
		}
	}
	
	class SplitCollectionAction extends AbstractAction {

		private static final long serialVersionUID = 1L;
		public SplitCollectionAction() {
	        super("Split");
	    }
		
	    public void actionPerformed(ActionEvent e) {
	        
	        List<AnalysisDataset> datasets = getSelectedRowsFromTreeTable();

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
	        			AnalysisDataset negative = analysisDatasets.get(  populationNames.get(selectedValue)  );

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

	        			addDataset(dataset.getChildDataset(newID));
	        			updatePopulationList();
	        			
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

			final List<AnalysisDataset> datasets = getSelectedRowsFromTreeTable();

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
	        
	        final List<AnalysisDataset> datasets = getSelectedRowsFromTreeTable();

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
	
	class MoveDatasetUpAction extends AbstractAction {

		private static final long serialVersionUID = 1L;
		public MoveDatasetUpAction() {
			super("Move up");
		}

		public void actionPerformed(ActionEvent e) {

			final List<AnalysisDataset> datasets = getSelectedRowsFromTreeTable();
			
			if(datasets.size()==1){

				AnalysisDataset dataToMove = datasets.get(0);

				int oldValue = treeOrderMap.get(dataToMove.getUUID());
				int newValue = oldValue;
				if(oldValue>0){ // do not change if already at the top
					newValue = oldValue-1;

					UUID replacedID = treeOrderMap.get(newValue); // find the dataset currently holding the spot
					treeOrderMap.put(dataToMove.getUUID(), newValue ); // move the dataset up
					treeOrderMap.put(replacedID, oldValue); // move the dataset in place down
					updatePopulationList();
				}
				
			}
		}
	}
	
	class MoveDatasetDownAction extends AbstractAction {

		private static final long serialVersionUID = 1L;
		public MoveDatasetDownAction() {
			super("Move down");
		}

		public void actionPerformed(ActionEvent e) {
			
//			for(int i=0; i<treeOrderMap.size();i++){
//				IJ.log(i+": "+analysisDatasets.get(treeOrderMap.get(i)).getName());
//			}

			final List<AnalysisDataset> datasets = getSelectedRowsFromTreeTable();

			if(datasets.size()==1){

				AnalysisDataset dataToMove = datasets.get(0);
				
				int oldValue = treeOrderMap.get(dataToMove.getUUID());
				int newValue = oldValue;
				if(oldValue<treeOrderMap.size()-1){ // do not change if already at the bottom
					newValue = oldValue+1;
//					IJ.log("Moving "+oldValue+" to "+newValue);

					UUID replacedID = treeOrderMap.get(newValue); // find the dataset currently holding the spot
					treeOrderMap.put(dataToMove.getUUID(), newValue ); // move the dataset up
					treeOrderMap.put(replacedID, oldValue); // move the dataset in place down
					updatePopulationList();
				}
				
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

				final List<AnalysisDataset> datasets = getSelectedRowsFromTreeTable();

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

			final List<AnalysisDataset> datasets = getSelectedRowsFromTreeTable();

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

			final List<AnalysisDataset> datasets = getSelectedRowsFromTreeTable();

			if(datasets.size()==1){
				// TODO make new thread
				Thread thr = new Thread() {
					public void run() {

						AnalysisDataset d = datasets.get(0);
						try{
							
							// get the desired population
							String[] names = populationNames.keySet().toArray(new String[0]);

							String selectedValue = (String) JOptionPane.showInputDialog(null,
									"Choose population", "Reapply segmentation",
									JOptionPane.INFORMATION_MESSAGE, null,
									names, names[0]);

							if(selectedValue!=null){

								UUID id = populationNames.get(selectedValue);
								AnalysisDataset source = analysisDatasets.get(id);

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
		protected JProgressBar progressBar = null;
		protected String errorMessage = null;
		
		public ProgressableAction(String barMessage, String errorMessage){
			this.progressBar = new JProgressBar(0, 100);
			this.progressBar.setString(barMessage);
			this.progressBar.setStringPainted(true);
			this.errorMessage = errorMessage;
			
			final List<AnalysisDataset> datasets = getSelectedRowsFromTreeTable();
			
			if(datasets.size()==1){

				d = datasets.get(0);
				
				progressPanel.add(this.progressBar);
				contentPane.revalidate();
				contentPane.repaint();				
			} else {
				log("Unable to analyse more than one dataset");
			}
		}
		
		/**
		 * Change the progress message from the default in the constructor
		 * @param messsage the string to display
		 */
		public void setProgressMessage(String messsage){
			this.progressBar.setString(messsage);
		}
		
		private void removeProgressBar(){
			progressPanel.remove(this.progressBar);
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

			d.getAnalysisOptions().setRefoldNucleus(true);
			d.getAnalysisOptions().setRefoldMode("Fast");
			List<AnalysisDataset> list = new ArrayList<AnalysisDataset>(0);
			list.add(d);
			updatePanels(list);
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
			super("Tail detection in progress", "Error in tail detection");
			
			DirectoryChooser openDialog = new DirectoryChooser("Select directory of tubulin images...");
			String folderName = openDialog.getDirectory();

			if(folderName==null) return; // user cancelled

			final File folder =  new File(folderName);

			if(!folder.isDirectory() ){
				this.cancel();
				return;
			}
			if(!folder.exists()){
				this.cancel();
				return; // check folder is ok
			}
			// create dialog to get image channel

			Object[] possibilities = {"Greyscale", "Red", "Green", "Blue"};
			String channelName = (String)JOptionPane.showInputDialog(
					MainWindow.this,
					"Select channel",
					"Select channel",
					JOptionPane.PLAIN_MESSAGE,
					null,
					possibilities,
					"Green");

			final int channel = channelName.equals("Red") 
					? Constants.RGB_RED
							: channelName.equals("Green") 
							? Constants.RGB_GREEN
									: Constants.RGB_BLUE;
			
			
			TubulinTailDetector t = new TubulinTailDetector(d, folder, channel);
			t.addPropertyChangeListener(this);
			t.execute();
		}
	}
	
	/**
	 * Add images containing nuclear signals
	 * @author bms41
	 *
	 */
	class AddNuclearSignalAction extends ProgressableAction {
		
		public AddNuclearSignalAction() {
			super("Signal detection in progress", "Error in signal detection");

			try{
				// add dialog for non-default detection options
				SignalDetectionSettingsWindow analysisSetup = new SignalDetectionSettingsWindow(d.getAnalysisOptions());
				
				final int channel = analysisSetup.getChannel();
				final String signalGroupName = analysisSetup.getSignalGroupName();


				NuclearSignalOptions options = d.getAnalysisOptions().getNuclearSignalOptions(signalGroupName);

				// the new signal group is one more than the highest in the collection
				int newSignalGroup = d.getCollection().getHighestSignalGroup()+1;
				
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
				

				SignalDetector t = new SignalDetector(d, folder, channel, options, newSignalGroup, signalGroupName);
				this.setProgressMessage("Signal detection in progress: "+signalGroupName);
				t.addPropertyChangeListener(this);
				t.execute();
			} catch (Exception e){
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
			super("Curve refolding in progress", "Error refolding nucleus");

			try{

				for(Component c : consensusChartPanel.getComponents() ){
					if(c.getClass()==JButton.class){
						c.setVisible(false);
					}
				}


				CurveRefolder refolder = new CurveRefolder(d.getCollection(), 
						d.getAnalysisOptions().getNucleusClass(), 
						"Fast");

				refolder.addPropertyChangeListener(this);
				refolder.execute();

			} catch(Exception e1){
				this.cancel();
				log("Error refolding nucleus");
			}
		}
	}
		
	
	/**
	 * Run a new shell analysis on the selected dataset
	 */
	class ShellAnalysisAction extends ProgressableAction {
				
		public ShellAnalysisAction() {
			super("Shell analysis in progress", "Error in shell analysis");
			
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
			super("Cluster analysis in progress", "Error in cluster analysis");
			
			ClusteringSetupWindow clusterSetup = new ClusteringSetupWindow(MainWindow.this);
			Map<String, Object> options = clusterSetup.getOptions();

			if(clusterSetup.isReadyToRun()){ // if dialog was cancelled, skip
				
				progressBar = new JProgressBar(0, 100);
				progressBar.setString("Cluster analysis in progress");
				progressBar.setIndeterminate(true);
				progressBar.setStringPainted(true);
				
				progressPanel.add(progressBar);
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

			progressPanel.remove(progressBar);
			contentPane.revalidate();
			contentPane.repaint();

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

				addDataset(d.getChildDataset(c.getID()));

			}
			updatePopulationList();	

		}
	}
	
}
