/*******************************************************************************
 *  	Copyright (C) 2016 Ben Skinner
 *   
 *     This file is part of Nuclear Morphology Analysis.
 *
 *     Nuclear Morphology Analysis is free software: you can redistribute it and/or modify
 *     it under the terms of the GNU General Public License as published by
 *     the Free Software Foundation, either version 3 of the License, or
 *     (at your option) any later version.
 *
 *     Nuclear Morphology Analysis is distributed in the hope that it will be useful,
 *     but WITHOUT ANY WARRANTY; without even the implied warranty of
 *     MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 *     GNU General Public License for more details.
 *
 *     You should have received a copy of the GNU General Public License
 *     along with Nuclear Morphology Analysis. If not, see <http://www.gnu.org/licenses/>.
 *******************************************************************************/
package gui.tabs.signals;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.ComponentAdapter;
import java.awt.event.ComponentEvent;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.io.File;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;

import javax.swing.BoxLayout;
import javax.swing.JCheckBox;
import javax.swing.JColorChooser;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTable;
import javax.swing.SwingUtilities;
import javax.swing.table.DefaultTableModel;
import javax.swing.table.TableModel;

import org.jfree.chart.ChartFactory;
import org.jfree.chart.ChartPanel;
import org.jfree.chart.JFreeChart;
import org.jfree.chart.plot.XYPlot;
import org.jfree.data.xy.XYDataset;

import analysis.AnalysisDataset;
import analysis.SignalManager;
import charting.charts.ConsensusNucleusChartFactory;
import charting.charts.MorphologyChartFactory;
import charting.charts.OutlineChartFactory;
import charting.datasets.NuclearSignalDatasetCreator;
import charting.options.ChartOptions;
import charting.options.TableOptions;
import charting.options.TableOptions.TableType;
import charting.options.TableOptionsBuilder;
import components.CellCollection;
import gui.InterfaceEvent.InterfaceMethod;
import gui.components.ColourSelecter;
import gui.components.ConsensusNucleusChartPanel;
import gui.components.ExportableTable;
import gui.tabs.DetailPanel;
import gui.tabs.SignalsDetailPanel;
import ij.io.DirectoryChooser;
import stats.SignalStatistic;

@SuppressWarnings("serial")
public class SignalsOverviewPanel extends DetailPanel implements ActionListener {

	private ConsensusNucleusChartPanel 	chartPanel; 		// consensus nucleus plus signals
	private ExportableTable 		statsTable;					// table for signal stats
	private JPanel 		consensusAndCheckboxPanel;	// holds the consensus chart and the checkbox
	private JPanel		checkboxPanel;
	
	
	public SignalsOverviewPanel(){
		super();
		
		this.setLayout(new BoxLayout(this, BoxLayout.X_AXIS));

		JScrollPane scrollPane = createStatsPane();
		this.add(scrollPane);
		
	
		consensusAndCheckboxPanel = createConsensusPanel();
		this.add(consensusAndCheckboxPanel);
		
	}
	
	private JScrollPane createStatsPane(){
		DefaultTableModel tableModel = new DefaultTableModel();
		tableModel.addColumn("");
		tableModel.addColumn("");
		statsTable = new ExportableTable(); // table  for basic stats
		statsTable.setModel(tableModel);
		statsTable.setEnabled(false);
		
		statsTable.addMouseListener(new MouseAdapter() {
			@Override
			public void mouseClicked(MouseEvent e) {
				
				JTable table = (JTable) e.getSource();
				
				int row = table.rowAtPoint(e.getPoint());
				int column = table.columnAtPoint(e.getPoint());
				
//				int signalGroupRow = 0;
//				int signalGroup = 0;
//				int rowsPerSignalGroup = 7 + SignalStatistic.values().length;
//				if(row>0){
//					signalGroupRow = row - (row % rowsPerSignalGroup);
//					signalGroup = (Integer) table.getModel().getValueAt(signalGroupRow, column);
//				}
				
				// double click
				if (e.getClickCount() == 2) {

					String rowName = table.getModel().getValueAt(row, 0).toString();
					String nextRowName = table.getModel().getValueAt(row+1, 0).toString();
					if(nextRowName.equals("Signal group")){
						int signalGroup = (int) table.getModel().getValueAt(row+1, 1);
						updateSignalColour( signalGroup );
					}
					
					if(rowName.equals("Source")){
						int signalGroup = (int) table.getModel().getValueAt(row-3, 1);
						updateSignalSource( signalGroup );
					}
						
				}

			}
		});
		
		JScrollPane scrollPane = new JScrollPane(statsTable);
		return scrollPane;
	}
	
	private void updateSignalSource(int signalGroup){
		if(isSingleDataset()){
			programLogger.log(Level.FINEST, "Updating signal source for signal group "+signalGroup);

			DirectoryChooser openDialog = new DirectoryChooser("Select directory of signal images...");
			String folderName = openDialog.getDirectory();

			if(folderName==null){
				programLogger.log(Level.FINEST, "Folder name null");
				return;
			}

			File folder =  new File(folderName);

			if(!folder.isDirectory() ){
				programLogger.log(Level.FINEST, "Folder is not directory");
				return;
			}
			if(!folder.exists()){
				programLogger.log(Level.FINEST, "Folder does not exist");
				return;
			}

			activeDataset().getCollection().getSignalManager().updateSignalSourceFolder(signalGroup, folder);
//			SignalsDetailPanel.this.update(getDatasets());
			refreshTableCache();
			programLogger.log(Level.FINEST, "Updated signal source for signal group "+signalGroup+" to "+folder.getAbsolutePath() );
		}
	}
	
	/**
	 * Update the colour of the clicked signal group
	 * @param row the row selected (the colour bar, one above the group name)
	 */
	private void updateSignalColour(int signalGroup){
		
		Color oldColour = ColourSelecter.getSignalColour( signalGroup-1 );
		
		Color newColor = JColorChooser.showDialog(
                 this,
                 "Choose signal Color",
                 oldColour);
		
		if(newColor != null){
			activeDataset().setSignalGroupColour(signalGroup, newColor);
			this.update(getDatasets());
//			fireSignalChangeEvent("SignalColourUpdate");
			fireInterfaceEvent(InterfaceMethod.RECACHE_CHARTS);
		}
	}
	
	private JPanel createConsensusPanel(){
		
		final JPanel panel = new JPanel(new BorderLayout());
		// make a blank chart for signal locations on a consensus nucleus
		JFreeChart signalsChart = ChartFactory.createXYLineChart(null,  // chart for conseusns
				null, null, null);
		XYPlot signalsPlot = signalsChart.getXYPlot();

		signalsPlot.setBackgroundPaint(Color.WHITE);
		signalsPlot.getDomainAxis().setVisible(false);
		signalsPlot.getRangeAxis().setVisible(false);
				
		// the chart is inside a chartPanel; the chartPanel is inside a JPanel
		// this allows a checkbox panel to be added to the JPanel later
		chartPanel = new ConsensusNucleusChartPanel(signalsChart);// {
		panel.add(chartPanel, BorderLayout.CENTER);
		
		
		chartPanel.addComponentListener(new ComponentAdapter() {
			@Override
			public void componentResized(ComponentEvent e) {
				resizePreview(chartPanel, panel);
				chartPanel.restoreAutoBounds();
			}
		});
		
		panel.addComponentListener(new ComponentAdapter() {
			@Override
			public void componentResized(ComponentEvent e) {
				resizePreview(chartPanel, panel);
			}
		});
		
		
		checkboxPanel = createSignalCheckboxPanel();
		
		panel.add(checkboxPanel, BorderLayout.NORTH);

		return panel;
	}
	
	private static void resizePreview(ChartPanel innerPanel, JPanel container) {
        int w = container.getWidth();
        int h = container.getHeight();
        int size =  Math.min(w, h);
        innerPanel.setPreferredSize(new Dimension(size, size));
        container.revalidate();
    }
	
	/**
	 * Create the checkboxes that set each signal channel visible or not
	 */
	private JPanel createSignalCheckboxPanel(){
		JPanel panel = new JPanel();
		
		panel.setLayout(new BoxLayout(panel, BoxLayout.X_AXIS));
		
		if(isSingleDataset()){
			try {

				for(int signalGroup : activeDataset().getCollection().getSignalManager().getSignalGroups()){

					boolean visible = activeDataset().isSignalGroupVisible(signalGroup);

					String name = activeDataset().getCollection().getSignalManager().getSignalGroupName(signalGroup);
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
				programLogger.log(Level.SEVERE, "Error creating signal checkboxes", e);
			}
		}
		return panel;
	}
	
//	protected void updateDetail(){
//		
//		programLogger.log(Level.FINE, "Updating signals detail panel");
//		SwingUtilities.invokeLater(new Runnable(){
//			public void run(){
//				
//				try{
//					updateCheckboxPanel();
//					updateSignalConsensusChart();
//					updateSignalStatsPanel();
//
//				} catch(Exception e){
//					programLogger.log(Level.SEVERE, "Error updating signals overview panel" ,e);
//					update( (List<AnalysisDataset>) null);
//				} finally {
//					setUpdating(false);
//				}
//			
//		}});
//	}
	
	/**
	 * Update the signal stats with the given datasets
	 * @param list the datasets
	 * @throws Exception 
	 */
	private void updateSignalStatsPanel() throws Exception{
		
//		TableModel model = NuclearSignalDatasetCreator.createSignalStatsTable(null);
		
		TableOptions options = new TableOptionsBuilder()
			.setDatasets(getDatasets())
			.setLogger(programLogger)
			.setType(TableType.SIGNAL_STATS_TABLE)
			.build();
		
		TableModel model = getTable(options);
		statsTable.setModel(model);

		// Add the signal group colours
		if(hasDatasets()){
			int columns = statsTable.getColumnModel().getColumnCount();
			if(columns>1){
				for(int i=1;i<columns;i++){
					statsTable.getColumnModel().getColumn(i).setCellRenderer(new StatsTableCellRenderer());
				}
			}
		}
			
		
	}
	
	private void updateCheckboxPanel(){
		if(isSingleDataset()){
							
			// make a new panel for the active dataset
			consensusAndCheckboxPanel.remove(checkboxPanel);
			checkboxPanel = createSignalCheckboxPanel();

			// add this new panel
			consensusAndCheckboxPanel.add(checkboxPanel, BorderLayout.NORTH);
			consensusAndCheckboxPanel.revalidate();
			consensusAndCheckboxPanel.repaint();
			consensusAndCheckboxPanel.setVisible(true);
		}
	}
	
	
	private void updateSignalConsensusChart(){
		try {
			JFreeChart chart;
			if(isSingleDataset()){

				chart = OutlineChartFactory.makeSignalCoMNucleusOutlineChart(activeDataset());

			} else { 
	
				chart = ConsensusNucleusChartFactory.makeEmptyNucleusOutlineChart();

			}
			
			chartPanel.setChart(chart);
			chartPanel.restoreAutoBounds();
		} catch(Exception e){
			programLogger.log(Level.SEVERE, "Error updating signals", e);
		}
	}
	
	/**
	 * Allows for cell background to be coloured based on poition in a list. Used to colour
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
					colour = activeDataset().getSignalGroupColour(Integer.valueOf(groupString));
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
	public void actionPerformed(ActionEvent e) {
		if(e.getActionCommand().startsWith("GroupVisble_")){
			
			int signalGroup = this.getIndexFromLabel(e.getActionCommand());
			JCheckBox box = (JCheckBox) e.getSource();
			activeDataset().setSignalGroupVisible(signalGroup, box.isSelected());
			fireSignalChangeEvent("GroupVisble_");
		}
		
	}

	@Override
	protected void updateSingle() throws Exception {
		updateMultiple();
		
	}

	@Override
	protected void updateMultiple() throws Exception {
		updateCheckboxPanel();
		updateSignalConsensusChart();
		updateSignalStatsPanel();
		
	}

	@Override
	protected void updateNull() throws Exception {
		updateMultiple();
		
	}
	
	@Override
	protected JFreeChart createPanelChartType(ChartOptions options) throws Exception {
		return null;
	}
	
	@Override
	protected TableModel createPanelTableType(TableOptions options) throws Exception{
		return NuclearSignalDatasetCreator.createSignalStatsTable(options);
	}
}
