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
	private JButton		getSourceButton = new JButton("Recover source");
	
	public MergesDetailPanel(Logger programLogger){
		super(programLogger);
		this.setLayout(new BorderLayout());
		
		
		try {
			TableOptions options = new TableOptionsBuilder()
			.setDatasets(null)
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

			this.add(mergeSources, BorderLayout.CENTER);
			this.add(mergeSources.getTableHeader(), BorderLayout.NORTH);

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
					//				fireSignalChangeEvent("ExtractSource_"+name);

				}
			});
			getSourceButton.setVisible(false);
			this.add(getSourceButton, BorderLayout.SOUTH);
		} catch (Exception e){
			programLogger.log(Level.SEVERE, "Error creating merge panel", e);
		}
	}
	
	/**
	 * This method must be overridden by the extending class
	 * to perform the actual update when a single dataset is selected
	 */
	protected void updateSingle() throws Exception {

		TableOptions options = new TableOptionsBuilder()
		.setDatasets(getDatasets())
		.setLogger(programLogger)
		.build();

		TableModel model = getTable(options);

		mergeSources.setModel(model);

		getSourceButton.setVisible(true);
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
		.setLogger(programLogger)
		.build();
		
		TableModel model = getTable(options);
		
		mergeSources.setModel(model);
	}
	
	@Override
	protected JFreeChart createPanelChartType(ChartOptions options) throws Exception {
		return null;
	}
	
	@Override
	protected TableModel createPanelTableType(TableOptions options) throws Exception{
		return NucleusTableDatasetCreator.createMergeSourcesTable(options);
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
