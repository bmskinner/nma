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
import javax.swing.filechooser.FileFilter;
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
import no.analysis.ShellAnalysis;
import no.analysis.SignalDetector;
import no.collections.CellCollection;
import no.components.AnalysisOptions.NuclearSignalOptions;
import no.components.Profile;
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
import java.awt.Container;
import java.awt.Dimension;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.GridLayout;
import java.awt.Shape;
import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;

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

import cell.Cell;
import cell.analysis.TubulinTailDetector;
import datasets.CellDatasetCreator;
import datasets.NucleusDatasetCreator;
import datasets.TailDatasetCreator;
import utility.Constants;
import utility.TreeOrderHashMap;

import javax.swing.JTabbedPane;

public class MainWindow extends JFrame implements ActionListener {
			
	private static final int PROFILE_TAB = 0;
	private static final int STATS_TAB = 1;
	private static final int ANALYSIS_TAB = 2;
	private static final int BOXPLOTS_TAB = 3;
	private static final int VARIABILITY_TAB = 4;
	private static final int SIGNALS_TAB = 5;
	private static final int CLUSTERS_TAB = 6;
	private static final int VENN_TAB = 7;
	
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
	private JTabbedPane signalsTabPane;
	
	private NucleusProfilesPanel nucleusProfilesPanel;
		
//	private ChartPanel profileChartPanel;
//	private ChartPanel frankenChartPanel;
	private ChartPanel consensusChartPanel;
//	private ChartPanel rawChartPanel;
//	
//	private JRadioButton rawProfileLeftButton  = new JRadioButton("Left"); // left align raw profiles in rawChartPanel
//	private JRadioButton rawProfileRightButton = new JRadioButton("Right"); // right align raw profiles in rawChartPanel
	
	private JButton runRefoldingButton;
	
	private NuclearBoxplotsPanel nuclearBoxplotsPanel;
	
	private ChartPanel variabilityChartPanel; 
	
	private ChartPanel shellsChartPanel; 
	private JPanel shellsPanel;
	
	private ChartPanel signalsChartPanel; // consensus nucleus plus signals
	private JPanel signalsPanel;// signals container for chart and stats table
	private JTable signalStatsTable;
	private JPanel signalAnalysisSetupPanel;
	private JTable signalAnalysisSetupTable;
	private JPanel signalSelectionVisiblePanel;
	private JPanel signalConsensusAndCheckboxPanel;
	
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
	
	// 
	private JCheckBox    normSegmentCheckBox = new JCheckBox("Normalised");	// to toggle raw or normalised segment profiles in segmentsProfileChartPanel
	private JRadioButton rawSegmentLeftButton  = new JRadioButton("Left"); // left align raw segment profiles in segmentsProfileChartPanel
	private JRadioButton rawSegmentRightButton = new JRadioButton("Right"); // right align raw segment profiles in segmentsProfileChartPanel
	
	
	
	private JPanel segmentsBoxplotPanel;// container for boxplots chart and decoration
	private JComboBox segmentSelectionBox; // choose which segments to compare
	
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
			// Create the log panel
			//---------------
			logPanel = createLogPanel();
			contentPane.add(logPanel, BorderLayout.WEST);

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
			
			
			GridBagConstraints c = new GridBagConstraints();			
			c.gridwidth = GridBagConstraints.RELATIVE;     //next to last
			c.fill = GridBagConstraints.BOTH;
			c.weightx = 1.0;
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
			// Create variabillity chart
			//---------------

			JFreeChart variablityChart = ChartFactory.createXYLineChart(null,
					"Position", "IQR", null);
			XYPlot variabilityPlot = variablityChart.getXYPlot();
			variabilityPlot.setBackgroundPaint(Color.WHITE);
			variabilityPlot.getDomainAxis().setRange(0,100);
			
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
			vennTable.setModel(NucleusDatasetCreator.createVennTable(null));
			
			
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
		progressPanel.add(new JLabel("Analyses in progress:"));
		panel.add(progressPanel, BorderLayout.SOUTH);
		
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
	
	
	/**
	 * Create the signals tab panel. Contains the overview, shells, histograms
	 * and analysis setup sub-tabs
	 * @return
	 */
	private JTabbedPane createSignalsTabPanel(){
		JTabbedPane signalsTabPane = new JTabbedPane(JTabbedPane.TOP);
				
		signalsPanel = new JPanel(); // main container in tab
		signalsPanel.setLayout(new BoxLayout(signalsPanel, BoxLayout.X_AXIS));

		//---------------
		// Stats panel
		//---------------
		DefaultTableModel signalsTableModel = new DefaultTableModel();
		signalsTableModel.addColumn("");
		signalsTableModel.addColumn("");
		signalStatsTable = new JTable(); // table  for basic stats
		signalStatsTable.setModel(signalsTableModel);
		signalStatsTable.setEnabled(false);
		
		JScrollPane signalStatsScrollPane = new JScrollPane(signalStatsTable);
		signalsPanel.add(signalStatsScrollPane);
		
		//---------------
		// Consensus chart
		//---------------
		
		// make a blank chart for signal locations on a consensus nucleus
		JFreeChart signalsChart = ChartFactory.createXYLineChart(null,  // chart for conseusns
				null, null, null);
		XYPlot signalsPlot = signalsChart.getXYPlot();
		
		signalsPlot.setBackgroundPaint(Color.WHITE);
		signalsPlot.getDomainAxis().setVisible(false);
		signalsPlot.getRangeAxis().setVisible(false);

		// the chart is inside a chartPanel; the chartPanel is inside a JPanel
		// this allows a checkbox panel to be added to the JPanel later
		signalsChartPanel = new ChartPanel(signalsChart);
		signalConsensusAndCheckboxPanel = new JPanel(new BorderLayout());
		signalConsensusAndCheckboxPanel.add(signalsChartPanel, BorderLayout.CENTER);
		
//		signalSelectionVisiblePanel = new JPanel(); // will hold checkboxes for signals visible in plots
//		signalSelectionVisiblePanel.setLayout(new BoxLayout(signalSelectionVisiblePanel, BoxLayout.X_AXIS));
//		signalSelectionVisiblePanel.add(new JCheckBox(""));
//		signalConsensusAndCheckboxPanel.add(signalSelectionVisiblePanel, BorderLayout.NORTH);
		
		signalsPanel.add(signalConsensusAndCheckboxPanel);
		
		signalsTabPane.addTab("Overview", null, signalsPanel, null);
		//---------------
		// Distance and angle histograms charts
		//---------------
		
		JFreeChart signalAngleChart = ChartFactory.createHistogram(null, "Angle", "Count", null, PlotOrientation.VERTICAL, true, true, true);
		signalAngleChart.getPlot().setBackgroundPaint(Color.white);
		
		JFreeChart signalDistanceChart = ChartFactory.createHistogram(null, "Distance", "Count", null, PlotOrientation.VERTICAL, true, true, true);
		signalDistanceChart.getPlot().setBackgroundPaint(Color.white);
		
		signalHistogramPanel = new JPanel(); // main container in tab
		signalHistogramPanel.setLayout(new BoxLayout(signalHistogramPanel, BoxLayout.Y_AXIS));
		signalAngleChartPanel = new ChartPanel(signalAngleChart);
		signalDistanceChartPanel = new ChartPanel(signalDistanceChart);

		signalHistogramPanel.add(signalAngleChartPanel);
		signalHistogramPanel.add(signalDistanceChartPanel);
		
		signalsTabPane.addTab("Signal histograms", null, signalHistogramPanel, null);
		
		//---------------
		// Create the shells panel
		//---------------
		JFreeChart shellsChart = ChartFactory.createBarChart(null, "Shell", "Percent", null);
		shellsChart.getCategoryPlot().setBackgroundPaint(Color.WHITE);
		shellsChart.getCategoryPlot().getRangeAxis().setRange(0,100);
		shellsChartPanel = new ChartPanel(shellsChart);

		signalsTabPane.addTab("Shells", null, shellsChartPanel, null);
		
		
		//---------------
		// Create the signal analysis settings panel
		//---------------
		signalAnalysisSetupPanel = new JPanel(new BorderLayout());
				
		signalAnalysisSetupTable  = new JTable(new DefaultTableModel());
		signalAnalysisSetupTable.setEnabled(false);
		JScrollPane signalAnalysisSetupScrollPane = new JScrollPane(signalAnalysisSetupTable);
		signalAnalysisSetupPanel.add(signalAnalysisSetupScrollPane, BorderLayout.CENTER);
		signalsTabPane.addTab("Detection settings", null, signalAnalysisSetupPanel, null);
		
		
		//---------------
		// Return the panel
		//---------------
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
		
		segmentSelectionBox = new JComboBox();
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
		
//		if(e.getActionCommand().equals("LeftAlignRawProfile")){
//			updateRawProfileImage(getSelectedRowsFromTreeTable(), false);
//		}
//		
//		if(e.getActionCommand().equals("RightAlignRawProfile")){
//			updateRawProfileImage(getSelectedRowsFromTreeTable(), true);
//		}
		
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
		
		if(e.getActionCommand().startsWith("GroupVisble_")){
			
			int signalGroup = this.getIndexFromLabel(e.getActionCommand());
			JCheckBox box = (JCheckBox) e.getSource();
			AnalysisDataset d = list.get(0);
			d.setSignalGroupVisible(signalGroup, box.isSelected());
			updateSignalConsensusChart(list);
			updateSignalHistogramPanel(list);
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
		
		wilcoxonAreaTable = new JTable(NucleusDatasetCreator.createWilcoxonAreaTable(null));
		addWilconxonTable(wilcoxonPartsPanel, wilcoxonAreaTable, "Areas");
//		panel.add(wilcoxonAreaTable.getTableHeader(), BorderLayout.NORTH);
		scrollPane.setColumnHeaderView(wilcoxonAreaTable.getTableHeader());

		
		wilcoxonPerimTable = new JTable(NucleusDatasetCreator.createWilcoxonPerimeterTable(null));
		addWilconxonTable(wilcoxonPartsPanel, wilcoxonPerimTable, "Perimeters");
		
		wilcoxonMinFeretTable = new JTable(NucleusDatasetCreator.createWilcoxonMinFeretTable(null));
		addWilconxonTable(wilcoxonPartsPanel, wilcoxonMinFeretTable, "Min feret");

		
		wilcoxonFeretTable = new JTable(NucleusDatasetCreator.createWilcoxonMaxFeretTable(null));
		addWilconxonTable(wilcoxonPartsPanel, wilcoxonFeretTable, "Feret");
		
		
		wilcoxonDifferenceTable = new JTable(NucleusDatasetCreator.createWilcoxonVariabilityTable(null));
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
					
//					FileFilter filter = new FileFilter(); TODO: make select ndb only
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
					
					nucleusProfilesPanel.update(list);
					updateConsensusImage(list);
					
					nuclearBoxplotsPanel.update(list);
					
					updateVariabilityChart(list);
					updateShellPanel(list);
					updateSignalsPanel(list);
					updateSignalHistogramPanel(list);
					updateSignalAnalysisSetupPanel(list);
					updateClusteringPanel(list);
					updateVennPanel(list);
					updateWilcoxonPanel(list);
					
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
	
	/**
	 * Update the wilcoxon panel with data from the given datasets
	 * @param list the datasets
	 */
	public void updateWilcoxonPanel(List<AnalysisDataset> list){
		// format the numbers and make into a tablemodel

		wilcoxonAreaTable.setModel(NucleusDatasetCreator.createWilcoxonAreaTable(list));
		
		int columns = wilcoxonAreaTable.getColumnModel().getColumnCount();
		for(int i=1;i<columns;i++){
			wilcoxonAreaTable.getColumnModel().getColumn(i).setCellRenderer(new WilcoxonTableCellRenderer());
		}
		
		wilcoxonPerimTable.setModel(NucleusDatasetCreator.createWilcoxonPerimeterTable(list));
		columns = wilcoxonPerimTable.getColumnModel().getColumnCount();
		for(int i=1;i<columns;i++){
			wilcoxonPerimTable.getColumnModel().getColumn(i).setCellRenderer(new WilcoxonTableCellRenderer());
		}
		
		wilcoxonMinFeretTable.setModel(NucleusDatasetCreator.createWilcoxonMinFeretTable(list));
		columns = wilcoxonMinFeretTable.getColumnModel().getColumnCount();
		for(int i=1;i<columns;i++){
			wilcoxonMinFeretTable.getColumnModel().getColumn(i).setCellRenderer(new WilcoxonTableCellRenderer());
		}
		
		wilcoxonFeretTable.setModel(NucleusDatasetCreator.createWilcoxonMaxFeretTable(list));
		columns = wilcoxonFeretTable.getColumnModel().getColumnCount();
		for(int i=1;i<columns;i++){
			wilcoxonFeretTable.getColumnModel().getColumn(i).setCellRenderer(new WilcoxonTableCellRenderer());
		}
		
		wilcoxonDifferenceTable.setModel(NucleusDatasetCreator.createWilcoxonVariabilityTable(list));
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
	
	
	/**
	 * Update the shells panel with data from the given datasets
	 * @param list the datasets
	 */
	public void updateShellPanel(List<AnalysisDataset> list){

		if(list.size()==1){ // single collection is easy
			
			AnalysisDataset dataset = list.get(0);
			CellCollection collection = dataset.getCollection();

			if(dataset.hasShellResult()){ // only if there is something to display

				CategoryDataset ds = NucleusDatasetCreator.createShellBarChartDataset(list);
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
	private JPanel makeNoShellAnalysisAvailablePanel(boolean showRunButton, CellCollection collection, String label){
		JPanel panel = new JPanel(); // container in tab if no shell chart
		
		panel.setLayout(new BorderLayout(0,0));
		JLabel lbl = new JLabel(label);
		
		if(showRunButton && collection !=null){
//			final UUID id = collection.getID();
			JButton btnShellAnalysis = new JButton("Run shell analysis");
//			btnShellAnalysis.addActionListener(new ShellAnalysisAction());
			btnShellAnalysis.addMouseListener(new MouseAdapter() {
				@Override
				public void mouseClicked(MouseEvent arg0) {
					new ShellAnalysisAction();
//					newShellAnalysis(MainWindow.this.analysisDatasets.get(id));
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
			
	public void updateVariabilityChart(List<AnalysisDataset> list){
		try {
			XYDataset ds = NucleusDatasetCreator.createIQRVariabilityDataset(list);
			if(list.size()==1){
				CellCollection n = list.get(0).getCollection();
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
		contentPane.revalidate();
		contentPane.repaint();	
	}
	
	/**
	 * Update the signal stats with the given datasets
	 * @param list the datasets
	 */
	private void updateSignalStatsPanel(List<AnalysisDataset> list){
		try{
			TableModel model = NucleusDatasetCreator.createSignalStatsTable(list);
			signalStatsTable.setModel(model);
		} catch (Exception e){
			log("Error updating signal stats: "+e.getMessage());
		}
		int columns = signalStatsTable.getColumnModel().getColumnCount();
		for(int i=1;i<columns;i++){
			signalStatsTable.getColumnModel().getColumn(i).setCellRenderer(new StatsTableCellRenderer());
		}
	}
	
	/**
	 * Update the signal analysis detection settings with the given datasets
	 * @param list the datasets
	 */
	private void updateSignalAnalysisSetupPanel(List<AnalysisDataset> list){
		try{
			TableModel model = NucleusDatasetCreator.createSignalDetectionParametersTable(list);
			this.signalAnalysisSetupTable.setModel(model);
		} catch (Exception e){
			log("Error updating signal analysis: "+e.getMessage());
			for(StackTraceElement e1 : e.getStackTrace()){
				log(e1.toString());
			}
		}
	}
	
	private void updateClusteringPanel(List<AnalysisDataset> list){
		
		if(list.size()==1){
			AnalysisDataset dataset = list.get(0);
			CellCollection collection = dataset.getCollection();
			final UUID id = collection.getID();
			
			clusteringPanel = new JPanel();
			clusteringPanel.setLayout(new BoxLayout(clusteringPanel, BoxLayout.Y_AXIS));
			
			if(!dataset.hasClusters()){ // only allow clustering once per population

				JButton btnNewClusterAnalysis = new JButton("Cluster population");
//				btnNewClusterAnalysis.addActionListener(new ClusterAnalysisAction());
				btnNewClusterAnalysis.addMouseListener(new MouseAdapter() {
					@Override
					public void mouseClicked(MouseEvent arg0) {
						new ClusterAnalysisAction();
//						clusterAnalysis(MainWindow.this.analysisDatasets.get(id).getCollection());
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
	
	/**
	 * Create the checkboxes that set each signal channel visible or not
	 */
	private JPanel createSignalsVisiblePanel(AnalysisDataset d){
		JPanel panel = new JPanel();
		panel.setLayout(new BoxLayout(panel, BoxLayout.X_AXIS));
		try {

			for(int signalGroup : d.getCollection().getSignalGroups()){

				boolean visible = d.isSignalGroupVisible(signalGroup);
				
				String name = d.getCollection().getSignalGroupName(signalGroup);
				// make a checkbox for each signal group in the dataset
				JCheckBox box = new JCheckBox(name);
				
				// get the status within each dataset
				box.setSelected(visible);
				
				// apply the appropriate action 
				box.setActionCommand("GroupVisble_"+signalGroup);
				box.addActionListener(this);
				panel.add(box);

			}

		} catch(Exception e){
			log("Error creating signal checkboxes: "+e.getMessage());
			for(StackTraceElement e1 : e.getStackTrace()){
				log(e1.toString());
			}
		}
		return panel;
	}
		
	private void updateSignalConsensusChart(List<AnalysisDataset> list){
		try {

			if(list.size()==1){
				
				AnalysisDataset dataset = list.get(0);
				
				signalSelectionVisiblePanel = createSignalsVisiblePanel(dataset);
				signalConsensusAndCheckboxPanel.add(signalSelectionVisiblePanel, BorderLayout.NORTH);
				signalConsensusAndCheckboxPanel.setVisible(true);

				CellCollection collection = list.get(0).getCollection();

				if(collection.hasConsensusNucleus()){ // if a refold is available
					
					XYDataset signalCoMs = NucleusDatasetCreator.createSignalCoMDataset(dataset);
					JFreeChart chart = makeConsensusChart(collection);

					XYPlot plot = chart.getXYPlot();
					plot.setDataset(1, signalCoMs);

					XYLineAndShapeRenderer  rend = new XYLineAndShapeRenderer();
					for(int series=0;series<signalCoMs.getSeriesCount();series++){
						String name = (String) signalCoMs.getSeriesKey(series);
						int seriesGroup = getIndexFromLabel(name);
						rend.setSeriesPaint(series, ColourSelecter.getSignalColour(seriesGroup-1, false));
						rend.setBaseLinesVisible(false);
						rend.setBaseShapesVisible(true);
						rend.setBaseSeriesVisibleInLegend(false);
					}
					plot.setRenderer(1, rend);

					for(int signalGroup : collection.getSignalGroups()){
						List<Shape> shapes = NucleusDatasetCreator.createSignalRadiusDataset(dataset, signalGroup);

						int signalCount = shapes.size();

						int alpha = (int) Math.floor( 255 / ((double) signalCount) );
						alpha = alpha < 5 ? 5 : alpha > 128 ? 128 : alpha;

						for(Shape s : shapes){
							XYShapeAnnotation an = new XYShapeAnnotation( s, null,
									null, ColourSelecter.getSignalColour(signalGroup-1, true, alpha)); // layer transparent signals
							plot.addAnnotation(an);
						}
					}
					signalsChartPanel.setChart(chart);
				} else { // no consensus to display
										
					signalConsensusAndCheckboxPanel.setVisible(false);
				}
			} else { // multiple populations
				
				signalConsensusAndCheckboxPanel.setVisible(false);
			}
		} catch(Exception e){
			log("Error updating signals: "+e.getMessage());
			for(StackTraceElement e1 : e.getStackTrace()){
				log(e1.toString());
			}
		}
	}
	
	private void updateSignalHistogramPanel(List<AnalysisDataset> list){
		try {
			updateSignalAngleHistogram(list);
			updateSignalDistanceHistogram(list);
		} catch (Exception e) {
			log("Error updating signal histograms: "+e.getMessage());
			for(StackTraceElement e1 : e.getStackTrace()){
				log(e1.toString());
			}
		}
	}
	
	private void updateSignalAngleHistogram(List<AnalysisDataset> list){
		try {
			HistogramDataset ds = NucleusDatasetCreator.createSignalAngleHistogramDataset(list);
			if(ds.getSeriesCount()>0){
				JFreeChart chart = ChartFactory.createHistogram(null, "Angle", "Count", ds, PlotOrientation.VERTICAL, true, true, true);
				XYPlot plot = chart.getXYPlot();
				plot.setBackgroundPaint(Color.white);
				XYBarRenderer rend = new XYBarRenderer();
				rend.setBarPainter(new StandardXYBarPainter());
				rend.setShadowVisible(false);
				plot.setRenderer(rend);
				plot.getDomainAxis().setRange(0,360);
				for (int j = 0; j < ds.getSeriesCount(); j++) {
					String name = (String) ds.getSeriesKey(j);
					int seriesGroup = getIndexFromLabel(name);
					plot.getRenderer().setSeriesVisibleInLegend(j, Boolean.FALSE);
					plot.getRenderer().setSeriesStroke(j, new BasicStroke(2));

					plot.getRenderer().setSeriesPaint(j, ColourSelecter.getSignalColour(seriesGroup-1, true, 128));
				}	
				signalAngleChartPanel.setChart(chart);
			} else {
				JFreeChart chart = ChartFactory.createHistogram(null, "Angle", "Count", null, PlotOrientation.VERTICAL, true, true, true);
				chart.getPlot().setBackgroundPaint(Color.white);
				signalAngleChartPanel.setChart(chart);
			}
		} catch (Exception e) {
			log("Error updating angle histograms: "+e.getMessage());
			for(StackTraceElement e1 : e.getStackTrace()){
				log(e1.toString());
			}
			JFreeChart chart = ChartFactory.createHistogram(null, "Angle", "Count", null, PlotOrientation.VERTICAL, true, true, true);
			chart.getPlot().setBackgroundPaint(Color.white);
			signalAngleChartPanel.setChart(chart);
		}
		
		
	}

	private void updateSignalDistanceHistogram(List<AnalysisDataset> list){
		try {
			HistogramDataset ds = NucleusDatasetCreator.createSignalDistanceHistogramDataset(list);
			
			if(ds.getSeriesCount()>0){
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
					plot.getRenderer().setSeriesPaint(j, ColourSelecter.getSignalColour(index-1, true, 128));
				}	
				signalDistanceChartPanel.setChart(chart);
			} else {
				JFreeChart chart = ChartFactory.createHistogram(null, "Distance", "Count", null, PlotOrientation.VERTICAL, true, true, true);
				chart.getPlot().setBackgroundPaint(Color.white);
				signalDistanceChartPanel.setChart(chart);
			}
		} catch (Exception e) {
			log("Error updating distance histograms: "+e.getMessage());
			for(StackTraceElement e1 : e.getStackTrace()){
				log(e1.toString());
			}
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
	 * Allows for cell background to be coloured based on poition in a list. Used to colour
	 * the signal stats list
	 *
	 */
	public class StatsTableCellRenderer extends javax.swing.table.DefaultTableCellRenderer {

		private static final long serialVersionUID = 1L;

		public java.awt.Component getTableCellRendererComponent(javax.swing.JTable table, java.lang.Object value, boolean isSelected, boolean hasFocus, int row, int column) {
	        
	      //Cells are by default rendered as a JLabel.
	        JLabel l = (JLabel) super.getTableCellRendererComponent(table, value, isSelected, hasFocus, row, column);
	        	        
	        int numberOfRowsPerSignalGroup = 11;
	        
	        // calculate the colour to be adding
	        int indexToColourise = ((row-1)/numberOfRowsPerSignalGroup);
	        
	        // make the cells white unless they are a multiple of the number of groups (ie a header)
	        Color colour = (row-1) % numberOfRowsPerSignalGroup == 0 
	        			 ? ColourSelecter.getSignalColour(  indexToColourise   ) 
	        			 : Color.WHITE;
	        
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
		        
		        double fivePct = Constants.FIVE_PERCENT_SIGNIFICANCE_LEVEL / divisor; // Bonferroni correction
		        double onePct = Constants.ONE_PERCENT_SIGNIFICANCE_LEVEL /   divisor;
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
	 * @author bms41
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
