package no.gui;

import ij.IJ;
import ij.io.DirectoryChooser;
import ij.io.OpenDialog;
import ij.io.SaveDialog;

import java.awt.BorderLayout;

import javax.swing.JFrame;
import javax.swing.JPanel;
import javax.swing.border.EmptyBorder;
import javax.swing.event.ListSelectionEvent;
import javax.swing.event.ListSelectionListener;
import javax.swing.event.TreeSelectionEvent;
import javax.swing.event.TreeSelectionListener;
import javax.swing.table.DefaultTableModel;
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
import no.analysis.ProfileSegmenter;
import no.analysis.ShellAnalysis;
import no.collections.NucleusCollection;
import no.components.Profile;
import no.export.PopulationExporter;
import no.export.StatsExporter;
import no.imports.PopulationImporter;
import no.nuclei.Nucleus;
import no.utility.TreeOrderHashMap;

import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;

import javax.swing.JTextArea;

import java.awt.SystemColor;
import java.io.File;
import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;
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
import javax.swing.JComboBox;
import javax.swing.JMenuItem;
import javax.swing.JOptionPane;
import javax.swing.JPopupMenu;
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
import java.awt.GridLayout;
import java.awt.Shape;

import javax.swing.ListSelectionModel;

import org.jfree.chart.ChartFactory;
import org.jfree.chart.ChartPanel;
import org.jfree.chart.JFreeChart;
import org.jfree.chart.annotations.XYShapeAnnotation;
import org.jfree.chart.plot.CategoryPlot;
import org.jfree.chart.plot.PlotOrientation;
import org.jfree.chart.plot.ValueMarker;
import org.jfree.chart.plot.XYPlot;
import org.jfree.chart.renderer.category.BoxAndWhiskerRenderer;
import org.jfree.chart.renderer.category.StandardBarPainter;
import org.jfree.chart.renderer.category.StatisticalBarRenderer;
import org.jfree.chart.renderer.xy.StandardXYBarPainter;
import org.jfree.chart.renderer.xy.StandardXYItemRenderer;
import org.jfree.chart.renderer.xy.XYBarRenderer;
import org.jfree.chart.renderer.xy.XYDifferenceRenderer;
import org.jfree.chart.renderer.xy.XYLineAndShapeRenderer;
import org.jfree.data.category.CategoryDataset;
import org.jfree.data.statistics.BoxAndWhiskerCategoryDataset;
import org.jfree.data.statistics.DefaultBoxAndWhiskerCategoryDataset;
import org.jfree.data.statistics.HistogramDataset;
import org.jfree.data.xy.DefaultXYDataset;
import org.jfree.data.xy.XYDataset;
import org.jfree.data.xy.XYSeriesCollection;

import javax.swing.JTabbedPane;

public class MainWindow extends JFrame implements ActionListener {
		
	/**
	 * The fields for setting the version. Version will be stored in AnalysisDatasets.
	 * Backwards compatability should be maintained between bugfix increments, but is not
	 * guaranteed between revision or major version increments.
	 */
	public static final int VERSION_MAJOR    = 1;
	public static final int VERSION_REVISION = 8;
	public static final int VERSION_BUGFIX   = 1;
	
	private static final int PROFILE_TAB = 0;
	private static final int STATS_TAB = 1;
	private static final int ANALYSIS_TAB = 2;
	private static final int BOXPLOTS_TAB = 3;
	private static final int VARIABILITY_TAB = 4;
	private static final int SIGNALS_TAB = 5;
	private static final int CLUSTERS_TAB = 6;
	private static final int VENN_TAB = 7;
	
	private static final double FIVE_PERCENT_SIGNIFICANCE_LEVEL = 0.05;
	private static final double ONE_PERCENT_SIGNIFICANCE_LEVEL = 0.01;

	private static final long serialVersionUID = 1L;
	private JPanel contentPane;
	private JTextArea textArea = new JTextArea();;
	private JLabel lblStatusLine = new JLabel("No analysis open");
//	private final JPanel panelAggregates = new JPanel(); // holds consensus, tab and population panels
	private JTable tablePopulationStats;
	private JTable tableAnalysisParamters;
	private final JPanel panelGeneralData = new JPanel(); // holds the tabs
	
	private final JPanel panelPopulations = new JPanel(); // holds list of active populations
	private JTable populationTable;
	private JXTreeTable treeTable;
	private PopulationListPopupMenu populationPopup;
	
	private JTabbedPane tabbedPane;
	private JTabbedPane signalsTabPane;
		
	private ChartPanel profileChartPanel;
	private ChartPanel frankenChartPanel;
	private ChartPanel consensusChartPanel;
	private ChartPanel rawChartPanel;
	private JButton runRefoldingButton;
	
	private ChartPanel areaBoxplotChartPanel;
	private ChartPanel perimBoxplotChartPanel;
	private ChartPanel maxFeretBoxplotChartPanel;
	private ChartPanel minFeretBoxplotChartPanel;
	private ChartPanel differenceBoxplotChartPanel;
	
	private ChartPanel variabilityChartPanel; 
	
	private ChartPanel shellsChartPanel; 
	private JPanel shellsPanel;
	
	private ChartPanel signalsChartPanel; // consensus nucleus plus signals
	private JPanel signalsPanel;// signals container for chart and stats table
	private JTable signalStatsTable;
	
	private ChartPanel signalAngleChartPanel; // consensus nucleus plus signals
	private ChartPanel signalDistanceChartPanel; // consensus nucleus plus signals
	private JPanel signalHistogramPanel;// signals container for chart and stats table
	
	private JPanel clusteringPanel;// container for clustering options and display
	
	private JPanel vennPanel; // show overlaps between populations
	private JTable vennTable;
	
	private JScrollPane wilcoxonPanel; // compare populations
	private JTable wilcoxonAreaTable;
	private JTable wilcoxonPerimTable;
	private JTable wilcoxonFeretTable;
	private JTable wilcoxonMinFeretTable;
	private JTable wilcoxonDifferenceTable;
	
	private ChartPanel segmentsBoxplotChartPanel; // for displaying the legnth of a given segment
	private ChartPanel segmentsProfileChartPanel; // for displaying the profiles of a given segment
	
	private JPanel segmentsBoxplotPanel;// container for boxplots chart and decoration
	private JComboBox segmentSelectionBox; // choose which segments to compare

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
			// Create the log panel
			//---------------
			contentPane.add(createLogPanel(), BorderLayout.WEST);

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
//			panelGeneralData.setLayout(new BoxLayout(panelGeneralData, BoxLayout.Y_AXIS));
			panelGeneralData.setLayout(new GridLayout(2, 0, 0, 0));
			
			// make a panel for the populations and consensus chart
			JPanel topGeneralPanel = new JPanel();
			topGeneralPanel.setLayout(new GridLayout(0, 2, 0, 0));
			panelGeneralData.add(topGeneralPanel);
			
			
			createPopulationsPanel();
			topGeneralPanel.add(panelPopulations);		
			
			
			
			
			//---------------
			// Create the consensus chart
			//---------------
			createConsensusChartPanel();
			topGeneralPanel.add(consensusChartPanel);
			
						
			//---------------
			// Create the variability chart
			//---------------
			JFreeChart variablityChart = ChartFactory.createXYLineChart(null,
					"Position", "IQR", null);
			XYPlot variabilityPlot = variablityChart.getXYPlot();
			variabilityPlot.setBackgroundPaint(Color.WHITE);
			variabilityPlot.getDomainAxis().setRange(0,100);
			
			tabbedPane = new JTabbedPane(JTabbedPane.TOP);
			panelGeneralData.add(tabbedPane);
			
			JTabbedPane profilesPane = createProfilesPanel();
			tabbedPane.addTab("Profiles", null, profilesPane, null);

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
			tabbedPane.addTab("Boxplots", createBoxplotsPanel());
			
			//---------------
			// Create variabillity chart
			//---------------

			variabilityChartPanel = new ChartPanel(variablityChart);
			tabbedPane.addTab("Variability", null, variabilityChartPanel, null);
//			shellsChartPanel = new ChartPanel(shellsChart);
			
			
			//---------------
			// Create the signals tab panel
			//---------------
			signalsTabPane = createSignalsTabPanel();
			tabbedPane.addTab("Signals", signalsTabPane);
			

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
			vennTable.setModel(DatasetCreator.createVennTable(null));
			
			
			//---------------
			// Create the Wilcoxon test panel
			//---------------
			wilcoxonPanel = createWilcoxonPanel();
			tabbedPane.addTab("Wilcoxon", null, wilcoxonPanel, null);
			
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
//			this.pack();

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
	private JScrollPane createLogPanel(){
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
		return scrollPane;
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
		return VERSION_MAJOR+"."+VERSION_REVISION+"."+VERSION_BUGFIX;
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
		if(Integer.valueOf(parts[0])!=this.VERSION_MAJOR){
			ok = false;
		}
		// dataset revision should be equal or greater to program
		if(Integer.valueOf(parts[1])<this.VERSION_REVISION){
			log("Dataset was created with an older version of the program");
			log("Some functionality may not work as expected");
//			ok = false;
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
	
	
	private JTabbedPane createProfilesPanel(){
		
		JTabbedPane profilesTabPanel = new JTabbedPane(JTabbedPane.TOP);
		Dimension minimumChartSize = new Dimension(50, 100);
		Dimension preferredChartSize = new Dimension(400, 300);
		
		//---------------
		// Create the regular profile chart
		//---------------
		JFreeChart profileChart = ChartFactory.createXYLineChart(null,
	            "Position", "Angle", null);
		XYPlot plot = profileChart.getXYPlot();
		plot.getDomainAxis().setRange(0,100);
		plot.getRangeAxis().setRange(0,360);
		plot.setBackgroundPaint(Color.WHITE);
		profileChartPanel = new ChartPanel(profileChart);
		profileChartPanel.setMinimumSize(minimumChartSize);
		profileChartPanel.setPreferredSize(preferredChartSize);
		profileChartPanel.setMinimumDrawWidth( 0 );
		profileChartPanel.setMinimumDrawHeight( 0 );
		
		//---------------
		// Create the franken profile chart
		//---------------
		JFreeChart frankenChart = ChartFactory.createXYLineChart(null,
				"Position", "Angle", null);
		XYPlot frankenPlot = frankenChart.getXYPlot();
		frankenPlot.getDomainAxis().setRange(0,100);
		frankenPlot.getRangeAxis().setRange(0,360);
		frankenPlot.setBackgroundPaint(Color.WHITE);
		frankenChartPanel = new ChartPanel(frankenChart);
		frankenChartPanel.setMinimumSize(minimumChartSize);
		frankenChartPanel.setPreferredSize(preferredChartSize);
		frankenChartPanel.setMinimumDrawWidth( 0 );
		frankenChartPanel.setMinimumDrawHeight( 0 );
		
		
		//---------------
		// Create the raw profile chart
		//---------------
		JPanel rawPanel = new JPanel();
		rawPanel.setLayout(new BorderLayout());
		JFreeChart rawChart = ChartFactory.createXYLineChart(null,
				"Position", "Angle", null);
		XYPlot rawPlot = rawChart.getXYPlot();
		rawPlot.getDomainAxis().setRange(0,100);
		rawPlot.getRangeAxis().setRange(0,360);
		rawPlot.setBackgroundPaint(Color.WHITE);
		rawChartPanel = new ChartPanel(rawChart);
		rawChartPanel.setMinimumDrawWidth( 0 );
		rawChartPanel.setMinimumDrawHeight( 0 );
		rawPanel.setMinimumSize(minimumChartSize);
		rawPanel.setPreferredSize(preferredChartSize);
		rawPanel.add(rawChartPanel, BorderLayout.CENTER);
		
		JRadioButton leftButton  = new JRadioButton("Left");
		JRadioButton rightButton = new JRadioButton("Right");
		
		leftButton.setSelected(true);
		
		leftButton.setActionCommand("LeftAlignRawProfile");
		rightButton.setActionCommand("RightAlignRawProfile");
		leftButton.addActionListener(this);
		rightButton.addActionListener(this);
		

		//Group the radio buttons.
		final ButtonGroup alignGroup = new ButtonGroup();
		alignGroup.add(leftButton);
		alignGroup.add(rightButton);
		
		JPanel alignPanel = new JPanel();
		alignPanel.setLayout(new BoxLayout(alignPanel, BoxLayout.X_AXIS));

		
		alignPanel.add(leftButton);
		alignPanel.add(rightButton);
		rawPanel.add(alignPanel, BorderLayout.NORTH);
		
		//---------------
		// Add to the tabbed panel
		//---------------
		profilesTabPanel.addTab("Normalised", null, profileChartPanel, null);
		profilesTabPanel.addTab("Raw", null, rawPanel, null);
		profilesTabPanel.addTab("FrankenProfile", null, frankenChartPanel, null);
				
		return profilesTabPanel;
		
	}
	
	/**
	 * Create the tree for populations analysed
	 */
	private void createPopulationsPanel(){
		//---------------
		// Create the populations list
		//---------------
		panelPopulations.setMinimumSize(new Dimension(50, 100));
//		panel.add(panelPopulations);
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

		runRefoldingButton.addMouseListener(new MouseAdapter() {
			@Override
			public void mouseClicked(MouseEvent arg0) {

				List<AnalysisDataset> datasets = getSelectedRowsFromTreeTable();

				if(datasets.size()==1){

					AnalysisDataset d = datasets.get(0);
					MainWindow.this.refoldNucleus(d);
				}

			}
		});
		runRefoldingButton.setVisible(false);
		consensusChartPanel.add(runRefoldingButton);
	}
	
	
	private JTabbedPane createSignalsTabPanel(){
		JTabbedPane signalsTabPane = new JTabbedPane(JTabbedPane.TOP);
				
		signalsPanel = new JPanel(); // main container in tab
		signalsPanel.setLayout(new BoxLayout(signalsPanel, BoxLayout.X_AXIS));

		// Stats panel & consensus
		
		DefaultTableModel signalsTableModel = new DefaultTableModel();
		signalsTableModel.addColumn("");
		signalsTableModel.addColumn("");
		signalStatsTable = new JTable(); // table  for basic stats
		signalStatsTable.setModel(signalsTableModel);
		signalStatsTable.setEnabled(false);
		
		JScrollPane signalStatsScrollPane = new JScrollPane(signalStatsTable);
		signalsPanel.add(signalStatsScrollPane);
		

		JFreeChart signalsChart = ChartFactory.createXYLineChart(null,  // chart for conseusns
				null, null, null);
		XYPlot signalsPlot = signalsChart.getXYPlot();
		signalsPlot.setBackgroundPaint(Color.WHITE);
		signalsPlot.getDomainAxis().setVisible(false);
		signalsPlot.getRangeAxis().setVisible(false);
		JFreeChart signalAngleChart = ChartFactory.createHistogram(null, "Angle", "Count", null, PlotOrientation.VERTICAL, true, true, true);
		signalAngleChart.getPlot().setBackgroundPaint(Color.white);
		
		signalsChartPanel = new ChartPanel(signalsChart);
		signalsPanel.add(signalsChartPanel);
		
		
		
		JFreeChart signalDistanceChart = ChartFactory.createHistogram(null, "Distance", "Count", null, PlotOrientation.VERTICAL, true, true, true);
		signalDistanceChart.getPlot().setBackgroundPaint(Color.white);
		
		//---------------
		// Create the shells panel
		//---------------
		JFreeChart shellsChart = ChartFactory.createBarChart(null, "Shell", "Percent", null);
		shellsChart.getCategoryPlot().setBackgroundPaint(Color.WHITE);
		shellsChart.getCategoryPlot().getRangeAxis().setRange(0,100);
		shellsChartPanel = new ChartPanel(shellsChart);
		
		signalsTabPane.addTab("Overview", null, signalsPanel, null);
		

		//---------------
		// Create the signal histograms panel
		//---------------
		signalHistogramPanel = new JPanel(); // main container in tab
		signalHistogramPanel.setLayout(new BoxLayout(signalHistogramPanel, BoxLayout.Y_AXIS));
		signalAngleChartPanel = new ChartPanel(signalAngleChart);
		signalDistanceChartPanel = new ChartPanel(signalDistanceChart);

		signalHistogramPanel.add(signalAngleChartPanel);
		signalHistogramPanel.add(signalDistanceChartPanel);
		signalsTabPane.addTab("Signal histograms", null, signalHistogramPanel, null);
		signalsTabPane.addTab("Shells", null, shellsChartPanel, null);
		return signalsTabPane;
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
		tablePopulationStats.setModel(DatasetCreator.createStatsTable(null));
		return scrollPane;
	}
	
	private JScrollPane createAnalysisParametersPanel(){
		
		JScrollPane scrollPane = new JScrollPane();
		JPanel panel = new JPanel();
		
		panel.setLayout(new BorderLayout(0, 0));

		tableAnalysisParamters = new JTable();
		tableAnalysisParamters.setModel(DatasetCreator.createAnalysisParametersTable(null));
		tableAnalysisParamters.setEnabled(false);
		panel.add(tableAnalysisParamters, BorderLayout.CENTER);

		scrollPane.setViewportView(panel);
		scrollPane.setColumnHeaderView(tableAnalysisParamters.getTableHeader());
		return scrollPane;
	}
	
	private JPanel createBoxplotsPanel(){
		JPanel boxplotSplitPanel = new JPanel(); // main container in tab

		boxplotSplitPanel.setLayout(new BoxLayout(boxplotSplitPanel, BoxLayout.X_AXIS));
		
		JFreeChart areaBoxplot = ChartFactory.createBoxAndWhiskerChart(null, null, null, new DefaultBoxAndWhiskerCategoryDataset(), false);	        
		JFreeChart perimBoxplot = ChartFactory.createBoxAndWhiskerChart(null, null, null, new DefaultBoxAndWhiskerCategoryDataset(), false);	        
		JFreeChart maxFeretBoxplot = ChartFactory.createBoxAndWhiskerChart(null, null, null, new DefaultBoxAndWhiskerCategoryDataset(), false);	        
		JFreeChart minFeretBoxplot = ChartFactory.createBoxAndWhiskerChart(null, null, null, new DefaultBoxAndWhiskerCategoryDataset(), false);	        
		JFreeChart differenceBoxplot = ChartFactory.createBoxAndWhiskerChart(null, null, null, new DefaultBoxAndWhiskerCategoryDataset(), false);	
		
		
		areaBoxplotChartPanel = new ChartPanel(areaBoxplot);
		boxplotSplitPanel.add(areaBoxplotChartPanel);
		
		perimBoxplotChartPanel = new ChartPanel(perimBoxplot);
		boxplotSplitPanel.add(perimBoxplotChartPanel);
		
		maxFeretBoxplotChartPanel = new ChartPanel(maxFeretBoxplot);
		boxplotSplitPanel.add(maxFeretBoxplotChartPanel);
		
		minFeretBoxplotChartPanel = new ChartPanel(minFeretBoxplot);
		boxplotSplitPanel.add(minFeretBoxplotChartPanel);
		
		differenceBoxplotChartPanel = new ChartPanel(differenceBoxplot);
		boxplotSplitPanel.add(differenceBoxplotChartPanel);
		
		return boxplotSplitPanel;
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
		return panel;
	}
	
	private JPanel createSegmentBoxplotsPanel(){
		JPanel panel = new JPanel(); // main container in tab

		panel.setLayout(new BorderLayout());
		
		JFreeChart boxplot = ChartFactory.createBoxAndWhiskerChart(null, null, null, new DefaultBoxAndWhiskerCategoryDataset(), false);	        

		segmentsBoxplotChartPanel = new ChartPanel(boxplot);
		panel.add(segmentsBoxplotChartPanel, BorderLayout.CENTER);
		
		segmentSelectionBox = new JComboBox();
		segmentSelectionBox.setActionCommand("SegmentBoxplotChoice");
		segmentSelectionBox.addActionListener(this);
		panel.add(segmentSelectionBox, BorderLayout.NORTH);
		


		return panel;
	}
		
	@Override
	public void actionPerformed(ActionEvent e) {

		if(e.getActionCommand().equals("SegmentBoxplotChoice")){
			String segName = (String) segmentSelectionBox.getSelectedItem();
			// create the appropriate chart
			//TODO
			updateSegmentsBoxplot(getSelectedRowsFromTreeTable(), segName);
			updateSegmentsProfile(getSelectedRowsFromTreeTable(), segName);
			
		}
		
		if(e.getActionCommand().equals("LeftAlignRawProfile")){
			updateRawProfileImage(getSelectedRowsFromTreeTable(), false);
		}
		
		if(e.getActionCommand().equals("RightAlignRawProfile")){
			updateRawProfileImage(getSelectedRowsFromTreeTable(), true);
		}
	}
	
	
	/**
	 * Create the panel that will hold the statistical tests between populations
	 */
	private JScrollPane createWilcoxonPanel(){
		JScrollPane scrollPane = new JScrollPane();
		JPanel panel = new JPanel();
		scrollPane.setViewportView(panel);
		panel.setLayout(new BorderLayout());
//		tabbedPane.addTab("Wilcoxon", null, wilcoxonPanel, null);
		
		JPanel wilcoxonPartsPanel = new JPanel();
		wilcoxonPartsPanel.setLayout(new BoxLayout(wilcoxonPartsPanel, BoxLayout.Y_AXIS));
		
		Dimension minSize = new Dimension(10, 10);
		Dimension prefSize = new Dimension(10, 10);
		Dimension maxSize = new Dimension(Short.MAX_VALUE, 10);
		wilcoxonPartsPanel.add(new Box.Filler(minSize, prefSize, maxSize));
		
		wilcoxonPartsPanel.add(new JLabel("Pairwise comparisons between populations using Mann-Whitney U test (aka Wilcoxon rank-sum test)"));
		wilcoxonPartsPanel.add(new JLabel("Above the diagonal: Mann-Whitney U statistics"));
		wilcoxonPartsPanel.add(new JLabel("Below the diagonal: p-values"));
		wilcoxonPartsPanel.add(new JLabel("p-values significant at 5% and 1% levels after Bonferroni correction are highlighted in yellow and green"));
		
		wilcoxonAreaTable = new JTable(DatasetCreator.createWilcoxonAreaTable(null));
		addWilconxonTable(wilcoxonPartsPanel, wilcoxonAreaTable, "Areas");
//		panel.add(wilcoxonAreaTable.getTableHeader(), BorderLayout.NORTH);
		scrollPane.setColumnHeaderView(wilcoxonAreaTable.getTableHeader());

		
		wilcoxonPerimTable = new JTable(DatasetCreator.createWilcoxonPerimeterTable(null));
		addWilconxonTable(wilcoxonPartsPanel, wilcoxonPerimTable, "Perimeters");
		
		wilcoxonMinFeretTable = new JTable(DatasetCreator.createWilcoxonMinFeretTable(null));
		addWilconxonTable(wilcoxonPartsPanel, wilcoxonMinFeretTable, "Min feret");

		
		wilcoxonFeretTable = new JTable(DatasetCreator.createWilcoxonMaxFeretTable(null));
		addWilconxonTable(wilcoxonPartsPanel, wilcoxonFeretTable, "Feret");
		
		
		wilcoxonDifferenceTable = new JTable(DatasetCreator.createWilcoxonVariabilityTable(null));
		addWilconxonTable(wilcoxonPartsPanel, wilcoxonDifferenceTable, "Differences to median");
		
		panel.add(wilcoxonPartsPanel, BorderLayout.CENTER);
		return scrollPane;
	}
	
	
	/**
	 * Prepare a wilcoxon table
	 * @param panel the JPanel to add the table to
	 * @param table the table to add
	 * @param model the model to provide
	 * @param label the label for the table
	 */
	private void addWilconxonTable(JPanel panel, JTable table, String label){
		Dimension minSize = new Dimension(10, 10);
		Dimension prefSize = new Dimension(10, 10);
		Dimension maxSize = new Dimension(Short.MAX_VALUE, 10);
		panel.add(new Box.Filler(minSize, prefSize, maxSize));
		panel.add(new JLabel(label));
//		table = new JTable();
		panel.add(table);
		table.setEnabled(false);
//		table.setModel(model);
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

			List<NucleusCollection> subs = fishMapper.getSubCollections();
			for(NucleusCollection sub : subs){
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
				
//				AnalysisSetupWindow test = new AnalysisSetupWindow();
				
				AnalysisCreator analysisCreator = new AnalysisCreator(MainWindow.this);
				analysisCreator.run();

//				List<NucleusCollection> result = analysisCreator.getPopulations();
				List<AnalysisDataset> datasets = analysisCreator.getDatasets();
				
				if(datasets.size()==0 || datasets==null){
					log("No datasets returned");
				}
				
				// new style datasets
				for(AnalysisDataset d : datasets){
					
					addDataset(d);
//					d.setName(checkName(d.getName()));
//					MainWindow.this.analysisDatasets.put(d.getUUID(), d);
//					MainWindow.this.populationNames.put(d.getName(), d.getUUID());
					
					
					for(AnalysisDataset child : d.getChildDatasets()){
						addDataset(child);
//						child.setName(checkName(child.getName()));
//						MainWindow.this.analysisDatasets.put(child.getUUID(), child);
//						MainWindow.this.populationNames.put(child.getCollection().getName(), child.getUUID());
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
	 * Call the setup for a new cluster analysis of an existing dataset.
	 * Results are added to the dataset list.
	 * @param collection the collection to cluster
	 */
	public void clusterAnalysis(NucleusCollection collection){
		if(collection !=null){
			final UUID id = collection.getID();
			Thread thr = new Thread() {
				public void run() {
					try{
						
						
						ClusteringSetupWindow clusterSetup = new ClusteringSetupWindow(MainWindow.this);
						Map<String, Object> options = clusterSetup.getOptions();
//						for(String key : options.keySet()){
//							IJ.log(key+": "+options.get(key).toString());
//						}
						if(clusterSetup.isReadyToRun()){ // if dialog was cancelled, skip

							logc("Running cluster analysis...");

							NucleusClusterer clusterer = new NucleusClusterer(  (Integer) options.get("type") );
							clusterer.setClusteringOptions(options);

							AnalysisDataset parent = MainWindow.this.analysisDatasets.get(id);
							boolean ok = clusterer.cluster(parent.getCollection());
							if(ok){
								log("OK");
								log("Found "+clusterer.getNumberOfClusters()+" clusters");

								parent.setClusterTree(clusterer.getNewickTree());

								for(int cluster=0;cluster<clusterer.getNumberOfClusters();cluster++){
									NucleusCollection c = clusterer.getCluster(cluster);
									log("Cluster "+cluster+":");

									logc("Reapplying morphology...");
									ok = MorphologyAnalysis.reapplyProfiles(c, MainWindow.this.analysisDatasets.get(id).getCollection());
									if(ok){
										log("OK");
									} else {
										log("Error");
									}

									// attach the clusters to their parent collection
									parent.addCluster(c);

									addDataset(parent.getChildDataset(c.getID()));
//									MainWindow.this.analysisDatasets.put(c.getID(), parent.getChildDataset(c.getID()));
//									if(MainWindow.this.populationNames.containsKey(c.getName())){
//										c.setName(c.getName()+"_1");
//									}
//									MainWindow.this.populationNames.put(c.getName(), c.getID());

								}
								updatePopulationList();	
								//							parent.save();
							} else {
								log("Error");
							}
						}
						clusterSetup.dispose();

					} catch (Exception e){
						log("Error in cluster analysis: "+e.getMessage());
					}
				}
			};
			thr.start();
		}

	}
	
	

	/**
	 * Rename an existing dataset and update the population list.
	 * @param dataset the dataset to rename
	 */
	public void renameCollection(AnalysisDataset dataset){
		NucleusCollection collection = dataset.getCollection();
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
	 * Call the setup for a new shell analysis on the given dataset,
	 * @param dataset the dataset to analyse
	 */
	public void newShellAnalysis(AnalysisDataset dataset){
		
		NucleusCollection collection = dataset.getCollection();
		if(collection!=null){
			String shellString = JOptionPane.showInputDialog(this, "Number of shells", 5);
			final UUID id = collection.getID();
			// validate
			if(!shellString.isEmpty() && shellString!=null){
				final int shellCount = Integer.parseInt(shellString);
				Thread thr = new Thread() {
					public void run() {
						try{
							logc("Running shell analysis...");
							boolean ok = ShellAnalysis.run(MainWindow.this.analysisDatasets.get(id), shellCount);
							if(ok){
								log("OK");
							} else {
								log("Error");
							}

							List<AnalysisDataset> list = new ArrayList<AnalysisDataset>(0);
							list.add(MainWindow.this.analysisDatasets.get(id));
							updatePanels(list);

						} catch (Exception e){
							log("Error in shell analysis");
						}
					}
				};
				thr.start();
			}
		}
	}
	
	

	/**
	 * Refold the consensus nucleus for the given dataset using default parameters
	 * @param dataset the dataset to refold
	 */
	public void refoldNucleus(AnalysisDataset dataset){
		NucleusCollection collection = dataset.getCollection();
		if(collection!=null){
			final UUID id = collection.getID();

			Thread thr = new Thread() {
				public void run() {
					try{
						AnalysisDataset d = MainWindow.this.analysisDatasets.get(id);

						for(Component c : consensusChartPanel.getComponents() ){
							if(c.getClass()==JButton.class){
								c.setVisible(false);
							}
						}
						
						logc("Refolding profile...");
						boolean ok = CurveRefolder.run(d.getCollection(), 
								d.getAnalysisOptions().getNucleusClass(), 
								"Fast");
						if(ok){
							log("OK");
							d.getAnalysisOptions().setRefoldNucleus(true);
							d.getAnalysisOptions().setRefoldMode("Fast");
							List<AnalysisDataset> list = new ArrayList<AnalysisDataset>(0);
							list.add(d);
							updatePanels(list);

						} else {
							log("Error");
						}
					} catch(Exception e){
						log("Error refolding");
					}
				}
			};
			thr.start();
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
					OpenDialog fileDialog = new OpenDialog("Select a saved dataset...");
					String fileName = fileDialog.getPath();
					if(fileName==null) return;
					
					logc("Opening dataset...");
					
					// read the dataset
					AnalysisDataset dataset = PopulationImporter.readDataset(new File(fileName));
					
					if(checkVersion( dataset.getVersion() )){

						dataset.setRoot(true);
						
						addDataset(dataset);
						
						for(AnalysisDataset child : dataset.getAllChildDatasets() ){
							addDataset(child);
						}
						
						log("OK");
						log("Opened dataset: "+dataset.getName());
	//					log("Dataset contains: "+dataset.getChildCount()+" subsets");
	
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
					updateProfileImage(list);
					updateRawProfileImage(list, false);
					updateFrankenProfileChart(list);
					updateConsensusImage(list);
					updateBoxplots(list);
					updateVariabilityChart(list);
					updateShellPanel(list);
					updateSignalsPanel(list);
					updateSignalHistogramPanel(list);
					updateClusteringPanel(list);
					updateVennPanel(list);
					updateWilcoxonPanel(list);
					
					if(list!=null){
						// get the list of segments from the datasets
						ComboBoxModel aModel = new DefaultComboBoxModel(list.get(0).getCollection().getSegmentNames().toArray(new String[0]));
						segmentSelectionBox.setModel(aModel);
						segmentSelectionBox.setSelectedIndex(0);
						updateSegmentsBoxplot(list, (String) segmentSelectionBox.getSelectedItem()); // get segname from panel
						updateSegmentsProfile(list, (String) segmentSelectionBox.getSelectedItem()); // get segname from panel
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
	 * is set in the DatasetCreator as part of the label
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
		TableModel model = DatasetCreator.createAnalysisParametersTable(list);
		tableAnalysisParamters.setModel(model);
	}
	
	
	
	/**
	 * Update the stats panel with data from the given datasets
	 * @param list the datasets
	 */
	public void updateStatsPanel(List<AnalysisDataset> list){
		// format the numbers and make into a tablemodel
		TableModel model = DatasetCreator.createStatsTable(list);
		tablePopulationStats.setModel(model);
	}
	
	/**
	 * Update the venn panel with data from the given datasets
	 * @param list the datasets
	 */
	public void updateVennPanel(List<AnalysisDataset> list){
		// format the numbers and make into a tablemodel
		TableModel model = DatasetCreator.createVennTable(list);
		vennTable.setModel(model);
		int columns = vennTable.getColumnModel().getColumnCount();
		for(int i=1;i<columns;i++){
			vennTable.getColumnModel().getColumn(i).setCellRenderer(new VennTableCellRenderer());

		}
	}
	
	/**
	 * Update the wilcoxon panel with data from the given datasets
	 * @param list the datasets
	 */
	public void updateWilcoxonPanel(List<AnalysisDataset> list){
		// format the numbers and make into a tablemodel

		wilcoxonAreaTable.setModel(DatasetCreator.createWilcoxonAreaTable(list));
		
		int columns = wilcoxonAreaTable.getColumnModel().getColumnCount();
		for(int i=1;i<columns;i++){
			wilcoxonAreaTable.getColumnModel().getColumn(i).setCellRenderer(new WilcoxonTableCellRenderer());
		}
		
		wilcoxonPerimTable.setModel(DatasetCreator.createWilcoxonPerimeterTable(list));
		columns = wilcoxonPerimTable.getColumnModel().getColumnCount();
		for(int i=1;i<columns;i++){
			wilcoxonPerimTable.getColumnModel().getColumn(i).setCellRenderer(new WilcoxonTableCellRenderer());
		}
		
		wilcoxonMinFeretTable.setModel(DatasetCreator.createWilcoxonMinFeretTable(list));
		columns = wilcoxonMinFeretTable.getColumnModel().getColumnCount();
		for(int i=1;i<columns;i++){
			wilcoxonMinFeretTable.getColumnModel().getColumn(i).setCellRenderer(new WilcoxonTableCellRenderer());
		}
		
		wilcoxonFeretTable.setModel(DatasetCreator.createWilcoxonMaxFeretTable(list));
		columns = wilcoxonFeretTable.getColumnModel().getColumnCount();
		for(int i=1;i<columns;i++){
			wilcoxonFeretTable.getColumnModel().getColumn(i).setCellRenderer(new WilcoxonTableCellRenderer());
		}
		
		wilcoxonDifferenceTable.setModel(DatasetCreator.createWilcoxonVariabilityTable(list));
		columns = wilcoxonDifferenceTable.getColumnModel().getColumnCount();
		for(int i=1;i<columns;i++){
			wilcoxonDifferenceTable.getColumnModel().getColumn(i).setCellRenderer(new WilcoxonTableCellRenderer());
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
	
	public void updateRawProfileImage(List<AnalysisDataset> list, boolean rightAlign){
		
		try {
			if(list.size()==1){

				// full segment colouring
				XYDataset ds = DatasetCreator.createRawSegmentDataset(list.get(0).getCollection());
				JFreeChart chart = makeProfileChart(ds);
				XYPlot plot = chart.getXYPlot();
				plot.getDomainAxis().setRange(0,list.get(0).getCollection().getMedianArrayLength());
				rawChartPanel.setChart(chart);
			} else {
				// many profiles, colour them all the same
				List<XYSeriesCollection> ds = DatasetCreator.createRawMultiProfileIQRDataset(list, rightAlign);				
				
				XYDataset profileDS = DatasetCreator.createRawMultiProfileDataset(list, rightAlign);
				
				JFreeChart chart = 
						ChartFactory.createXYLineChart(null,
						                "Position", "Angle", null, PlotOrientation.VERTICAL, true, true,
						                false);
				
				// find the maximum profile length
				int length = 100;
				for(AnalysisDataset d : list){
					if(   (int) d.getCollection().getMedianArrayLength()>length){
						length = (int) d.getCollection().getMedianArrayLength();
					}
				}
				

				XYPlot plot = chart.getXYPlot();
				plot.getDomainAxis().setRange(0,length);
				plot.getRangeAxis().setRange(0,360);
				plot.setBackgroundPaint(Color.WHITE);
				plot.addRangeMarker(new ValueMarker(180, Color.BLACK, new BasicStroke(2.0f)));
				
				int i=0;
				for(XYSeriesCollection c : ds){

					// find the series index
					String name = (String) c.getSeriesKey(0);
					String[] names = name.split("_");
					int index = Integer.parseInt(names[1]);
					
					// add to dataset
					plot.setDataset(i, c);
					
					// make a transparent color based on teh profile segmenter system
					Color pColor = ColourSelecter.getSegmentColor(index);
					Color color = new Color(pColor.getRed(), pColor.getGreen(), pColor.getBlue(), 128);
					
					
					XYDifferenceRenderer xydr = new XYDifferenceRenderer(color, color, false);
					
					// go through each series in the collection, and set the line colour
					for(int series=0;series<c.getSeriesCount();series++){
						xydr.setSeriesPaint(series, color);
						xydr.setSeriesVisibleInLegend(series, false);
						
					}
					plot.setRenderer(i, xydr);
					
					
					i++;
				}

				plot.setDataset(i, profileDS);
				plot.setRenderer(i, new StandardXYItemRenderer());

				for (int j = 0; j < profileDS.getSeriesCount(); j++) {
					plot.getRenderer(i).setSeriesVisibleInLegend(j, Boolean.FALSE);
					plot.getRenderer(i).setSeriesStroke(j, new BasicStroke(2));
					String name = (String) profileDS.getSeriesKey(j);
					String[] names = name.split("_");
					plot.getRenderer(i).setSeriesPaint(j, ColourSelecter.getSegmentColor(Integer.parseInt(names[1])).darker());
				}	
				
				rawChartPanel.setChart(chart);
			}
			
		} catch (Exception e) {
			log("Error in plotting profile");
		} 
	}
	
	
	/**
	 * Update the profile panel with data from the given datasets
	 * @param list the datasets
	 */	
	public void updateProfileImage(List<AnalysisDataset> list){
		
		try {
			if(list.size()==1){

				// full segment colouring
				XYDataset ds = DatasetCreator.createNormalisedSegmentDataset(list.get(0).getCollection());
				JFreeChart chart = makeProfileChart(ds);
				profileChartPanel.setChart(chart);
			} else {
				// many profiles, colour them all the same
				List<XYSeriesCollection> ds = DatasetCreator.createMultiProfileIQRDataset(list);				
				
				XYDataset profileDS = DatasetCreator.createMultiProfileDataset(list);
				
				JFreeChart chart = 
						ChartFactory.createXYLineChart(null,
						                "Position", "Angle", null, PlotOrientation.VERTICAL, true, true,
						                false);

				XYPlot plot = chart.getXYPlot();
				plot.getDomainAxis().setRange(0,100);
				plot.getRangeAxis().setRange(0,360);
				plot.setBackgroundPaint(Color.WHITE);
				plot.addRangeMarker(new ValueMarker(180, Color.BLACK, new BasicStroke(2.0f)));
				
				int i=0;
				for(XYSeriesCollection c : ds){

					// find the series index
					String name = (String) c.getSeriesKey(0);
					String[] names = name.split("_");
					int index = Integer.parseInt(names[1]);
					
					// add to dataset
					plot.setDataset(i, c);
					
					// make a transparent color based on teh profile segmenter system
					Color pColor = ColourSelecter.getSegmentColor(index);
					Color color = new Color(pColor.getRed(), pColor.getGreen(), pColor.getBlue(), 128);
					
					
					XYDifferenceRenderer xydr = new XYDifferenceRenderer(color, color, false);
					
					// go through each series in the collection, and set the line colour
					for(int series=0;series<c.getSeriesCount();series++){
						xydr.setSeriesPaint(series, color);
						xydr.setSeriesVisibleInLegend(series, false);
						
					}
					plot.setRenderer(i, xydr);
					
					
					i++;
				}

				plot.setDataset(i, profileDS);
				plot.setRenderer(i, new StandardXYItemRenderer());

				for (int j = 0; j < profileDS.getSeriesCount(); j++) {
					plot.getRenderer(i).setSeriesVisibleInLegend(j, Boolean.FALSE);
					plot.getRenderer(i).setSeriesStroke(j, new BasicStroke(2));
					String name = (String) profileDS.getSeriesKey(j);
					String[] names = name.split("_");
					plot.getRenderer(i).setSeriesPaint(j, ColourSelecter.getSegmentColor(Integer.parseInt(names[1])).darker());
				}	
				
				profileChartPanel.setChart(chart);
			}
			
		} catch (Exception e) {
			log("Error in plotting profile");
		} 
	}
		
	/**
	 * Update the frankenprofile panel with data from the given datasets
	 * @param list the datasets
	 */	
	public void updateFrankenProfileChart(List<AnalysisDataset> list){
		
		try {
			if(list.size()==1){

				// full segment colouring
				XYDataset ds = DatasetCreator.createFrankenSegmentDataset(list.get(0).getCollection());
				JFreeChart chart = makeProfileChart(ds);
				frankenChartPanel.setChart(chart);
			} else {
				// many profiles, colour them all the same
				List<XYSeriesCollection> ds = DatasetCreator.createMultiProfileIQRFrankenDataset(list);				
				
				XYDataset profileDS = DatasetCreator.createMultiProfileFrankenDataset(list);
				
				JFreeChart chart = 
						ChartFactory.createXYLineChart(null,
						                "Position", "Angle", null, PlotOrientation.VERTICAL, true, true,
						                false);

				XYPlot plot = chart.getXYPlot();
				plot.getDomainAxis().setRange(0,100);
				plot.getRangeAxis().setRange(0,360);
				plot.setBackgroundPaint(Color.WHITE);
				plot.addRangeMarker(new ValueMarker(180, Color.BLACK, new BasicStroke(2.0f)));
				
				int i=0;
				for(XYSeriesCollection c : ds){

					// find the series index
					String name = (String) c.getSeriesKey(0);
					String[] names = name.split("_");
					int index = Integer.parseInt(names[1]);
					
					// add to dataset
					plot.setDataset(i, c);
					
					// make a transparent color based on teh profile segmenter system
					Color pColor = ColourSelecter.getSegmentColor(index);
					Color color = new Color(pColor.getRed(), pColor.getGreen(), pColor.getBlue(), 128);
					
					
					XYDifferenceRenderer xydr = new XYDifferenceRenderer(color, color, false);
					
					// go through each series in the collection, and set the line colour
					for(int series=0;series<c.getSeriesCount();series++){
						xydr.setSeriesPaint(series, color);
						xydr.setSeriesVisibleInLegend(series, false);
						
					}
					plot.setRenderer(i, xydr);
					
					
					i++;
				}

				plot.setDataset(i, profileDS);
				plot.setRenderer(i, new StandardXYItemRenderer());

				for (int j = 0; j < profileDS.getSeriesCount(); j++) {
					plot.getRenderer(i).setSeriesVisibleInLegend(j, Boolean.FALSE);
					plot.getRenderer(i).setSeriesStroke(j, new BasicStroke(2));
					String name = (String) profileDS.getSeriesKey(j);
					String[] names = name.split("_");
					plot.getRenderer(i).setSeriesPaint(j, ColourSelecter.getSegmentColor(Integer.parseInt(names[1])).darker());
				}	
				
				frankenChartPanel.setChart(chart);
			}
						
		} catch (Exception e) {
			log("Error in plotting frankenprofile: "+e.getMessage());
			for(StackTraceElement el : e.getStackTrace()){
				log(el.toString());
			}
		} 
	}
	
	/**
	 * Update the shells panel with data from the given datasets
	 * @param list the datasets
	 */
	public void updateShellPanel(List<AnalysisDataset> list){

		if(list.size()==1){ // single collection is easy
			
			AnalysisDataset dataset = list.get(0);
			NucleusCollection collection = dataset.getCollection();

			if(dataset.hasShellResult()){ // only if there is something to display

				CategoryDataset ds = DatasetCreator.createShellBarChartDataset(list);
				JFreeChart shellsChart = ChartFactory.createBarChart(null, "Shell", "Percent", ds);
				shellsChart.getCategoryPlot().setBackgroundPaint(Color.WHITE);
				shellsChart.getCategoryPlot().getRangeAxis().setRange(0,100);
				StatisticalBarRenderer rend = new StatisticalBarRenderer();
				rend.setBarPainter(new StandardBarPainter());
				rend.setShadowVisible(false);
				rend.setErrorIndicatorPaint(Color.black);
				rend.setErrorIndicatorStroke(new BasicStroke(2));
				shellsChart.getCategoryPlot().setRenderer(rend);

				for (int j = 0; j < ds.getRowCount(); j++) {
					rend.setSeriesVisibleInLegend(j, Boolean.FALSE);
					rend.setSeriesStroke(j, new BasicStroke(2));
					int index = getIndexFromLabel( (String) ds.getRowKey((j)));
					rend.setSeriesPaint(j, ColourSelecter.getSignalColour(index));
				}	

				shellsChartPanel.setChart(shellsChart);
				

				
				signalsTabPane.setComponentAt(2, shellsChartPanel);
			} else { // no shell analysis available

				if(collection.hasSignals()){
					// if signals, offer to run
					shellsPanel = makeNoShellAnalysisAvailablePanel(true, collection, "No shell results available; run new analysis?"); // allow option to run analysis
					signalsTabPane.setComponentAt(2, shellsPanel);
				} else {
					// otherwise don't show button
					shellsPanel = makeNoShellAnalysisAvailablePanel(false, null, "No signals in population"); // container in tab if no shell chart
					signalsTabPane.setComponentAt(2, shellsPanel);
				}
			}
		} else {

			// Multiple populations. Do not display
			// container in tab if no shell chart
			shellsPanel = makeNoShellAnalysisAvailablePanel(false, null, "Cannot display shell results for multiple populations");
			signalsTabPane.setComponentAt(2, shellsPanel);
		}
	}
	
	/**
	 * Create a panel to display when a shell analysis is not available
	 * @param showRunButton should there be an option to run a shell analysis on the dataset
	 * @param collection the nucleus collection from the dataset
	 * @param label the text to display on the panel
	 * @return a panel to put in the shell tab
	 */
	private JPanel makeNoShellAnalysisAvailablePanel(boolean showRunButton, NucleusCollection collection, String label){
		JPanel panel = new JPanel(); // container in tab if no shell chart
		
		panel.setLayout(new BorderLayout(0,0));
		JLabel lbl = new JLabel(label);
		
		if(showRunButton && collection !=null){
			final UUID id = collection.getID();
			JButton btnShellAnalysis = new JButton("Run shell analysis");
			btnShellAnalysis.addMouseListener(new MouseAdapter() {
				@Override
				public void mouseClicked(MouseEvent arg0) {
					newShellAnalysis(MainWindow.this.analysisDatasets.get(id));
				}
			});
			panel.add(btnShellAnalysis, BorderLayout.SOUTH);
		}
		lbl.setHorizontalAlignment(SwingConstants.CENTER);
		panel.add(lbl, BorderLayout.NORTH);
		return panel;
	}
	
	

	/**
	 * Create a consenusus chart for the given nucleus collection
	 * @param collection the NucleusCollection to draw the consensus from
	 * @return the consensus chart
	 */
	public JFreeChart makeConsensusChart(NucleusCollection collection){
		XYDataset ds = DatasetCreator.createNucleusOutline(collection);
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

		NucleusCollection collection = list.get(0).getCollection();
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
					
					final UUID id = collection.getID();
					runRefoldingButton.setVisible(true);


				} else {
					runRefoldingButton.setVisible(false);
//					for(Component c : consensusChartPanel.getComponents() ){
//						if(c.getClass()==JButton.class){
//							c.setVisible(false);
//						}
//					}
					JFreeChart chart = makeConsensusChart(collection);
					consensusChartPanel.setChart(chart);
				} 
			}else {
				// multiple nuclei
				XYDataset ds = DatasetCreator.createMultiNucleusOutline(list);
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
	
	/**
	 * Update all the boxplots for the given collection
	 * @param collection
	 */
	public void updateBoxplots(List<AnalysisDataset> list){
				
		try {
			updateAreaBoxplot(list);
			updatePerimBoxplot(list);
			updateMaxFeretBoxplot(list);
			updateMinFeretBoxplot(list);
			updateDifferenceBoxplot(list);
		} catch (Exception e) {
			log("Error updating boxplots: "+e.getMessage());
		}
	}
	
	/**
	 * Update the boxplot panel for areas with a list of NucleusCollections
	 * @param list
	 */
	public void updateAreaBoxplot(List<AnalysisDataset> list){
		BoxAndWhiskerCategoryDataset ds = DatasetCreator.createAreaBoxplotDataset(list);
		JFreeChart boxplotChart = ChartFactory.createBoxAndWhiskerChart(null, null, null, ds, false); 
		formatBoxplotChart(boxplotChart);
		areaBoxplotChartPanel.setChart(boxplotChart);
	}
	
	/**
	 * Update the boxplot panel for perimeters with a list of NucleusCollections
	 * @param list
	 */
	public void updatePerimBoxplot(List<AnalysisDataset> list){
		BoxAndWhiskerCategoryDataset ds = DatasetCreator.createPerimBoxplotDataset(list);
		JFreeChart boxplotChart = ChartFactory.createBoxAndWhiskerChart(null, null, null, ds, false); 
		formatBoxplotChart(boxplotChart);
		perimBoxplotChartPanel.setChart(boxplotChart);
	}
	
	/**
	 * Update the boxplot panel for longest diameter across CoM with a list of NucleusCollections
	 * @param list
	 */
	public void updateMaxFeretBoxplot(List<AnalysisDataset> list){
		BoxAndWhiskerCategoryDataset ds = DatasetCreator.createMaxFeretBoxplotDataset(list);
		JFreeChart boxplotChart = ChartFactory.createBoxAndWhiskerChart(null, null, null, ds, false); 
		formatBoxplotChart(boxplotChart);
		maxFeretBoxplotChartPanel.setChart(boxplotChart);
	}

	/**
	 * Update the boxplot panel for shortest diameter across CoM with a list of NucleusCollections
	 * @param list
	 */
	public void updateMinFeretBoxplot(List<AnalysisDataset> list){
		BoxAndWhiskerCategoryDataset ds = DatasetCreator.createMinFeretBoxplotDataset(list);
		JFreeChart boxplotChart = ChartFactory.createBoxAndWhiskerChart(null, null, null, ds, false); 
		formatBoxplotChart(boxplotChart);
		minFeretBoxplotChartPanel.setChart(boxplotChart);
	}
	
	/**
	 * Update the boxplot panel for shortest diameter across CoM with a list of NucleusCollections
	 * @param list
	 */
	public void updateDifferenceBoxplot(List<AnalysisDataset> list){
		BoxAndWhiskerCategoryDataset ds = DatasetCreator.createDifferenceBoxplotDataset(list);
		JFreeChart boxplotChart = ChartFactory.createBoxAndWhiskerChart(null, null, null, ds, false); 
		formatBoxplotChart(boxplotChart);
		differenceBoxplotChartPanel.setChart(boxplotChart);
	}
	
	/**
	 * Apply the default formatting to a boxplot
	 * @param boxplot
	 */
	public void formatBoxplotChart(JFreeChart boxplot){
		CategoryPlot plot = boxplot.getCategoryPlot();
		plot.setBackgroundPaint(Color.WHITE);
		BoxAndWhiskerRenderer renderer = new BoxAndWhiskerRenderer();
		plot.setRenderer(renderer);
		renderer.setUseOutlinePaintForWhiskers(true);   
		renderer.setBaseOutlinePaint(Color.BLACK);
		renderer.setBaseFillPaint(Color.LIGHT_GRAY);
		for(int i=0;i<plot.getDataset().getRowCount();i++){
//			Color color = i%2==0 ? Color.LIGHT_GRAY : Color.DARK_GRAY;
			Color color = ColourSelecter.getSegmentColor(i);
			renderer.setSeriesPaint(i, color);
		}
		renderer.setMeanVisible(false);
	}
	

	
	public void updateSegmentsBoxplot(List<AnalysisDataset> list, String segName){
		BoxAndWhiskerCategoryDataset ds = DatasetCreator.createSegmentLengthDataset(list, segName);
		JFreeChart boxplotChart = ChartFactory.createBoxAndWhiskerChart(null, null, null, ds, false); 
		formatBoxplotChart(boxplotChart);
		segmentsBoxplotChartPanel.setChart(boxplotChart);
	}
	
	public void updateSegmentsProfile(List<AnalysisDataset> list, String segName){
		DefaultXYDataset ds = DatasetCreator.createMultiProfileSegmentDataset(list, segName);
		try {
				
				JFreeChart chart = 
						ChartFactory.createXYLineChart(null,
						                "Position", "Angle", ds, PlotOrientation.VERTICAL, true, true,
						                false);

				XYPlot plot = chart.getXYPlot();
				plot.getDomainAxis().setRange(0,100);
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
			
	public void updateVariabilityChart(List<AnalysisDataset> list){
		try {
			XYDataset ds = DatasetCreator.createIQRVariabilityDataset(list);
			if(list.size()==1){
				NucleusCollection n = list.get(0).getCollection();
				JFreeChart chart = makeProfileChart(ds);
				XYPlot plot = chart.getXYPlot();
				plot.setBackgroundPaint(Color.WHITE);
				plot.getDomainAxis().setRange(0,100);
				plot.getRangeAxis().setLabel("IQR");
				plot.getRangeAxis().setAutoRange(true);
				List<Integer> maxima = n.getProfileCollection().findMostVariableRegions(n.getOrientationPoint());
				Profile xpoints = n.getProfileCollection().getProfile(n.getOrientationPoint()).getPositions(100);
				for(Integer i : maxima){
					//				log("Maxima at "+i);
					plot.addDomainMarker(new ValueMarker(xpoints.get(i), Color.BLACK, new BasicStroke(1.0f)));
				}

				variabilityChartPanel.setChart(chart);
			} else { // multiple nuclei
				JFreeChart chart = 
						ChartFactory.createXYLineChart(null,
						                "Position", "IQR", ds, PlotOrientation.VERTICAL, true, true,
						                false);

				XYPlot plot = chart.getXYPlot();
				plot.getDomainAxis().setRange(0,100);
				plot.getRangeAxis().setAutoRange(true);
				plot.setBackgroundPaint(Color.WHITE);

				for (int j = 0; j < ds.getSeriesCount(); j++) {
					plot.getRenderer().setSeriesVisibleInLegend(j, Boolean.FALSE);
					plot.getRenderer().setSeriesStroke(j, new BasicStroke(2));
					int index = getIndexFromLabel( (String) ds.getSeriesKey(j));
					plot.getRenderer().setSeriesPaint(j, ColourSelecter.getSegmentColor(index));
				}	
				variabilityChartPanel.setChart(chart);
			}
		} catch (Exception e) {
			log("Error drawing variability chart: "+e.getMessage());
		}	
	}
	
	public void updateSignalsPanel(List<AnalysisDataset> list){
		updateSignalConsensusChart(list);
		updateSignalStatsPanel(list);
	}
	
	private void updateSignalStatsPanel(List<AnalysisDataset> list){
		try{
			TableModel model = DatasetCreator.createSignalStatsTable(list);
			signalStatsTable.setModel(model);
		} catch (Exception e){
			log("Error updating signal stats: "+e.getMessage());
		}
		int columns = signalStatsTable.getColumnModel().getColumnCount();
		for(int i=1;i<columns;i++){
			signalStatsTable.getColumnModel().getColumn(i).setCellRenderer(new StatsTableCellRenderer());
		}
	}
	
	private void updateClusteringPanel(List<AnalysisDataset> list){
		
		if(list.size()==1){
			AnalysisDataset dataset = list.get(0);
			NucleusCollection collection = dataset.getCollection();
			final UUID id = collection.getID();
			
			clusteringPanel = new JPanel();
			clusteringPanel.setLayout(new BoxLayout(clusteringPanel, BoxLayout.Y_AXIS));
			
			if(!dataset.hasClusters()){ // only allow clustering once per population

				JButton btnNewClusterAnalysis = new JButton("Cluster population");
				btnNewClusterAnalysis.addMouseListener(new MouseAdapter() {
					@Override
					public void mouseClicked(MouseEvent arg0) {
						clusterAnalysis(MainWindow.this.analysisDatasets.get(id).getCollection());
					}
				});
				clusteringPanel.add(btnNewClusterAnalysis);
				
			} else { // clusters present, show the tree if available
				JTextArea label = new JTextArea(dataset.getClusterTree());
				label.setLineWrap(true);
				
//				DefaultMutableTreeNode top = new DefaultMutableTreeNode("root node");
//				DefaultMutableTreeNode category = addChildNodes(id);
//
//				top.add(category);
//				JTree tree = new JTree(top);
//				tree.setRootVisible( false );
//				for (int i = 0; i < tree.getRowCount(); i++) { // ensure we start with all open
//					tree.expandRow(i);
//				}
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
	
	private void updateSignalConsensusChart(List<AnalysisDataset> list){
		try {

			if(list.size()==1){

				NucleusCollection collection = list.get(0).getCollection();

				if(collection.hasConsensusNucleus()){ // if a refold is available
					XYDataset signalCoMs = DatasetCreator.createSignalCoMDataset(collection);
					JFreeChart chart = makeConsensusChart(collection);

					XYPlot plot = chart.getXYPlot();
					plot.setDataset(1, signalCoMs);

					XYLineAndShapeRenderer  rend = new XYLineAndShapeRenderer();
					for(int series=0;series<signalCoMs.getSeriesCount();series++){
						int channel = series+2; // channel is from 2, series from 0
						rend.setSeriesPaint(series, ColourSelecter.getSignalColour(channel, false));
						rend.setBaseLinesVisible(false);
						rend.setBaseShapesVisible(true);
						rend.setBaseSeriesVisibleInLegend(false);
					}
					plot.setRenderer(1, rend);

					for(int channel : collection.getSignalChannels()){
						List<Shape> shapes = DatasetCreator.createSignalRadiusDataset(collection, channel);

						int signalCount = shapes.size();

						int alpha = (int) Math.floor( 255 / ((double) signalCount) );
						alpha = alpha < 5 ? 5 : alpha > 128 ? 128 : alpha;

						//					int alpha 	= signalCount > 255 
						//								? 2 
						//								: signalCount > 128 
						//								? 8
						//								: signalCount > 64
						//								? 16
						//								: signalCount > 32
						//								? 20
						//								: 20;

						for(Shape s : shapes){
							XYShapeAnnotation an = new XYShapeAnnotation( s, null,
									null, ColourSelecter.getSignalColour(channel, true, alpha)); // layer transparent signals
							plot.addAnnotation(an);
						}
					}
					signalsChartPanel.setChart(chart);
				} else { // no consensus to display
					JFreeChart chart = ChartFactory.createXYLineChart(null,  // chart for conseusns
							null, null, null);
					XYPlot plot = chart.getXYPlot();
					plot.setBackgroundPaint(Color.WHITE);
					plot.getDomainAxis().setVisible(false);
					plot.getRangeAxis().setVisible(false);
					signalsChartPanel.setChart(chart);
				}
			} else { // multiple populations. Avoid confusion with blank chart
				JFreeChart chart = ChartFactory.createXYLineChart(null,  // chart for conseusns
						null, null, null);
				XYPlot plot = chart.getXYPlot();
				plot.setBackgroundPaint(Color.WHITE);
				plot.getDomainAxis().setVisible(false);
				plot.getRangeAxis().setVisible(false);
				signalsChartPanel.setChart(chart);
			}
		} catch(Exception e){
			log("Error updating signals: "+e.getMessage());
		}
	}
	
	private void updateSignalHistogramPanel(List<AnalysisDataset> list){
		try {
			updateSignalAngleHistogram(list);
			updateSignalDistanceHistogram(list);
		} catch (Exception e) {
			log("Error updating signal histograms: "+e.getMessage());
		}
	}
	
	private void updateSignalAngleHistogram(List<AnalysisDataset> list){
		try {
			HistogramDataset ds = DatasetCreator.createSignalAngleHistogramDataset(list);
			JFreeChart chart = ChartFactory.createHistogram(null, "Angle", "Count", ds, PlotOrientation.VERTICAL, true, true, true);
			XYPlot plot = chart.getXYPlot();
			plot.setBackgroundPaint(Color.white);
			XYBarRenderer rend = new XYBarRenderer();
			rend.setBarPainter(new StandardXYBarPainter());
			rend.setShadowVisible(false);
			plot.setRenderer(rend);
			plot.getDomainAxis().setRange(0,360);
			for (int j = 0; j < ds.getSeriesCount(); j++) {
				plot.getRenderer().setSeriesVisibleInLegend(j, Boolean.FALSE);
				plot.getRenderer().setSeriesStroke(j, new BasicStroke(2));
				int index = getIndexFromLabel( (String) ds.getSeriesKey(j));
				plot.getRenderer().setSeriesPaint(j, ColourSelecter.getSignalColour(index, true, 128));
			}	
			signalAngleChartPanel.setChart(chart);
		} catch (Exception e) {
			log("Error updating angle histograms: "+e.getMessage());
			JFreeChart chart = ChartFactory.createHistogram(null, "Angle", "Count", null, PlotOrientation.VERTICAL, true, true, true);
			chart.getPlot().setBackgroundPaint(Color.white);
			signalAngleChartPanel.setChart(chart);
		}
		
		
	}

	private void updateSignalDistanceHistogram(List<AnalysisDataset> list){
		try {
			HistogramDataset ds = DatasetCreator.createSignalDistanceHistogramDataset(list);
			JFreeChart chart = ChartFactory.createHistogram(null, "Distance", "Count", ds, PlotOrientation.VERTICAL, true, true, true);
			XYPlot plot = chart.getXYPlot();
			plot.setBackgroundPaint(Color.white);
			XYBarRenderer rend = new XYBarRenderer();
			rend.setBarPainter(new StandardXYBarPainter());
			rend.setShadowVisible(false);
			plot.setRenderer(rend);
			plot.getDomainAxis().setRange(0,1);
			for (int j = 0; j < ds.getSeriesCount(); j++) {
				plot.getRenderer().setSeriesVisibleInLegend(j, Boolean.FALSE);
				plot.getRenderer().setSeriesStroke(j, new BasicStroke(2));
				int index = getIndexFromLabel( (String) ds.getSeriesKey(j));
				plot.getRenderer().setSeriesPaint(j, ColourSelecter.getSignalColour(index, true, 128));
			}	
			signalDistanceChartPanel.setChart(chart);
		} catch (Exception e) {
			log("Error updating distance histograms: "+e.getMessage());
			JFreeChart chart = ChartFactory.createHistogram(null, "Distance", "Count", null, PlotOrientation.VERTICAL, true, true, true);
			chart.getPlot().setBackgroundPaint(Color.white);
			signalDistanceChartPanel.setChart(chart);
		}
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
	 * Allows for cell background to be coloured based on poition in a list
	 *
	 */
	public class StatsTableCellRenderer extends javax.swing.table.DefaultTableCellRenderer {

		private static final long serialVersionUID = 1L;

		public java.awt.Component getTableCellRendererComponent(javax.swing.JTable table, java.lang.Object value, boolean isSelected, boolean hasFocus, int row, int column) {
	        
	      //Cells are by default rendered as a JLabel.
	        JLabel l = (JLabel) super.getTableCellRendererComponent(table, value, isSelected, hasFocus, row, column);
	        int choose = ((row-1)/8)+2;
	        Color colour = (row-1) % 8 == 0 ? ColourSelecter.getSignalColour(  choose   ) : Color.WHITE;
	        
	        l.setBackground(colour);

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
	
	
	/**
	 * Colour a table cell background based on its value to show statistical 
	 * significance. Shows yellow for values below a Bonferroni-corrected cutoff
	 * of 0.05, and green for values below a Bonferroni-corrected cutoff
	 * of 0.01
	 */
	public class WilcoxonTableCellRenderer extends javax.swing.table.DefaultTableCellRenderer {

		private static final long serialVersionUID = 1L;

		public java.awt.Component getTableCellRendererComponent(javax.swing.JTable table, java.lang.Object value, boolean isSelected, boolean hasFocus, int row, int column) {
	        
	      //Cells are by default rendered as a JLabel.
	        JLabel l = (JLabel) super.getTableCellRendererComponent(table, value, isSelected, hasFocus, row, column);

	        String cellContents = l.getText();
	        if(cellContents!=null && !cellContents.equals("")){ // ensure value
//	        	
		        double pvalue = Double.valueOf(cellContents);
		        
		        Color colour = Color.WHITE; // default
		        
		        int numberOfTests = 5; // correct for the different variables measured;
		        double divisor = (double) (   (table.getColumnCount()-2)  * numberOfTests); // for > 2 datasets with numberOFtests tests per dataset
		        
		        double fivePct = MainWindow.FIVE_PERCENT_SIGNIFICANCE_LEVEL / divisor; // Bonferroni correction
		        double onePct = MainWindow.ONE_PERCENT_SIGNIFICANCE_LEVEL /   divisor;
//		        IJ.log("Columns: "+table.getColumnCount());
		        
		        if(pvalue<=fivePct){
		        	colour = Color.YELLOW;
		        }
		        
		        if(pvalue<=onePct){
		        	colour = Color.GREEN;
		        }
		        l.setBackground(colour);

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
						Class<?> testClass = datasets.get(0).getAnalysisOptions().getCollectionClass();
						for(AnalysisDataset d : datasets){

							if(d.getAnalysisOptions().getCollectionClass()!=testClass){
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
						NucleusCollection newCollection = makeNewCollection(datasets.get(0), "Merged");
						for(AnalysisDataset d : datasets){

							for(Nucleus n : d.getCollection().getNuclei()){
								if(!newCollection.getNuclei().contains(n)){
									newCollection.addNucleus(n);
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
//						populationNames.put(newDataset.getName(), newDataset.getUUID());
//						analysisDatasets.put(newDataset.getUUID(), newDataset);
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
				
				// get the ids as a list, so we don't iterate over datasets
				// when we could delete a child of the list in progress
				List<UUID> list = new ArrayList<UUID>(0);
				for(AnalysisDataset d : datasets){
					list.add(d.getUUID());
				}

				for(UUID id : list){
					
//					IJ.log("Removing " +id.toString());
					// check dataset still exists
					if(analysisDatasets.containsKey(id)){

//						IJ.log("   Dataset exists");
						
						AnalysisDataset d = analysisDatasets.get(id);

//						if(d.isRoot()){
								
							// remove all children of the collection
						for(UUID u : d.getAllChildUUIDs()){
							String name = analysisDatasets.get(u).getName();
//							IJ.log("   Removing children");
							if(analysisDatasets.containsKey(u)){
								analysisDatasets.remove(u);
							}
//							IJ.log("   Removed child id");
							
							if(populationNames.containsValue(u)){
								populationNames.remove(name);
							}
//							IJ.log("   Removed child name");
							

							d.deleteChild(u);
//							IJ.log("   Deleted child");
//
						}
						
						for(UUID parentID : analysisDatasets.keySet()){
							AnalysisDataset parent = analysisDatasets.get(parentID);
							if(parent.hasChild(id)){
								parent.deleteChild(id);
							}
						}
//						IJ.log("   Deleted indirect children");
						
						
//						IJ.log("   Clearing maps");
						populationNames.remove(d.getName());
						analysisDatasets.remove(id);

						if(d.isRoot()){
//							IJ.log("   Removing root");
							treeOrderMap.remove(id);
						}
						
							
//						} else { // not root
//							
//							// remove all children of the collection
//							for(UUID u : d.getAllChildUUIDs()){
//	
//								if(populationNames.containsKey(analysisDatasets.get(u).getName())){
//	
//									populationNames.remove(analysisDatasets.get(u).getName());
//									analysisDatasets.remove(u);
//								}
//	
//							}
//							if(populationNames.containsKey(analysisDatasets.get(id).getName())){
//								populationNames.remove(d.getName());
//								analysisDatasets.remove(id);
//							}
//
//						}

					}
//					IJ.log("   Cleared " +id.toString());
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
	        			NucleusCollection collection = dataset.getCollection();

	        			NucleusCollection newCollection = makeNewCollection(dataset, "Subtraction");

	        			for(Nucleus n : collection.getNuclei()){
	        				if(! negative.getCollection().getNuclei().contains(n)){
	        					newCollection.addNucleus(n);
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
//	        			populationNames.put(newCollection.getName(), newID);
//	        			analysisDatasets.put(newID, dataset.getChildDataset(newID));
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
			
//			for(int i=0; i<treeOrderMap.size();i++){
//				IJ.log(i+": "+analysisDatasets.get(treeOrderMap.get(i)).getName());
//			}

			if(datasets.size()==1){

				AnalysisDataset dataToMove = datasets.get(0);

				int oldValue = treeOrderMap.get(dataToMove.getUUID());
				int newValue = oldValue;
				if(oldValue>0){ // do not change if already at the top
					newValue = oldValue-1;
//					IJ.log("Moving "+oldValue+" to "+newValue);

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
				// TODO make new thread
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
	 * Create a new NucleusCollection of the same class as the given dataset
	 * @param template the dataset to base on for analysis options, folders
	 * @param name the collection name
	 * @return a new empty collection
	 */
	public static NucleusCollection makeNewCollection(AnalysisDataset template, String name){

		NucleusCollection newCollection = null;

		try {

			NucleusCollection templateCollection = template.getCollection();

			Constructor<?> collectionConstructor =  template.getAnalysisOptions().getCollectionClass().getConstructor(new Class<?>[]{File.class, String.class, String.class, File.class});

			newCollection = (NucleusCollection) collectionConstructor.newInstance(templateCollection.getFolder(), 
					templateCollection.getOutputFolderName(), 
					name, 
					templateCollection.getDebugFile()
					);

		} catch (NoSuchMethodException e) {
			IJ.log(e.getMessage());
			for(StackTraceElement el : e.getStackTrace()){
				IJ.log(el.toString());
			}
		} catch (SecurityException e) {
			IJ.log(e.getMessage());
			for(StackTraceElement el : e.getStackTrace()){
				IJ.log(el.toString());
			}
		} catch (InstantiationException e) {
			IJ.log(e.getMessage());
			for(StackTraceElement el : e.getStackTrace()){
				IJ.log(el.toString());
			}
		} catch (IllegalAccessException e) {
			IJ.log(e.getMessage());
			for(StackTraceElement el : e.getStackTrace()){
				IJ.log(el.toString());
			}
		} catch (IllegalArgumentException e) {
			IJ.log(e.getMessage());
			for(StackTraceElement el : e.getStackTrace()){
				IJ.log(el.toString());
			}
		} catch (InvocationTargetException e) {
			IJ.log(e.getMessage());
			for(StackTraceElement el : e.getStackTrace()){
				IJ.log(el.toString());
			}
		}
		return newCollection;
	}
	
}
