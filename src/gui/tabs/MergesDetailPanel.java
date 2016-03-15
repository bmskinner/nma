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

import gui.DatasetEvent.DatasetMethod;
import gui.components.AnalysisTableCellRenderer;
import gui.components.ExportableTable;
import java.awt.BorderLayout;
import java.awt.Dimension;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.ArrayList;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;

import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.JButton;
import javax.swing.JComponent;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.table.TableModel;

import org.jfree.chart.JFreeChart;

import charting.datasets.NucleusTableDatasetCreator;
import charting.options.ChartOptions;
import charting.options.TableOptions;
import charting.options.TableOptionsBuilder;
import charting.options.TableOptions.TableType;
import analysis.AnalysisDataset;

@SuppressWarnings("serial")
public class MergesDetailPanel extends DetailPanel {
		
	private ExportableTable sourceParametersTable;
	
	private JPanel		getSourceButtonPanel;
	
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
		
		getSourceButtonPanel = createGetSourcePanel(null);
		
		mainPanel.add(paramScrollPane);
		mainPanel.add(getSourceButtonPanel);
		
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

		for(final AnalysisDataset source : activeDataset().getAllMergeSources()){

			JButton button = new JButton(RECOVER_BUTTON_TEXT);
			button.addActionListener( new ActionListener() {

				public void actionPerformed(ActionEvent e) {
					Thread thr = new Thread(){
						public void run(){

							List<AnalysisDataset> list = new ArrayList<AnalysisDataset>();
							list.add(source);

							fireDatasetEvent(DatasetMethod.EXTRACT_SOURCE, list);
						}};
						thr.start();
				}
			});    
			result.add(button);

		}
		
		return result;
	}
	
	private JPanel createAnalysisParametersPanel() throws Exception{
		JPanel panel = new JPanel(new BorderLayout());
		
		TableOptions options = new TableOptionsBuilder()
		.setDatasets(null)
		.setType(TableType.ANALYSIS_PARAMETERS)
		.build();
		
		TableModel model = getTable(options);
		
		sourceParametersTable =  new ExportableTable(model);
		panel.add(sourceParametersTable, BorderLayout.CENTER);
		return panel;
	}
	
	
	private JPanel createHeaderPanel() throws Exception{
		JPanel panel = new JPanel();
		
		return panel;
		
	}
	
	private void updateSourceButtonsPanel(){
		
		mainPanel.remove(getSourceButtonPanel);
		
		List<JComponent> buttons = createGetSourceButtons();
		
		
		getSourceButtonPanel = createGetSourcePanel(buttons);

		// add this new panel
		mainPanel.add(getSourceButtonPanel);
		mainPanel.revalidate();
		mainPanel.repaint();
		mainPanel.setVisible(true);
	}
	
	/**
	 * This method must be overridden by the extending class
	 * to perform the actual update when a single dataset is selected
	 */
	protected void updateSingle() throws Exception {

//		TableOptions options = new TableOptionsBuilder()
//		.setDatasets(getDatasets())
//		.setLogger(programLogger)
//		.setType(TableType.MERGE_SOURCES)
//		.build();
//
//		TableModel model = getTable(options);

//		mergeSources.setModel(model);

		updateSourceButtonsPanel();
		
		
		TableOptions parameterOptions = new TableOptionsBuilder()
		.setDatasets(activeDataset().getAllMergeSources())
		.setType(TableType.ANALYSIS_PARAMETERS)
		.build();
		
		TableModel parameterModel = getTable(parameterOptions);
		
		sourceParametersTable.setModel(parameterModel);
		setRenderer(sourceParametersTable, new AnalysisTableCellRenderer());
		
	}
	
	/**
	 * This method must be overridden by the extending class
	 * to perform the actual update when a multiple datasets are selected
	 */
	protected void updateMultiple() throws Exception {
		updateNull();
	}
	
	/**
	 * This method must be overridden by the extending class
	 * to perform the actual update when a no datasets are selected
	 */
	protected void updateNull() throws Exception {
		getSourceButtonPanel.setVisible(false);
		
//		TableOptions options = new TableOptionsBuilder()
//		.setDatasets(null)
//		.setType(TableType.MERGE_SOURCES)
//		.setLogger(programLogger)
//		.build();
//		
//		TableModel model = getTable(options);
		
		
		TableOptions parameterOptions = new TableOptionsBuilder()
		.setDatasets(null)
		.setType(TableType.ANALYSIS_PARAMETERS)
		.build();
		
		TableModel parameterModel = getTable(parameterOptions);
		
		sourceParametersTable.setModel(parameterModel);
	}
	
	@Override
	protected JFreeChart createPanelChartType(ChartOptions options) throws Exception {
		return null;
	}
	
	@Override
	protected TableModel createPanelTableType(TableOptions options) throws Exception{
		if(options.getType().equals(TableType.MERGE_SOURCES)){
			return NucleusTableDatasetCreator.createMergeSourcesTable(options);
		} else {
			return NucleusTableDatasetCreator.createAnalysisTable(options);
		}
	}
}
