package no.gui;

import ij.IJ;
import ij.ImagePlus;
import ij.gui.ImageCanvas;
import ij.gui.Plot;
import ij.io.OpenDialog;
import ij.process.ByteProcessor;

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
import no.analysis.ProfileSegmenter;
import no.collections.NucleusCollection;
import no.components.NucleusBorderSegment;
import no.components.Profile;
import no.components.ProfileCollection;
import no.imports.PopulationImporter;
import no.nuclei.Nucleus;
import no.utility.MappingFileParser;

import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;

import javax.swing.JTextArea;

import java.awt.SystemColor;
import java.io.File;
import java.lang.reflect.Constructor;
import java.text.DecimalFormat;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

import javax.swing.DefaultListModel;
import javax.swing.JScrollPane;
import javax.swing.BoxLayout;
import javax.swing.SwingConstants;

import java.awt.Font;

import javax.swing.JTable;

import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Component;
import java.awt.GridLayout;
import java.awt.Paint;

import javax.swing.JList;
import javax.swing.ListSelectionModel;

import org.jfree.chart.ChartFactory;
import org.jfree.chart.ChartPanel;
import org.jfree.chart.JFreeChart;
import org.jfree.chart.plot.PlotOrientation;
import org.jfree.chart.plot.ValueMarker;
import org.jfree.chart.plot.XYPlot;
import org.jfree.data.xy.DefaultXYDataset;
import org.jfree.data.xy.XYDataset;
import org.jfree.data.xy.XYSeries;
import org.jfree.data.xy.XYSeriesCollection;

public class MainWindow extends JFrame {

	/**
	 * 
	 */
	private static final long serialVersionUID = 1L;
	private JPanel contentPane;
	private JTextArea textArea = new JTextArea();;
	private JLabel lblStatusLine = new JLabel("No analysis open");
	private final JPanel panelAggregates = new JPanel();
	private JTable table;
	private final JPanel panelGeneralData = new JPanel();
	
	private final JPanel panelPopulations = new JPanel();
	private JList<String> populationList;
	
	private ImageCanvas ic = new ImageCanvas(new ImagePlus(null, new ByteProcessor(600,400)));
	
	private ChartPanel cp;
	
	private HashMap<String, NucleusCollection> analysisPopulations = new HashMap<String, NucleusCollection>();;


	/**
	 * Create the frame.
	 */
	public MainWindow() {
		try {
			setTitle("Nuclear Morphology Analysis");
			setDefaultCloseOperation(JFrame.DISPOSE_ON_CLOSE);
			setBounds(100, 100, 992, 514);
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
			
//		JLabel lblStatusLine = new JLabel("No analysis open");
			panelFooter.add(lblStatusLine);
			contentPane.add(panelGeneralData, BorderLayout.CENTER);
			panelGeneralData.setLayout(new BoxLayout(panelGeneralData, BoxLayout.Y_AXIS));
			
			JPanel panel = new JPanel();
			panelGeneralData.add(panel);
			panel.setLayout(new GridLayout(0, 2, 0, 0));
			panel.add(panelPopulations);
			panelPopulations.setLayout(new BoxLayout(panelPopulations, BoxLayout.Y_AXIS));
			
			JLabel lblPopulations = new JLabel("Populations");
			lblPopulations.setAlignmentX(Component.CENTER_ALIGNMENT);
			panelPopulations.add(lblPopulations);
			

			DefaultListModel<String> model = new DefaultListModel<String>();
			model.addElement("No populations");
			populationList = new JList<String>(model);
			populationList.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);;
			
			ListSelectionModel listSelectionModel = populationList.getSelectionModel();
			listSelectionModel.addListSelectionListener(
			                        new ListSelectionHandler());
			
			panelPopulations.add(populationList);
			
			JPanel panelStats = new JPanel();
			panel.add(panelStats);
			panelStats.setLayout(new BoxLayout(panelStats, BoxLayout.Y_AXIS));
			
			JLabel lblPopulationStatistics = new JLabel("Statistics");
			lblPopulationStatistics.setAlignmentX(Component.CENTER_ALIGNMENT);
			lblPopulationStatistics.setHorizontalAlignment(SwingConstants.CENTER);
			panelStats.add(lblPopulationStatistics);
			
			table = new JTable();
			table.setEnabled(false);
			table.setModel(this.createStatsPanel());
			panelStats.add(table);
			
			JLabel lblAggregates = new JLabel("Segmented Profile");
			lblAggregates.setAlignmentX(Component.CENTER_ALIGNMENT);
			panelGeneralData.add(panelAggregates);
			
			panelAggregates.setLayout(new BoxLayout(panelAggregates, BoxLayout.Y_AXIS));
			panelAggregates.add(lblAggregates);
			
			
			JFreeChart chart = ChartFactory.createXYLineChart(null,
		            "Position", "Angle", null);
			XYPlot plot = chart.getXYPlot();
			plot.getDomainAxis().setRange(0,100);
			plot.getRangeAxis().setRange(0,360);
			plot.setBackgroundPaint(Color.WHITE);

			cp = new ChartPanel(chart);
			
//			panelAggregates.add(ic);
			panelAggregates.add(cp);
		} catch (Exception e) {
			IJ.log("Error in Main");
			e.printStackTrace();
		}
//		panelAggregates.setVisible(true);
		
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
			this.analysisPopulations.put(remapCollection.getOutputFolderName()+" - "+remapCollection.getType()+" - "+remapCollection.getNucleusCount()+" nuclei", remapCollection);
			log("Created subcollection from mapping file");
			updateStatsPanel(remapCollection);
			updatePopulationList();
		} catch(Exception e){
			
		}
	}
	
	public void newAnalysis(){

		Thread thr = new Thread() {
			public void run() {
				lblStatusLine.setText("New analysis in progress");
//				panelAggregates.remove(ic);
				
				AnalysisCreator analysisCreator = new AnalysisCreator(MainWindow.this);
				analysisCreator.run();
				// post-analysis displays: make a new class eventually to handle gui? Or do it here

				List<NucleusCollection> result = analysisCreator.getPopulations();
				updateStatsPanel(result.get(0));
				updateProfileImage(result.get(0));
				
				for(NucleusCollection c : result){
					String key = c.getOutputFolderName()+" - "+c.getType()+" - "+c.getNucleusCount()+" nuclei";
					MainWindow.this.analysisPopulations.put(key, c);
				}
				
				lblStatusLine.setText("New analysis complete: "+MainWindow.this.analysisPopulations.size()+" populations ready to view");
				updatePopulationList();				
			}
		};
		thr.start();		
	}
	
	public void loadNuclei(){
		OpenDialog fileDialog = new OpenDialog("Select a save file...");
		String fileName = fileDialog.getPath();
		if(fileName==null) return;
		NucleusCollection collection = PopulationImporter.readPopulation(new File(fileName), this);
		String key = collection.getOutputFolderName()+" - "+collection.getType()+" - "+collection.getNucleusCount()+" nuclei";
		MainWindow.this.analysisPopulations.put(key, collection);
		log("Opened collection: "+collection.getType());
		
		updateStatsPanel(collection);
		updateProfileImage(collection);
		updatePopulationList();
	}
	
	private TableModel createStatsPanel(){
		String[] columnNames = {"Field", "Value"};

		Object[][] data = {
				{"Nuclei", null},
				{"Median area", null},
				{"Median perimeter", null},
				{"Median feret", null},
				{"Signal channels", null},
				{"Profile window", null},
				{"Ran", null},
				{"Type", null}
		};

		TableModel model = new DefaultTableModel(data, columnNames);
		return model;
	}
	
	
	public void updateStatsPanel(NucleusCollection collection){
		// format the numbers and make into a tablemodel
		DecimalFormat df = new DecimalFormat("#.00"); 
		lblStatusLine.setText("Showing: "+collection.getOutputFolderName()+" - "+collection.getFolder()+" - "+collection.getType());

		String[] columnNames = {"Field", "Value"};

		Object[][] data = {
				{"Nuclei", collection.getNucleusCount()},
				{"Median area", df.format(collection.getMedianNuclearArea())},
				{"Median perimeter", df.format(collection.getMedianNuclearPerimeter())},
				{"Median feret", df.format(collection.getMedianFeretLength())},
				{"Signal channels", collection.getSignalChannels().size()},
				{"Profile window", collection.getProfileWindowSize()},
				{"Ran", collection.getOutputFolderName()},
				{"Type", collection.getClass().getSimpleName()}
		};

		TableModel model = new DefaultTableModel(data, columnNames);
		table.setModel(model);
	}
	
	public void updateProfileImage(NucleusCollection collection){
		
		try {
			
			XYDataset ds = createSegmentDataset(collection);
						
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
					plot.getRenderer().setSeriesStroke(i, new BasicStroke(3));
					plot.getRenderer().setSeriesPaint(i, ProfileSegmenter.getColor(i));
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

			cp.setChart(chart);
			
		} catch (Exception e) {
			log("Error in plotting");
		} 
	}
	
	public void updatePopulationList(){
				
		DefaultListModel<String> model = new DefaultListModel<String>();
		if(this.analysisPopulations.size()==0){
			model.addElement("No populations");
		} else {
			for(NucleusCollection c : this.analysisPopulations.values()){
				model.addElement(c.getOutputFolderName()+" - "+c.getType()+" - "+c.getNucleusCount()+" nuclei");
			}
		}
		populationList.setModel(model);
		populationList.setVisible(true);		
	}
	
	
	class ListSelectionHandler implements ListSelectionListener {
		public void valueChanged(ListSelectionEvent e) {

			String key = populationList.getSelectedValue();
			if(!key.equals("No populations")){
				NucleusCollection c = MainWindow.this.analysisPopulations.get(key);
				updateStatsPanel(c);
				updateProfileImage(c);
			}
		}
	}
	
	private XYDataset createSegmentDataset(NucleusCollection collection){
		DefaultXYDataset ds = new DefaultXYDataset();
		Profile profile = collection.getProfileCollection().getProfile(collection.getOrientationPoint());
		Profile xpoints = profile.getPositions(100);
		
		// rendering order will be first on top
		
		// add the segments

		List<NucleusBorderSegment> segments = collection.getProfileCollection().getSegments(collection.getOrientationPoint());

		for(NucleusBorderSegment seg : segments){

			if(seg.getStartIndex()>seg.getEndIndex()){ // case when array wraps

				// beginning of array
				Profile subProfileA = profile.getSubregion(0, seg.getEndIndex());
				Profile subPointsA = xpoints.getSubregion(0, seg.getEndIndex());
				double[][] dataA = { subPointsA.asArray(), subProfileA.asArray() };
				ds.addSeries(seg.getSegmentType(), dataA);

				// end of array
				Profile subProfileB = profile.getSubregion(seg.getStartIndex(), profile.size()-1);
				Profile subPointsB = xpoints.getSubregion(seg.getStartIndex(), profile.size()-1);
				double[][] dataB = { subPointsB.asArray(), subProfileB.asArray() };
				ds.addSeries(seg.getSegmentType(), dataB);
				continue;
			} 
			Profile subProfile = profile.getSubregion(seg.getStartIndex(), seg.getEndIndex());
			Profile subPoints  = xpoints.getSubregion(seg.getStartIndex(), seg.getEndIndex());
			double[][] data = { subPoints.asArray(), subProfile.asArray() };
			ds.addSeries(seg.getSegmentType(), data);
		}

		// make the IQR
		Profile profile25 = collection.getProfileCollection().getProfile(collection.getOrientationPoint()+"25");
		Profile profile75 = collection.getProfileCollection().getProfile(collection.getOrientationPoint()+"75");
		double[][] data25 = { xpoints.asArray(), profile25.asArray() };
		ds.addSeries("Q25", data25);
		double[][] data75 = { xpoints.asArray(), profile75.asArray() };
		ds.addSeries("Q75", data75);

		// add the individual nuclei
		for(Nucleus n : collection.getNuclei()){
			Profile angles = n.getAngleProfile(collection.getOrientationPoint()).interpolate(profile.size());
			double[][] ndata = { xpoints.asArray(), angles.asArray() };
			ds.addSeries("Nucleus_"+n.getImageName()+"-"+n.getNucleusNumber(), ndata);
		}


		return ds;
	}
}
