package no.gui;

import ij.IJ;
import ij.io.OpenDialog;

import java.awt.BorderLayout;

import javax.swing.JFrame;
import javax.swing.JPanel;
import javax.swing.border.EmptyBorder;
import javax.swing.event.ListSelectionEvent;
import javax.swing.event.ListSelectionListener;
import javax.swing.table.DefaultTableModel;
import javax.swing.table.TableModel;
import javax.swing.text.DefaultCaret;
import javax.swing.JLabel;
import javax.swing.JButton;

import no.analysis.AnalysisCreator;
import no.analysis.CurveRefolder;
import no.analysis.MorphologyAnalysis;
import no.analysis.NucleusClusterer;
import no.analysis.ProfileSegmenter;
import no.analysis.ShellAnalysis;
import no.collections.NucleusCollection;
import no.components.Profile;
import no.imports.PopulationImporter;
import no.nuclei.Nucleus;
import no.utility.MappingFileParser;

import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;

import javax.swing.JTextArea;

import java.awt.SystemColor;
import java.io.File;
import java.lang.reflect.Constructor;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.UUID;

import javax.swing.JOptionPane;
import javax.swing.JScrollPane;
import javax.swing.BoxLayout;
import javax.swing.SwingConstants;

import java.awt.Font;

import javax.swing.JTable;

import java.awt.BasicStroke;
import java.awt.Color;
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
import org.jfree.data.xy.XYDataset;
import org.jfree.data.xy.XYSeriesCollection;

import javax.swing.JTabbedPane;

public class MainWindow extends JFrame {

	/**
	 * 
	 */
	private static final long serialVersionUID = 1L;
	private JPanel contentPane;
	private JTextArea textArea = new JTextArea();;
	private JLabel lblStatusLine = new JLabel("No analysis open");
	private final JPanel panelAggregates = new JPanel(); // holds consensus, tab and population panels
	private JTable tablePopulationStats;
	private final JPanel panelGeneralData = new JPanel(); // holds the tabs
	
	private final JPanel panelPopulations = new JPanel(); // holds list of active populations
	private JTable populationTable;
	
	private JTabbedPane tabbedPane;
		
	private ChartPanel profileChartPanel;
	private ChartPanel frankenChartPanel;
	private ChartPanel consensusChartPanel;
	
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

	private HashMap<UUID, NucleusCollection> analysisPopulations = new HashMap<UUID, NucleusCollection>();
	private HashMap<String, UUID> populationNames = new HashMap<String, UUID>();


	/**
	 * Create the frame.
	 */
	public MainWindow() {
		try {
			setTitle("Nuclear Morphology Analysis");
			setDefaultCloseOperation(JFrame.DISPOSE_ON_CLOSE);
			setBounds(100, 100, 1012, 604);
			contentPane = new JPanel();
			contentPane.setBorder(new EmptyBorder(5, 5, 5, 5));
			contentPane.setLayout(new BorderLayout(0, 0));
			setContentPane(contentPane);
			
			JScrollPane scrollPane = new JScrollPane();
			contentPane.add(scrollPane, BorderLayout.WEST);
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
			
			//---------------
			// Create the header buttons
			//---------------
			
			JPanel panelHeader = new JPanel();
			contentPane.add(panelHeader, BorderLayout.NORTH);
//		
			JButton btnNewAnalysis = new JButton("New analysis");
			btnNewAnalysis.addMouseListener(new MouseAdapter() {
				@Override
				public void mouseClicked(MouseEvent arg0) {
					newAnalysis();
				}
			});
			panelHeader.add(btnNewAnalysis);
			
			JButton btnLoadSavedNuclei = new JButton("Load saved nuclei");
			btnLoadSavedNuclei.addMouseListener(new MouseAdapter() {
				@Override
				public void mouseClicked(MouseEvent arg0) {
					loadNuclei();
				}
			});
			panelHeader.add(btnLoadSavedNuclei);
			
			JButton btnPostanalysisMapping = new JButton("Post-analysis mapping");
			btnPostanalysisMapping.addMouseListener(new MouseAdapter() {
				@Override
				public void mouseClicked(MouseEvent arg0) {
					postAnalysis();
				}
			});
			panelHeader.add(btnPostanalysisMapping);
			
			JPanel panelFooter = new JPanel();
			contentPane.add(panelFooter, BorderLayout.SOUTH);
			
			panelFooter.add(lblStatusLine);
			contentPane.add(panelGeneralData, BorderLayout.CENTER);
			panelGeneralData.setLayout(new BoxLayout(panelGeneralData, BoxLayout.Y_AXIS));
			
			JPanel panel = new JPanel();
			panelGeneralData.add(panel);
			panel.setLayout(new GridLayout(0, 2, 0, 0));
			
			//---------------
			// Create the populations list
			//---------------
			
			panel.add(panelPopulations);
			panelPopulations.setLayout(new BoxLayout(panelPopulations, BoxLayout.Y_AXIS));
			
			// table approach
			DefaultTableModel populationTableModel = new DefaultTableModel();
			populationTableModel.addColumn("Population");
			populationTableModel.addColumn("Nuclei");
			populationTableModel.addColumn("");
			populationTable = new JTable() {
				@Override
				public boolean isCellEditable(int rowIndex, int vColIndex) {
					return false;
				}
				
				
			};
			populationTable.setModel(populationTableModel);
			populationTable.setEnabled(true);
			populationTable.setCellSelectionEnabled(false);
			populationTable.setColumnSelectionAllowed(false);
			populationTable.setRowSelectionAllowed(true);
			populationTable.getColumnModel().getColumn(2).setCellRenderer(new PopulationTableCellRenderer());
			populationTable.getColumnModel().getColumn(0).setPreferredWidth(120);
			populationTable.getColumnModel().getColumn(2).setPreferredWidth(5);
						
			populationTable.addMouseListener(new MouseAdapter() {
				@Override
				public void mouseClicked(MouseEvent e) {
					JTable table = (JTable) e.getSource();
			        if (e.getClickCount() == 2) {
			          int index = table.rowAtPoint((e.getPoint()));
			          if (index >= 0) {
			        	  Object o = table.getModel().getValueAt(index, 0);
			        	  UUID id = MainWindow.this.populationNames.get(o.toString());
			        	  renameCollection(MainWindow.this.analysisPopulations.get(id));
			          }
			        }
				}
			});
			
			ListSelectionModel tableSelectionModel = populationTable.getSelectionModel();
			tableSelectionModel.addListSelectionListener(new ListSelectionHandler());
			
			JScrollPane populationScrollPane = new JScrollPane(populationTable);
			panelPopulations.add(populationScrollPane);
			
			panelGeneralData.add(panelAggregates);
			
			panelAggregates.setLayout(new BoxLayout(panelAggregates, BoxLayout.Y_AXIS));
			
			tabbedPane = new JTabbedPane(JTabbedPane.TOP);
			panelAggregates.add(tabbedPane);
			
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
			tabbedPane.addTab("Profile", null, profileChartPanel, null);
			
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
			tabbedPane.addTab("FrankenProfile", null, frankenChartPanel, null);
			
			//---------------
			// Create the consensus chart
			//---------------
			JFreeChart consensusChart = ChartFactory.createXYLineChart(null,
					null, null, null);
			XYPlot consensusPlot = consensusChart.getXYPlot();
			consensusPlot.setBackgroundPaint(Color.WHITE);
			consensusPlot.getDomainAxis().setVisible(false);
			consensusPlot.getRangeAxis().setVisible(false);
			
			consensusChartPanel = new ChartPanel(consensusChart);
			panel.add(consensusChartPanel);
			
			//---------------
			// Create the general stats page
			//---------------
			
			JPanel panelGeneralStats = new JPanel();
			tabbedPane.addTab("Basic statistics", null, panelGeneralStats, null);
			panelGeneralStats.setLayout(new BorderLayout(0, 0));
			
			tablePopulationStats = new JTable();
			panelGeneralStats.add(tablePopulationStats, BorderLayout.CENTER);
			tablePopulationStats.setEnabled(false);
			panelGeneralStats.add(tablePopulationStats.getTableHeader(), BorderLayout.NORTH);
			tablePopulationStats.setModel(DatasetCreator.createStatsTable(null));
						
			//---------------
			// Create panel for split boxplots
			//---------------
			JPanel boxplotSplitPanel = new JPanel(); // main container in tab
			
			boxplotSplitPanel.setLayout(new BoxLayout(boxplotSplitPanel, BoxLayout.X_AXIS));
			JFreeChart areaBoxplot = ChartFactory.createBoxAndWhiskerChart(null, null, null, new DefaultBoxAndWhiskerCategoryDataset(), false);	        
			areaBoxplotChartPanel = new ChartPanel(areaBoxplot);
			boxplotSplitPanel.add(areaBoxplotChartPanel);
			
			JFreeChart perimBoxplot = ChartFactory.createBoxAndWhiskerChart(null, null, null, new DefaultBoxAndWhiskerCategoryDataset(), false);	        
			perimBoxplotChartPanel = new ChartPanel(perimBoxplot);
			boxplotSplitPanel.add(perimBoxplotChartPanel);
			
			JFreeChart maxFeretBoxplot = ChartFactory.createBoxAndWhiskerChart(null, null, null, new DefaultBoxAndWhiskerCategoryDataset(), false);	        
			maxFeretBoxplotChartPanel = new ChartPanel(maxFeretBoxplot);
			boxplotSplitPanel.add(maxFeretBoxplotChartPanel);
			
			JFreeChart minFeretBoxplot = ChartFactory.createBoxAndWhiskerChart(null, null, null, new DefaultBoxAndWhiskerCategoryDataset(), false);	        
			minFeretBoxplotChartPanel = new ChartPanel(minFeretBoxplot);
			boxplotSplitPanel.add(minFeretBoxplotChartPanel);
			
			JFreeChart differenceBoxplot = ChartFactory.createBoxAndWhiskerChart(null, null, null, new DefaultBoxAndWhiskerCategoryDataset(), false);	        
			differenceBoxplotChartPanel = new ChartPanel(differenceBoxplot);
			boxplotSplitPanel.add(differenceBoxplotChartPanel);
			
			tabbedPane.addTab("Boxplots", null, boxplotSplitPanel, null);
			
			//---------------
			// Create the variability chart
			//---------------
			JFreeChart variablityChart = ChartFactory.createXYLineChart(null,
					"Position", "IQR", null);
			XYPlot variabilityPlot = variablityChart.getXYPlot();
			variabilityPlot.setBackgroundPaint(Color.WHITE);
			variabilityPlot.getDomainAxis().setRange(0,100);
			
			variabilityChartPanel = new ChartPanel(variablityChart);
			tabbedPane.addTab("Variability", null, variabilityChartPanel, null);
			
			//---------------
			// Create the shells chart
			//---------------
			JFreeChart shellsChart = ChartFactory.createBarChart(null, "Shell", "Percent", null);
			shellsChart.getCategoryPlot().setBackgroundPaint(Color.WHITE);
			shellsChart.getCategoryPlot().getRangeAxis().setRange(0,100);
			shellsChartPanel = new ChartPanel(shellsChart);
			tabbedPane.addTab("Shells", null, shellsChartPanel, null);
			
			//---------------
			// Create the signals panel
			//---------------
			signalsPanel = new JPanel(); // main container in tab
			signalsPanel.setLayout(new BoxLayout(signalsPanel, BoxLayout.X_AXIS));
			

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
			signalsChartPanel = new ChartPanel(signalsChart);
			signalsPanel.add(signalsChartPanel);

			tabbedPane.addTab("Signals", null, signalsPanel, null);
			
			//---------------
			// Create the signal histograms panel
			//---------------
			signalHistogramPanel = new JPanel(); // main container in tab
			signalHistogramPanel.setLayout(new BoxLayout(signalHistogramPanel, BoxLayout.Y_AXIS));
			JFreeChart signalAngleChart = ChartFactory.createHistogram(null, "Angle", "Count", null, PlotOrientation.VERTICAL, true, true, true);
			signalAngleChart.getPlot().setBackgroundPaint(Color.white);
			signalAngleChartPanel = new ChartPanel(signalAngleChart);
			
			JFreeChart signalDistanceChart = ChartFactory.createHistogram(null, "Distance", "Count", null, PlotOrientation.VERTICAL, true, true, true);
			signalDistanceChart.getPlot().setBackgroundPaint(Color.white);
			signalDistanceChartPanel = new ChartPanel(signalDistanceChart);
			
			signalHistogramPanel.add(signalAngleChartPanel);
			signalHistogramPanel.add(signalDistanceChartPanel);
			tabbedPane.addTab("Signal histograms", null, signalHistogramPanel, null);
			
			//---------------
			// Create the signal histograms panel
			//---------------
			clusteringPanel = new JPanel();
			clusteringPanel.setLayout(new BoxLayout(clusteringPanel, BoxLayout.Y_AXIS));
			tabbedPane.addTab("Clusters", null, clusteringPanel, null);
			

		} catch (Exception e) {
			IJ.log("Error initialising Main: "+e.getMessage());
		}
		
	}
	
	/**
	 * Standard log - append a newline
	 * @param s the string to log
	 */
	public void log(String s){
		textArea.append(s+"\n");
	}
	
	/**
	 * Continuous log - do not append a newline
	 * @param s the string to log
	 */
	public void logc(String s){
		textArea.append(s);
	}
	
	public void postAnalysis(){
		PopulationSplitWindow splitter = new PopulationSplitWindow(new ArrayList<NucleusCollection>(this.analysisPopulations.values()));

		try{
			File f = splitter.addMappingFile();

			if(f==null) return;

			if(!f.exists()) return;

			NucleusCollection subjectCollection = splitter.getCollection();
			if(subjectCollection==null) return;

			// import and parse the mapping file
			List<String> pathList = MappingFileParser.parse(f);

			// create a new collection to hold the nuclei
			Constructor<?> collectionConstructor = subjectCollection.getClass().getConstructor(new Class<?>[]{File.class, String.class, String.class});
			NucleusCollection remapCollection = (NucleusCollection) collectionConstructor.newInstance(subjectCollection.getFolder(), 
																										subjectCollection.getOutputFolderName(), 
																										f.getName(), 
																										subjectCollection.getDebugFile());

			// add nuclei to the new population based on the mapping info
			for(Nucleus n : subjectCollection.getNuclei()){
				if(pathList.contains(n.getPath()+"\t"+n.getNucleusNumber())){
					remapCollection.addNucleus(n);
				}
			}
			this.analysisPopulations.put(remapCollection.getID(), remapCollection);
			log("Created subcollection from mapping file");
			List<NucleusCollection> list = new ArrayList<NucleusCollection>();
			list.add(remapCollection);
			updatePanels(list);
			updatePopulationList();
		} catch(Exception e){
			
		}
	}
	
	public void newAnalysis(){

		Thread thr = new Thread() {
			public void run() {
				lblStatusLine.setText("New analysis in progress");
				
				AnalysisCreator analysisCreator = new AnalysisCreator(MainWindow.this);
				analysisCreator.run();
				// post-analysis displays: make a new class eventually to handle gui? Or do it here

				List<NucleusCollection> result = analysisCreator.getPopulations();
				updatePanels(result);
				
				for(NucleusCollection c : result){
					MainWindow.this.analysisPopulations.put(c.getID(), c);
					MainWindow.this.populationNames.put(c.getName(), c.getID());
				}
				
				lblStatusLine.setText("New analysis complete: "+MainWindow.this.analysisPopulations.size()+" populations ready to view");
				updatePopulationList();	
				
				ListSelectionModel lsm = MainWindow.this.populationTable.getSelectionModel();
				int index = 0;
				for(NucleusCollection c : MainWindow.this.analysisPopulations.values()){
					index++;
					if(c.getID().equals(result.get(0).getID())){
						lsm.setSelectionInterval(index, index); // set the selection to the first result
					}
				}
				
				
			}
		};
		thr.start();		
	}
	
	public void clusterAnalysis(NucleusCollection collection){
		if(collection !=null){
			final UUID id = collection.getID();
			Thread thr = new Thread() {
				public void run() {
					try{
						logc("Running cluster analysis...");
						NucleusClusterer clusterer = new NucleusClusterer();
						boolean ok = clusterer.cluster(MainWindow.this.analysisPopulations.get(id));
						if(ok){
							log("OK");
							log("Found "+clusterer.getNumberOfClusters()+" clusters");
						} else {
							log("Error");
						}

						for(int cluster=0;cluster<clusterer.getNumberOfClusters();cluster++){
							NucleusCollection c = clusterer.getCluster(cluster);
							log("Cluster "+cluster+":");

							logc("Reapplying morphology...");
							ok = MorphologyAnalysis.reapplyProfiles(c, MainWindow.this.analysisPopulations.get(id));
							if(ok){
								log("OK");
							} else {
								log("Error");
							}
							
							logc("Refolding profile...");
							  ok = CurveRefolder.run(c, c.getAnalysisOptions().getNucleusClass(), c.getAnalysisOptions().getRefoldMode());
							  if(ok){
								  log("OK");
							  } else {
								  log("Error");
							  }

							MainWindow.this.analysisPopulations.put(c.getID(), c);
							MainWindow.this.populationNames.put(c.getName(), c.getID());

						}
						updatePopulationList();	


					} catch (Exception e){
						log("Error in cluster analysis: "+e.getMessage());
					}
				}
			};
			thr.start();
		}

	}
	
	public void renameCollection(NucleusCollection collection){
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
				
				List<NucleusCollection> list = new ArrayList<NucleusCollection>(0);
				list.add(collection);
				updatePanels(list);
			}
		}
	}
	
	public void newShellAnalysis(NucleusCollection collection){
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
							boolean ok = ShellAnalysis.run(MainWindow.this.analysisPopulations.get(id), shellCount);
							if(ok){
								log("OK");
							} else {
								log("Error");
							}

							List<NucleusCollection> list = new ArrayList<NucleusCollection>(0);
							list.add(MainWindow.this.analysisPopulations.get(id));
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
	
	public void loadNuclei(){
		Thread thr = new Thread() {
			public void run() {
				try {
					OpenDialog fileDialog = new OpenDialog("Select a save file...");
					String fileName = fileDialog.getPath();
					if(fileName==null) return;
					NucleusCollection collection = PopulationImporter.readPopulation(new File(fileName), MainWindow.this);
					MainWindow.this.analysisPopulations.put(collection.getID(), collection);
					MainWindow.this.populationNames.put(collection.getName(), collection.getID());
					log("Opened collection: "+collection.getType());

					List<NucleusCollection> list = new ArrayList<NucleusCollection>(0);
					list.add(collection);

					updatePanels(list);
					updatePopulationList();
				} catch (Exception e) {
					log("Error opening file: "+e.getMessage());
				}
			}
		};
		thr.start();
	}
	
	public void updatePanels(final List<NucleusCollection> list){

		Thread thr = new Thread() {
			public void run() {
				try {
					updateStatsPanel(list);
					updateProfileImage(list);
					updateConsensusImage(list);
					updateBoxplots(list);
					updateVariabilityChart(list);
					updateShellPanel(list);
					updateSignalsPanel(list);
					updateSignalHistogramPanel(list);
					updateClusteringPanel(list);
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
	
	private int getIndexFromLabel(String label){
		String[] names = label.split("_");
		return Integer.parseInt(names[1]);
	}
	
	// methods for getting signal colours. Defaults are no transparency
	public Color getSignalColour(int channel, boolean transparent, int defaultAlpha){
		Color result;
		switch (channel){
			case 1: result = transparent ? new Color(0,0,255,defaultAlpha) : Color.BLUE;
					break;
			case 2: result = transparent ? new Color(255,0,0,defaultAlpha) : Color.RED;
					break;
			case 3: result = transparent ? new Color(0,255,0,defaultAlpha) :Color.GREEN;
					break;
			case 4: result = transparent ? new Color(0,255,255,defaultAlpha) : Color.CYAN;
					break;
			default: result = transparent ?  new Color(128,128,128,defaultAlpha) : Color.LIGHT_GRAY;
			break;
		}
		return result;
	}
	
	public Color getSignalColour(int channel, boolean transparent){
		return getSignalColour(channel, transparent, 10);
	}
	
	public Color getSignalColour(int channel){
		return getSignalColour(channel, false);
	}
	
	public void updateStatsPanel(List<NucleusCollection> list){
		// format the numbers and make into a tablemodel
		TableModel model = DatasetCreator.createStatsTable(list);
		tablePopulationStats.setModel(model);
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
				plot.getRenderer().setSeriesPaint(i, ProfileSegmenter.getColor(colourIndex));
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
	
	public void updateProfileImage(List<NucleusCollection> list){
		
		try {
			if(list.size()==1){

				// full segment colouring
				XYDataset ds = DatasetCreator.createSegmentDataset(list.get(0));
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
					Color pColor = ProfileSegmenter.getColor(index);
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
					plot.getRenderer(i).setSeriesPaint(j, ProfileSegmenter.getColor(Integer.parseInt(names[1])).darker());
				}	
				
				profileChartPanel.setChart(chart);
			}
			
//			XYDataset fs = DatasetCreator.createFrankenSegmentDataset(collection);
//			JFreeChart frankenChart = makeProfileChart(fs);
//			frankenChartPanel.setChart(frankenChart);
			
		} catch (Exception e) {
			log("Error in plotting frankenprofile or profile");
		} 
	}
		
	public void updateShellPanel(List<NucleusCollection> list){

		if(list.size()==1){ // single collection is easy
			
			NucleusCollection collection = list.get(0);
//			log("Selected collection: "+collection.getName());
//			log("Shells: "+collection.hasShellResult());

			if(collection.hasShellResult()){ // only if there is something to display

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
					rend.setSeriesPaint(j, getSignalColour(index));
				}	

				shellsChartPanel.setChart(shellsChart);
				tabbedPane.setComponentAt(5, shellsChartPanel);
			} else { // no shell analysis available

				if(collection.hasSignals()){
					// if signals, offer to run
					shellsPanel = makeNoShellAnalysisAvailablePanel(true, collection, "No shell results available; run new analysis?"); // allow option to run analysis
					tabbedPane.setComponentAt(5, shellsPanel);
				} else {
					// otherwise don't show button
					shellsPanel = makeNoShellAnalysisAvailablePanel(false, null, "No signals in population"); // container in tab if no shell chart
					tabbedPane.setComponentAt(5, shellsPanel);
				}
			}
		} else {

			// Multiple populations. Do not display
			// container in tab if no shell chart
			shellsPanel = makeNoShellAnalysisAvailablePanel(false, null, "Cannot display shell results for multiple populations");
			tabbedPane.setComponentAt(5, shellsPanel);
		}
	}
	
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
					newShellAnalysis(MainWindow.this.analysisPopulations.get(id));
				}
			});
			panel.add(btnShellAnalysis, BorderLayout.SOUTH);
		}
		lbl.setHorizontalAlignment(SwingConstants.CENTER);
		panel.add(lbl, BorderLayout.NORTH);
		return panel;
	}
	
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
				plot.getRenderer().setSeriesPaint(i, ProfileSegmenter.getColor(colourIndex));
			} 
			if(name.startsWith("Q")){
				plot.getRenderer().setSeriesStroke(i, new BasicStroke(2));
				plot.getRenderer().setSeriesPaint(i, Color.DARK_GRAY);
			} 

		}	
		return chart;
	}
	
	public void updateConsensusImage(List<NucleusCollection> list){

		NucleusCollection collection = list.get(0);
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

				} else {
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
					plot.getRenderer().setSeriesPaint(i, ProfileSegmenter.getColor(Integer.parseInt(names[1])));
					if(name.startsWith("Q")){
						// make the IQR distinct from the median
						plot.getRenderer().setSeriesPaint(i, ProfileSegmenter.getColor(Integer.parseInt(names[1])).darker());
					}
					
				}
				consensusChartPanel.setChart(chart);
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
	public void updateBoxplots(List<NucleusCollection> list){
				
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
	public void updateAreaBoxplot(List<NucleusCollection> list){
		BoxAndWhiskerCategoryDataset ds = DatasetCreator.createAreaBoxplotDataset(list);
		JFreeChart boxplotChart = ChartFactory.createBoxAndWhiskerChart(null, null, null, ds, false); 
		formatBoxplotChart(boxplotChart);
		areaBoxplotChartPanel.setChart(boxplotChart);
	}
	
	/**
	 * Update the boxplot panel for perimeters with a list of NucleusCollections
	 * @param list
	 */
	public void updatePerimBoxplot(List<NucleusCollection> list){
		BoxAndWhiskerCategoryDataset ds = DatasetCreator.createPerimBoxplotDataset(list);
		JFreeChart boxplotChart = ChartFactory.createBoxAndWhiskerChart(null, null, null, ds, false); 
		formatBoxplotChart(boxplotChart);
		perimBoxplotChartPanel.setChart(boxplotChart);
	}
	
	/**
	 * Update the boxplot panel for longest diameter across CoM with a list of NucleusCollections
	 * @param list
	 */
	public void updateMaxFeretBoxplot(List<NucleusCollection> list){
		BoxAndWhiskerCategoryDataset ds = DatasetCreator.createMaxFeretBoxplotDataset(list);
		JFreeChart boxplotChart = ChartFactory.createBoxAndWhiskerChart(null, null, null, ds, false); 
		formatBoxplotChart(boxplotChart);
		maxFeretBoxplotChartPanel.setChart(boxplotChart);
	}

	/**
	 * Update the boxplot panel for shortest diameter across CoM with a list of NucleusCollections
	 * @param list
	 */
	public void updateMinFeretBoxplot(List<NucleusCollection> list){
		BoxAndWhiskerCategoryDataset ds = DatasetCreator.createMinFeretBoxplotDataset(list);
		JFreeChart boxplotChart = ChartFactory.createBoxAndWhiskerChart(null, null, null, ds, false); 
		formatBoxplotChart(boxplotChart);
		minFeretBoxplotChartPanel.setChart(boxplotChart);
	}
	
	/**
	 * Update the boxplot panel for shortest diameter across CoM with a list of NucleusCollections
	 * @param list
	 */
	public void updateDifferenceBoxplot(List<NucleusCollection> list){
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
			Color color = ProfileSegmenter.getColor(i);
			renderer.setSeriesPaint(i, color);
		}
		renderer.setMeanVisible(false);
	}

	
	/**
	 *  Find the populations in memory, and display them in the population chooser
	 */
	public void updatePopulationList(){
					
		// new method using table
		if(this.analysisPopulations.size()>0){
			DefaultTableModel populationTableModel = new DefaultTableModel();
			populationTableModel.addColumn("Population");
			populationTableModel.addColumn("Nuclei");
			populationTableModel.addColumn("");
			for(NucleusCollection c : this.analysisPopulations.values()){
				Object[] data  = {c.getName(),c.getNucleusCount(), null};
				populationTableModel.addRow( data );
			}

			populationTable.setModel(populationTableModel);
		}

	}
		
	public void updateVariabilityChart(List<NucleusCollection> list){
		try {
			XYDataset ds = DatasetCreator.createIQRVariabilityDataset(list);
			if(list.size()==1){
				NucleusCollection n = list.get(0);
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
					plot.getRenderer().setSeriesPaint(j, ProfileSegmenter.getColor(index));
				}	
				variabilityChartPanel.setChart(chart);
			}
		} catch (Exception e) {
			log("Error drawing variability chart: "+e.getMessage());
		}	
	}
	
	public void updateSignalsPanel(List<NucleusCollection> list){
		updateSignalConsensusChart(list);
		updateSignalStatsPanel(list);
	}
	
	private void updateSignalStatsPanel(List<NucleusCollection> list){
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
	
	private void updateClusteringPanel(List<NucleusCollection> list){
		
		if(list.size()==1){
			NucleusCollection collection = list.get(0);
			final UUID id = collection.getID();
			
			clusteringPanel = new JPanel();
			clusteringPanel.setLayout(new BoxLayout(clusteringPanel, BoxLayout.Y_AXIS));

			JButton btnNewClusterAnalysis = new JButton("Cluster population");
			btnNewClusterAnalysis.addMouseListener(new MouseAdapter() {
				@Override
				public void mouseClicked(MouseEvent arg0) {
					clusterAnalysis(MainWindow.this.analysisPopulations.get(id));
				}
			});
			clusteringPanel.add(btnNewClusterAnalysis);
			tabbedPane.setComponentAt(8, clusteringPanel);
			
		} else {
			clusteringPanel = new JPanel();
			clusteringPanel.setLayout(new BoxLayout(clusteringPanel, BoxLayout.Y_AXIS));
			tabbedPane.setComponentAt(8, clusteringPanel);
		}
		
	}
	
	private void updateSignalConsensusChart(List<NucleusCollection> list){
		try {

			if(list.size()==1){

				NucleusCollection collection = list.get(0);

				if(collection.hasConsensusNucleus()){ // if a refold is available
					XYDataset signalCoMs = DatasetCreator.createSignalCoMDataset(collection);
					JFreeChart chart = makeConsensusChart(collection);

					XYPlot plot = chart.getXYPlot();
					plot.setDataset(1, signalCoMs);

					XYLineAndShapeRenderer  rend = new XYLineAndShapeRenderer();
					for(int series=0;series<signalCoMs.getSeriesCount();series++){
						int channel = series+2; // channel is from 2, series from 0
						rend.setSeriesPaint(series, getSignalColour(channel, false));
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
									null, getSignalColour(channel, true, alpha)); // layer transparent signals
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
	
	private void updateSignalHistogramPanel(List<NucleusCollection> list){
		try {
			updateSignalAngleHistogram(list);
			updateSignalDistanceHistogram(list);
		} catch (Exception e) {
			log("Error updating signal histograms: "+e.getMessage());
		}
	}
	
	private void updateSignalAngleHistogram(List<NucleusCollection> list){
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
				plot.getRenderer().setSeriesPaint(j, getSignalColour(index, true, 128));
			}	
			signalAngleChartPanel.setChart(chart);
		} catch (Exception e) {
			log("Error updating angle histograms: "+e.getMessage());
			JFreeChart chart = ChartFactory.createHistogram(null, "Angle", "Count", null, PlotOrientation.VERTICAL, true, true, true);
			chart.getPlot().setBackgroundPaint(Color.white);
			signalAngleChartPanel.setChart(chart);
		}
		
		
	}

	private void updateSignalDistanceHistogram(List<NucleusCollection> list){
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
				plot.getRenderer().setSeriesPaint(j, getSignalColour(index, true, 128));
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
			
			List<NucleusCollection> list = new ArrayList<NucleusCollection>(0);
			
			ListSelectionModel lsm = (ListSelectionModel)e.getSource();
			
			List<Integer> selectedIndexes = new ArrayList<Integer>(0);

			if (!lsm.isSelectionEmpty()) {
				// Find out which indexes are selected.
				int minIndex = lsm.getMinSelectionIndex();
				int maxIndex = lsm.getMaxSelectionIndex();
				for (int i = minIndex; i <= maxIndex; i++) {
					if (lsm.isSelectedIndex(i)) {
//						String key = populationList.getModel().getElementAt(i);
						String key = (String) populationTable.getModel().getValueAt(i, 0); // row i, column 0
						if(!key.equals("No populations")){
							
							// get uuid from populationNames, then population via uuid from analysisPopulations
							list.add(analysisPopulations.get(populationNames.get(key)));
							selectedIndexes.add(i);
							
						}

					}
				}
				String count = list.size() == 1 ? "population" : "populations"; // it matters to ME
				lblStatusLine.setText(list.size()+" "+count+"  selected");
				populationTable.getColumnModel().getColumn(2).setCellRenderer(new PopulationTableCellRenderer(selectedIndexes));
				updatePanels(list);
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
	          l.setBackground(ProfileSegmenter.getColor(list.indexOf(row)));
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
	        Color colour = (row-1) % 8 == 0 ? MainWindow.this.getSignalColour(  choose   ) : Color.WHITE;
	        
	        l.setBackground(colour);

	      //Return the JLabel which renders the cell.
	      return l;
	    }
	}
}
