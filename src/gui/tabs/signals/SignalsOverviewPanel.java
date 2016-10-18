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
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.util.UUID;
import java.util.logging.Level;

import javax.swing.BoxLayout;
import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JColorChooser;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTable;
import javax.swing.table.DefaultTableModel;
import javax.swing.table.TableModel;

import org.jfree.chart.JFreeChart;

import analysis.AnalysisDataset;
import analysis.signals.ShellRandomDistributionCreator;
import analysis.signals.SignalManager;
import charting.charts.OutlineChartFactory;
import charting.charts.panels.ConsensusNucleusChartPanel;
import charting.charts.panels.ExportableChartPanel;
import charting.datasets.NuclearSignalDatasetCreator;
import charting.datasets.SignalTableCell;
import charting.options.ChartOptions;
import charting.options.ChartOptionsBuilder;
import charting.options.TableOptions;
import charting.options.TableOptions.TableType;
import charting.options.TableOptionsBuilder;
import components.nuclear.NucleusBorderSegment;
import gui.ChartSetEvent;
import gui.ChartSetEventListener;
import gui.InterfaceEvent.InterfaceMethod;
import gui.components.ExportableTable;
import gui.tabs.DetailPanel;

@SuppressWarnings("serial")
public class SignalsOverviewPanel extends DetailPanel implements ActionListener, ChartSetEventListener {

	private ConsensusNucleusChartPanel 	chartPanel; 		// consensus nucleus plus signals
	private ExportableTable 		statsTable;					// table for signal stats
	private JPanel 		consensusAndCheckboxPanel;	// holds the consensus chart and the checkbox
	private JPanel		checkboxPanel;
	
	private JButton warpButton;
	
//	private GenericCheckboxPanel warpPanel = new GenericCheckboxPanel("Warp");
	
	
	public SignalsOverviewPanel(){
		super();
		
		this.setLayout(new BoxLayout(this, BoxLayout.X_AXIS));

		JScrollPane scrollPane = createStatsPane();
		this.add(scrollPane);
		
	
		consensusAndCheckboxPanel = createConsensusPanel();
		this.add(consensusAndCheckboxPanel);
		
	}
	
	private JPanel createConsensusPanel(){
		
		final JPanel panel = new JPanel(new BorderLayout());
		
		ChartOptions options = new ChartOptionsBuilder()
				.build();
		
		JFreeChart chart = null;
		try {
			chart = getChart(options);
		} catch (Exception e1) {
			warn("Error creating blank signals chart");
			log(Level.FINE, "Error creating blank signals chart", e1);
		}
						
		// the chart is inside a chartPanel; the chartPanel is inside a JPanel
		// this allows a checkbox panel to be added to the JPanel later
		chartPanel = new ConsensusNucleusChartPanel(chart);// {
		panel.add(chartPanel, BorderLayout.CENTER);
		chartPanel.setFillConsensus(false);
		
		checkboxPanel = createSignalCheckboxPanel();
		
		panel.add(checkboxPanel, BorderLayout.NORTH);

		return panel;
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
								
				// double click
				if (e.getClickCount() == 2) {

//					String rowName = table.getModel().getValueAt(row, 0).toString();
					String nextRowName = table.getModel().getValueAt(row+1, 0).toString();
					if(nextRowName.equals("Signal group")){
						SignalTableCell signalGroup = getSignalGroupFromTable(table, row+1, column);
						updateSignalColour( signalGroup );
					}
											
				}

			}
		});
		
		JScrollPane scrollPane = new JScrollPane(statsTable);
		return scrollPane;
	}
	
	private SignalTableCell getSignalGroupFromTable(JTable table, int row, int column){
		return (SignalTableCell) table.getModel().getValueAt(row, column);
	}
	
	
	/**
	 * Update the colour of the clicked signal group
	 * @param row the row selected (the colour bar, one above the group name)
	 */
	private void updateSignalColour(SignalTableCell signalGroup){
        Color oldColour = signalGroup.getColor();
		
		Color newColor = JColorChooser.showDialog(
                 this,
                 "Choose signal Color",
                 oldColour);
		
		if(newColor != null){
            activeDataset().getCollection().getSignalGroup(signalGroup.getID()).setGroupColour(newColor);
			this.update(getDatasets());
			fireInterfaceEvent(InterfaceMethod.RECACHE_CHARTS);
		}
	}
			
	/**
	 * Create the checkboxes that set each signal channel visible or not
	 */
	private JPanel createSignalCheckboxPanel(){
		JPanel panel = new JPanel();
		
		panel.setLayout(new BoxLayout(panel, BoxLayout.X_AXIS));
		

		if(isSingleDataset()){
			
		
            for(UUID signalGroup : activeDataset().getCollection().getSignalGroupIDs()){
            	
            	if(signalGroup.equals(ShellRandomDistributionCreator.RANDOM_SIGNAL_ID)){
            		continue;
            	}

            	// get the status within each dataset
                boolean visible = activeDataset().getCollection().getSignalGroup(signalGroup).isVisible();

				String name = activeDataset().getCollection().getSignalManager().getSignalGroupName(signalGroup);
				
				// make a checkbox for each signal group in the dataset
				JCheckBox box = new JCheckBox(name, visible);

				// Don't enable when the consensus is missing
                if(activeDataset().getCollection().hasConsensusNucleus()){
                    box.setEnabled(true);
                } else {
                    box.setEnabled(false);
                }


				// apply the appropriate action 
				box.setActionCommand("GroupVisble_"+signalGroup);
				box.addActionListener(this);
				panel.add(box);

			}


		}
		
		warpButton = new JButton("Warp signals");
		warpButton.setToolTipText("Requires consensus nucleus refolded, at least one dataset with signals, and all datasets to have matching segments");
		warpButton.addActionListener( e -> { 
			
				new SignalWarpingDialog(  getDatasets() );
			}  
		);

		warpButton.setEnabled(false);
		

		panel.add(warpButton);
		return panel;
	}
		
	/**
	 * Update the signal stats with the given datasets
	 * @param list the datasets
	 * @throws Exception 
	 */
	private void updateSignalStatsPanel() {
		
		TableOptions options = new TableOptionsBuilder()
			.setDatasets(getDatasets())
			.setType(TableType.SIGNAL_STATS_TABLE)
			.build();
		
		TableModel model = getTable(options);

		statsTable.setModel(model);

		// Add the signal group colours
		if(hasDatasets()){
			int columns = statsTable.getColumnModel().getColumnCount();
			if(columns>1){
				for(int i=1;i<columns;i++){
                    statsTable.getColumnModel().getColumn(i).setCellRenderer(new SignalTableCellRenderer());
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
			
			if(activeDataset().getCollection().hasConsensusNucleus()
					&& activeDataset().getCollection().getSignalManager().hasSignals()){
				warpButton.setEnabled(true);
			}

		}
		
		if(isMultipleDatasets()){
			if(AnalysisDataset.haveConsensusNuclei(getDatasets())){
				
				// Check at least one of the selected datasets has signals
				boolean hasSignals = false;
				for(AnalysisDataset d : getDatasets()){
				
					SignalManager m =  d.getCollection().getSignalManager();
					if(m.hasSignals()){
						hasSignals = true;
						break;
					}
				}
				
				// Segments need to match for mesh creation
				boolean segmentsMatch = NucleusBorderSegment.segmentCountsMatch(getDatasets());		
				
				if(hasSignals && segmentsMatch){
					warpButton.setEnabled(true);
				} else {
					warpButton.setEnabled(false);
				}
				
				
			} else {
				warpButton.setEnabled(false);
			}
		}
	}
	
	
	private void updateSignalConsensusChart(){
		try {

			// The options do not hold which signal groups are visible
			// so we must invalidate the cache whenever they change
			this.clearChartCache(getDatasets());
			
			ChartOptions options = new ChartOptionsBuilder()
					.setDatasets(getDatasets())
					.setShowWarp(false)
					.setTarget(chartPanel)
					.build();
			
			setChart(options);		
			
		} catch(Exception e){
			warn("Error updating signal overview panel");
			log(Level.FINE, "Error updating signal overview panel", e);
		}
	}
	
	

	private UUID getSignalGroupFromLabel(String label){
		String[] names = label.split("_");
		return UUID.fromString(names[1]);
	}

	@Override
	public void actionPerformed(ActionEvent e) {
		if(e.getActionCommand().startsWith("GroupVisble_")){
			
			UUID signalGroup = getSignalGroupFromLabel(e.getActionCommand());
			JCheckBox box = (JCheckBox) e.getSource();
            activeDataset().getCollection().getSignalGroup(signalGroup).setVisible( box.isSelected());
			fireSignalChangeEvent("GroupVisble_");
			this.refreshChartCache(getDatasets());
		}
		updateSignalConsensusChart();
		
	}

	@Override
	protected void updateSingle() {
		updateMultiple();
		
	}

	@Override
	protected void updateMultiple() {
		
		updateCheckboxPanel();
		updateSignalConsensusChart();
		updateSignalStatsPanel();		
	}

	@Override
	protected void updateNull() {
		updateMultiple();
		
	}
	
	@Override
	protected JFreeChart createPanelChartType(ChartOptions options) throws Exception {
		return new OutlineChartFactory(options).makeSignalOutlineChart();
	}
	
	@Override
	protected TableModel createPanelTableType(TableOptions options) throws Exception{
		return NuclearSignalDatasetCreator.getInstance().createSignalStatsTable(options);
	}

	@Override
	public void chartSetEventReceived(ChartSetEvent e) {
		((ExportableChartPanel) e.getSource()).restoreAutoBounds();
		
	}
}
