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
package gui.tabs;

import gui.DatasetEvent;
import gui.Labels;
import gui.components.AnalysisTableCellRenderer;
import gui.components.ExportableTable;

import java.awt.BorderLayout;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.util.ArrayList;
import java.util.List;
import java.util.logging.Level;

import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.JButton;
import javax.swing.JComponent;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.table.TableModel;

import org.jfree.chart.JFreeChart;

import charting.datasets.AbstractDatasetCreator;
import charting.datasets.AnalysisDatasetTableCreator;
import charting.options.ChartOptions;
import charting.options.TableOptions;
import charting.options.TableOptionsBuilder;
import charting.options.DefaultTableOptions.TableType;
import analysis.IAnalysisDataset;

/**
 * This panel shows any merge sources for a merged dataset, and
 * the analysis options used to create the merge
 * @author bms41
 * @since 1.9.0
 *
 */
@SuppressWarnings("serial")
public class MergesDetailPanel extends DetailPanel {
		
	private ExportableTable sourceParametersTable;
	
	private JPanel sourceButtonPanel;
	
	private JLabel headerLabel = new JLabel(Labels.NULL_DATASETS);
	
	private static final String RECOVER_BUTTON_TEXT = "Recover source";
	
	private JPanel mainPanel;

	
	public MergesDetailPanel(){
		super();

		try {

			createUI();
			
		} catch (Exception e){
			log(Level.SEVERE, "Error creating merge panel", e);
		}
	}
	
	private void createUI() throws Exception{
		
		/*
		 * The header is currently an empty panel
		 */
		this.setLayout(new BorderLayout());
		JPanel headerPanel = createHeaderPanel();
		this.add(headerPanel, BorderLayout.NORTH);
		
		/*
		 * Make a vertical box panel to hold the table and recover
		 * buttons
		 */
		
		mainPanel = new JPanel();
		mainPanel.setLayout(new BoxLayout(mainPanel, BoxLayout.Y_AXIS));
		

		JPanel parameters = createAnalysisParametersPanel();
		JScrollPane paramScrollPane = new JScrollPane(parameters);
		
		
		paramScrollPane.setColumnHeaderView(sourceParametersTable.getTableHeader());
		
		sourceButtonPanel = createGetSourcePanel(null);
		
		
		mainPanel.add(paramScrollPane);
		mainPanel.add(sourceButtonPanel);
		
		this.add(mainPanel, BorderLayout.CENTER);
		
	}
	
	private JPanel createGetSourcePanel(List<JComponent> buttons){
		JPanel panel = new JPanel();
		
		GridBagLayout gbl = new GridBagLayout();
		panel.setLayout(gbl);
		
		GridBagConstraints c = new GridBagConstraints();
		
		c.anchor = GridBagConstraints.CENTER; // place the buttons in the middle of their grid
		c.gridwidth = buttons==null ? 1 : buttons.size()+1; // one button per column, plus a blank
		c.gridheight = 1;
		c.fill = GridBagConstraints.NONE;      // don't resize the buttons
		c.weightx = 1.0; 						// buttons have padding between them
		
		/*
		 * Add a blank box to cover the first column
		 */
		Dimension fillerSize = new Dimension(100, 5);
		panel.add(new Box.Filler(fillerSize, fillerSize, fillerSize), c);
		
		/*
		 * Add the buttons
		 */
		if(buttons!=null){
			for(JComponent button : buttons){
				panel.add(button, c);
			}
		}

		return panel;
	}
	
	private List<JComponent> createGetSourceButtons(){
		 
		if(!hasDatasets()){
			return null;
		}
		
		List<JComponent> result = new  ArrayList<JComponent>(); 

		for(final IAnalysisDataset source : activeDataset().getAllMergeSources()){

			JButton button = new JButton(RECOVER_BUTTON_TEXT);
			button.addActionListener( e -> {
				fireDatasetEvent(DatasetEvent.EXTRACT_SOURCE, source);
			});
 
			result.add(button);

		}
		
		return result;
	}
	
	@Override
	public void setChartsAndTablesLoading(){
		super.setChartsAndTablesLoading();
		sourceParametersTable.setModel(AbstractDatasetCreator.createLoadingTable());
	}
	
	private JPanel createAnalysisParametersPanel() throws Exception{
		JPanel panel = new JPanel(new BorderLayout());
		
		
		TableModel model = AbstractDatasetCreator.createBlankTable();
	
		sourceParametersTable =  new ExportableTable(model);
		panel.add(sourceParametersTable, BorderLayout.CENTER);
		return panel;
	}
	
	
	private JPanel createHeaderPanel() throws Exception{
		JPanel panel = new JPanel();
		panel.add(headerLabel);
		return panel;
		
	}
	
	private void updateSourceButtonsPanel(){
		
		mainPanel.remove(sourceButtonPanel);
		
		List<JComponent> buttons = createGetSourceButtons();
		
		
		sourceButtonPanel = createGetSourcePanel(buttons);

		// add this new panel
		mainPanel.add(sourceButtonPanel);
		mainPanel.revalidate();
		mainPanel.repaint();
		mainPanel.setVisible(true);
	}
	
	@Override
	protected void updateSingle()  {

		headerLabel.setText(Labels.SINGLE_DATASET
				+" with "
				+activeDataset().getAllMergeSources().size()
				+" merge sources");
		updateSourceButtonsPanel();
		
		List<IAnalysisDataset> mergeSources = new ArrayList<IAnalysisDataset>(activeDataset().getAllMergeSources());
		
		TableOptions options = new TableOptionsBuilder()
			.setDatasets(mergeSources)
			.setType(TableType.ANALYSIS_PARAMETERS)
			.setTarget(sourceParametersTable)
			.setRenderer(TableOptions.ALL_EXCEPT_FIRST_COLUMN, new AnalysisTableCellRenderer())
			.build();
		
		setTable(options);
		
	}
	
	@Override
	protected void updateMultiple() {
		updateNull();
		headerLabel.setText(Labels.MULTIPLE_DATASETS);
	}
	
	@Override
	protected void updateNull() {
		headerLabel.setText(Labels.NULL_DATASETS);
		sourceButtonPanel.setVisible(false);
			
		
		TableOptions options = new TableOptionsBuilder()
			.setDatasets(null)
			.setType(TableType.ANALYSIS_PARAMETERS)
			.setTarget(sourceParametersTable)
			.build();
		
		setTable(options);
	}
	
	@Override
	protected JFreeChart createPanelChartType(ChartOptions options){
		return null;
	}
	
	@Override
	protected TableModel createPanelTableType(TableOptions options){
		if(options.getType().equals(TableType.MERGE_SOURCES)){
			return new AnalysisDatasetTableCreator(options).createMergeSourcesTable();
		} else {
			return new AnalysisDatasetTableCreator(options).createAnalysisTable();
		}
	}
}
