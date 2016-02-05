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
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.Vector;
import java.util.logging.Level;
import java.util.logging.Logger;

import javax.swing.JButton;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.SwingUtilities;
import javax.swing.table.DefaultTableModel;
import javax.swing.table.TableModel;

import org.jfree.chart.JFreeChart;

import charting.charts.MorphologyChartFactory;
import charting.datasets.NucleusTableDatasetCreator;
import charting.options.ChartOptions;
import charting.options.TableOptions;
import charting.options.TableOptionsBuilder;
import charting.options.TableOptions.TableType;
import analysis.AnalysisDataset;

@SuppressWarnings("serial")
public class MergesDetailPanel extends DetailPanel {
	
	private ExportableTable		mergeSources;
	
	private ExportableTable sourceParametersTable;
	
	private JButton		getSourceButton = new JButton("Recover source");
	
	public MergesDetailPanel(Logger programLogger){
		super(programLogger);
		this.setLayout(new BorderLayout());
		
		
		try {

			createUI();
			
		} catch (Exception e){
			programLogger.log(Level.SEVERE, "Error creating merge panel", e);
		}
	}
	
	private void createUI() throws Exception{
		JPanel headerPanel = createHeaderPanel();
		
		this.add(headerPanel, BorderLayout.NORTH);
		
		JPanel content = new JPanel(new BorderLayout());

		JPanel parameters = createAnalysisParametersPanel();
		
		
		JScrollPane paramScrollPane = new JScrollPane(parameters);
		
		
		paramScrollPane.setColumnHeaderView(sourceParametersTable.getTableHeader());
		
		
		content.add(paramScrollPane, BorderLayout.CENTER);
		
		this.add(content, BorderLayout.CENTER);
		
	}
	
	private JPanel createAnalysisParametersPanel() throws Exception{
		JPanel panel = new JPanel(new BorderLayout());
		
		TableOptions options = new TableOptionsBuilder()
		.setDatasets(null)
		.setLogger(programLogger)
		.setType(TableType.ANALYSIS_PARAMETERS)
		.build();
		
		TableModel model = getTable(options);
		
		sourceParametersTable =  new ExportableTable(model);
		panel.add(sourceParametersTable, BorderLayout.CENTER);
		return panel;
	}
	
	private JPanel createMergeSourcePanel() throws Exception{
		JPanel panel = new JPanel(new BorderLayout());
		TableOptions options = new TableOptionsBuilder()
		.setDatasets(null)
		.setType(TableType.MERGE_SOURCES)
		.setLogger(programLogger)
		.build();

		TableModel model = getTable(options);


		mergeSources = new ExportableTable(model){
			@Override
			public boolean isCellEditable(int rowIndex, int columnIndex) {
				return false;
			}
		};
		mergeSources.setEnabled(true);
		mergeSources.setCellSelectionEnabled(false);
		mergeSources.setColumnSelectionAllowed(false);
		mergeSources.setRowSelectionAllowed(true);

		panel.add(mergeSources, BorderLayout.CENTER);
		return panel;
	}
	
	private JPanel createHeaderPanel() throws Exception{
		JPanel panel = new JPanel();
		
		JPanel merges     = createMergeSourcePanel();
		JScrollPane mergeScrollPane = new JScrollPane(merges);
		mergeScrollPane.setColumnHeaderView(mergeSources.getTableHeader());
		panel.add(mergeScrollPane);
		
		getSourceButton.addMouseListener(new MouseAdapter() {
			@Override
			public void mouseClicked(MouseEvent arg0) {

				// get the dataset selected
				List<AnalysisDataset> list = new ArrayList<AnalysisDataset>();
				String name = (String) mergeSources.getModel().getValueAt(mergeSources.getSelectedRow(), 0);

				// get the dataset with the selected name
				for( UUID id : activeDataset().getMergeSources()){
					AnalysisDataset mergeSource = activeDataset().getMergeSource(id);
					if(mergeSource.getName().equals(name)){
						list.add(mergeSource);
					}
				}
				fireDatasetEvent(DatasetMethod.EXTRACT_SOURCE, list);

			}
		});
		getSourceButton.setVisible(false);
		panel.add(getSourceButton);
		return panel;
		
	}
	
	/**
	 * This method must be overridden by the extending class
	 * to perform the actual update when a single dataset is selected
	 */
	protected void updateSingle() throws Exception {

		TableOptions options = new TableOptionsBuilder()
		.setDatasets(getDatasets())
		.setLogger(programLogger)
		.setType(TableType.MERGE_SOURCES)
		.build();

		TableModel model = getTable(options);

		mergeSources.setModel(model);

		getSourceButton.setVisible(true);
		
		
		TableOptions parameterOptions = new TableOptionsBuilder()
		.setDatasets(activeDataset().getAllMergeSources())
		.setLogger(programLogger)
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
		getSourceButton.setVisible(false);
		
		TableOptions options = new TableOptionsBuilder()
		.setDatasets(null)
		.setType(TableType.MERGE_SOURCES)
		.setLogger(programLogger)
		.build();
		
		TableModel model = getTable(options);
		
		mergeSources.setModel(model);
		
		TableOptions parameterOptions = new TableOptionsBuilder()
		.setDatasets(null)
		.setLogger(programLogger)
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
	
	
//	private DefaultTableModel makeBlankTable(){
//		DefaultTableModel model = new DefaultTableModel();
//
//		Vector<Object> names 	= new Vector<Object>();
//		Vector<Object> nuclei 	= new Vector<Object>();
//
//		names.add("No merge sources");
//		nuclei.add("");
//
//
//		model.addColumn("Merge source", names);
//		model.addColumn("Nuclei", nuclei);
//		return model;
//	}
}
