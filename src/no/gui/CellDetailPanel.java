package no.gui;

import ij.IJ;

import java.awt.BasicStroke;
import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.swing.ComboBoxModel;
import javax.swing.DefaultComboBoxModel;
import javax.swing.JComboBox;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTable;

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

public class CellDetailPanel extends JPanel implements ActionListener {

	private static final long serialVersionUID = 1L;

	private JTable cellStatsTable; // individual cell stats
	
	private ChartPanel cellOutlineChartPanel; // holds the chart with the cell
	private JComboBox<String> cellSelectionBox; // choose which cell to look at individually
	private List<AnalysisDataset> list;
	
	
	public CellDetailPanel() {

		this.setLayout(new BorderLayout());
		
		// make the chart for each nucleus
		JFreeChart chart = ChartFactory.createXYLineChart(null,
				null, null, null);       

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
		statsScrollPane.setViewportView(cellStatsTable);
		statsScrollPane.setColumnHeaderView(cellStatsTable.getTableHeader());

		this.add(statsScrollPane, BorderLayout.WEST);
		
	}
	
	public void updateList(List<AnalysisDataset> list){
		this.list = list;
		
		if(list.size()==1){

			ComboBoxModel<String> cellModel = new DefaultComboBoxModel<String>(list.get(0).getCollection().getNucleusPathsAndNumbers());
			cellSelectionBox.setModel(cellModel);
			cellSelectionBox.setSelectedIndex(0);
		} 
	}
	
	private void updateCell(Cell cell, AnalysisDataset dataset){
		
		// update the stats table
		cellStatsTable.setModel(CellDatasetCreator.createCellInfoTable(cell));
		
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
			List<DefaultXYDataset> signalsDatasets = NucleusDatasetCreator.createSignalOutlines(cell, dataset);
			
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

				// nucleus colour
				if(hash.get(key).equals("Nucleus")){

					plot.getRenderer(key).setSeriesPaint(i, Color.BLUE);
				}

				// signal colours
				if(hash.get(key).startsWith("SignalGroup_")){
					
					int colourIndex = getIndexFromLabel(hash.get(key));

					plot.getRenderer(key).setSeriesPaint(i, ColourSelecter.getSignalColour(colourIndex-1, true, 128));
					plot.getRenderer(key).setSeriesVisibleInLegend(i, true);

				}

				// tail border
				if(hash.get(key).equals("TailBorder")){

					plot.getRenderer(key).setSeriesPaint(i, Color.GREEN);
					plot.getRenderer(key).setSeriesVisibleInLegend(i, false);

				}

				
				// tail skeleton
				if(hash.get(key).equals("TailSkeleton")){

					plot.getRenderer(key).setSeriesPaint(i, Color.BLACK);
					plot.getRenderer(key).setSeriesVisibleInLegend(i, false);

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

					AnalysisDataset d = list.get(0);
					Cell cell = d.getCollection().getCell(name);
					
					updateCell(cell, d);
				} catch (Exception e1){
					IJ.log("Error fetching cell: "+e1.getMessage());
					for(StackTraceElement e2 : e1.getStackTrace()){
						IJ.log(e2.toString());
					}
				}
			}

		}
		
	}

}
