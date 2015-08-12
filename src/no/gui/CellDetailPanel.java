package no.gui;

import ij.IJ;

import java.awt.BasicStroke;
import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.beans.PropertyChangeSupport;
import java.util.ArrayList;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import javax.swing.BoxLayout;
import javax.swing.ComboBoxModel;
import javax.swing.DefaultComboBoxModel;
import javax.swing.JColorChooser;
import javax.swing.JComboBox;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTable;
import javax.swing.event.ChangeEvent;
import javax.swing.table.TableColumn;

import no.analysis.AnalysisDataset;
import no.nuclei.Nucleus;

import org.jfree.chart.ChartFactory;
import org.jfree.chart.ChartPanel;
import org.jfree.chart.JFreeChart;
import org.jfree.chart.plot.PlotOrientation;
import org.jfree.chart.plot.ValueMarker;
import org.jfree.chart.plot.XYPlot;
import org.jfree.chart.renderer.xy.XYLineAndShapeRenderer;
import org.jfree.data.xy.DefaultXYDataset;
import org.jfree.data.xy.XYDataset;

import cell.Cell;
import datasets.CellDatasetCreator;
import datasets.NucleusDatasetCreator;
import datasets.NucleusTableDatasetCreator;
import datasets.TailDatasetCreator;

public class CellDetailPanel extends JPanel implements ActionListener, SignalChangeListener {

	private static final long serialVersionUID = 1L;
	
	public static final String SOURCE_COMPONENT = "CellDetailPanel"; 

	private JComboBox<String> 	cellSelectionBox; 	// choose which cell to look at individually
	private 	List<AnalysisDataset> list;
	protected AnalysisDataset activeDataset;	
	private Cell activeCell;
	
	protected ProfilePanel	 	profilePanel; 		// the nucleus angle profile
	protected OutlinePanel 	 	outlinePanel; 		// the outline of the cell and detected objects
	protected CellStatsPanel 	cellStatsPanel;		// the stats table
	protected SegmentStatsPanel segmentStatsPanel;	// details of the individual segments
	
	private List<Object> listeners = new ArrayList<Object>();
	
	public CellDetailPanel() {

		this.setLayout(new BorderLayout());
		
		// make the chart for each nucleus
		outlinePanel = new OutlinePanel();
		this.add(outlinePanel, BorderLayout.CENTER);
		
		// make the combobox for selecting nuclei
		cellSelectionBox = new JComboBox<String>();
		cellSelectionBox.setActionCommand("CellSelectionChoice");
		cellSelectionBox.addActionListener(this);
		this.add(cellSelectionBox, BorderLayout.NORTH);
		
		
		JPanel westPanel = new JPanel();
		westPanel.setLayout(new BoxLayout(westPanel, BoxLayout.Y_AXIS));
		
		
		cellStatsPanel = new CellStatsPanel();
		westPanel.add(cellStatsPanel);
		
		profilePanel = new ProfilePanel();
		westPanel.add(profilePanel);
		
		segmentStatsPanel = new SegmentStatsPanel();
		westPanel.add(segmentStatsPanel);
		
		this.add(westPanel, BorderLayout.WEST);
		
	}
	
	/**
	 * Update the panel with a list of AnalysisDatasets. Data
	 * will only be displayed if the list contains one dataset.
	 * @param list the datsets
	 */
	public void updateList(List<AnalysisDataset> list){
		this.list = list;
		
		if(list.size()==1){
			activeDataset = list.get(0);
			ComboBoxModel<String> cellModel = new DefaultComboBoxModel<String>(activeDataset.getCollection().getNucleusPathsAndNumbers());
			cellSelectionBox.setModel(cellModel);
			cellSelectionBox.setSelectedIndex(0);
		} else {
			
			ComboBoxModel<String> cellModel = new DefaultComboBoxModel<String>();
			cellSelectionBox.setModel(cellModel);
			updateCell(null);
		}
	}
	
	
	/**
	 * Display data for the given cell
	 * @param cell
	 */
	private void updateCell(Cell cell){
		
		cellStatsPanel.update(cell);
		outlinePanel.update(cell);
		profilePanel.update(cell);
		segmentStatsPanel.update(cell);
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
	

	@Override
	public void actionPerformed(ActionEvent arg0) {

		if(arg0.getActionCommand().equals("CellSelectionChoice")){

			String name = cellSelectionBox.getItemAt(cellSelectionBox.getSelectedIndex());

			if(list.size()==1){	
				try{
					
					activeCell = activeDataset.getCollection().getCell(name);
					updateCell(activeCell);
				} catch (Exception e1){
					
					IJ.log("Error fetching cell: "+e1.getMessage());
					for(StackTraceElement e2 : e1.getStackTrace()){
						IJ.log(e2.toString());
					}
				}
			}

		}
		
	}
	
	public synchronized void addSignalChangeListener( SignalChangeListener l ) {
        listeners.add( l );
    }
    
    public synchronized void removeSignalChangeListener( SignalChangeListener l ) {
        listeners.remove( l );
    }
     
    private synchronized void fireSignalChangeEvent(String message) {
        SignalChangeEvent event = new SignalChangeEvent( this, message, SOURCE_COMPONENT );
        Iterator iterator = listeners.iterator();
        while( iterator.hasNext() ) {
            ( (SignalChangeListener) iterator.next() ).signalChangeReceived( event );
        }
    }
	
	
	/**
	 * Allows for cell background to be coloured based on position in a list. Used to colour
	 * the signal stats list
	 *
	 */
	private class StatsTableCellRenderer extends javax.swing.table.DefaultTableCellRenderer {

		private static final long serialVersionUID = 1L;

		public java.awt.Component getTableCellRendererComponent(javax.swing.JTable table, java.lang.Object value, boolean isSelected, boolean hasFocus, int row, int column) {

			// default cell colour is white
			Color colour = Color.WHITE;

			// get the value in the first column of the row below
			if(row<table.getModel().getRowCount()-1){
				String nextRowHeader = table.getModel().getValueAt(row+1, 0).toString();

				if(nextRowHeader.equals("Signal group")){
					// we want to colour this cell preemptively
					// get the signal group from the table
					String groupString = table.getModel().getValueAt(row+1, 1).toString();
					colour = activeDataset.getSignalGroupColour(Integer.valueOf(groupString));
//					colour = ColourSelecter.getSignalColour(  Integer.valueOf(groupString)-1   ); 
				}
			}
			//Cells are by default rendered as a JLabel.
			JLabel l = (JLabel) super.getTableCellRendererComponent(table, value, isSelected, hasFocus, row, column);
			l.setBackground(colour);

			//Return the JLabel which renders the cell.
			return l;
		}
	}
	
	/**
	 * Allows for cell background to be coloured based on position in a list. Used to colour
	 * the segment stats list
	 *
	 */
	private class SegmentTableCellRenderer extends javax.swing.table.DefaultTableCellRenderer {

		private static final long serialVersionUID = 1L;

		public java.awt.Component getTableCellRendererComponent(javax.swing.JTable table, java.lang.Object value, boolean isSelected, boolean hasFocus, int row, int column) {

			// default cell colour is white
			Color colour = Color.WHITE;
			
			// only apply to first row, after the first column
			if(column>0 && row==0){
				String colName = table.getColumnName(column); // will be Seg_x

				int segment = Integer.valueOf(colName.replace("Seg_", ""));

				colour = ColourSelecter.getSegmentColor(segment);
			}
			
			//Cells are by default rendered as a JLabel.
			JLabel l = (JLabel) super.getTableCellRendererComponent(table, value, isSelected, hasFocus, row, column);
			l.setBackground(colour);

			//Return the JLabel which renders the cell.
			return l;
		}
	}


	@Override
	public void signalChangeReceived(SignalChangeEvent event) {
		if(event.type().equals("SignalColourUpdate")){
			updateCell(activeCell);
		}

	}
	
	
	/**
	 * Show the profile for the nuclei in the given cell
	 *
	 */
	protected class ProfilePanel extends JPanel{
		
		private static final long serialVersionUID = 1L;
		private ChartPanel profileChartPanel; // holds the chart with the cell
		
		protected ProfilePanel(){
			this.setLayout(new BorderLayout());
			
			JFreeChart chart = ChartFactory.createXYLineChart(null,
					"Position", "Angle", null);
			XYPlot plot = chart.getXYPlot();
			plot.getDomainAxis().setRange(0,100);
			plot.getRangeAxis().setRange(0,360);
			plot.setBackgroundPaint(Color.WHITE);
			
			profileChartPanel = new ChartPanel(chart);
			this.add(profileChartPanel, BorderLayout.CENTER);
			
		}
		
		protected void update(Cell cell){

			if(cell==null){
				JFreeChart chart = ChartFactory.createXYLineChart(null,
						"Position", "Angle", null);
				XYPlot plot = chart.getXYPlot();
				plot.getDomainAxis().setRange(0,100);
				plot.getRangeAxis().setRange(0,360);
				plot.setBackgroundPaint(Color.WHITE);
				profileChartPanel.setChart(chart);

			} else {
				
				Nucleus nucleus = cell.getNucleus();

				XYDataset ds = NucleusDatasetCreator.createSegmentedProfileDataset(nucleus);

				// full segment colouring
				JFreeChart chart = 
						ChartFactory.createXYLineChart(null,
								"Position", "Angle", ds, PlotOrientation.VERTICAL, true, true,
								false);


				XYPlot plot = chart.getXYPlot();
				plot.getDomainAxis().setRange(0,nucleus.getLength());
				plot.getRangeAxis().setRange(0,360);
				plot.setBackgroundPaint(Color.WHITE);
				plot.addRangeMarker(new ValueMarker(180, Color.BLACK, new BasicStroke(1.0f)));

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

				}	

				profileChartPanel.setChart(chart);
			}

		}

	}
	
	protected class OutlinePanel extends JPanel{

		private static final long serialVersionUID = 1L;
		
		private ChartPanel panel;
		
		protected OutlinePanel(){
			
			// make the chart for each nucleus
			this.setLayout(new BorderLayout());
			JFreeChart chart = ChartFactory.createXYLineChart(null,
					null, null, null);       
			chart.getPlot().setBackgroundPaint(Color.WHITE);

			panel = new ChartPanel(chart);
			
			this.add(panel, BorderLayout.CENTER);
			
		}
		
		protected void update(Cell cell){
			
			
			if(cell==null){
				JFreeChart chart = ChartFactory.createXYLineChart(null,
						null, null, null);       
				chart.getPlot().setBackgroundPaint(Color.WHITE);
				panel.setChart(chart);

			} else {

				// make an empty chart
				JFreeChart chart = 
						ChartFactory.createXYLineChart(null,
								null, null, null, PlotOrientation.VERTICAL, true, true,
								false);

				XYPlot plot = chart.getXYPlot();
				plot.setBackgroundPaint(Color.WHITE);
				plot.getRangeAxis().setInverted(true);

				// make a hash to track the contents of each dataset produced
				Map<Integer, String> hash = new HashMap<Integer, String>(0); 
				Map<Integer, XYDataset> datasetHash = new HashMap<Integer, XYDataset>(0); 


				// get the nucleus dataset
				XYDataset nucleus = NucleusDatasetCreator.createNucleusOutline(cell);
				hash.put(hash.size(), "Nucleus"); // add to the first free entry
				datasetHash.put(datasetHash.size(), nucleus);


				// get the signals datasets and add each group to the hash
				if(cell.getNucleus().hasSignal()){
					List<DefaultXYDataset> signalsDatasets = NucleusDatasetCreator.createSignalOutlines(cell, activeDataset);

					for(XYDataset d : signalsDatasets){

						String name = "default_0";
						for (int i = 0; i < d.getSeriesCount(); i++) {
							name = (String) d.getSeriesKey(i);	
						}
						int signalGroup = getIndexFromLabel(name);
						hash.put(hash.size(), "SignalGroup_"+signalGroup); // add to the first free entry	
						datasetHash.put(datasetHash.size(), d);
					}
				}

				// get tail datasets if present
				if(cell.hasTail()){

					XYDataset tailBorder = TailDatasetCreator.createTailOutline(cell);
					hash.put(hash.size(), "TailBorder");
					datasetHash.put(datasetHash.size(), tailBorder);
					XYDataset skeleton = TailDatasetCreator.createTailSkeleton(cell);
					hash.put(hash.size(), "TailSkeleton");
					datasetHash.put(datasetHash.size(), skeleton);
				}

				// set the rendering options for each dataset type

				for(int key : hash.keySet()){

					//				IJ.log("Drawing dataset "+hash.get(key));

					plot.setDataset(key, datasetHash.get(key));
					plot.setRenderer(key, new XYLineAndShapeRenderer(true, false));

					int seriesCount = plot.getDataset(key).getSeriesCount();
					// go through each series in the dataset
					for(int i=0; i<seriesCount;i++){

						// all datasets use the same stroke
						plot.getRenderer(key).setSeriesStroke(i, new BasicStroke(2));
						plot.getRenderer(key).setSeriesVisibleInLegend(i, false);

						// nucleus colour
						if(hash.get(key).equals("Nucleus")){

							plot.getRenderer(key).setSeriesPaint(i, Color.BLUE);
						}

						// signal colours
						if(hash.get(key).startsWith("SignalGroup_")){
							int colourIndex = getIndexFromLabel(hash.get(key));
							//						IJ.log("Drawing signal "+i+" of "+seriesCount+" in series group "+colourIndex);
							Color colour = activeDataset.getSignalGroupColour(colourIndex);
							plot.getRenderer(key).setSeriesPaint(i, colour);
						}

						// tail border
						if(hash.get(key).equals("TailBorder")){

							plot.getRenderer(key).setSeriesPaint(i, Color.GREEN);
						}


						// tail skeleton
						if(hash.get(key).equals("TailSkeleton")){

							plot.getRenderer(key).setSeriesPaint(i, Color.BLACK);
						}
					}

				}

				panel.setChart(chart);
			}
		}

	}
	
	protected class CellStatsPanel extends JPanel {
		
		private static final long serialVersionUID = 1L;
		private JTable table; // individual cell stats
		
		private JScrollPane scrollPane;
		
		protected CellStatsPanel(){
			
			this.setLayout(new BorderLayout());
			
			scrollPane = new JScrollPane();
						
			table = new JTable(CellDatasetCreator.createCellInfoTable(null));
			table.setEnabled(false);
			
			table.addMouseListener(new MouseAdapter() {
				@Override
				public void mouseClicked(MouseEvent e) {
					
					JTable table = (JTable) e.getSource();
					
					// double click
					if (e.getClickCount() == 2) {
						int row = table.rowAtPoint((e.getPoint()));

						String value = table.getModel().getValueAt(row+1, 0).toString();
						if(value.equals("Signal group")){
							
							// the group number is in the next row down
							String groupString = table.getModel().getValueAt(row+1, 1).toString();
							int signalGroup = Integer.valueOf(groupString);
							
							Color oldColour = ColourSelecter.getSignalColour( signalGroup-1 );
							
							Color newColor = JColorChooser.showDialog(
				                     CellDetailPanel.this,
				                     "Choose signal Color",
				                     oldColour);
							
							if(newColor != null){
								activeDataset.setSignalGroupColour(signalGroup, newColor);
								updateCell(activeCell);
								fireSignalChangeEvent("SignalColourUpdate");
							}
						}
							
					}

				}
			});
			
			scrollPane.setViewportView(table);
			scrollPane.setColumnHeaderView(table.getTableHeader());
			
			this.add(scrollPane, BorderLayout.CENTER);
		}
		
		protected void update(Cell cell){
			
			if(cell==null){
				table.setModel(CellDatasetCreator.createCellInfoTable(null));
			} else {
				table.setModel(CellDatasetCreator.createCellInfoTable(cell));
				table.getColumnModel().getColumn(1).setCellRenderer(new StatsTableCellRenderer());
			}
		}
	}
	
	protected class SegmentStatsPanel extends JPanel {
		
		private static final long serialVersionUID = 1L;
		private JTable table; // individual cell stats
		
		private JScrollPane scrollPane;
		
		protected SegmentStatsPanel(){
			
			this.setLayout(new BorderLayout());
			
			scrollPane = new JScrollPane();
						
			table = new JTable(NucleusTableDatasetCreator.createSegmentStatsTable(null));
			table.setEnabled(false);
						
			scrollPane.setViewportView(table);
			scrollPane.setColumnHeaderView(table.getTableHeader());
			
			this.add(scrollPane, BorderLayout.CENTER);
		}
		
		protected void update(Cell cell){
			
			if(cell==null){
				table.setModel(NucleusTableDatasetCreator.createSegmentStatsTable(null));
			} else {
				table.setModel(NucleusTableDatasetCreator.createSegmentStatsTable(cell.getNucleus()));

				Enumeration<TableColumn> columns = table.getColumnModel().getColumns();

				while(columns.hasMoreElements()){
					TableColumn column = columns.nextElement();
					column.setCellRenderer(new SegmentTableCellRenderer());
				}
			}
		}
	}

}
