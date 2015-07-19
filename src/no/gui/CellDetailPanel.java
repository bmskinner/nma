package no.gui;

import ij.IJ;

import java.awt.BasicStroke;
import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.beans.PropertyChangeSupport;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import javax.swing.ComboBoxModel;
import javax.swing.DefaultComboBoxModel;
import javax.swing.JColorChooser;
import javax.swing.JComboBox;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTable;
import javax.swing.event.ChangeEvent;

import no.analysis.AnalysisDataset;

import org.jfree.chart.ChartFactory;
import org.jfree.chart.ChartPanel;
import org.jfree.chart.JFreeChart;
import org.jfree.chart.plot.PlotOrientation;
import org.jfree.chart.plot.XYPlot;
import org.jfree.chart.renderer.xy.XYLineAndShapeRenderer;
import org.jfree.data.xy.DefaultXYDataset;
import org.jfree.data.xy.XYDataset;

import cell.Cell;
import datasets.CellDatasetCreator;
import datasets.NucleusDatasetCreator;
import datasets.TailDatasetCreator;

public class CellDetailPanel extends JPanel implements ActionListener, SignalChangeListener {

	private static final long serialVersionUID = 1L;

	private JTable cellStatsTable; // individual cell stats
	
	private ChartPanel cellOutlineChartPanel; // holds the chart with the cell
	private JComboBox<String> cellSelectionBox; // choose which cell to look at individually
	private List<AnalysisDataset> list;
	private AnalysisDataset activeDataset;	
	private Cell activeCell;
	
	private List<Object> listeners = new ArrayList<Object>();
	
	public CellDetailPanel() {

		this.setLayout(new BorderLayout());
		
		// make the chart for each nucleus
		JFreeChart chart = ChartFactory.createXYLineChart(null,
				null, null, null);       
		chart.getPlot().setBackgroundPaint(Color.WHITE);

		cellOutlineChartPanel = new ChartPanel(chart);
		this.add(cellOutlineChartPanel, BorderLayout.CENTER);
		
		// make the combobox for selecting nuclei
		cellSelectionBox = new JComboBox<String>();
		cellSelectionBox.setActionCommand("CellSelectionChoice");
		cellSelectionBox.addActionListener(this);
		this.add(cellSelectionBox, BorderLayout.NORTH);
		
		// make the stats panel

		JScrollPane statsScrollPane = new JScrollPane();
		
		cellStatsTable = new JTable(CellDatasetCreator.createCellInfoTable(null));
		cellStatsTable.setEnabled(false);
		
		cellStatsTable.addMouseListener(new MouseAdapter() {
			@Override
			public void mouseClicked(MouseEvent e) {
				
				JTable table = (JTable) e.getSource();
				
				// double click
				if (e.getClickCount() == 2) {
					int row = table.rowAtPoint((e.getPoint()));

					String value = table.getModel().getValueAt(row+1, 0).toString();
					if(value.equals("Signal group")){
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
							fireSignalChangeEvent();
						}
					}
						
				}

			}
		});
		
		
		statsScrollPane.setViewportView(cellStatsTable);
		statsScrollPane.setColumnHeaderView(cellStatsTable.getTableHeader());

		this.add(statsScrollPane, BorderLayout.WEST);
		
	}
	
	public void updateList(List<AnalysisDataset> list){
		this.list = list;
		
		if(list.size()==1){
			activeDataset = list.get(0);
			ComboBoxModel<String> cellModel = new DefaultComboBoxModel<String>(activeDataset.getCollection().getNucleusPathsAndNumbers());
			cellSelectionBox.setModel(cellModel);
			cellSelectionBox.setSelectedIndex(0);
		} 
	}
	
	private void updateCell(Cell cell){
		
		// update the stats table
		cellStatsTable.setModel(CellDatasetCreator.createCellInfoTable(cell));
		cellStatsTable.getColumnModel().getColumn(1).setCellRenderer(new StatsTableCellRenderer());

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

			plot.setDataset(key, datasetHash.get(key));
			plot.setRenderer(key, new XYLineAndShapeRenderer(true, false));
			

			// go through each series in the dataset
			for(int i=0; i<plot.getDataset(key).getSeriesCount();i++){
				
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
		
		
		this.cellOutlineChartPanel.setChart(chart);
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
     
    private synchronized void fireSignalChangeEvent() {
        SignalChangeEvent event = new SignalChangeEvent( this, "SignalColourUpdate" );
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


	@Override
	public void signalChangeReceived(SignalChangeEvent event) {
		if(event.type().equals("SignalColourUpdate")){
			updateCell(activeCell);
		}

	}

}
